package gui;

import commonInfo.Doctor;
import commonInfo.Patient;
import commonInfo.PatientHistory;
import commonInfo.Recording;
import commonInfo.SymptomReport;
import commonInfo.User;
import doctor.DoctorClient;
import patient.BitalinoRecorder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Menú del médico
 * Pantallas: menú, lista de pacientes y info
 */
public class DoctorGUI {

    private static final DateTimeFormatter BIRTH_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final User user;
    private final GUI.App mainFrame;
    private final DoctorClient doctorClient;
    private PingThread pingThread;

    private Label message;
    private Doctor doctor;
    private final List<Patient> patients = new ArrayList<>();

    public DoctorGUI(User user, GUI.App mainFrame, DoctorClient doctorClient) {
        this.user = user;
        this.mainFrame = mainFrame;
        this.doctorClient = doctorClient;
        loadDoctor();
        String loadError = loadPatients();
        showDoctorMenu();
        if (loadError != null) {
            showMessage(loadError);
        }
        pingThread = new PingThread(doctorClient, mainFrame);
        Thread t = new Thread(pingThread);
        t.setDaemon(true);//para que no impida el cierre de la app
        t.start();
    }


    /** cuenta de este usuario */
    private void loadDoctor() {
        try {
            for (Doctor d : doctorClient.getDoctors()) {
                if (user.getId() != null && user.getId().equals(d.getUserId())) {
                    doctor = d;
                }}
        } catch (IOException | DoctorClient.ServerException e) {
            System.out.println("Could not load the doctor's name: " + e.getMessage());
        }}

    /**
     * GET_PATIENTS: rellena la lista de pacientes.
     */
    private String loadPatients() {
        try {
            List<Patient> received = doctorClient.getPatients();
            patients.clear();
            patients.addAll(received);
            patients.sort(Comparator.comparing((Patient p) -> safe(p.getSurname()))
                    .thenComparing(p -> safe(p.getName())));
            return null;
        } catch (DoctorClient.ServerException e) {
            return "Could not load your patients: " + e.getMessage();
        } catch (IOException e) {
            return "Can't reach the hospital server .";
        }}
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
        String countText;
        if (patients.isEmpty()) {
            countText = "You have no patients assigned.";
        } else {
            countText = patients.size() + " patients. See their symptom reports and ECG recordings.";
        }
        Label cardText = new Label(countText);
        GUI.muted(cardText);
        cardText.setWrapText(true);
        VBox cardInfo = new VBox(4, cardTitle, cardText);

        Region cardSpacer = new Region();
        HBox.setHgrow(cardSpacer, Priority.ALWAYS);
        Button patientsButton = new Button("View patients");
        GUI.primaryButton(patientsButton);
        patientsButton.setPrefWidth(180);
        patientsButton.setMaxWidth(Region.USE_PREF_SIZE);
        patientsButton.setOnAction(e -> showPatients());

        HBox patientsCard = new HBox(16, cardInfo, cardSpacer, patientsButton);
        patientsCard.setAlignment(Pos.CENTER_LEFT);
        GUI.card(patientsCard);

        VBox page = new VBox(24, header, message, patientsCard);
        page.setPadding(new Insets(32, 40, 32, 40));
        GUI.background(page, GUI.FONDO, 0);
        mainFrame.setScreen(page);
    }


    /** Lista de los pacientes  */
    private void showPatients() {
        Hyperlink backLink = new Hyperlink("← Back to menu");
        GUI.link(backLink);
        backLink.setOnAction(e -> showDoctorMenu());

        Label pageTitle = new Label("My patients");
        GUI.title(pageTitle, 28);
        VBox titleBox = new VBox(4, pageTitle);

        String loadError = loadPatients();

        VBox list = new VBox(12);
        if (loadError != null) {
            list.getChildren().add(errorLabel(loadError));
        }
        if (patients.isEmpty()) {
            Label emptyTitle = new Label("You have no patients yet.");
            GUI.text(emptyTitle, 16, true, GUI.AZUL_NOCHE);
            VBox empty = new VBox(8, emptyTitle);
            empty.setAlignment(Pos.CENTER);
            GUI.card(empty);
            empty.setPadding(new Insets(40));
            list.getChildren().add(empty);
        } else {
            for (Patient patient : patients) {
                list.getChildren().add(patientRow(patient));
            }
        }

        mainFrame.setScreen(scrollPage(new VBox(20, backLink, titleBox, list)));
    }

    /**Fila de un paciente */
    private HBox patientRow(Patient patient) {
        Label nameLabel = new Label(fullName(patient));
        GUI.text(nameLabel, 16, true, GUI.AZUL_NOCHE);

        List<String> details = new ArrayList<>();
        if (patient.getBirthDate() != null) {
            details.add(age(patient.getBirthDate()) + " years");
        }
        if (patient.getSex() != null && !patient.getSex().isBlank()) {
            details.add(patient.getSex());
        }
        if (patient.getPhone() != null && !patient.getPhone().isBlank()) {
            details.add(patient.getPhone());
        }
        Label detailsLabel = new Label(details.isEmpty() ? "No personal details" : String.join(" · ", details));
        GUI.muted(detailsLabel);

        VBox info = new VBox(2, nameLabel, detailsLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openButton = new Button("Open");
        GUI.secondaryButton(openButton);
        openButton.setPrefHeight(40);
        openButton.setPadding(new Insets(0, 20, 0, 20));
        openButton.setOnAction(e -> showPatientHistory(patient.getId(), null));

        HBox row = new HBox(16, info, spacer, openButton);
        row.setAlignment(Pos.CENTER_LEFT);
        GUI.card(row);
        return row;
    }


    /**
     * aciente: GET_PATIENT_HISTORY
     * @param notice mensaje para enseñar
     */
    private void showPatientHistory(Integer patientId, String notice) {
        Hyperlink backLink = new Hyperlink("← Back to my patients");
        GUI.link(backLink);
        backLink.setOnAction(e -> showPatients());

        PatientHistory history;
        try {
            history = doctorClient.getPatientHistory(patientId);
        } catch (DoctorClient.ServerException e) {
            showErrorPage(backLink, "Could not load this patient: " + e.getMessage());
            return;
        } catch (IOException e) {
            showErrorPage(backLink, "Can't reach the hospital server.");
            return;
        }
        Patient patient = history.getPatient();

        Label pageTitle = new Label(fullName(patient));
        GUI.title(pageTitle, 28);
        VBox titleBox = new VBox(4, pageTitle);
        if (notice != null) {
            Label noticeLabel = new Label(notice);
            GUI.status(noticeLabel);
            titleBox.getChildren().add(noticeLabel);
        }

        HBox personal = new HBox(32,
                PatientGUI.infoPair("Date of birth", patient.getBirthDate() == null ? null
                        : patient.getBirthDate().format(BIRTH_DATE) + " (" + age(patient.getBirthDate()) + ")"),
                PatientGUI.infoPair("Sex", blankToNull(patient.getSex())),
                PatientGUI.infoPair("Phone", blankToNull(patient.getPhone())));
        Region personalSpacer = new Region();
        HBox.setHgrow(personalSpacer, Priority.ALWAYS);
        Button editButton = new Button("Edit details");
        GUI.secondaryButton(editButton);
        editButton.setPrefHeight(40);
        editButton.setPadding(new Insets(0, 20, 0, 20));
        editButton.setOnAction(e -> showEditPatient(patient));
        HBox personalCard = new HBox(16, personal, personalSpacer, editButton);
        personalCard.setAlignment(Pos.CENTER_LEFT);
        GUI.card(personalCard);
        List<Recording> recordings = new ArrayList<>();
        if (history.getRecordings() != null) {
            recordings.addAll(history.getRecordings());}
        recordings.sort(Comparator.comparing(Recording::getStartTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<SymptomReport> reports = new ArrayList<>();
        if (history.getReports() != null) {
            reports.addAll(history.getReports());
        }
        reports.sort(Comparator.comparing(SymptomReport::getDateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        VBox reportsBox = new VBox(16, sectionTitle("Symptom reports (" + reports.size() + ")"));
        if (reports.isEmpty()) {
            reportsBox.getChildren().add(emptyCard("This patient has not sent any symptom reports yet."));
        } else {
            for (SymptomReport report : reports) {
                reportsBox.getChildren().add(reportCard(report, patientId, recordings));
            }}


        VBox recordingsBox = new VBox(12, sectionTitle("Recordings (" + recordings.size() + ")"));
        if (recordings.isEmpty()) {
            recordingsBox.getChildren().add(emptyCard("This patient has not made any recordings yet."));
        }
        for (Recording recording : recordings) {
            recordingsBox.getChildren().add(PatientGUI.sessionBox(recording.getId(), recordings,
                    viewEcgButton(recording, patientId)));
        }

        mainFrame.setScreen(scrollPage(new VBox(20, backLink, titleBox, personalCard, reportsBox, recordingsBox)));
    }

    /** Botón que abre la gráfica de una grabación */
    private Button viewEcgButton(Recording recording, Integer patientId) {
        Button button = new Button("View ECG");
        GUI.secondaryButton(button);
        button.setOnAction(e -> showRecording(recording, patientId));
        return button;
    }

    /** Tarjeta de un informe con el comentario  */
    private VBox reportCard(SymptomReport report, Integer patientId, List<Recording> recordings) {
        String doctorText = report.getDoctorComment();
        boolean reviewed = doctorText != null && !doctorText.isBlank();

        Label date = new Label(report.getDateTime() == null ? "—"
                : report.getDateTime().format(PatientGUI.REPORT_DATE));
        GUI.text(date, 15, true, GUI.AZUL_NOCHE);
        Label badge = new Label(reviewed ? "Reviewed" : "Pending review");
        GUI.text(badge, 12, true, reviewed ? PatientGUI.DOCTOR_GREEN : GUI.CORAL_TEXTO);
        GUI.background(badge, reviewed ? PatientGUI.DOCTOR_GREEN_BG : GUI.LAVANDA, 20);
        badge.setPadding(new Insets(5, 12, 5, 12));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, date, spacer, badge);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox state = new HBox(32,
                PatientGUI.infoPair("Physical", PatientGUI.commentPart(report.getComments(), "Physical")),
                PatientGUI.infoPair("Mood", PatientGUI.commentPart(report.getComments(), "Mood")));

        Label symptomsLabel = new Label("Symptoms");
        GUI.muted(symptomsLabel);
        FlowPane chips = new FlowPane(8, 8);
        PatientGUI.addChip(chips, report.isChestPain(), "Chest pain");
        PatientGUI.addChip(chips, report.isShortnessOfBreath(), "Shortness of breath");
        PatientGUI.addChip(chips, report.isDizziness(), "Dizziness");
        PatientGUI.addChip(chips, report.isPalpitations(), "Palpitations");
        PatientGUI.addChip(chips, report.isFatigue(), "Severe fatigue");
        if (chips.getChildren().isEmpty()) {
            Label none = new Label("No symptoms reported");
            GUI.text(none, 14, false, GUI.AZUL_NOCHE);
            chips.getChildren().add(none);
        }

        Recording session = PatientGUI.findRecording(recordings, report.getSessionId());
        Button viewEcg = null;
        if (session != null) {
            viewEcg = new Button("View ECG");
            GUI.secondaryButton(viewEcg);
            viewEcg.setOnAction(e -> showRecording(session, patientId));
        }

        VBox card = new VBox(16, top, state, new VBox(6, symptomsLabel, chips),
                PatientGUI.sessionBox(report.getSessionId(), recordings, viewEcg));

        String recordingState = PatientGUI.commentPart(report.getComments(), "Recording");
        if (recordingState != null) {
            Label interruptedLabel = new Label("Recording: " + recordingState);
            GUI.fieldError(interruptedLabel);
            card.getChildren().add(1, interruptedLabel);
        }

        String notes = PatientGUI.commentPart(report.getComments(), "Notes");
        if (notes != null) {
            Label notesLabel = new Label("Patient's notes");
            GUI.muted(notesLabel);
            Label notesText = new Label(notes);
            GUI.text(notesText, 14, false, GUI.AZUL_NOCHE);
            notesText.setWrapText(true);
            card.getChildren().add(new VBox(4, notesLabel, notesText));
        }


        String when = report.getDoctorCommentTime() == null ? ""
                : " · " + report.getDoctorCommentTime().format(PatientGUI.REPORT_DATE);
        Label commentLabel = new Label(reviewed ? "Your comment" + when : "Your comment");
        GUI.text(commentLabel, 12, true, PatientGUI.DOCTOR_GREEN);

        TextArea commentArea = new TextArea(reviewed ? doctorText : "");
        commentArea.setPromptText("Write your assessment or advice for the patient…");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        styleTextArea(commentArea);

        Label commentError = errorLabel("");
        commentError.setVisible(false);
        commentError.setManaged(false);

        Button saveButton = new Button(reviewed ? "Update comment" : "Save comment");
        GUI.primaryButton(saveButton);
        saveButton.setPrefWidth(180);
        saveButton.setMaxWidth(Region.USE_PREF_SIZE);
        saveButton.setOnAction(e -> {
            String comment = commentArea.getText().trim();
            if (comment.isEmpty()) {
                showFieldError(commentError, "Write a comment before saving.");
                return;
            }
            try {
                doctorClient.addNote(report.getId(), comment);
            } catch (DoctorClient.ServerException ex) {
                showFieldError(commentError, "The server could not save the comment: " + ex.getMessage());
                return;
            } catch (IOException ex) {
                showFieldError(commentError, "Can't reach the hospital server. Try again.");
                return;
            }
            showPatientHistory(patientId, "Comment saved. The patient can now read it.");
        });

        VBox commentBox = new VBox(8, commentLabel, commentArea, commentError, saveButton);
        GUI.background(commentBox, PatientGUI.DOCTOR_GREEN_BG, 12);
        commentBox.setPadding(new Insets(12, 14, 14, 14));
        card.getChildren().add(commentBox);

        GUI.card(card);
        card.setPadding(new Insets(22, 26, 22, 26));
        return card;
    }

    /** GET_RECORDING_DATA */
    private void showRecording(Recording recording, Integer patientId) {
        Hyperlink backLink = new Hyperlink("← Back to the patient");
        GUI.link(backLink);
        backLink.setOnAction(e -> showPatientHistory(patientId, null));
        int[][] samples;
        try {
            samples = doctorClient.getRecordingData(recording.getId());
        } catch (Exception e) {
            showErrorPage(backLink, "Could not load the recording: " + e.getMessage());
            return;}
        int[] ecg = samples[BitalinoRecorder.ECG_ROW];
        int[] emg = samples[BitalinoRecorder.EMG_ROW];

        Label title = new Label("ECG and EMG recording");
        GUI.title(title, 28);
        // Cuanto dura
        int seconds = ecg.length / recording.getSamplingRate();
        Label secondsLabel = new Label(seconds + " seconds");
        GUI.muted(secondsLabel);

        // Datos de la grabación: frecuencia, canales y unidades
        Label infoLabel = new Label(recording.getSamplingRate() + " Hz   ·   Channels: "
                + recording.getChannel() + "   ·   Units: ADC (0-1023)");
        GUI.muted(infoLabel);

        // Escala de la gráfica en mV: de 0 ADC (abajo) a 1023 ADC (arriba)
        Label ecgLabel = new Label("ECG (" + BitalinoRecorder.ECG_CHANNEL_NAME + ")   ·   scale: "
                + String.format(Locale.ENGLISH, "%.2f to %.2f mV",
                        BitalinoRecorder.ecgToMillivolts(0), BitalinoRecorder.ecgToMillivolts(1023)));
        GUI.fieldLabel(ecgLabel);
        Canvas ecgCanvas = new Canvas(1600, 320);
        drawSignal(ecgCanvas, ecg);

        Label emgLabel = new Label("EMG (" + BitalinoRecorder.EMG_CHANNEL_NAME + ")   ·   scale: "
                + String.format(Locale.ENGLISH, "%.2f to %.2f mV",
                        BitalinoRecorder.emgToMillivolts(0), BitalinoRecorder.emgToMillivolts(1023)));
        GUI.fieldLabel(emgLabel);
        Canvas emgCanvas = new Canvas(1600, 320);
        drawSignal(emgCanvas, emg);

        VBox card = new VBox(14, infoLabel, ecgLabel, ecgCanvas, emgLabel, emgCanvas, secondsLabel);
        GUI.card(card);
        VBox content = new VBox(20, backLink, title, card);
        ScrollPane page = scrollPage(content);
        content.setMaxWidth(1724);
        mainFrame.setScreen(page);}

    /** Dibuja una señal */
    private void drawSignal(Canvas canvas, int[] samples) {
            GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setStroke(GUI.AZUL_NOCHE);
        g.setLineWidth(1);
        for (int i = 1; i < samples.length; i++) {
            double x1 = (i - 1) * width / samples.length; //pixeles ppor muestra y donde quedan segun i
            double x2 = i * width / samples.length;
            double y1 = height - samples[i - 1] * height / 1023.0;
            double y2 = height - samples[i] * height / 1023.0;
            g.strokeLine(x1, y1, x2, y2);
        }
    }

    /**UPDATE_PATIENT */
    private void showEditPatient(Patient patient) {
        Hyperlink backLink = new Hyperlink("← Back to the patient");
        GUI.link(backLink);
        backLink.setOnAction(e -> showPatientHistory(patient.getId(), null));

        Label pageTitle = new Label("Edit patient details");
        GUI.title(pageTitle, 28);
        Label pageSubtitle = new Label(fullName(patient));
        GUI.subtitle(pageSubtitle);
        VBox titleBox = new VBox(4, pageTitle, pageSubtitle);

        TextField nameField = new TextField(safe(patient.getName()));
        GUI.input(nameField);
        TextField surnameField = new TextField(safe(patient.getSurname()));
        GUI.input(surnameField);
        DatePicker birthPicker = new DatePicker(patient.getBirthDate());
        GUI.selectInput(birthPicker);
        ComboBox<String> sexBox = new ComboBox<>();
        sexBox.getItems().addAll("Female", "Male");

        if (sexBox.getItems().contains(patient.getSex())) {
            sexBox.setValue(patient.getSex());
        }
        GUI.selectInput(sexBox);
        TextField phoneField = new TextField(safe(patient.getPhone()));
        phoneField.setPromptText("Optional");
        GUI.input(phoneField);

        Label formError = errorLabel("");
        formError.setVisible(false);
        formError.setManaged(false);

        Button cancelButton = new Button("Cancel");
        GUI.secondaryButton(cancelButton);
        cancelButton.setPrefHeight(46);
        cancelButton.setPadding(new Insets(0, 20, 0, 20));
        cancelButton.setOnAction(e -> showPatientHistory(patient.getId(), null));

        Button saveButton = new Button("Save changes");
        GUI.primaryButton(saveButton);
        saveButton.setPrefWidth(180);
        saveButton.setMaxWidth(Region.USE_PREF_SIZE);
        saveButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String surname = surnameField.getText().trim();

            // La fecha se lee del texto escrito (si se escribe a mano, getValue() puede ser null)
            LocalDate birthDate;
            try {
                birthDate = GUI.readDate(birthPicker);
            } catch (DateTimeParseException ex) {
                showFieldError(formError, "Write the date of birth as dd/mm/yyyy, e.g. 03/05/1990.");
                return;
            }

            if (name.isEmpty() || surname.isEmpty()) {
                showFieldError(formError, "Name and surname are required.");
                return;
            }
            if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
                showFieldError(formError, "The date of birth can't be in the future.");
                return;
            }
            String phone = phoneField.getText().trim();

            Patient edited = new Patient(patient.getUserId(), name, surname, birthDate,
                    sexBox.getValue(), phone.isEmpty() ? null : phone, patient.getDoctorId());
            edited.setId(patient.getId());
            try {
                doctorClient.updatePatient(edited);
            } catch (DoctorClient.ServerException ex) {
                showFieldError(formError, "The server could not save the changes: " + ex.getMessage());
                return;
            } catch (IOException ex) {
                showFieldError(formError, "Can't reach the hospital server. Try again.");
                return;
            }
            showPatientHistory(patient.getId(), "Patient details updated.");
        });

        Region buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, Priority.ALWAYS);
        HBox buttonBar = new HBox(12, cancelButton, buttonSpacer, saveButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                twoColumns(labeled("Name", nameField), labeled("Surname", surnameField)),
                twoColumns(labeled("Date of birth", birthPicker), labeled("Sex", sexBox)),
                labeled("Phone", phoneField),
                formError,
                buttonBar);
        GUI.card(card);
        card.setPadding(new Insets(28, 32, 28, 32));

        mainFrame.setScreen(scrollPage(new VBox(20, backLink, titleBox, card)));
    }


    /** Cierra la sesión y vuelve al login. */
    private void logout() {
        pingThread.stop();
        boolean serverNotified = true;
        try {
            doctorClient.logout();
        } catch (IOException e) {
            System.out.println("Could not notify the server about the logout: " + e.getMessage());
            serverNotified = false;
        }
        mainFrame.showLogin();
        if (!serverNotified) {
            mainFrame.showMessage("You have been logged out.");
        }
    }



    /** scroll si no cabe. */
    private ScrollPane scrollPage(VBox content) {
        content.setMaxWidth(760);
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
        return scroll;
    }

    private void showErrorPage(Hyperlink backLink, String text) {
        mainFrame.setScreen(scrollPage(new VBox(20, backLink, errorLabel(text))));
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        GUI.title(label, 20);
        return label;
    }

    private static VBox emptyCard(String text) {
        Label label = new Label(text);
        GUI.muted(label);
        label.setWrapText(true);
        VBox card = new VBox(label);
        GUI.card(card);
        return card;
    }

    private static Label errorLabel(String text) {
        Label label = new Label(text);
        GUI.fieldError(label);
        label.setWrapText(true);
        return label;
    }

    private static void showFieldError(Label label, String text) {
        label.setText(text);
        label.setVisible(true);
        label.setManaged(true);
    }

    /** Etiqueta encima de un campo. */
    private static VBox labeled(String text, Region control) {
        Label label = new Label(text);
        GUI.fieldLabel(label);
        return new VBox(6, label, control);
    }

    /** Dos campos en la misma fila */
    private static HBox twoColumns(VBox left, VBox right) {
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(0);
        right.setPrefWidth(0);
        return new HBox(12, left, right);
    }

    private static void styleTextArea(TextArea area) {
        area.setFont(Font.font("System", 14));
        area.setStyle("-fx-control-inner-background: #FFFFFF; -fx-background-color: #FFFFFF;"
                + " -fx-background-radius: 10; -fx-background-insets: 0; -fx-focus-color: #4A44B5; -fx-faint-focus-color: transparent;");
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
    }

    private static String fullName(Patient patient) {
        String name = (safe(patient.getName()) + " " + safe(patient.getSurname())).trim();
        return name.isEmpty() ? "Patient " + patient.getId() : name;
    }

    private static int age(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }


    private String displayName() {
        if (doctor != null && doctor.getName() != null && !doctor.getName().isBlank()) {
            return "Dr. " + (doctor.getName() + " " + safe(doctor.getSurname())).trim();
        }
        String first = user.getEmail().split("@")[0].split("[._-]")[0];
        if (first.isEmpty()) {
            return user.getEmail();
        }
        return "Dr. " + Character.toUpperCase(first.charAt(0)) + first.substring(1);
    }
}
