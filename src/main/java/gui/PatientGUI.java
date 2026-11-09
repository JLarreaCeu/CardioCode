package gui;

import POJOs.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import patient.PatientClient;

import java.io.IOException;

/**
 * Menú del paciente. Se abre desde GUI al hacer login.
 */
public class PatientGUI {

    private final User user;
    private final GUI.App mainFrame;
    private final PatientClient patientClient;

    private Label message;

    public PatientGUI(User user, GUI.App mainFrame, PatientClient patientClient) {
        this.user = user;
        this.mainFrame = mainFrame;
        this.patientClient = patientClient;
        showPatientMenu();
    }

    public void showPatientMenu() {

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



        Label ecgTitle = new Label("Record your ECG");
        GUI.title(ecgTitle, 20);
        Label ecgText = new Label("Connect your BITalino and stay still for 30 seconds.");
        GUI.muted(ecgText);
        ecgText.setWrapText(true);

        Button recordButton = new Button("Record ECG");
        GUI.primaryButton(recordButton);


        recordButton.setOnAction(e -> showMessage("ECG recording is not available yet."));
        VBox ecgCard = new VBox(10, ecgTitle, ecgText, recordButton);
        GUI.card(ecgCard);


        Label symptomsTitle = new Label("How do you feel today?");
        GUI.title(symptomsTitle, 20);
        Label symptomsText = new Label("Palpitations, dizziness, tiredness, chest pain…");
        GUI.muted(symptomsText);
        symptomsText.setWrapText(true);
        Region space = new Region();
        VBox.setVgrow(space, Priority.ALWAYS);
        Button symptomsButton = new Button("Log symptoms");
        GUI.primaryButton(symptomsButton);

        symptomsButton.setOnAction(e -> logsymptoms());
        VBox symptomsCard = new VBox(10, symptomsTitle, symptomsText, space, symptomsButton);
        GUI.card(symptomsCard);


        ecgCard.setPrefWidth(500);
        symptomsCard.setPrefWidth(500);
        HBox.setHgrow(ecgCard, Priority.ALWAYS);
        HBox.setHgrow(symptomsCard, Priority.ALWAYS);
        HBox cards = new HBox(16, ecgCard, symptomsCard);

        VBox page = new VBox(24, header, message, cards);
        page.setPadding(new Insets(32, 40, 32, 40));
        GUI.background(page, GUI.FONDO, 0);
        mainFrame.setScreen(page);
    }
    private void logsymptoms(){



    }
    /** lougout */
    private void logout() {
        try {
            patientClient.logout();
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
        return Character.toUpperCase(first.charAt(0)) + first.substring(1);
    }
}
