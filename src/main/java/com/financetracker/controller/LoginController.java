package com.financetracker.controller;

import com.financetracker.MainApp;
import com.financetracker.model.User;
import com.financetracker.service.UserService;
import com.financetracker.util.SupabaseClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controller for Login and Registration views
 * Handles Supabase Auth authentication and local database user management
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    // Static reference to current user (shared across controllers)
    private static User currentUser;

    // FXML Components - Login
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label signupLabel; // Legacy?
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label connectionStatusLabel;

    // FXML Components - Registration
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML private TextField regEmailField;
    @FXML private TextField regNameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;
    @FXML private Button registerButton;

    // FXML Components - Shared
    @FXML private StackPane loadingOverlay;

    // Services
    private UserService userService;
    private SupabaseClient supabaseClient;

    @FXML
    public void initialize() {
        logger.info("Initializing LoginController");

        userService = new UserService();
        supabaseClient = SupabaseClient.getInstance();

        // Hide error label and loading indicator initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        } else if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        // Add enter key listener for password field
        if (passwordField != null) {
            passwordField.setOnAction(event -> handleLogin());
        }

        // Test Supabase connection on startup
        testSupabaseConnection();

        logger.info("LoginController initialized");
    }

    /**
     * Test Supabase connection and display status
     */
    private void testSupabaseConnection() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("Testing connection...");
            connectionStatusLabel.setStyle("-fx-text-fill: #F59E0B;"); // Amber
        }

        Task<Boolean> connectionTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // SupabaseClient now uses HikariCP, so this should be fast if pool is ready
                try (Connection conn = supabaseClient.getConnection()) {
                    return conn != null && !conn.isClosed();
                } catch (Exception e) {
                    logger.error("Connection test failed", e);
                    return false;
                }
            }
        };

        connectionTask.setOnSucceeded(event -> {
            boolean connected = connectionTask.getValue();
            Platform.runLater(() -> {
                if (connectionStatusLabel != null) {
                    if (connected) {
                        connectionStatusLabel.setText("Connected to Supabase");
                        connectionStatusLabel.setStyle("-fx-text-fill: #059669;"); // Emerald Green
                        logger.info("Supabase connection successful");
                    } else {
                        connectionStatusLabel.setText("Connection failed");
                        connectionStatusLabel.setStyle("-fx-text-fill: #DC2626;"); // Red
                    }
                }
            });
        });

        connectionTask.setOnFailed(event -> {
            Throwable ex = connectionTask.getException();
            logger.error("Supabase connection failed task", ex);
            Platform.runLater(() -> {
                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText("Connection failed");
                    connectionStatusLabel.setStyle("-fx-text-fill: #DC2626;"); // Red
                }
            });
        });

        new Thread(connectionTask).start();
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Show loading state
        setLoading(true);
        hideError();

        // Perform authentication in background thread
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                logger.info("Attempting login for: {}", email);

                // Step 1: Authenticate with Supabase Auth
                logger.info("Step 1: Authenticating with Supabase Auth...");
                try {
                    boolean authSuccess = supabaseClient.signIn(email, password);
                    if (!authSuccess) {
                        throw new IOException("Invalid login credentials");
                    }
                    logger.info("Supabase Auth successful");
                } catch (IOException e) {
                    logger.error("Supabase Auth failed: {}", e.getMessage());
                    throw new IOException("Invalid email or password");
                }

                // Step 2: Get user from our application's users table
                logger.info("Step 2: Fetching user profile from database...");
                User user = userService.getUserByEmail(email);

                if (user == null) {
                    // User exists in Supabase Auth but not in our users table
                    logger.warn("User authenticated but not found in users table");

                    // Auto-create user profile in our table
                    logger.info("Step 3: Creating user profile in database...");
                    user = new User();
                    user.setEmail(email);
                    user.setFullName(extractNameFromEmail(email));
                    user.setCurrencyPreference("USD");
                    user.setActive(true);

                    boolean created = userService.createUserWithoutPassword(user);

                    if (created) {
                        user = userService.getUserByEmail(email);
                        logger.info("User profile created in database");
                    } else {
                        throw new SQLException("Failed to create user profile in database");
                    }
                }

                logger.info("Login successful for: {}", user.getEmail());
                return user;
            }
        };

        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();
            Platform.runLater(() -> {
                setLoading(false);
                if (user != null) {
                    currentUser = user;
                    logger.info("Navigating to dashboard for user: {}", user.getDisplayName());
                    MainApp.showDashboard();
                } else {
                    showError("Login failed. Please try again.");
                }
            });
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            logger.error("Login failed", ex);
            Platform.runLater(() -> {
                setLoading(false);

                String errorMessage = "Login failed";
                if (ex != null) {
                    String msg = ex.getMessage();
                    if (msg.contains("Invalid login credentials") || msg.contains("Invalid email or password")) {
                        errorMessage = "Invalid email or password";
                    } else if (msg.contains("connection") || msg.contains("Connection")) {
                        errorMessage = "Cannot connect to server. Check your internet connection.";
                    } else if (msg.contains("Email not confirmed")) {
                        errorMessage = "Please confirm your email address first.";
                    } else {
                        errorMessage = msg;
                    }
                }
                showError(errorMessage);
            });
        });

        new Thread(loginTask).start();
    }

    /**
     * Handle signup/register link click
     */
    @FXML
    private void handleSignup(MouseEvent event) {
        handleShowRegister();
    }

    /**
     * Show registration form
     */
    @FXML
    private void handleShowRegister() {
        logger.info("Showing registration form");

        if (loginForm != null && registerForm != null) {
            loginForm.setVisible(false);
            loginForm.setManaged(false);
            registerForm.setVisible(true);
            registerForm.setManaged(true);
            hideError();
        } else {
            showRegistrationDialog();
        }
    }

    /**
     * Show login form (hide registration)
     */
    @FXML
    private void handleShowLogin() {
        logger.info("Showing login form");

        if (loginForm != null && registerForm != null) {
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            loginForm.setVisible(true);
            loginForm.setManaged(true);
            hideError();
        }
    }

    /**
     * Handle registration form submission
     */
    @FXML
    private void handleRegister() {
        String email = regEmailField != null ? regEmailField.getText().trim() : "";
        String name = regNameField != null ? regNameField.getText().trim() : "";
        String password = regPasswordField != null ? regPasswordField.getText() : "";
        String confirmPassword = regConfirmPasswordField != null ? regConfirmPasswordField.getText() : "";

        // Validate input
        if (email.isEmpty()) {
            showError("Email is required");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        if (password.isEmpty()) {
            showError("Password is required");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        setLoading(true);
        hideError();

        Task<Boolean> registerTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                logger.info("Attempting registration for: {}", email);

                // Step 1: Register with Supabase Auth
                logger.info("Step 1: Creating user in Supabase Auth...");
                boolean authCreated = supabaseClient.signUp(email, password);

                if (!authCreated) {
                    throw new Exception("Failed to create account. Email may already be registered.");
                }
                logger.info("User created in Supabase Auth");

                // Step 2: Create user profile in our users table
                logger.info("Step 2: Creating user profile in database...");
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name.isEmpty() ? extractNameFromEmail(email) : name);
                newUser.setCurrencyPreference("USD");
                newUser.setActive(true);

                boolean profileCreated = userService.createUserWithoutPassword(newUser);

                if (!profileCreated) {
                    logger.warn("User created in Auth but failed to create profile");
                    // We don't throw here, just log it. The profile will be auto-created on first login.
                }

                logger.info("Registration successful for: {}", email);
                return true;
            }
        };

        registerTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                setLoading(false);
                if (registerTask.getValue()) {
                    showSuccess("Account created! Check your email to confirm, then log in.");
                    handleShowLogin();

                    if (emailField != null) {
                        emailField.setText(email);
                    }
                }
            });
        });

        registerTask.setOnFailed(event -> {
            Throwable ex = registerTask.getException();
            logger.error("Registration failed", ex);
            Platform.runLater(() -> {
                setLoading(false);
                String errorMsg = ex != null ? ex.getMessage() : "Registration failed";

                if (errorMsg.contains("already registered") || errorMsg.contains("already exists")) {
                    showError("An account with this email already exists.");
                } else {
                    showError(errorMsg);
                }
            });
        });

        new Thread(registerTask).start();
    }

    /**
     * Show registration dialog (alternative to separate form)
     */
    private void showRegistrationDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        dialog.setHeaderText("Register for Finance Tracker");

        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 6 characters)");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm Password");

        content.getChildren().addAll(
                new Label("Full Name:"), nameField,
                new Label("Email:"), emailField,
                new Label("Password:"), passwordField,
                new Label("Confirm Password:"), confirmField
        );

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                if (emailField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                    showError("Email and password are required");
                    return null;
                }
                if (!passwordField.getText().equals(confirmField.getText())) {
                    showError("Passwords do not match");
                    return null;
                }
                if (passwordField.getText().length() < 6) {
                    showError("Password must be at least 6 characters");
                    return null;
                }

                if (regEmailField != null) regEmailField.setText(emailField.getText());
                if (regNameField != null) regNameField.setText(nameField.getText());
                if (regPasswordField != null) regPasswordField.setText(passwordField.getText());
                if (regConfirmPasswordField != null) regConfirmPasswordField.setText(confirmField.getText());

                User tempUser = new User();
                tempUser.setEmail(emailField.getText());
                tempUser.setFullName(nameField.getText());
                return tempUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(user -> {
            performRegistration(user.getEmail(), user.getFullName(), confirmField.getText());
        });
    }

    /**
     * Perform registration with provided values
     */
    private void performRegistration(String email, String name, String password) {
        setLoading(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                boolean authCreated = supabaseClient.signUp(email, password);
                if (authCreated) {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(name);
                    userService.createUserWithoutPassword(newUser);
                    return true;
                }
                return false;
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue()) {
                showSuccess("Account created! Check your email to confirm.");
                if (this.emailField != null) {
                    this.emailField.setText(email);
                }
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showError("Registration failed: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    /**
     * Handle forgot password link click
     */
    @FXML
    private void handleForgotPassword(MouseEvent event) {
        handleForgotPassword();
    }

    /**
     * Handle forgot password - sends reset email via Supabase
     */
    @FXML
    private void handleForgotPassword() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address first");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Password Reset");
        confirm.setContentText("Send password reset email to:\n" + email + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                setLoading(true);

                Task<Boolean> resetTask = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return supabaseClient.resetPassword(email);
                    }
                };

                resetTask.setOnSucceeded(e -> {
                    setLoading(false);
                    if (resetTask.getValue()) {
                        showSuccess("Password reset email sent! Check your inbox.");
                    } else {
                        showError("Failed to send reset email. Please try again.");
                    }
                });

                resetTask.setOnFailed(e -> {
                    setLoading(false);
                    showError("Failed to send reset email.");
                });

                new Thread(resetTask).start();
            }
        });
    }

    /**
     * Extract a display name from email address
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        String namePart = email.split("@")[0];
        namePart = namePart.replace(".", " ").replace("_", " ");
        if (!namePart.isEmpty()) {
            namePart = namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
        }
        return namePart;
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-background-color: rgba(220, 38, 38, 0.1); -fx-padding: 8; -fx-background-radius: 4;");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #059669; -fx-background-color: rgba(5, 150, 105, 0.1); -fx-padding: 8; -fx-background-radius: 4;");
            errorLabel.setVisible(true);
        }
    }

    /**
     * Hide error/success message
     */
    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    /**
     * Set loading state
     */
    private void setLoading(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(loading);
        } else if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
        }
        
        if (loginButton != null) {
            loginButton.setDisable(loading);
        }
        if (registerButton != null) {
            registerButton.setDisable(loading);
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // ============================================
    // Static methods for session management
    // ============================================

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clearCurrentUser() {
        currentUser = null;
        SupabaseClient.getInstance().signOut();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}