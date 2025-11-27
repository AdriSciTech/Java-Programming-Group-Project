package com.financetracker.controller;

import com.financetracker.MainApp;
import com.financetracker.model.User;
import com.financetracker.service.UserService;
import com.financetracker.util.SupabaseClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for Login View
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label signupLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label connectionStatusLabel;  // New: Connection status label

    private UserService userService;
    private SupabaseClient supabaseClient;
    private static User currentUser;

    @FXML
    public void initialize() {
        userService = new UserService();
        supabaseClient = SupabaseClient.getInstance();

        // Hide error label and loading indicator initially
        errorLabel.setVisible(false);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        // Add enter key listener for password field
        passwordField.setOnAction(event -> handleLogin());

        // Test Supabase connection on startup
        testSupabaseConnection();
    }

    /**
     * Test Supabase connection and display status
     */
    private void testSupabaseConnection() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("Testing connection...");
            connectionStatusLabel.setStyle("-fx-text-fill: #FFA500;"); // Orange
        }

        // Test connection in background thread
        new Thread(() -> {
            try {
                // Test database connection
                java.sql.Connection conn = supabaseClient.getConnection();

                if (conn != null && !conn.isClosed()) {
                    // Connection successful
                    javafx.application.Platform.runLater(() -> {
                        if (connectionStatusLabel != null) {
                            connectionStatusLabel.setText("✓ Connected to Supabase");
                            connectionStatusLabel.setStyle("-fx-text-fill: #5CB85C;"); // Green
                        }
                        logger.info("✓ Supabase connection successful");
                        logger.info("Database URL: {}", conn.toString());
                    });
                    conn.close();
                } else {
                    throw new Exception("Connection is null or closed");
                }

            } catch (Exception e) {
                logger.error("✗ Supabase connection failed", e);
                javafx.application.Platform.runLater(() -> {
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("✗ Connection Failed: " + e.getMessage());
                        connectionStatusLabel.setStyle("-fx-text-fill: #D9534F;"); // Red
                    }
                    // Also show in error label
                    showError("Cannot connect to database. Check application.properties");
                });
            }
        }).start();
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

        // Show loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        // Perform authentication in background thread
        new Thread(() -> {
            try {
                logger.info("Attempting login for: {}", email);

                // Step 1: Test database connection
                logger.info("Step 1: Testing database connection...");
                java.sql.Connection testConn = supabaseClient.getConnection();
                if (testConn == null || testConn.isClosed()) {
                    throw new Exception("Database connection failed");
                }
                logger.info("✓ Database connection OK");
                testConn.close();

                // Step 2: Authenticate with Supabase Auth
                logger.info("Step 2: Authenticating with Supabase Auth...");
                supabaseClient.signIn(email, password);
                logger.info("✓ Supabase Auth successful");

                // Step 3: Get user details from database
                logger.info("Step 3: Fetching user from database...");
                User user = userService.authenticateUser(email, password);

                if (user != null) {
                    currentUser = user;
                    logger.info("✓ User found in database: {}", user.getFullName());

                    // Switch to dashboard on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        logger.info("Login successful for user: {}", email);
                        MainApp.showDashboard();
                    });
                } else {
                    logger.warn("✗ User authenticated in Supabase but not found in users table");
                    javafx.application.Platform.runLater(() -> {
                        showError("User exists in Auth but not in database. Run SQL to add user to 'users' table.");
                        resetLoginForm();
                    });
                }

            } catch (java.io.IOException e) {
                // Supabase Auth error
                logger.error("✗ Supabase Auth failed", e);
                javafx.application.Platform.runLater(() -> {
                    if (e.getMessage().contains("Invalid login credentials")) {
                        showError("Invalid email or password. Check Supabase Authentication → Users");
                    } else {
                        showError("Auth Error: " + e.getMessage());
                    }
                    resetLoginForm();
                });
            } catch (java.sql.SQLException e) {
                // Database connection error
                logger.error("✗ Database error", e);
                javafx.application.Platform.runLater(() -> {
                    showError("Database Error: Check application.properties credentials");
                    resetLoginForm();
                });
            } catch (Exception e) {
                // Other errors
                logger.error("✗ Login error", e);
                javafx.application.Platform.runLater(() -> {
                    showError("Error: " + e.getMessage());
                    resetLoginForm();
                });
            }
        }).start();
    }

    /**
     * Handle signup label click
     */
    @FXML
    private void handleSignup(MouseEvent event) {
        try {
            // TODO: Load signup view
            logger.info("Signup clicked");
            showError("Signup feature coming soon!");
        } catch (Exception e) {
            logger.error("Error loading signup", e);
        }
    }

    /**
     * Handle forgot password label click
     */
    @FXML
    private void handleForgotPassword(MouseEvent event) {
        try {
            // TODO: Implement forgot password functionality
            logger.info("Forgot password clicked");
            showError("Password reset feature coming soon!");
        } catch (Exception e) {
            logger.error("Error in forgot password", e);
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Reset login form
     */
    private void resetLoginForm() {
        loginButton.setDisable(false);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        passwordField.clear();
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    /**
     * Get current logged in user
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Set current user (for testing or external login)
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}