package gui;

import Interfaces.UserManager;
import JDBC.JDBCUserManager;
import POJOs.Role;
import POJOs.User;
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

import java.io.IOException;
import java.util.Random;

/**
 *pantalla de login y de singup.
 *abre el menú de cada rol
 */
public class GUI {
    
    static UserManager userManager;

    public static void main(String[] args) {
        userManager = new JDBCUserManager();
        userManager.connect();   //base de datos
        Application.launch(App.class, args);
    }


    public static class App extends Application {

        // Dónde está el HospitalServer
        private static final String SERVER_ADDRESS = "localhost";

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
            // sttauts Server
            Circle dot = new Circle(4, GUI.GRIS_OFFLINE);
            Label serverLabel = new Label("Server: not connected", dot);
            serverLabel.setGraphicTextGap(8);
            GUI.chip(serverLabel);

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
                    serverLabel,
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

            //enviar Commands.LOGIN y esperar OK.
            User user = userManager.checkPassword(email, password);
            if (user == null) {
                showError(passwordField, passwordError, "Wrong email or password.");
                return;
            }

            String role = user.getRole().getDescription().toLowerCase();
            switch (role) {
                case "patient":
                    try {
                        PatientClient patientClient = new PatientClient(SERVER_ADDRESS);
                        patientMn = new PatientGUI(user, this, patientClient);
                    } catch (IOException e) {
                        showMessage(loginMessage, "Can't reach the hospital server.");
                    }
                    break;

                case "doctor":
                    try {
                        DoctorClient doctorClient = new DoctorClient(SERVER_ADDRESS);
                        doctorMn = new DoctorGUI(user, this, doctorClient);
                    } catch (IOException e) {
                        showMessage(loginMessage, "Can't reach the hospital server.");
                    }
                    break;

                default:
                    showMessage(loginMessage, "Unknown role. Access denied.");
            }
        }


        private void showSignUp() {
            Label title = new Label("Create your account");
            GUI.title(title, 30);
            Label subtitle = new Label("Sign up as a new patient");
            GUI.subtitle(subtitle);

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

            VBox form = new VBox(20,
                    new VBox(6, title, subtitle),
                    fieldBox("Email", newEmailField, newEmailError),
                    fieldBox("Password", newPasswordField, newPasswordError),
                    fieldBox("Confirm password", confirmField, confirmError),
                    createButton,
                    signUpMessage,
                    loginRow);
            showNextToBrand(form);
        }

        /**guarda el nuevo paciente en la base de datos. */
        private void handleSignUp() {
            String email = newEmailField.getText().trim();
            String password = newPasswordField.getText();
            String confirm = confirmField.getText();
            boolean ok = true;

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

            if (userManager.getUserByEmail(email) != null) {
                showError(newEmailField, newEmailError, "There is already an account with this email.");
                return;
            }

            Role role = getPatientRole();
            if (role == null) {
                showMessage(signUpMessage, "The Patient role is missing in the database.");
                return;
            }

            try {
                byte[] hashedPassword = JDBCUserManager.hashPassword(password);
                User user = new User(email, hashedPassword, role);
                userManager.newUser(user);
                if (user.getId() == null) {   //si tiene ID quiere decir que funciona
                    showMessage(signUpMessage, "The account could not be created. Try again.");
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showMessage(signUpMessage, "The account could not be created. Try again.");
                return;
            }

            newPasswordField.clear();
            confirmField.clear();
            showMessage(signUpMessage, "Account created! You can now sign in.");
        }

        private Role getPatientRole() {
            for (Role role : userManager.getRoles()) {
                if (role.getDescription().equalsIgnoreCase("Patient")) {
                    return role;
                }
            }
            return null;
        }


        /**  formulario a la derecha y el panel azul */
        private void showNextToBrand(VBox form) {
            form.setMaxWidth(360);
            StackPane formPanel = new StackPane(form);
            GUI.formPanel(formPanel);
            HBox.setHgrow(formPanel, Priority.ALWAYS);

            VBox brandPanel = makeBrandPanel();

            HBox root = new HBox(brandPanel, formPanel);
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

            Label heroText = new Label("Log your symptoms and record your ECG with BITalino. "
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

        private void showMessage(Label message, String text) {
            message.setText(text);
            show(message, true);
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
