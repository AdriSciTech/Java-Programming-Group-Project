package com.financetracker.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main Dashboard Controller
 * Handles navigation between different views in the application
 *
 * This controller manages:
 * - View navigation and loading
 * - Active button state management
 * - User session information display
 * - Initial dashboard data loading
 *
 * @author Finance Tracker Team
 * @version 1.0
 */
public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    // FXML Components - Main Layout
    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;

    // FXML Components - Dashboard Summary (initial view)
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label netBalanceLabel;
    @FXML private Label budgetStatusLabel;

    // FXML Components - Navigation Buttons
    @FXML private Button dashboardButton;
    @FXML private Button incomeButton;
    @FXML private Button expensesButton;
    @FXML private Button budgetsButton;
    @FXML private Button analyticsButton;
    @FXML private Button settingsButton;

    // Instance Variables
    private String currentUsername;
    private Button activeButton;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");

    /**
     * Initialize the dashboard controller
     * Called automatically after FXML loading
     */
    @FXML
    public void initialize() {
        LOGGER.info("Initializing DashboardController");

        // Set initial date
        updateDateDisplay();

        // Set dashboard as active initially
        setActiveButton(dashboardButton);

        // Load initial dashboard data
        loadDashboardData();

        LOGGER.info("DashboardController initialized successfully");
    }

    /**
     * Set the current username and update welcome message
     * Should be called after login
     *
     * @param username The username to display
     */
    public void setUsername(String username) {
        this.currentUsername = username;
        if (username != null && !username.isEmpty()) {
            welcomeLabel.setText("Welcome, " + username);
            LOGGER.info("Username set to: " + username);
        } else {
            welcomeLabel.setText("Welcome");
            LOGGER.warning("Username is null or empty");
        }
    }

    /**
     * Update the date display with current date
     */
    private void updateDateDisplay() {
        try {
            dateLabel.setText(LocalDate.now().format(DATE_FORMATTER));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error formatting date", e);
            dateLabel.setText(LocalDate.now().toString());
        }
    }

    /**
     * Load dashboard summary data
     * TODO: Replace with actual database queries through service layer
     */
    private void loadDashboardData() {
        try {
            // TODO: Implement service calls
            // Example:
            // FinancialSummaryService service = new FinancialSummaryService();
            // BigDecimal income = service.getTotalIncome(currentUserId, Month.CURRENT);
            // totalIncomeLabel.setText("$" + String.format("%.2f", income));

            totalIncomeLabel.setText("$0.00");
            totalExpenseLabel.setText("$0.00");
            netBalanceLabel.setText("$0.00");
            budgetStatusLabel.setText("No budgets configured");

            LOGGER.info("Dashboard data loaded successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading dashboard data", e);
            showErrorLabels();
        }
    }

    /**
     * Display error state in summary labels
     */
    private void showErrorLabels() {
        totalIncomeLabel.setText("Error");
        totalExpenseLabel.setText("Error");
        netBalanceLabel.setText("Error");
        budgetStatusLabel.setText("Error loading data");
    }

    // ==================== NAVIGATION HANDLERS ====================

    /**
     * Handle Dashboard button click
     * Loads the dashboard overview view
     */
    @FXML
    private void handleDashboard() {
        LOGGER.info("Navigating to Dashboard");
        setActiveButton(dashboardButton);
        loadView("dashboard-view.fxml", "Dashboard Overview");
    }

    /**
     * Handle Income button click
     * Loads the income management view
     */
    @FXML
    private void handleIncome() {
        LOGGER.info("Navigating to Income");
        setActiveButton(incomeButton);
        loadView("income-view.fxml", "Income Management");
    }

    /**
     * Handle Expenses button click
     * Loads the expense management view
     */
    @FXML
    private void handleExpenses() {
        LOGGER.info("Navigating to Expenses");
        setActiveButton(expensesButton);
        loadView("expenses-view.fxml", "Expense Management");
    }

    /**
     * Handle Budgets button click
     * Loads the budget management view
     */
    @FXML
    private void handleBudgets() {
        LOGGER.info("Navigating to Budgets");
        setActiveButton(budgetsButton);
        loadView("budgets-view.fxml", "Budget Management");
    }

    /**
     * Handle Analytics button click
     * Loads the analytics and reports view
     */
    @FXML
    private void handleAnalytics() {
        LOGGER.info("Navigating to Analytics");
        setActiveButton(analyticsButton);
        loadView("analytics-view.fxml", "Analytics & Reports");
    }

    /**
     * Handle Settings button click
     * Loads the settings view
     */
    @FXML
    private void handleSettings() {
        LOGGER.info("Navigating to Settings");
        setActiveButton(settingsButton);
        loadView("settings-view.fxml", "Settings");
    }

    /**
     * Handle Logout button click
     * Returns user to login screen and clears session
     */
    @FXML
    private void handleLogout() {
        LOGGER.info("User logging out: " + currentUsername);

        try {
            // TODO: Clear session data
            // SessionManager.getInstance().clearSession();

            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            // Get current stage and set login scene
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Finance Tracker - Login");

            LOGGER.info("Logout successful, returned to login screen");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading login screen", e);
            // TODO: Show error dialog to user
        }
    }

    // ==================== VIEW MANAGEMENT ====================

    /**
     * Load a view into the center pane of the dashboard
     *
     * @param fxmlFile The FXML file to load (e.g., "dashboard-view.fxml")
     * @param title The title of the view (for logging/breadcrumbs)
     */
    private void loadView(String fxmlFile, String title) {
        try {
            LOGGER.info("Loading view: " + fxmlFile);

            // Create loader for the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));

            // Load the view
            Parent view = loader.load();

            // Set the loaded view as the center of the BorderPane
            mainBorderPane.setCenter(view);

            // Optional: Update date label as breadcrumb
            // dateLabel.setText(title);

            LOGGER.info("View loaded successfully: " + fxmlFile);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading view: " + fxmlFile, e);
            showErrorView(fxmlFile);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error loading view: " + fxmlFile, e);
            showErrorView(fxmlFile);
        }
    }

    /**
     * Show error view when a view fails to load
     *
     * @param failedView The view that failed to load
     */
    private void showErrorView(String failedView) {
        Label errorLabel = new Label("Error loading view: " + failedView + "\nPlease check console for details.");
        errorLabel.setStyle("-fx-text-fill: #D9534F; -fx-font-size: 16px; -fx-padding: 20px;");
        mainBorderPane.setCenter(errorLabel);
    }

    /**
     * Set the active navigation button and update styling
     * Manages the visual state of navigation buttons
     *
     * @param button The button to set as active
     */
    private void setActiveButton(Button button) {
        if (button == null) {
            LOGGER.warning("Attempted to set null button as active");
            return;
        }

        // Remove active style from previous button
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
            if (!activeButton.getStyleClass().contains("nav-button")) {
                activeButton.getStyleClass().add("nav-button");
            }
        }

        // Add active style to new button
        button.getStyleClass().remove("nav-button");
        button.getStyleClass().add("nav-button-active");
        activeButton = button;

        LOGGER.fine("Active button set to: " + button.getText());
    }

    // ==================== GETTERS ====================

    /**
     * Get the current username
     *
     * @return The current username or null if not set
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * Get the main BorderPane container
     * Useful for advanced navigation scenarios
     *
     * @return The main BorderPane
     */
    public BorderPane getMainBorderPane() {
        return mainBorderPane;
    }
}