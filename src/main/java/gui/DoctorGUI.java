package gui;

import POJOs.User;
import doctor.DoctorClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Menú del médico. Se abre desde GUI al hacer login.
 *
 */
public class DoctorGUI {

    private final User user;
    private final GUI.App mainFrame;
    private final DoctorClient doctorClient;

    private Label message;

    public DoctorGUI(User user, GUI.App mainFrame, DoctorClient doctorClient) {
        this.user = user;
        this.mainFrame = mainFrame;
        this.doctorClient = doctorClient;
        showDoctorMenu();
    }

    public void showDoctorMenu() {

        Label greeting = new Label("Welcome,");
        GUI.muted(greeting);
        Label name = new Label(displayName());
        GUI.title(name, 28);
        VBox hello = new VBox(2, greeting, name);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Log out");
        GUI.secondaryButton(logoutButton);
        logoutButton.setOnAction(e -> logout());

        HBox header = new HBox(12, hello, spacer, logoutButton);
        header.setAlignment(Pos.CENTER_LEFT);


        message = new Label();
        GUI.status(message);
        showMessage("Connected to the hospital server.");


        Label cardTitle = new Label("My patients");
        GUI.title(cardTitle, 20);
        Label cardText = new Label("your patients, their symptom reports and their ECG recordings.");
        GUI.muted(cardText);
        cardText.setWrapText(true);

        VBox patientsCard = new VBox(10, cardTitle, cardText);
        GUI.card(patientsCard);

        VBox page = new VBox(24, header, message, patientsCard);
        page.setPadding(new Insets(32, 40, 32, 40));
        GUI.background(page, GUI.FONDO, 0);
        mainFrame.setScreen(page);
    }

    /** logout */
    private void logout() {
        try {
            doctorClient.logout();
        } catch (IOException e) {
        }
        mainFrame.showLogin();
    }
    private void showMessage(String text) {
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
    }

    private String displayName() {
        String first = user.getEmail().split("@")[0].split("[._-]")[0];
        if (first.isEmpty()) {
            return user.getEmail();
        }
        return "Dr. " + Character.toUpperCase(first.charAt(0)) + first.substring(1);
    }
}
