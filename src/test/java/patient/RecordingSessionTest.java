package patient;

import bitalino.BITalinoErrorTypes;
import bitalino.BITalinoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecordingSessionTest {
    private static final int BLOCKS_BEFORE_CUT=5;
    private static final int BLOCKS_SIZE= BitalinoRecorder.SAMPLING_RATE/10;

    @Test
    void bitalinoDisconnectsDuringRecording() {
        DisconnectingRecorder recorder = new DisconnectingRecorder();
        ResultListener listener = new ResultListener();
        RecordingSession session = new RecordingSession(recorder, 30, listener);
        session.run();

        assertTrue(listener.finished,"onFinished must be called");
        assertInstanceOf(BITalinoException.class,listener.error);
        assertEquals(BITalinoErrorTypes.LOST_COMMUNICATION.getValue(),((BITalinoException)listener.error).code);
        assertEquals(BLOCKS_BEFORE_CUT*BLOCKS_SIZE,listener.totalSamples);
        assertEquals(BLOCKS_BEFORE_CUT*BLOCKS_SIZE,listener.receivedSamples);
        assertTrue(recorder.stopped,"The BITalino must be stopped after the cut");
        assertFalse(session.isRunning());
    }

    private static class DisconnectingRecorder extends BitalinoRecorder{
        private int blocks=0;
        private boolean stopped=false;

        @Override
        public int [][] readBlock(int blockSize)throws BITalinoException,InterruptedException{
            if(blocks== BLOCKS_BEFORE_CUT){
                throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);

            }
            blocks++;
            return super.readBlock(blockSize);
        }

        @Override
        public void stop()throws BITalinoException{
            stopped=true;
            super.stop();
        }
    }

    private static class ResultListener implements RecordingSession.Listener{
        private int receivedSamples=0;
        private int totalSamples=-1;
        private Exception error;
        private boolean finished=false;

        @Override
        public void onBlock(int[][]samples){
            receivedSamples= receivedSamples+samples[BitalinoRecorder.ECG_ROW].length;

        }

        @Override
        public void onFinished(int totalSamples,Exception error){
            this.totalSamples=totalSamples;
            this.error=error;
            this.finished=true;
        }
    }
}
