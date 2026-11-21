package server;

import commonInfo.Doctor;
import commonInfo.Role;
import commonInfo.User;
import server.db.interfaces.DoctorManager;
import server.db.interfaces.UserManager;
import server.db.jdbc.JDBCDoctorManager;
import server.db.jdbc.JDBCUserManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdminConsole implements Runnable {

    private static final Path ADMIN_FILE = Paths.get("db","admin.key");
    private HospitalServer server;

    public AdminConsole(HospitalServer server) {
        this.server = server;
    }

    /**
     * La primera vez que arranca el server, pide crear la contraseña del admin y verifica que hace match con su segunda instancia
     * @throws IOException
     */
    public static void setupAdminPassword() throws IOException {
        if(Files.exists(ADMIN_FILE)){
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        boolean creada = false;
        while(!creada) {
            System.out.println("No admin password yet. Create one:");
            String firstConfirmation = reader.readLine();
            System.out.println("Repeat the password:");
            String secondConfirmation = reader.readLine();

            if(firstConfirmation == null || secondConfirmation == null){
                throw new IOException("No password introduced in first or second time");
            } else if(firstConfirmation.length() < 4) {
                System.out.println("The password must have at least 4 characters");
            } else if(!firstConfirmation.equals(secondConfirmation)){
                System.out.println("Passwords dont match");
            } else {
                Files.createDirectories(ADMIN_FILE.getParent());
                Files.write(ADMIN_FILE, PasswordHasher.hash(firstConfirmation));
                System.out.println("Admin password created");
                creada = true;
            }
        }
    }

    /**
     * Pide contraseña de admin y la compara con el hash guardado
     * @return
     * @throws IOException
     */
    public static boolean checkAdminPassword() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Admin password:");
        String password = reader.readLine();
        if(password == null) {
            return false;
        }
        byte[] stored = Files.readAllBytes(ADMIN_FILE);
        return PasswordHasher.verify(password, stored);
    }



    public static boolean doubleCheck(String keyword) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Type " + keyword + ": ");
        String first = reader.readLine();
        if(first == null || !first.trim().equals(keyword)){
            return false;
        }

        System.out.println("Type CONFIRM:");
        String second = reader.readLine();
        if (second == null || !second.trim().equals("CONFIRM")) {
            return false;
        }
        return true;
    }

    public void registerDoctor() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Doctor email (must end with @doctor.com):");
        String email = reader.readLine();
        System.out.println("Doctor password:");
        String password = reader.readLine();
        System.out.println("Name:");
        String name = reader.readLine();
        System.out.println("Surname:");
        String surname = reader.readLine();

        if(email == null || password == null || name == null || surname == null) {
            System.out.println("No input. Doctor not created");
            return;
        }

        if(!email.trim().toLowerCase().endsWith("@doctor.com")){
            System.out.println("Doctor emails must end with @doctor.com. Doctor not created");
            return;
        }

        UserManager userManager = new JDBCUserManager();
        DoctorManager doctorManager = new JDBCDoctorManager();

        User user = new User(email.trim(), password, Role.DOCTOR);
        try{
            userManager.newUser(user);
        } catch (IllegalArgumentException | IllegalStateException e){
            System.out.println("Could not create the account: " + e.getMessage());
            return;
        }

        Doctor doctor = new Doctor(user.getId(), name.trim(), surname.trim());
        try{
            doctorManager.addDoctor(doctor);
        } catch(IllegalArgumentException | IllegalStateException e) {
            userManager.deleteUser(user);
            System.out.println("Could not create the doctor: " + e.getMessage());
            return;
        }
        System.out.println("Doctor " + doctor.getName() + " " + doctor.getSurname() + " created");
    }

    @Override
    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); //no se cierra, cerraria System.in
        boolean active = true;
        try {
            while (active) {
                printMenu();
                String option = reader.readLine();

                if (option == null) {
                    active = false;
                } else {
                    option = option.trim();

                    if (option.equals("1")) {
                        System.out.println("Connected clients: " + server.getConnectedClients());
                    } else if (option.equals("2")) {
                        if(!checkAdminPassword()) {
                            System.out.println("Wrong password");
                        } else {
                            registerDoctor();
                        }
                    } else if(option.equals("3")) {
                        if (!checkAdminPassword()) {
                            System.out.println("Wrong password. Shutdown cancelled");
                        } else {
                            int connected = server.getConnectedClients();
                            if(connected > 0){
                                System.out.println("Warning: there are " +connected + "clientes connected. They will be disconnected");
                            }
                            if (doubleCheck("SHUTDOWN")) {
                                server.shutdown();
                                active = false;
                            } else {
                                System.out.println("Shutdown cancelled");
                            }
                        }
                    } else {
                        System.out.println("Invalid option");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading admin input: " + e.getMessage());
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("ADMIN MENU");
        System.out.println("1. Show connected clients");
        System.out.println("2. Register doctor");
        System.out.println("3. Shut down server");
        System.out.println("Choose an option:");
    }
}

