package gui;

import commonInfo.Patient;
import commonInfo.PatientHistory;
import commonInfo.Recording;
import commonInfo.User;
import commonInfo.SymptomReport;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import patient.BitalinoRecorder;
import patient.PatientClient;
import patient.RecordingSession;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Menú del paciente. Se abre desde GUI al hacer login.
 */
public class PatientGUI {
    static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.ENGLISH);
    static final Color DOCTOR_GREEN = Color.web("#1E8E3E");
    static final Color DOCTOR_GREEN_BG = Color.web("#E6F4EA");
    private static final Color SYMPTOM_BG = Color.web("#FCE8EB");
    private PingThread pingThread;

    private final User user;
    private final GUI.App mainFrame;
    private final PatientClient patientClient;
    private RecordingSession session;

    private Label message;


    private Patient patient;
    private final List<SymptomReport> reports = new ArrayList<>();
    private final List<Recording> recordings = new ArrayList<>();

    public PatientGUI(User user, GUI.App mainFrame, PatientClient patientClient) {
        this.user = user;
        this.mainFrame = mainFrame;
        this.patientClient = patientClient;
        String loadError = loadHistory();

        if (!recordings.isEmpty() && !hasReport(recordings.get(0).getId())) {
            logSymptoms(recordings.get(0).getId());
        } else {
            showPatientMenu();
            if (loadError != null) {
                showMessage(loadError);}}
        pingThread = new PingThread(patientClient, mainFrame);
        Thread thread = new Thread(pingThread);
        thread.setDaemon(true); //para que no impida cerrar la app
        thread.start();
    }

    /**
     * GET_PATIENT_HISTORY
     */
    private String loadHistory() {
        try {
            PatientHistory history = patientClient.getMyhistory();
            patient = history.getPatient();
            reports.clear();
            if (history.getReports() != null) {
                reports.addAll(history.getReports());
            }
            reports.sort(Comparator.comparing(SymptomReport::getDateTime,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            recordings.clear();
            if (history.getRecordings() != null) {
                recordings.addAll(history.getRecordings());
            }
            recordings.sort(Comparator.comparing(Recording::getStartTime,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return null;
        } catch (PatientClient.ServerException e) {
            return "Could not load your reports: " + e.getMessage();
        } catch (IOException e) {
            return "Can't reach the hospital server ";
        }
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


        recordButton.setOnAction(e -> {
            try {
                bitalinoReacording();
            } catch (PatientClient.ServerException | IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        VBox ecgCard = new VBox(10, ecgTitle, ecgText, recordButton);
        GUI.card(ecgCard);

        HBox.setHgrow(ecgCard, Priority.ALWAYS);
        HBox cards = new HBox(16, ecgCard);

        Label reportsTitle = new Label("My health reports");
        GUI.title(reportsTitle, 20);
        Label reportsText = new Label(reports.isEmpty()
                ? "Your symptom reports and your doctor's comments will appear here."
                : reports.size() + (reports.size() == 1 ? " report" : " reports"));
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
    //falata todo lo del EMG
    private void bitalinoReacording() throws PatientClient.ServerException, IOException {
            Hyperlink backLink = new Hyperlink("< Back to menu");
            GUI.link(backLink);
            backLink.setOnAction(e -> {
                stopRecording();
                showPatientMenu();
            });

            Label title = new Label("Record your ECG");
            GUI.title(title, 28);
            TextField macField = new TextField();
            macField.setPromptText("BITalino MAC (empty = test mode)");
            GUI.input(macField);
            Label status = new Label("Press Start and stay still");

            Button startButton = new Button("Start");
            GUI.primaryButton(startButton);
            Button stopButton = new Button("Stop");
            GUI.secondaryButton(stopButton);

            startButton.setOnAction(e -> {
                String mac = macField.getText().trim();
                BitalinoRecorder recorder;
                if (mac.isEmpty()) {
                    recorder = new BitalinoRecorder();      //  prueba
                } else {
                    recorder = new BitalinoRecorder(mac);   // BITalino
                }

                try {
                    session = patientClient.startECGRecording(recorder, 30, new RecordingSession.Listener() {
                        @Override
                        public void onBlock(int[][] samples) {
                            Platform.runLater(() ->
                                    status.setText("Recording... " + (int) session.getRecorderSeconds() + " s"));
                        }

                        @Override
                        public void onFinished(int totalSamples, Exception error) {
                            Platform.runLater(() -> {
                                if (totalSamples > 0) {

                                    recordingFinished();
                                } else if (error == null) {
                                    status.setText("Recording finished and sent to your doctor.");
                                } else {
                                    status.setText("Error: " + error.getMessage());
                                }
                            });
                        }
                    });
                } catch (Exception ex) {
                    status.setText("Could not start: " + ex.getMessage());
                }
            });

            stopButton.setOnAction(e -> stopRecording());

            VBox card = new VBox(16, macField, status, new HBox(12, startButton, stopButton));
            GUI.card(card);

            VBox page = new VBox(20, backLink, title, card);
            page.setPadding(new Insets(32, 40, 32, 40));
            GUI.background(page, GUI.FONDO, 0);
            mainFrame.setScreen(page);

    }
    private void stopRecording() {
        if (session != null && session.isRunning()) {
            session.stop();
        }
    }
    /**
     * Se llama cuando termina una grabación de ECG
     * Abre el informe de síntomas
     */
    private void recordingFinished() {
        String loadError = loadHistory();
        if (recordings.isEmpty()) {
            showPatientMenu();
            showMessage(loadError != null ? loadError : "Recording saved.");
            return;
        }
        Recording newRecording = recordings.get(0);
        logSymptoms(newRecording.getId());
    }

    /** Dice si ya hay un informe de síntomas de esa grabación. */
    private boolean hasReport(int recordingId) {
        for (SymptomReport report : reports) {
            if (report.getSessionId() != null && report.getSessionId() == recordingId) {
                return true;
            }
        }
        return false;
    }

    /** Formulario de síntomas de una sesión  */
    private void logSymptoms(Integer sessionId) {
        Label formTitle = new Label("How did you feel during the recording?");
        GUI.title(formTitle, 28);
        formTitle.setWrapText(true);
        Label formSubtitle = new Label("Your recording is saved. Answer this form to finish; it is required.");
        GUI.subtitle(formSubtitle);
        formSubtitle.setWrapText(true);
        VBox titleBox = new VBox(12, new VBox(4, formTitle, formSubtitle), sessionBox(sessionId, recordings, null));
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
        notesArea.setPromptText("Any observations");
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
            report.setSessionId(sessionId);


            try {
                patientClient.sendSymptoms(report);
            } catch (PatientClient.ServerException ex) {
                formError.setText("The server could not save your symptoms: " + ex.getMessage());
                formError.setVisible(true);
                formError.setManaged(true);
                return;
            } catch (IOException ex) {
                formError.setText("Can't reach the hospital server.");
                formError.setVisible(true);
                formError.setManaged(true);
                return;
            }


            if (loadHistory() != null) {
                reports.addFirst(report);
            }
            showPatientMenu();
            showMessage(String.format("Symptoms sent at %s.",
                    report.getDateTime().toLocalTime().withNano(0)));
        });

        Region buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(12, clearButton, buttonSpacer, submitButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(24, physicalSection, moodSection, symptomsSection, notesSection, formError, buttonBar);
        GUI.card(card);
        card.setPadding(new Insets(28, 32, 28, 32));

        VBox content = new VBox(20, titleBox, card);
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


        scroll.viewportBoundsProperty().addListener((obs, was, now) -> centered.setMinHeight(now.getHeight()));
        mainFrame.setScreen(scroll);
    }

    /** Historial de informes del paciente */
    private void showReports() {

        Hyperlink backLink = new Hyperlink("← Back to menu");
        GUI.link(backLink);
        backLink.setOnAction(e -> showPatientMenu());

        Label pageTitle = new Label("My Health Reports");
        GUI.title(pageTitle, 28);
        VBox titleBox = new VBox(4, pageTitle);

        String loadError = loadHistory();

        VBox list = new VBox(16);
        if (loadError != null) {
            Label errorLabel = new Label(loadError);
            GUI.fieldError(errorLabel);
            errorLabel.setWrapText(true);
            list.getChildren().add(errorLabel);
        }
        if (reports.isEmpty()) {
            Label emptyTitle = new Label("You have no health reports yet.");
            GUI.text(emptyTitle, 16, true, GUI.AZUL_NOCHE);
            Label emptyText = new Label("When you log your symptoms, they will show up here.");
            GUI.muted(emptyText);
            VBox empty = new VBox(8, emptyTitle, emptyText);
            empty.setAlignment(Pos.CENTER);
            GUI.card(empty);
            empty.setPadding(new Insets(40));
            list.getChildren().add(empty);
        } else {
            for (SymptomReport report : reports) {   // ya vienen del más reciente al más antiguo
                list.getChildren().add(reportCard(report));
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
        scroll.viewportBoundsProperty().addListener((obs, was, now) -> centered.setMinHeight(now.getHeight()));
        mainFrame.setScreen(scroll);
    }

    /**  fecha, quién lo ha revisado */
    private VBox reportCard(SymptomReport report) {
        String doctorText = report.getDoctorComment();
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


        HBox state = new HBox(32,
                infoPair("Physical", commentPart(report.getComments(), "Physical")),
                infoPair("Mood", commentPart(report.getComments(), "Mood")));


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

        VBox card = new VBox(16, top, state, symptoms, sessionBox(report.getSessionId(), recordings, null));

        String notes = commentPart(report.getComments(), "Notes");
        if (notes != null) {
            Label notesLabel = new Label("Your notes");
            GUI.muted(notesLabel);
            Label notesText = new Label(notes);
            GUI.text(notesText, 14, false, GUI.AZUL_NOCHE);
            notesText.setWrapText(true);
            card.getChildren().add(new VBox(4, notesLabel, notesText));
        }


        if (reviewed) {
            String when = report.getDoctorCommentTime() == null ? ""
                    : " · " + report.getDoctorCommentTime().format(REPORT_DATE);
            Label doctorLabel = new Label("Doctor's comments" + when);
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



    /**
     * Caja con la grabación de ECG a la que pertenece un informe.
     */
    static VBox sessionBox(Integer sessionId, List<Recording> recordings, Node action) {
        Recording recording = findRecording(recordings, sessionId);
        String date = null;
        if (recording != null) {
            date = recording.getStartTime().format(REPORT_DATE);}

        HBox data = new HBox(28,
                infoPair("Recording", recordingNumber(recordings, sessionId)),
                infoPair("Recorded", date),
                infoPair("Episode duration", recordingLength(recording)));
        data.setAlignment(Pos.CENTER_LEFT);
        if (action != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            data.getChildren().addAll(spacer, action);}
        Label title = new Label("Linked ECG recording");
        GUI.text(title, 12, true, GUI.INDIGO);
        VBox box = new VBox(8, title, data);
        GUI.background(box, GUI.LAVANDA, 12);
        box.setPadding(new Insets(12, 14, 12, 14));
        return box;}

    /**
     * Número de una  grabacion
     */
    static String recordingNumber(List<Recording> recordings, Integer id) {
        Recording target = findRecording(recordings, id);
        if (target == null || target.getStartTime() == null) {
            return null;
        }
        int number = 1;
        for (Recording recording : recordings) {
            if (recording.getStartTime() != null && recording.getStartTime().isBefore(target.getStartTime())) {
                number++;}}return "#" + number;}

    /** Busca la grabacion con ese id */
    static Recording findRecording(List<Recording> recordings, Integer id) {
        if (id == null) {
            return null;
        }
        for (Recording recording : recordings) {
            if (id.equals(recording.getId())) {
                return recording;
            }
        }
        return null;
    }

    /**duracion una grabacion */
    static String recordingLength(Recording recording) {
        if (recording == null || recording.getStartTime() == null || recording.getEndTime() == null) {
            return null;}
        long seconds = Duration.between(recording.getStartTime(), recording.getEndTime()).getSeconds();
        if (seconds < 60) {
            return seconds + " s";}return (seconds / 60) + " min " + (seconds % 60) + " s";
    }

    static VBox infoPair(String name, String value) {
        Label nameLabel = new Label(name);
        GUI.muted(nameLabel);
        Label valueLabel = new Label(value == null ? "—" : value);
        GUI.text(valueLabel, 14, true, GUI.AZUL_NOCHE);
        return new VBox(2, nameLabel, valueLabel);
    }

    static void addChip(FlowPane chips, boolean present, String text) {
        if (present) {
            Label chip = new Label(text);
            GUI.text(chip, 12, true, GUI.CORAL_TEXTO);
            GUI.background(chip, SYMPTOM_BG, 20);
            chip.setPadding(new Insets(5, 12, 5, 12));
            chips.getChildren().add(chip);
        }
    }

    /** Saca una parte de los comentarios */
    static String commentPart(String comments, String key) {
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

    /** Pregunta del formulario: */
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

    /** Cierra la sesión en el servidor y vuelve al login. */
    private void logout() {
        pingThread.stop();
        boolean serverNotified = true;
        try {
            patientClient.logout();
        } catch (IOException e) {
            System.out.println("Could not notify the server about the logout: " + e.getMessage());
            serverNotified = false;
        }
        mainFrame.showLogin();
        if (!serverNotified) {
            mainFrame.showMessage("You have been logged out, but the hospital server could not be reached.");
        }
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
    }

    /** Nombre del paciente*/
    private String displayName() {
        if (patient != null && patient.getName() != null && !patient.getName().isBlank()) {
            if (patient.getSurname() != null && !patient.getSurname().isBlank()) {
                return patient.getName() + " " + patient.getSurname();
            }
            return patient.getName();
        }
        String first = user.getEmail().split("@")[0].split("[._-]")[0];
        if (first.isEmpty()) {
            return user.getEmail();
        }
        return Character.toUpperCase(first.charAt(0)) + first.substring(1);
    }
}
