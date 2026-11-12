package gui;

import POJOs.User;
import commonInfo.SymptomReport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import patient.PatientClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Menú del paciente. Se abre desde GUI al hacer login.
 */
public class PatientGUI {

    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.ENGLISH);
    private static final Color DOCTOR_GREEN = Color.web("#1E8E3E");
    private static final Color DOCTOR_GREEN_BG = Color.web("#E6F4EA");
    private static final Color SYMPTOM_BG = Color.web("#FCE8EB");
    private PingThread pingThread;

    private final User user;
    private final GUI.App mainFrame;
    private final PatientClient patientClient;

    private Label message;

    // Informes enviados en esta sesión (el servidor todavía no los guarda)
    private final List<SymptomReport> reports = new ArrayList<>();

    public PatientGUI(User user, GUI.App mainFrame, PatientClient patientClient) {
        this.user = user;
        this.mainFrame = mainFrame;
        this.patientClient = patientClient;
        showPatientMenu();
        pingThread = new PingThread(patientClient, mainFrame);
        Thread thread = new Thread(pingThread);
        thread.setDaemon(true); //para que no impida cerrar la app
        thread.start();
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

        symptomsButton.setOnAction(e -> logSymptoms());
        VBox symptomsCard = new VBox(10, symptomsTitle, symptomsText, space, symptomsButton);
        GUI.card(symptomsCard);


        ecgCard.setPrefWidth(500);
        symptomsCard.setPrefWidth(500);
        HBox.setHgrow(ecgCard, Priority.ALWAYS);
        HBox.setHgrow(symptomsCard, Priority.ALWAYS);
        HBox cards = new HBox(16, ecgCard, symptomsCard);

        Label reportsTitle = new Label("My health reports");
        GUI.title(reportsTitle, 20);
        Label reportsText = new Label(reports.isEmpty()
                ? "Your symptom reports and your doctor's comments will appear here."
                : reports.size() + (reports.size() == 1 ? " report" : " reports") + " so far.");
        GUI.muted(reportsText);
        reportsText.setWrapText(true);
        VBox reportsInfo = new VBox(4, reportsTitle, reportsText);
        Region reportsSpacer = new Region();
        HBox.setHgrow(reportsSpacer, Priority.ALWAYS);
        Button reportsButton = new Button("View reports");
        GUI.secondaryButton(reportsButton);
        reportsButton.setPrefHeight(46);
        reportsButton.setPadding(new Insets(0, 20, 0, 20));
        reportsButton.setOnAction(e -> showReports());
        HBox reportsCard = new HBox(16, reportsInfo, reportsSpacer, reportsButton);
        reportsCard.setAlignment(Pos.CENTER_LEFT);
        GUI.card(reportsCard);

        VBox page = new VBox(24, header, message, cards, reportsCard);
        page.setPadding(new Insets(32, 40, 32, 40));
        GUI.background(page, GUI.FONDO, 0);
        mainFrame.setScreen(page);
    }
    /** Formulario de síntomas */
    private void logSymptoms() {
        Hyperlink backLink = new Hyperlink("← Back to menu");
        GUI.link(backLink);
        backLink.setOnAction(e -> showPatientMenu());
        Label formTitle = new Label("How do you feel today?");
        GUI.title(formTitle, 28);
        Label formSubtitle = new Label("Your answers help your doctor follow your heart health. It only takes a minute.");
        GUI.subtitle(formSubtitle);
        formSubtitle.setWrapText(true);
        VBox titleBox = new VBox(4, formTitle, formSubtitle);
        ToggleGroup physicalGroup = new ToggleGroup();
        FlowPane physicalPills = pills(physicalGroup, "Excellent", "Good", "Fair", "Poor", "Terrible");
        VBox physicalSection = section("How do you feel physically today?", null, physicalPills);
        ToggleGroup moodGroup = new ToggleGroup();
        FlowPane moodPills = pills(moodGroup, "Calm / Relaxed", "Mildly Stressed", "Anxious", "Sad / Depressed", "Highly Stressed");
        VBox moodSection = section("How is your mood or stress level?", null, moodPills);

        ToggleButton chestPain = pill("Chest pain / discomfort", null);
        ToggleButton shortnessOfBreath = pill("Shortness of breath", null);
        ToggleButton dizziness = pill("Dizziness / lightheadedness", null);
        ToggleButton palpitations = pill("Palpitations (racing heart)", null);
        ToggleButton fatigue = pill("Severe fatigue", null);
        FlowPane symptomPills = new FlowPane(8, 8, chestPain, shortnessOfBreath, dizziness, palpitations, fatigue);
        VBox symptomsSection = section("Have you experienced any of these symptoms?", "Select all that apply.", symptomPills);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Any extra context, triggers or observations…");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.setFont(Font.font("System", 14));
        notesArea.setStyle("-fx-control-inner-background: #E4E4FB; -fx-background-color: #E4E4FB;"
                + " -fx-background-radius: 10; -fx-background-insets: 0; -fx-focus-color: #4A44B5; -fx-faint-focus-color: transparent;");
        VBox notesSection = section("Additional notes", "Optional.", notesArea);

        Label formError = new Label();
        GUI.fieldError(formError);
        formError.setVisible(false);
        formError.setManaged(false);

        Button clearButton = new Button("Clear");
        GUI.secondaryButton(clearButton);
        clearButton.setPrefHeight(46);
        clearButton.setPadding(new Insets(0, 20, 0, 20));
        clearButton.setOnAction(e -> {
            physicalGroup.selectToggle(null);
            moodGroup.selectToggle(null);
            chestPain.setSelected(false);
            shortnessOfBreath.setSelected(false);
            dizziness.setSelected(false);
            palpitations.setSelected(false);
            fatigue.setSelected(false);
            notesArea.clear();
            formError.setVisible(false);
            formError.setManaged(false);
        });

        Button submitButton = new Button("Submit");
        GUI.primaryButton(submitButton);
        submitButton.setPrefWidth(180);
        submitButton.setMaxWidth(Region.USE_PREF_SIZE);
        submitButton.setOnAction(e -> {
            // El estado físico y el ánimo son obligatorios
            if (physicalGroup.getSelectedToggle() == null || moodGroup.getSelectedToggle() == null) {
                formError.setText("Please tell us how you feel physically and your mood level.");
                formError.setVisible(true);
                formError.setManaged(true);
                return;
            }
            String physical = ((ToggleButton) physicalGroup.getSelectedToggle()).getText();
            String mood = ((ToggleButton) moodGroup.getSelectedToggle()).getText();

            // El estado físico y el ánimo no tienen campo propio en SymptomReport: van en los comentarios
            StringBuilder comments = new StringBuilder();
            comments.append("Physical: ").append(physical).append(" | Mood: ").append(mood);
            String notes = notesArea.getText().trim();
            if (!notes.isEmpty()) {
                comments.append(" | Notes: ").append(notes);
            }

            SymptomReport report = new SymptomReport(
                    user.getId(),
                    LocalDateTime.now(),
                    palpitations.isSelected(),
                    dizziness.isSelected(),
                    shortnessOfBreath.isSelected(),
                    chestPain.isSelected(),
                    fatigue.isSelected(),
                    0,
                    comments.toString()
            );

            // TODO: enviar el informe al servidor (Commands.SEND_SYMPTOMS) cuando PatientClient lo soporte
            reports.add(report);
            showPatientMenu();
            showMessage(String.format("Symptoms recorded at %s (offline preview).",
                    report.getDateTime().toLocalTime().withNano(0)));
        });

        Region buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(12, clearButton, buttonSpacer, submitButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(24, physicalSection, moodSection, symptomsSection, notesSection, formError, buttonBar);
        GUI.card(card);
        card.setPadding(new Insets(28, 32, 28, 32));

        VBox content = new VBox(20, backLink, titleBox, card);
        content.setMaxWidth(720);
        content.setPadding(new Insets(28, 40, 32, 40));

        // El contenido va centrado; si la ventana es baja, se puede hacer scroll
        StackPane centered = new StackPane(content);
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        GUI.background(centered, GUI.FONDO, 0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setBorder(null);
        GUI.background(scroll, GUI.FONDO, 0);
        // el fondo llega hasta abajo aunque haya poco contenido
        scroll.viewportBoundsProperty().addListener((obs, was, now) -> centered.setMinHeight(now.getHeight()));
        mainFrame.setScreen(scroll);
    }

    /** Historial de informes del paciente, del más reciente al más antiguo. */
    private void showReports() {

        Hyperlink backLink = new Hyperlink("← Back to menu");
        GUI.link(backLink);
        backLink.setOnAction(e -> showPatientMenu());

        Label pageTitle = new Label("My Health Reports");
        GUI.title(pageTitle, 28);
        Label pageSubtitle = new Label("Everything you have reported, and what your doctor said about it.");
        GUI.subtitle(pageSubtitle);
        pageSubtitle.setWrapText(true);
        VBox titleBox = new VBox(4, pageTitle, pageSubtitle);

        VBox list = new VBox(16);
        if (reports.isEmpty()) {
            Label emptyTitle = new Label("You have no health reports yet.");
            GUI.text(emptyTitle, 16, true, GUI.AZUL_NOCHE);
            Label emptyText = new Label("When you log your symptoms, they will show up here.");
            GUI.muted(emptyText);
            Button logButton = new Button("Log symptoms");
            GUI.primaryButton(logButton);
            logButton.setPrefWidth(180);
            logButton.setMaxWidth(Region.USE_PREF_SIZE);
            logButton.setOnAction(e -> logSymptoms());
            VBox empty = new VBox(8, emptyTitle, emptyText, logButton);
            VBox.setMargin(logButton, new Insets(8, 0, 0, 0));
            empty.setAlignment(Pos.CENTER);
            GUI.card(empty);
            empty.setPadding(new Insets(40));
            list.getChildren().add(empty);
        } else {
            for (int i = reports.size() - 1; i >= 0; i--) {
                list.getChildren().add(reportCard(reports.get(i)));
            }
        }

        VBox content = new VBox(20, backLink, titleBox, list);
        content.setMaxWidth(720);
        content.setPadding(new Insets(28, 40, 32, 40));

        StackPane centered = new StackPane(content);
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        GUI.background(centered, GUI.FONDO, 0);

        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setBorder(null);
        GUI.background(scroll, GUI.FONDO, 0);
        // el fondo llega hasta abajo aunque haya poco contenido
        scroll.viewportBoundsProperty().addListener((obs, was, now) -> centered.setMinHeight(now.getHeight()));
        mainFrame.setScreen(scroll);
    }

    /** Tarjeta de un informe: fecha, quién lo ha revisado, estado, síntomas, notas y comentarios del médico. */
    private VBox reportCard(SymptomReport report) {
        String doctorText = report.getDoctorComments();
        boolean reviewed = doctorText != null && !doctorText.isBlank();

        // Fecha + etiqueta
        Label date = new Label(report.getDateTime().format(REPORT_DATE));
        GUI.text(date, 15, true, GUI.AZUL_NOCHE);
        Label badge = new Label(reviewed ? "Reviewed by doctor" : "Self-reported");
        GUI.text(badge, 12, true, reviewed ? DOCTOR_GREEN : GUI.INDIGO);
        GUI.background(badge, reviewed ? DOCTOR_GREEN_BG : GUI.LAVANDA, 20);
        badge.setPadding(new Insets(5, 12, 5, 12));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, date, spacer, badge);
        top.setAlignment(Pos.CENTER_LEFT);

        // Estado físico y ánimo
        HBox state = new HBox(32,
                infoPair("Physical", commentPart(report.getComments(), "Physical")),
                infoPair("Mood", commentPart(report.getComments(), "Mood")));

        // Síntomas
        Label symptomsLabel = new Label("Symptoms");
        GUI.muted(symptomsLabel);
        FlowPane symptomChips = new FlowPane(8, 8);
        addChip(symptomChips, report.isChestPain(), "Chest pain");
        addChip(symptomChips, report.isShortnessOfBreath(), "Shortness of breath");
        addChip(symptomChips, report.isDizziness(), "Dizziness");
        addChip(symptomChips, report.isPalpitations(), "Palpitations");
        addChip(symptomChips, report.isFatigue(), "Severe fatigue");
        if (symptomChips.getChildren().isEmpty()) {
            Label none = new Label("No symptoms reported");
            GUI.text(none, 14, false, GUI.AZUL_NOCHE);
            symptomChips.getChildren().add(none);
        }
        VBox symptoms = new VBox(6, symptomsLabel, symptomChips);

        VBox card = new VBox(16, top, state, symptoms);

        // Notas del paciente
        String notes = commentPart(report.getComments(), "Notes");
        if (notes != null) {
            Label notesLabel = new Label("Your notes");
            GUI.muted(notesLabel);
            Label notesText = new Label(notes);
            GUI.text(notesText, 14, false, GUI.AZUL_NOCHE);
            notesText.setWrapText(true);
            card.getChildren().add(new VBox(4, notesLabel, notesText));
        }

        // Comentarios del médico
        if (reviewed) {
            Label doctorLabel = new Label("Doctor's comments");
            GUI.text(doctorLabel, 12, true, DOCTOR_GREEN);
            Label doctorComment = new Label(doctorText);
            GUI.text(doctorComment, 14, false, GUI.AZUL_NOCHE);
            doctorComment.setWrapText(true);
            VBox doctorBox = new VBox(4, doctorLabel, doctorComment);
            GUI.background(doctorBox, DOCTOR_GREEN_BG, 12);
            doctorBox.setPadding(new Insets(12, 14, 12, 14));
            card.getChildren().add(doctorBox);
        }

        GUI.card(card);
        card.setPadding(new Insets(22, 26, 22, 26));
        return card;
    }

    /** Dato pequeño con su nombre encima, p. ej. "Mood / Anxious". */
    private static VBox infoPair(String name, String value) {
        Label nameLabel = new Label(name);
        GUI.muted(nameLabel);
        Label valueLabel = new Label(value == null ? "—" : value);
        GUI.text(valueLabel, 14, true, GUI.AZUL_NOCHE);
        return new VBox(2, nameLabel, valueLabel);
    }

    private static void addChip(FlowPane chips, boolean present, String text) {
        if (present) {
            Label chip = new Label(text);
            GUI.text(chip, 12, true, GUI.CORAL_TEXTO);
            GUI.background(chip, SYMPTOM_BG, 20);
            chip.setPadding(new Insets(5, 12, 5, 12));
            chips.getChildren().add(chip);
        }
    }

    /** Saca una parte de los comentarios ("Physical: Good | Mood: Anxious | Notes: ..."). */
    private static String commentPart(String comments, String key) {
        if (comments == null) {
            return null;
        }
        for (String part : comments.split(" \\| ")) {
            if (part.startsWith(key + ": ")) {
                return part.substring(key.length() + 2);
            }
        }
        return null;
    }

    /** Pregunta del formulario: título, ayuda opcional y el control. */
    private static VBox section(String question, String hint, Node control) {
        Label questionLabel = new Label(question);
        GUI.text(questionLabel, 15, true, GUI.AZUL_NOCHE);
        questionLabel.setWrapText(true);
        VBox box = new VBox(4, questionLabel);
        if (hint != null) {
            Label hintLabel = new Label(hint);
            GUI.muted(hintLabel);
            box.getChildren().add(hintLabel);
        }
        VBox.setMargin(control, new Insets(8, 0, 0, 0));
        box.getChildren().add(control);
        return box;
    }

    /** Fila de pastillas en la que solo se puede elegir una. */
    private static FlowPane pills(ToggleGroup group, String... options) {
        FlowPane row = new FlowPane(8, 8);
        for (String option : options) {
            row.getChildren().add(pill(option, group));
        }
        return row;
    }

    /** Pastilla seleccionable: lavanda normal, índigo cuando está marcada. */
    private static ToggleButton pill(String text, ToggleGroup group) {
        ToggleButton pill = new ToggleButton(text);
        if (group != null) {
            pill.setToggleGroup(group);
        }
        pill.setPadding(new Insets(9, 16, 9, 16));
        pill.setCursor(Cursor.HAND);
        pill.setBorder(null);
        Runnable paint = () -> {
            if (pill.isSelected()) {
                GUI.text(pill, 13, true, Color.WHITE);
                GUI.background(pill, pill.isHover() ? GUI.INDIGO_DARK : GUI.INDIGO, 20);
            } else {
                GUI.text(pill, 13, false, GUI.AZUL_NOCHE);
                GUI.background(pill, pill.isHover() ? GUI.LILA : GUI.LAVANDA, 20);
            }
        };
        pill.selectedProperty().addListener((obs, was, now) -> paint.run());
        pill.hoverProperty().addListener((obs, was, now) -> paint.run());
        paint.run();
        return pill;
    }

    /** lougout */
    private void logout() {
        try {
            pingThread.stop();
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
