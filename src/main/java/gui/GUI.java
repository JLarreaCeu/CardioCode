package gui;


import Interfaces.UserManager;
import JDBC.JDBCUserManager;
import POJOs.Role;
import POJOs.User;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 *APP PRINCIPAL
 */
public class GUI {


    static UserManager userManager;

    public static void main(String[] args) {
        userManager = new JDBCUserManager();
        userManager.connect();   //base de datos
        Application.launch(App.class, args);
    }


    public static class App extends Application {

        private Stage stage;
        private Scene scene;
        private PatientGUI patientMn;
        private DoctorGUI doctorMn;
        private AdminGUI adminMn;
        @Override
        public void start(Stage stage) {
            this.stage = stage;
            stage.setTitle("CardioCare - Patient");
            stage.setMinWidth(980);
            stage.setMinHeight(640);
            showLogin();
            stage.show();
        }

        /** Cambia ventana por otra pantalla */
        void setScreen(Parent root) {
            if (scene == null) {
                scene = new Scene(root, 1100, 700);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        }

        /** ECG */
        private VBox brandPanel() {
            Label brandName = new Label("CardioCare");
            GUIDecor.brandName(brandName);
            HBox brandRow = new HBox(12, GUIDecor.logo(20), brandName);
            brandRow.setAlignment(Pos.CENTER_LEFT);

            Label heroTitle = new Label("Your heart,\nmonitored from home.");
            GUIDecor.heroTitle(heroTitle);
            heroTitle.setWrapText(true);

            Label heroText = new Label("Log your symptoms and record your ECG with BITalino. "
                    + "Your cardiology team reviews everything from the hospital.");
            GUIDecor.heroText(heroText);
            heroText.setWrapText(true);
            heroText.setMaxWidth(420);

            Pane ecg = GUIDecor.ecgTrace(420, 130);

            Region topSpacer = new Region();
            Region bottomSpacer = new Region();
            VBox.setVgrow(topSpacer, Priority.ALWAYS);
            VBox.setVgrow(bottomSpacer, Priority.ALWAYS);


            VBox brandPanel = new VBox(18, brandRow, topSpacer, heroTitle, heroText,
                    ecg, bottomSpacer);
            GUIDecor.brandPanel(brandPanel);
            brandPanel.setMinWidth(480);
            return brandPanel;
        }


        void showLogin() {


            VBox brandPanel = brandPanel();
            // Indicador del servidor
            Circle dot = new Circle(4);
            GUIDecor.dotOffline(dot);
            Label serverChip = new Label("Server: not connected", dot);
            GUIDecor.serverChip(serverChip);
            serverChip.setGraphicTextGap(8);

            Label title = new Label("Welcome back");
            GUIDecor.formTitle(title);
            Label subtitle = new Label("Sign in to your patient account");
            GUIDecor.formSubtitle(subtitle);


            TextField emailField = new TextField();
            emailField.setPromptText("name@example.com");
            GUIDecor.input(emailField);
            Label emailError = errorLabel();
            VBox emailGroup = fieldGroup("Email", emailField, emailError);


            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Your password");
            GUIDecor.input(passwordField);
            Label passwordError = errorLabel();
            VBox passwordGroup = fieldGroup("Password", passwordField, passwordError);

            // IP del servidor del hospital (oculta hasta pulsar "Connection settings")
            TextField serverField = new TextField("localhost");
            GUIDecor.input(serverField);
            Label serverError = errorLabel();
            VBox serverGroup = fieldGroup("Hospital server address", serverField, serverError);
            show(serverGroup, false);

            Hyperlink settingsLink = new Hyperlink("Connection settings");
            GUIDecor.link(settingsLink);
            settingsLink.setOnAction(e -> show(serverGroup, !serverGroup.isVisible()));


            Button signInButton = new Button("Sign in");
            GUIDecor.primaryButton(signInButton);
            signInButton.setMaxWidth(Double.MAX_VALUE);
            signInButton.setDefaultButton(true);


            Label formStatus = new Label();
            GUIDecor.formStatus(formStatus);
            formStatus.setWrapText(true);
            formStatus.setMaxWidth(Double.MAX_VALUE);
            show(formStatus, false);

            Label newPatient = new Label("New patient?");
            GUIDecor.muted(newPatient);
            Hyperlink signupLink = new Hyperlink("Create an account");
            GUIDecor.link(signupLink);
            signupLink.setOnAction(e -> showSignUp());
            HBox signupRow = new HBox(6, newPatient, signupLink);
            signupRow.setAlignment(Pos.CENTER);


            signInButton.setOnAction(e -> handleLogin(
                    emailField, emailError, passwordField, passwordError,
                    serverField, serverError, serverGroup, formStatus));


            clearErrorOnType(emailField, emailError);
            clearErrorOnType(passwordField, passwordError);
            clearErrorOnType(serverField, serverError);

            VBox heading = new VBox(6, title, subtitle);
            VBox form = new VBox(20, serverChip, heading, emailGroup, passwordGroup,
                    settingsLink, serverGroup, signInButton, formStatus, signupRow);
            form.setMaxWidth(360);
            form.setFillWidth(true);

            StackPane formPanel = new StackPane(form);
            GUIDecor.formPanel(formPanel);


            HBox root = new HBox(brandPanel, formPanel);
            brandPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
            HBox.setHgrow(formPanel, Priority.ALWAYS);
            setScreen(root);
        }

        /** login for user*/
        private void handleLogin(TextField emailField, Label emailError,
                                 PasswordField passwordField, Label passwordError,
                                 TextField serverField, Label serverError,
                                 VBox serverGroup, Label formStatus) {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String server = serverField.getText().trim();
            boolean ok = true;

            if (email.isEmpty()) {
                setError(emailField, emailError, "Enter your email.");
                ok = false;
            } else if (!email.contains("@") || !email.contains(".")) {
                setError(emailField, emailError, "Enter a valid email, e.g. name@example.com");
                ok = false;
            }
            if (password.isEmpty()) {
                setError(passwordField, passwordError, "Enter your password.");
                ok = false;
            }
            if (server.isEmpty()) {
                show(serverGroup, true);
                setError(serverField, serverError, "Enter the server address (e.g. localhost).");
                ok = false;
            }
            if (!ok) return;

            //enviar Commands.LOGIN y esperar OK.
            User user = userManager.checkPassword(email, password);
            if (user == null) {
                setError(passwordField, passwordError, "Wrong email or password.");
                return;
            }


            String role = user.getRole().getDescription().toLowerCase();
            switch (role) {
                case "patient":
                    patientMn = new PatientGUI(user, this);
                    break;

                case "doctor":
                    doctorMn = new DoctorGUI(user, this);
                    break;

                default:
                    setStatus(formStatus, "Unknown role. Access denied.");
            }
        }


        private void showSignUp() {
            Label title = new Label("Create your account");
            GUIDecor.formTitle(title);
            Label subtitle = new Label("Sign up as a new patient");
            GUIDecor.formSubtitle(subtitle);

            TextField emailField = new TextField();
            emailField.setPromptText("name@example.com");
            GUIDecor.input(emailField);
            Label emailError = errorLabel();
            VBox emailGroup = fieldGroup("Email", emailField, emailError);

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("At least 8 characters");
            GUIDecor.input(passwordField);
            Label passwordError = errorLabel();
            VBox passwordGroup = fieldGroup("Password", passwordField, passwordError);

            PasswordField confirmField = new PasswordField();
            confirmField.setPromptText("Repeat your password");
            GUIDecor.input(confirmField);
            Label confirmError = errorLabel();
            VBox confirmGroup = fieldGroup("Confirm password", confirmField, confirmError);

            Button createButton = new Button("Create account");
            GUIDecor.primaryButton(createButton);
            createButton.setMaxWidth(Double.MAX_VALUE);
            createButton.setDefaultButton(true);

            Label formStatus = new Label();
            GUIDecor.formStatus(formStatus);
            formStatus.setWrapText(true);
            formStatus.setMaxWidth(Double.MAX_VALUE);
            show(formStatus, false);

            Label haveAccount = new Label("Already have an account?");
            GUIDecor.muted(haveAccount);
            Hyperlink loginLink = new Hyperlink("Sign in");
            GUIDecor.link(loginLink);
            loginLink.setOnAction(e -> showLogin());
            HBox loginRow = new HBox(6, haveAccount, loginLink);
            loginRow.setAlignment(Pos.CENTER);

            createButton.setOnAction(e -> handleSignUp(
                    emailField, emailError, passwordField, passwordError,
                    confirmField, confirmError, formStatus));

            clearErrorOnType(emailField, emailError);
            clearErrorOnType(passwordField, passwordError);
            clearErrorOnType(confirmField, confirmError);

            VBox heading = new VBox(6, title, subtitle);
            VBox form = new VBox(20, heading, emailGroup, passwordGroup, confirmGroup,
                    createButton, formStatus, loginRow);
            form.setMaxWidth(360);
            form.setFillWidth(true);

            StackPane formPanel = new StackPane(form);
            GUIDecor.formPanel(formPanel);


            VBox brandPanel = brandPanel();
            HBox root = new HBox(brandPanel, formPanel);
            brandPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
            HBox.setHgrow(formPanel, Priority.ALWAYS);
            setScreen(root);
        }

        /** guarda el nuevo usuario . */
        private void handleSignUp(TextField emailField, Label emailError, PasswordField passwordField, Label passwordError, PasswordField confirmField, Label confirmError, Label formStatus) {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            boolean ok = true;

            if (email.isEmpty()) {
                setError(emailField, emailError, "Enter your email.");
                ok = false;
            } else if (!email.contains("@") || !email.contains(".")) {
                setError(emailField, emailError, "Enter a valid email, e.g. name@example.com");
                ok = false;
            }
            if (!confirm.equals(password)) {
                setError(confirmField, confirmError, "The passwords don't match.");
                ok = false;
            }
            if (!ok) return;

            if (userManager.getUserByEmail(email) != null) {
                setError(emailField, emailError, "There is already an account with this email.");
                return;
            }

            // El rol tiene que venir de la base de datos para tener su id
            Role role = patientRole();
            if (role == null) {
                setStatus(formStatus, "The Patient role is missing in the database.");
                return;
            }

            try {

                byte[] hashedPassword = JDBCUserManager.hashPassword(password);
                User user = new User(email, hashedPassword, role);
                userManager.newUser(user);
                if (user.getId() == null) {   //si tiene ID queire decir q dunciona
                    setStatus(formStatus, "The account could not be created. Try again.");
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                setStatus(formStatus, "The account could not be created. Try again.");
                return;
            }

            passwordField.clear();
            confirmField.clear();
            setStatus(formStatus, "Account created! You can now sign in.");
        }

        private Role patientRole() {
            for (Role role : userManager.getRoles()) {
                if ("Patient".equalsIgnoreCase(role.getDescription())) return role;
            }
            return null;
        }


        /** Etiqueta + campo + error. */
        private VBox fieldGroup(String labelText, Control field, Label error) {
            Label label = new Label(labelText);
            GUIDecor.fieldLabel(label);
            label.setLabelFor(field);
            field.setMaxWidth(Double.MAX_VALUE);
            return new VBox(6, label, field, error);
        }

        private Label errorLabel() {
            Label error = new Label();
            GUIDecor.fieldError(error);
            error.setWrapText(true);
            show(error, false);
            return error;
        }

        private void setError(Control field, Label error, String message) {
            GUIDecor.setInvalid(field, true);   // borde coral
            error.setText(message);
            show(error, true);
        }

        private void clearErrorOnType(TextInputControl field, Label error) {
            field.textProperty().addListener((obs, oldText, newText) -> {
                GUIDecor.setInvalid(field, false);
                show(error, false);
            });
        }

        private void setStatus(Label status, String message) {
            status.setText(message);
            show(status, true);
        }


        private void show(Region node, boolean visible) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }






}
