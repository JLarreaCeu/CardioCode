package gui;

import commonInfo.*;
import doctor.DoctorClient;
import patient.PatientClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.LocalDateStringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Random;

/**
 *pantalla de login y de singup.
 *abre el menú de cada rol
 */
public class GUI {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }


    public static class App extends Application {

        private Stage stage;
        private Scene scene;
        private PatientGUI patientMn;
        private DoctorGUI doctorMn;

        // Campos de login
        private TextField emailField;
        private PasswordField passwordField;
        private Label emailError;
        private Label passwordError;
        private Label loginMessage;

        private TextField newEmailField;
        private PasswordField newPasswordField;
        private PasswordField confirmField;
        private Label newEmailError;
        private Label newPasswordError;
        private Label confirmError;
        private Label signUpMessage;

        private TextField newNameField;
        private TextField newSurnameField;
        private TextField newPhoneField;
        private DatePicker birthDatePicker;
        private ComboBox<String> sexBox;
        private ComboBox<Doctor> doctorBox;
        private Label newNameError;
        private Label newSurnameError;
        private Label birthDateError;
        private Label doctorError;

        @Override
        public void start(Stage stage) {
            this.stage = stage;
            stage.setTitle("CardioCare");
            stage.setMinWidth(980);
            stage.setMinHeight(640);
            showLogin();
            stage.show();
        }

        /** Cambia lo que se ve en la ventana por otra pantalla */
        void setScreen(Parent root) {
            if (scene == null) {
                scene = new Scene(root, 1100, 700);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        }


        void showLogin() {
            Label title = new Label("Welcome back");
            GUI.title(title, 30);
            Label subtitle = new Label("Sign in to your account");
            GUI.subtitle(subtitle);

            emailField = new TextField();
            emailField.setPromptText("name@example.com");
            GUI.input(emailField);
            emailError = makeErrorLabel();

            passwordField = new PasswordField();
            passwordField.setPromptText("Your password");
            GUI.input(passwordField);
            passwordError = makeErrorLabel();

            Button signInButton = new Button("Sign in");
            GUI.primaryButton(signInButton);
            signInButton.setDefaultButton(true);   // con Enter también se pulsa
            signInButton.setOnAction(e -> handleLogin());

            loginMessage = makeMessageLabel();

            Label newPatient = new Label("New patient?");
            GUI.muted(newPatient);
            Hyperlink signUpLink = new Hyperlink("Create an account");
            GUI.link(signUpLink);
            signUpLink.setOnAction(e -> showSignUp());
            HBox signUpRow = new HBox(6, newPatient, signUpLink);
            signUpRow.setAlignment(Pos.CENTER);

            VBox form = new VBox(20,
                    new VBox(6, title, subtitle),
                    fieldBox("Email", emailField, emailError),
                    fieldBox("Password", passwordField, passwordError),
                    signInButton,
                    loginMessage,
                    signUpRow);
            showNextToBrand(form);
        }

        /**abre el menú del rol. */
        private void handleLogin() {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            boolean ok = true;

            if (!checkEmail(email, emailField, emailError)) {
                ok = false;
            }
            if (password.isEmpty()) {
                showError(passwordField, passwordError, "Enter your password.");
                ok = false;
            }
            if (!ok) {
                return;
            }

            // LOGIN contra el servidor: es el único que tiene la base de datos.
            // El rol no se sabe hasta que contesta, así que se entra con una conexión de paciente
            // y, si resulta ser un médico, se cambia a una conexión de médico.
            PatientClient patientClient = null;
            User user;
            try {
                patientClient = new PatientClient(Config.getServerHost());
                user = patientClient.login(email, password);
            } catch (IOException e) {
                close(patientClient);
                showMessage(loginMessage, "Can't reach the hospital server.");
                return;
            } catch (PatientClient.ServerException e) {
                close(patientClient);
                if ("Wrong email or password".equals(e.getMessage())) {
                    showError(passwordField, passwordError, "Wrong email or password.");
                } else {
                    showMessage(loginMessage, "The server rejected the login: " + e.getMessage());
                }
                return;
            }

            Role role = user.getRole();
            if (role == Role.PATIENT) {
                patientMn = new PatientGUI(user, this, patientClient);
            } else if (role == Role.DOCTOR) {
                close(patientClient);   // esta conexión ya no hace falta
                openDoctor(email, password);
            } else {
                close(patientClient);
                showMessage(loginMessage, "Unknown role. Access denied.");
            }
        }

        /** Abre la conexión de médico, hace LOGIN y abre su menú. */
        private void openDoctor(String email, String password) {
            DoctorClient doctorClient = null;
            try {
                doctorClient = new DoctorClient(Config.getServerHost());
                User user = doctorClient.login(email, password);
                doctorMn = new DoctorGUI(user, this, doctorClient);
            } catch (IOException e) {
                showMessage(loginMessage, "Can't reach the hospital server.");
            } catch (DoctorClient.ServerException e) {
                try {
                    doctorClient.logout();
                } catch (IOException ex) {
                    System.out.println("Could not close the connection: " + ex.getMessage());
                }
                showMessage(loginMessage, "The server rejected the login: " + e.getMessage());
            }
        }


        private void showSignUp() {
            Label title = new Label("Create your account");
            GUI.title(title, 30);
            Label subtitle = new Label("Sign up as a new patient");
            GUI.subtitle(subtitle);

            newNameField = new TextField();
            newNameField.setPromptText("Name");
            GUI.input(newNameField);
            newNameError = makeErrorLabel();

            newSurnameField = new TextField();
            newSurnameField.setPromptText("Surname");
            GUI.input(newSurnameField);
            newSurnameError = makeErrorLabel();

            birthDatePicker = new DatePicker();
            birthDatePicker.setPromptText("dd/mm/yyyy");
            GUI.selectInput(birthDatePicker);
            birthDateError = makeErrorLabel();
            birthDatePicker.valueProperty().addListener((obs, was, now) -> show(birthDateError, false));
            birthDatePicker.getEditor().textProperty().addListener((obs, was, now) -> show(birthDateError, false));

            sexBox = new ComboBox<>();
            sexBox.getItems().addAll("Female", "Male");
            sexBox.setPromptText("Choose");
            GUI.selectInput(sexBox);

            newPhoneField = new TextField();
            newPhoneField.setPromptText("Optional");
            GUI.input(newPhoneField);

            doctorBox = new ComboBox<>();
            doctorBox.setPromptText("Choose your cardiologist");
            GUI.selectInput(doctorBox);
            doctorError = makeErrorLabel();
            doctorBox.valueProperty().addListener((obs, was, now) -> show(doctorError, false));

            newEmailField = new TextField();
            newEmailField.setPromptText("name@example.com");
            GUI.input(newEmailField);
            newEmailError = makeErrorLabel();

            newPasswordField = new PasswordField();
            newPasswordField.setPromptText("At least 8 characters");
            GUI.input(newPasswordField);
            newPasswordError = makeErrorLabel();

            confirmField = new PasswordField();
            confirmField.setPromptText("Repeat your password");
            GUI.input(confirmField);
            confirmError = makeErrorLabel();

            Button createButton = new Button("Create account");
            GUI.primaryButton(createButton);
            createButton.setDefaultButton(true);
            createButton.setOnAction(e -> handleSignUp());

            signUpMessage = makeMessageLabel();

            Label haveAccount = new Label("Already have an account?");
            GUI.muted(haveAccount);
            Hyperlink loginLink = new Hyperlink("Sign in");
            GUI.link(loginLink);
            loginLink.setOnAction(e -> showLogin());
            HBox loginRow = new HBox(6, haveAccount, loginLink);
            loginRow.setAlignment(Pos.CENTER);

            VBox form = new VBox(14,
                    new VBox(6, title, subtitle),
                    twoColumns(fieldBox("Name", newNameField, newNameError),
                            fieldBox("Surname", newSurnameField, newSurnameError)),
                    twoColumns(controlBox("Date of birth", birthDatePicker, birthDateError),
                            controlBox("Sex", sexBox, null)),
                    twoColumns(fieldBox("Phone", newPhoneField, makeErrorLabel()),
                            controlBox("Doctor", doctorBox, doctorError)),
                    fieldBox("Email", newEmailField, newEmailError),
                    fieldBox("Password", newPasswordField, newPasswordError),
                    fieldBox("Confirm password", confirmField, confirmError),
                    createButton,
                    signUpMessage,
                    loginRow);
            form.setMaxWidth(460);
            showNextToBrand(form);
            loadDoctors();
        }

        /** Pide al servidor GET_DOCTORS. */
        private void loadDoctors() {
            PatientClient client = null;
            try {
                client = new PatientClient(Config.getServerHost());
                List<Doctor> doctors = client.getDoctors();
                doctorBox.getItems().setAll(doctors);
                if (doctors.isEmpty()) {
                    doctorBox.setPromptText("No doctors available yet");
                }
            } catch (IOException e) {
                showMessage(signUpMessage, "Can't reach the hospital server. You need it to create an account.");
            } catch (PatientClient.ServerException e) {
                showMessage(signUpMessage, "Could not load the list of doctors: " + e.getMessage());
            } finally {
                close(client);
            }
        }

        /** Cierra una conexión*/
        private void close(PatientClient client) {
            if (client == null) {
                return;
            }
            try {
                client.logout();
            } catch (IOException e) {
                System.out.println("Could not close the connection: " + e.getMessage());
            }
        }

        /** Dos campos en la misma fila */
        private HBox twoColumns(VBox left, VBox right) {
            HBox.setHgrow(left, Priority.ALWAYS);
            HBox.setHgrow(right, Priority.ALWAYS);
            left.setPrefWidth(0);
            right.setPrefWidth(0);
            return new HBox(12, left, right);
        }

        /** Etiqueta + fecha + error  */
        private VBox controlBox(String labelText, Control control, Label error) {
            Label label = new Label(labelText);
            GUI.fieldLabel(label);
            label.setLabelFor(control);
            VBox box = new VBox(6, label, control);
            if (error != null) {
                box.getChildren().add(error);
            }
            return box;
        }

        /** Crea la cuenta y REGISTER_PATIENT */
        private void handleSignUp() {
            String email = newEmailField.getText().trim();
            String password = newPasswordField.getText();
            String name = newNameField.getText().trim();
            String surname = newSurnameField.getText().trim();
            String phone = newPhoneField.getText().trim();
            Doctor doctor = doctorBox.getValue();
            String confirm = confirmField.getText();
            boolean ok = true;
            LocalDate birthDate = null;
            try {
                birthDate = GUI.readDate(birthDatePicker);
            } catch (DateTimeParseException e) {
                birthDateError.setText("Write the date dd/mm/yyyy");
                show(birthDateError, true);
                ok = false;
            }

            if (name.isEmpty()) {
                showError(newNameField, newNameError, "Enter your name.");
                ok = false;
            }
            if (surname.isEmpty()) {
                showError(newSurnameField, newSurnameError, "Enter your surname.");
                ok = false;
            }
            if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
                birthDateError.setText("The date can't be in the future.");
                show(birthDateError, true);
                ok = false;
            }
            if (doctor == null && !doctorBox.getItems().isEmpty()) {
                doctorError.setText("Choose your doctor.");
                show(doctorError, true);
                ok = false;
            }
            if (!checkEmail(email, newEmailField, newEmailError)) {
                ok = false;
            }
            if (password.length() < 8) {
                showError(newPasswordField, newPasswordError, "Use at least 8 characters.");
                ok = false;
            }
            if (!confirm.equals(password)) {
                showError(confirmField, confirmError, "The passwords don't match.");
                ok = false;
            }
            if (!ok) {
                return;
            }

            Patient patient = new Patient(null, name, surname, birthDate,
                    sexBox.getValue(), phone.isEmpty() ? null : phone,
                    doctor == null ? null : doctor.getId());

            PatientClient client = null;
            try {
                client = new PatientClient(Config.getServerHost());
                client.register(email, password, patient);
            } catch (IOException e) {
                showMessage(signUpMessage, "Can't reach the hospital server. Try again.");
                return;
            } catch (PatientClient.ServerException e) {
                String reason = e.getMessage();
                if (reason != null && reason.startsWith("Email already registered")) {
                    showError(newEmailField, newEmailError, "There is already an account with this email.");
                } else {
                    showMessage(signUpMessage, "The account could not be created: " + reason);
                }
                return;
            } finally {
                close(client);
            }

            showLogin();
            emailField.setText(email);
            showMessage(loginMessage, "Account created! You can now sign in.");
        }


        /**  formulario y el panel  */
        private void showNextToBrand(VBox form) {
            if (form.getMaxWidth() <= 0 || form.getMaxWidth() == Double.MAX_VALUE) {
                form.setMaxWidth(360);
            }
            StackPane formPanel = new StackPane(form);
            GUI.formPanel(formPanel);

            ScrollPane formScroll = new ScrollPane(formPanel);
            formScroll.setFitToWidth(true);
            formScroll.setFitToHeight(true);
            formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            formScroll.setBorder(null);
            GUI.background(formScroll, Color.WHITE, 0);
            HBox.setHgrow(formScroll, Priority.ALWAYS);

            VBox brandPanel = makeBrandPanel();

            HBox root = new HBox(brandPanel, formScroll);
            brandPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
            setScreen(root);
        }

        /** LOGO y ECG animado. */
        private VBox makeBrandPanel() {
            Label brandName = new Label("CardioCare");
            GUI.text(brandName, 22, true, Color.WHITE);
            HBox brandRow = new HBox(12, GUI.logo(20), brandName);
            brandRow.setAlignment(Pos.CENTER_LEFT);

            Label heroTitle = new Label("Your heart,\nmonitored from home.");
            GUI.text(heroTitle, 38, true, Color.WHITE);

            Label heroText = new Label("Log your ptoms and record your ECG with BITalino. "
                    + "Your cardiologists reviews everything from the hospital.");
            GUI.text(heroText, 15, false, Color.rgb(255, 255, 255, 0.88));
            heroText.setWrapText(true);
            heroText.setMaxWidth(420);

            Region topSpace = new Region();
            Region bottomSpace = new Region();
            VBox.setVgrow(topSpace, Priority.ALWAYS);
            VBox.setVgrow(bottomSpace, Priority.ALWAYS);

            VBox brandPanel = new VBox(18, brandRow, topSpace, heroTitle, heroText,
                    GUI.ecgTrace(420, 130), bottomSpace);
            brandPanel.setMinWidth(480);
            GUI.brandPanel(brandPanel);
            return brandPanel;
        }

        /** Etiqueta + campo + error*/
        private VBox fieldBox(String labelText, TextField field, Label error) {
            Label label = new Label(labelText);
            GUI.fieldLabel(label);
            label.setLabelFor(field);

            field.textProperty().addListener((observable, oldText, newText) -> {
                GUI.setInvalid(field, false);
                show(error, false);
            });

            return new VBox(6, label, field, error);
        }

        /** Texto rojo de error . */
        private Label makeErrorLabel() {
            Label error = new Label();
            GUI.fieldError(error);
            show(error, false);
            return error;
        }

        /** Mensaje bajo el botón. */
        private Label makeMessageLabel() {
            Label message = new Label();
            GUI.status(message);
            show(message, false);
            return message;
        }

        /** controla el email */
        private boolean checkEmail(String email, TextField field, Label error) {
            if (email.isEmpty()) {
                showError(field, error, "Enter your email.");
                return false;
            }
            if (!email.contains("@") || !email.contains(".")) {
                showError(field, error, "Enter a valid email, e.g. name@example.com");
                return false;
            }
            return true;
        }

        /** enseña error debajo. */
        private void showError(TextField field, Label error, String text) {
            GUI.setInvalid(field, true);
            error.setText(text);
            show(error, true);
        }

        public void showMessage(Label message, String text) {
            message.setText(text);
            show(message, true);
        }

        /** Mensaje en la pantalla de login (p. ej. "conexión perdida"). Llamar después de showLogin(). */
        public void showMessage(String text) {
            showMessage(loginMessage, text);
        }

        /** Muestra u oculta algo sin dejar un hueco vacío. */
        private void show(Region node, boolean visible) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    public static final Color INDIGO       = Color.web("#4A44B5");  // botones principales
    public static final Color INDIGO_DARK  = Color.web("#3A3592");  // botón principal con el ratón encima
    public static final Color LILA         = Color.web("#AFB1F1");  // botones secundarios
    public static final Color LILA_DARK    = Color.web("#A1A3DE");  // botón secundario con el ratón encima
    public static final Color LAVANDA      = Color.web("#E4E4FB");  // fondo de campos y mensajes
    public static final Color CIAN         = Color.web("#8FD3EA");  // línea del ECG sobre fondo oscuro
    public static final Color AZUL_NOCHE   = Color.web("#2B2D5B");  // texto principal
    public static final Color TEXTO_SUAVE  = Color.web("#5B5D84");  // texto secundario
    public static final Color CORAL        = Color.web("#E8566C");  // logo
    public static final Color CORAL_TEXTO  = Color.web("#B83246");  // errores
    public static final Color FONDO        = Color.web("#F1F1F8");  // fondo de las pantallas con tarjetas
    public static final Color GRIS_OFFLINE = Color.web("#9A9CC0");  // punto de "sin conexión"


    /** Tamaño,color de texto*/
    public static void text(Labeled node, double size, boolean bold, Color color) {
        if (bold) {
            node.setFont(Font.font("System", FontWeight.BOLD, size));
        } else {
            node.setFont(Font.font("System", size));
        }
        node.setTextFill(color);
    }

    /** Título en negrita*/
    public static void title(Label label, double size) {
        text(label, size, true, AZUL_NOCHE);
    }

    public static void subtitle(Label label) {
        text(label, 14, false, TEXTO_SUAVE);
    }

    /** Texto gris pequeño. */
    public static void muted(Label label) {
        text(label, 13, false, TEXTO_SUAVE);
    }

    /** Nombre de un campo */
    public static void fieldLabel(Label label) {
        text(label, 13, true, AZUL_NOCHE);
    }

    /** Texto rojo de error debajo de un campo. */
    public static void fieldError(Label label) {
        text(label, 12, false, CORAL_TEXTO);
        label.setWrapText(true);
    }

    /** Caja  con un mensaje */
    public static void status(Label label) {
        text(label, 13, false, AZUL_NOCHE);
        background(label, LAVANDA, 10);
        label.setPadding(new Insets(10, 12, 10, 12));
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    /** Pastilla pequeña */
    public static void chip(Label label) {
        text(label, 12, false, TEXTO_SUAVE);
        background(label, LAVANDA, 20);
        label.setPadding(new Insets(5, 12, 5, 10));
    }


    /** Color de fondo */
    public static void background(Region region, Paint color, double radius) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radius), Insets.EMPTY)));
    }

    /** Mitad izquierda del login fondo */
    public static void brandPanel(Region region) {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, INDIGO), new Stop(1, Color.web("#2F2A86")));
        background(region, gradient, 0);
        region.setPadding(new Insets(48, 56, 40, 56));
    }

    /**  fondo blanco. */
    public static void formPanel(Region region) {
        background(region, Color.WHITE, 0);
        region.setPadding(new Insets(40));
    }

    /** Tarjeta blanca redondeada. */
    public static void card(Region region) {
        background(region, Color.WHITE, 22);
        region.setPadding(new Insets(20, 22, 20, 22));
    }



    /** Campo de texto . */
    public static void input(TextField field) {
        background(field, LAVANDA, 10);
        field.setFont(Font.font("System", 14));
        field.setPrefHeight(46);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setPadding(new Insets(0, 12, 0, 12));
    }

    /** Desplegable o selector de fecha con el mismo aspecto que los campos de texto. */
    public static void selectInput(ComboBoxBase<?> control) {
        background(control, LAVANDA, 10);
        control.setPrefHeight(46);
        control.setMaxWidth(Double.MAX_VALUE);
        control.setStyle("-fx-font-size: 14px;");
        if (control instanceof DatePicker) {
            DatePicker picker = (DatePicker) control;
            background(picker.getEditor(), Color.TRANSPARENT, 10);
            // Las fechas siempre se ven y se escriben como 03/05/1990
            picker.setConverter(new LocalDateStringConverter(DATE_FORMAT, DATE_FORMAT));
        }
    }

    /** Formato de las fechas: día/mes/año. Acepta 03/05/1990 y 3/5/1990. STRICT rechaza fechas que no existen, como 31/02. */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Lee la fecha de un DatePicker. Lee el texto escrito, porque si la fecha se escribe
     * a mano y no se pulsa Enter, getValue() devuelve null.
     * @return la fecha, o null si el campo está vacío
     * @throws DateTimeParseException si lo escrito no es una fecha válida
     */
    public static LocalDate readDate(DatePicker picker) {
        String text = picker.getEditor().getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        return LocalDate.parse(text, DATE_FORMAT);
    }

    /** Borde rojo si el campo tiene un error*/
    public static void setInvalid(TextField field, boolean invalid) {
        if (invalid) {
            field.setBorder(new Border(new BorderStroke(CORAL_TEXTO, BorderStrokeStyle.SOLID,
                    new CornerRadii(10), new BorderWidths(2))));
        } else {
            field.setBorder(null);
        }
    }

    /** Botón principal */
    public static void primaryButton(Button button) {
        text(button, 15, true, Color.WHITE);
        button.setPrefHeight(46);
        button.setMaxWidth(Double.MAX_VALUE);
        colorButton(button, INDIGO, INDIGO_DARK);
    }

    /** Botón secundario*/
    public static void secondaryButton(Button button) {
        text(button, 13, true, AZUL_NOCHE);
        colorButton(button, LILA, LILA_DARK);
    }

    /** lo oscurece al pasar el ratón por encima. */
    private static void colorButton(Button button, Color normal, Color hover) {
        background(button, normal, 10);
        button.setCursor(Cursor.HAND);
        button.setOnMouseEntered(e -> background(button, hover, 10));
        button.setOnMouseExited(e -> background(button, normal, 10));
    }

    /** Enlace índigo, subrayado al pasar el ratón por encima. */
    public static void link(Hyperlink link) {
        text(link, 13, true, INDIGO);
        link.setPadding(Insets.EMPTY);
        link.setBorder(null);
        link.setOnMouseEntered(e -> link.setUnderline(true));
        link.setOnMouseExited(e -> link.setUnderline(false));
    }



    /** Logo */
    public static Node logo(double radius) {
        Circle circle = new Circle(radius, CORAL);
        Rectangle vertical = new Rectangle(radius * 0.36, radius * 1.1, Color.WHITE);
        Rectangle horizontal = new Rectangle(radius * 1.1, radius * 0.36, Color.WHITE);

        StackPane logo = new StackPane(circle, vertical, horizontal);   // los tres centrados, uno encima de otro
        logo.setMaxSize(radius * 2, radius * 2);
        return logo;
    }



    /** ECG para el panel*/
    public static Pane ecgTrace(double width, double height) {
        return ecgTrace(width, height, Color.rgb(255, 255, 255, 0.06), Color.rgb(255, 255, 255, 0.07), CIAN);
    }

    /** ECG dentro de una tarjeta blanca*/
    public static Pane ecgTraceOnCard(double width, double height) {
        return ecgTrace(width, height, LAVANDA, Color.rgb(74, 68, 181, 0.08), INDIGO);
    }

    /**
     * Dibuja un ECG en bulce
     */
    private static Pane ecgTrace(double width, double height, Color boxColor, Color gridColor, Color lineColor) {
        Pane pane = new Pane();
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        background(pane, boxColor, 14);

        // Cuadrícula, como el papel del ECG
        for (double x = 20; x < width; x += 20) {
            Line line = new Line(x, 0, x, height);
            line.setStroke(gridColor);
            pane.getChildren().add(line);
        }
        for (double y = 20; y < height; y += 20) {
            Line line = new Line(0, y, width, y);
            line.setStroke(gridColor);
            pane.getChildren().add(line);
        }

        // La línea del ECG
        double base = height * 0.62;                                    // altura de la línea base
        double[] gaps = {74, 108, 82, 126, 66, 99, 88, 117, 71, 104};   // distancia entre latidos
        Random random = new Random(7);                                  // misma semilla: siempre igual

        Path ecg = new Path();
        ecg.getElements().add(new MoveTo(0, base));
        double x = 14;
        int beat = 0;
        while (x < width) {
            ecg.getElements().add(new LineTo(x, base));
            ecg.getElements().add(new LineTo(x + 3, base + 5));
            ecg.getElements().add(new LineTo(x + 7, base - height * 0.48));
            ecg.getElements().add(new LineTo(x + 11, base + 11));
            ecg.getElements().add(new LineTo(x + 15, base));
            ecg.getElements().add(new LineTo(x + 26, base));
            ecg.getElements().add(new LineTo(x + 38, base - 12));
            ecg.getElements().add(new LineTo(x + 56, base));

            // Hasta el siguiente latido
            double next = x + gaps[beat % gaps.length];
            for (double t = x + 60; t < next; t += 4) {
                ecg.getElements().add(new LineTo(t, base + (random.nextDouble() - 0.5) * 6));
            }
            x = next;
            beat++;
        }
        ecg.setStroke(lineColor);
        ecg.setStrokeWidth(2.5);

        Rectangle visiblePart = new Rectangle(0, height);
        ecg.setClip(visiblePart);
        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(visiblePart.widthProperty(), 0)),
                new KeyFrame(Duration.seconds(4.5), new KeyValue(visiblePart.widthProperty(), width)),
                new KeyFrame(Duration.seconds(6)));   // pausa con el ECG completo
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();

        pane.getChildren().add(ecg);
        return pane;
    }



  }

