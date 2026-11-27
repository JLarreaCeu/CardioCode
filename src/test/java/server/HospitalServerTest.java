package server;

import commonInfo.Doctor;
import commonInfo.Recording;
import commonInfo.Role;
import commonInfo.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.db.jdbc.JDBCDoctorManager;
import server.db.jdbc.JDBCManager;
import server.db.jdbc.JDBCUserManager;

import static org.junit.jupiter.api.Assertions.*;

public class HospitalServerTest {

    private static HospitalServer server;
    private static Integer doctorId;
    private static ClientTest doctor;

    @BeforeAll
    static void setUp() throws Exception {
        new JDBCManager();
        server = new HospitalServer();
        assertTrue(server.open(), "Close the running server before the tests");
        new ServerThread().start();

        String email = "doctor" + System.nanoTime() + "@doctor.com";
        User user = new User(email, "doctorpassword", Role.DOCTOR);
        new JDBCUserManager().newUser(user);
        Doctor newDoctor = new Doctor(user.getId(), "Synthetic", "Doctor");
        new JDBCDoctorManager().addDoctor(newDoctor);
        doctorId = newDoctor.getId();

        doctor = new ClientTest();
        doctor.login(email, "doctorpassword");
    }

    @AfterAll
    static void tearDown() throws Exception {
        doctor.logout();
        server.shutdown();
    }

    @Test
    void twoPatientsRecordAtTheSameTimeWithoutMixingData() throws Exception {
        Recorder one = new Recorder(ClientTest.newPatient("One", doctorId), 100);
        Recorder two = new Recorder(ClientTest.newPatient("Two", doctorId), 900);
        one.start();
        two.start();
        one.join();
        two.join();

        assertNull(one.error);
        assertNull(two.error);
        Recording recordingOne = one.client.history(null).getRecordings().get(0);
        Recording recordingTwo = two.client.history(null).getRecordings().get(0);
        assertNotEquals(recordingOne.getId(), recordingTwo.getId());
        assertNotEquals(recordingOne.getFilePath(), recordingTwo.getFilePath());
        assertAllSamples(doctor.recordingData(recordingOne.getId()), 100, 2000);
        assertAllSamples(doctor.recordingData(recordingTwo.getId()), 900, 2000);

        one.client.logout();
        two.client.logout();
    }

    @Test
    void serverKeepsWorkingWhenAClientIsCutDuringARecording() throws Exception {
        ClientTest other = ClientTest.newPatient("Other", doctorId);
        ClientTest dropped = ClientTest.newPatient("Dropped", doctorId);
        int before = server.getConnectedClients();
        dropped.startRecording();
        dropped.sendBlock(300, 100);
        Recording recording = dropped.history(null).getRecordings().get(0);

        dropped.cut();
        Thread.sleep(1000);

        assertEquals(before - 1, server.getConnectedClients());
        assertTrue(other.ping());
        ClientTest newClient = new ClientTest();
        assertTrue(newClient.ping());
        newClient.logout();
        Recording closed = doctor.history(recording.getPatientId()).getRecordings().get(0);
        assertNotNull(closed.getEndTime());
        assertAllSamples(doctor.recordingData(recording.getId()), 300, 100);

        other.logout();
    }

    private static void assertAllSamples(int[][] samples, int value, int count) {
        assertEquals(2, samples.length);
        assertEquals(count, samples[1].length);
        for (int[] row : samples) {
            for (int sample : row) {
                assertEquals(value, sample);
            }
        }
    }

    private static class ServerThread extends Thread {
        @Override
        public void run() {
            server.start();
        }
    }

    private static class Recorder extends Thread {
        private final ClientTest client;
        private final int value;
        private Exception error;

        Recorder(ClientTest client, int value) {
            this.client = client;
            this.value = value;
        }

        @Override
        public void run() {
            try {
                client.startRecording();
                for (int i = 0; i < 20; i++) {
                    client.sendBlock(value, 100);
                    Thread.sleep(50);
                }
                client.stopRecording();
            } catch (Exception e) {
                error = e;
            }
        }
    }
}
