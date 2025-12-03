package com.financetracker.controller;

import com.financetracker.model.Budget;
import com.financetracker.model.Category;
import com.financetracker.service.BudgetService;
import com.financetracker.service.CategoryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for Budget Management View
 */
public class BudgetController {
    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);

    @FXML private TableView<Budget> budgetTable;
    @FXML private TableColumn<Budget, String> nameColumn;
    @FXML private TableColumn<Budget, String> categoryColumn;
    @FXML private TableColumn<Budget, String> limitColumn;
    @FXML private TableColumn<Budget, String> spentColumn;
    @FXML private TableColumn<Budget, String> remainingColumn;
    @FXML private TableColumn<Budget, String> percentageColumn;
    @FXML private TableColumn<Budget, String> statusColumn;

    private BudgetService budgetService;
    private CategoryService categoryService;
    private ObservableList<Budget> budgetList;
    private UUID currentUserId;
    private List<Category> expenseCategories;

    @FXML
    public void initialize() {
        logger.info("Initializing BudgetController");

        budgetService = new BudgetService();
        categoryService = new CategoryService();
        budgetList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        loadBudgetData();

        logger.info("BudgetController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("budgetName"));
        
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "Uncategorized");
        });

        limitColumn.setCellValueFactory(cellData -> {
            BigDecimal limit = cellData.getValue().getAmountLimit();
            return new SimpleStringProperty(String.format("$%,.2f", limit));
        });
        limitColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        spentColumn.setCellValueFactory(cellData -> {
            BigDecimal spent = cellData.getValue().getSpentAmount();
            return new SimpleStringProperty(String.format("$%,.2f", spent != null ? spent : BigDecimal.ZERO));
        });
        spentColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        remainingColumn.setCellValueFactory(cellData -> {
            BigDecimal remaining = cellData.getValue().getRemainingAmount();
            return new SimpleStringProperty(String.format("$%,.2f", remaining != null ? remaining : BigDecimal.ZERO));
        });
        remainingColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        percentageColumn.setCellValueFactory(cellData -> {
            double percentage = cellData.getValue().getPercentageUsed();
            return new SimpleStringProperty(String.format("%.1f%%", percentage));
        });
        percentageColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status);
        });

        budgetTable.setItems(budgetList);

        // Double-click to edit
        budgetTable.setRowFactory(tv -> {
            TableRow<Budget> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditBudget();
                }
            });
            return row;
        });
    }

    /**
     * Load budget data from database
     */
    private void loadBudgetData() {
        if (currentUserId == null) {
            logger.warn("No user logged in, cannot load budget data");
            return;
        }

        try {
            expenseCategories = categoryService.getExpenseCategories(currentUserId);
            List<Budget> budgets = budgetService.getBudgetsByUser(currentUserId);
            budgetList.setAll(budgets);
            checkAlerts();
        } catch (Exception e) {
            logger.error("Error loading budget data", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load budget data: " + e.getMessage());
        }
    }

    /**
     * Check for budget alerts (80%, 90%, 100%)
     */
    private void checkAlerts() {
        for (Budget budget : budgetList) {
            double percentage = budget.getPercentageUsed();
            String status = budget.getStatus();
            
            if (status.equals("EXCEEDED")) {
                showAlert(Alert.AlertType.WARNING, "Budget Exceeded", 
                    "Budget '" + budget.getBudgetName() + "' has been exceeded!");
            } else if (status.equals("DANGER")) {
                showAlert(Alert.AlertType.WARNING, "Budget Warning", 
                    "Budget '" + budget.getBudgetName() + "' is at " + String.format("%.1f%%", percentage) + "!");
            } else if (status.equals("WARNING")) {
                // Silent warning - just log
                logger.info("Budget '{}' reached {}% threshold", budget.getBudgetName(), percentage);
            }
        }
    }

    @FXML
    private void handleAddBudget() {
        showBudgetDialog(null);
    }

    @FXML
    private void handleEditBudget() {
        Budget selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a budget to edit.");
            return;
        }
        showBudgetDialog(selected);
    }

    @FXML
    private void handleDeleteBudget() {
        Budget selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a budget to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Budget");
        confirm.setContentText("Are you sure you want to delete this budget?\n\n" +
                "Name: " + selected.getBudgetName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (budgetService.deleteBudget(selected.getBudgetId())) {
                budgetList.remove(selected);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Budget deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete budget.");
            }
        }
    }

    /**
     * Show Budget Add/Edit Dialog
     */
    private void showBudgetDialog(Budget existingBudget) {
        Dialog<Budget> dialog = new Dialog<>();
        dialog.setTitle(existingBudget == null ? "Add Budget" : "Edit Budget");
        dialog.setHeaderText(existingBudget == null ? "Enter budget details" : "Update budget details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Budget Name");

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
        categoryCombo.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getCategoryName() : "";
            }
            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.setItems(FXCollections.observableArrayList("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"));
        periodCombo.setValue("MONTHLY");

        DatePicker startDatePicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker endDatePicker = new DatePicker(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        TextField thresholdField = new TextField("80");
        thresholdField.setPromptText("Alert threshold (0-100)");

        if (existingBudget != null) {
            nameField.setText(existingBudget.getBudgetName());
            amountField.setText(existingBudget.getAmountLimit().toString());
            periodCombo.setValue(existingBudget.getPeriod());
            startDatePicker.setValue(existingBudget.getStartDate());
            endDatePicker.setValue(existingBudget.getEndDate());
            thresholdField.setText(String.valueOf(existingBudget.getAlertThreshold()));

            if (existingBudget.getCategoryId() != null) {
                for (Category cat : expenseCategories) {
                    if (cat.getCategoryId().equals(existingBudget.getCategoryId())) {
                        categoryCombo.setValue(cat);
                        break;
                    }
                }
            }
        }

        grid.add(new Label("Budget Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryCombo, 1, 1);
        grid.add(new Label("Amount Limit:"), 0, 2);
        grid.add(amountField, 1, 2);
        grid.add(new Label("Period:"), 0, 3);
        grid.add(periodCombo, 1, 3);
        grid.add(new Label("Start Date:"), 0, 4);
        grid.add(startDatePicker, 1, 4);
        grid.add(new Label("End Date:"), 0, 5);
        grid.add(endDatePicker, 1, 5);
        grid.add(new Label("Alert Threshold (%):"), 0, 6);
        grid.add(thresholdField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (nameField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Budget name is required.");
                        return null;
                    }

                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(amountField.getText().replace(",", ""));
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be greater than 0.");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid amount format.");
                        return null;
                    }

                    int threshold;
                    try {
                        threshold = Integer.parseInt(thresholdField.getText());
                        if (threshold < 0 || threshold > 100) {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", "Threshold must be between 0 and 100.");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid threshold format.");
                        return null;
                    }

                    if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Start date and end date are required.");
                        return null;
                    }

                    if (periodCombo.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Period is required.");
                        return null;
                    }

                    Budget budget = existingBudget != null ? existingBudget : new Budget();
                    budget.setUserId(currentUserId);
                    budget.setBudgetName(nameField.getText().trim());
                    budget.setAmountLimit(amount);
                    budget.setPeriod(periodCombo.getValue());
                    budget.setStartDate(startDatePicker.getValue());
                    budget.setEndDate(endDatePicker.getValue());
                    budget.setAlertThreshold(threshold);

                    if (categoryCombo.getValue() != null) {
                        budget.setCategoryId(categoryCombo.getValue().getCategoryId());
                        budget.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    return budget;
                } catch (Exception e) {
                    logger.error("Error creating budget object", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save budget: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Budget> result = dialog.showAndWait();
        result.ifPresent(budget -> {
            boolean success;
            if (existingBudget == null) {
                success = budgetService.createBudget(budget);
            } else {
                success = budgetService.updateBudget(budget);
            }

            if (success) {
                loadBudgetData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Budget " + (existingBudget == null ? "added" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingBudget == null ? "add" : "update") + " budget.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadBudgetData();
    }
}
