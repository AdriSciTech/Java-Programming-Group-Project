package com.financetracker.controller;

import com.financetracker.model.Category;
import com.financetracker.model.Expense;
import com.financetracker.service.CategoryService;
import com.financetracker.service.ExpenseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for Expense Management View
 */
public class ExpenseController {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);

    // FXML Components
    @FXML private Label totalExpenseLabel;
    @FXML private Label monthlyExpenseLabel;
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> dateColumn;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, String> vendorColumn;
    @FXML private TableColumn<Expense, String> amountColumn;
    @FXML private TableColumn<Expense, String> descriptionColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    // Services
    private ExpenseService expenseService;
    private CategoryService categoryService;

    // Data
    private ObservableList<Expense> expenseList;
    private UUID currentUserId;
    private List<Category> expenseCategories;

    // Date formatter
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        logger.info("Initializing ExpenseController");

        expenseService = new ExpenseService();
        categoryService = new CategoryService();
        expenseList = FXCollections.observableArrayList();

        // Get current user ID from LoginController
        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        setupFilters();
        loadExpenseData();
        updateSummary();

        logger.info("ExpenseController initialized");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Date column
        dateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getExpenseDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "");
        });

        // Category column
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "Uncategorized");
        });

        // Vendor column
        vendorColumn.setCellValueFactory(new PropertyValueFactory<>("vendor"));

        // Amount column with currency formatting
        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            return new SimpleStringProperty(String.format("$%,.2f", amount));
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Description column
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Set table data
        expenseTable.setItems(expenseList);

        // Double-click to edit
        expenseTable.setRowFactory(tv -> {
            TableRow<Expense> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditExpense();
                }
            });
            return row;
        });

        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> handleEditExpense());
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDeleteExpense());
        contextMenu.getItems().addAll(editItem, deleteItem);
        expenseTable.setContextMenu(contextMenu);
    }

    /**
     * Setup filter controls
     */
    private void setupFilters() {
        // Filter combo box options
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    "All Time", "This Month", "Last Month", "This Year", "Custom Range"
            ));
            filterComboBox.setValue("This Month");
            filterComboBox.setOnAction(e -> applyFilter());
        }

        // Date pickers - default to current month
        LocalDate now = LocalDate.now();
        if (fromDatePicker != null) {
            fromDatePicker.setValue(now.withDayOfMonth(1));
            fromDatePicker.setOnAction(e -> applyFilter());
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
            toDatePicker.setOnAction(e -> applyFilter());
        }

        // Search field listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterBySearch(newVal);
            });
        }
    }

    /**
     * Load expense data from database
     */
    private void loadExpenseData() {
        if (currentUserId == null) {
            logger.warn("No user logged in, cannot load expense data");
            return;
        }

        try {
            // Load categories for dropdown
            expenseCategories = categoryService.getExpenseCategories(currentUserId);

            // Apply current filter
            applyFilter();

        } catch (Exception e) {
            logger.error("Error loading expense data", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load expense data: " + e.getMessage());
        }
    }

    /**
     * Apply date filter
     */
    private void applyFilter() {
        if (currentUserId == null || filterComboBox == null) return;

        LocalDate startDate;
        LocalDate endDate;
        LocalDate now = LocalDate.now();

        String filter = filterComboBox.getValue();
        if (filter == null) {
            filter = "This Month"; // Default fallback
        }

        switch (filter) {
            case "This Month":
                startDate = now.withDayOfMonth(1);
                endDate = now.withDayOfMonth(now.lengthOfMonth());
                break;
            case "Last Month":
                LocalDate lastMonth = now.minusMonths(1);
                startDate = lastMonth.withDayOfMonth(1);
                endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
                break;
            case "This Year":
                startDate = now.withDayOfYear(1);
                endDate = now.withDayOfYear(now.lengthOfYear());
                break;
            case "Custom Range":
                startDate = fromDatePicker.getValue();
                endDate = toDatePicker.getValue();
                if (startDate == null) startDate = now.minusYears(1);
                if (endDate == null) endDate = now;
                break;
            case "All Time":
            default:
                startDate = LocalDate.of(2000, 1, 1);
                endDate = now.plusYears(1);
                break;
        }

        // Update date pickers
        if (fromDatePicker != null) fromDatePicker.setValue(startDate);
        if (toDatePicker != null) toDatePicker.setValue(endDate);

        // Load filtered data
        List<Expense> filteredExpenses = expenseService.getExpensesByDateRange(currentUserId, startDate, endDate);
        expenseList.setAll(filteredExpenses);

        updateSummary();
    }

    /**
     * Filter by search text
     */
    private void filterBySearch(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            applyFilter();
            return;
        }

        List<Expense> searchResults = expenseService.searchExpenses(currentUserId, searchText);
        expenseList.setAll(searchResults);
        updateSummary();
    }

    /**
     * Update summary labels
     */
    private void updateSummary() {
        if (currentUserId == null) return;

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // Calculate monthly expenses
        BigDecimal monthlyTotal = expenseService.getTotalExpenses(currentUserId, monthStart, monthEnd);
        if (monthlyExpenseLabel != null) {
            monthlyExpenseLabel.setText(String.format("$%,.2f", monthlyTotal));
        }

        // Calculate total from current filter/table
        BigDecimal tableTotal = BigDecimal.ZERO;
        for (Expense expense : expenseList) {
            if (expense != null && expense.getAmount() != null) {
                tableTotal = tableTotal.add(expense.getAmount());
            }
        }
        if (totalExpenseLabel != null) {
            totalExpenseLabel.setText(String.format("$%,.2f", tableTotal));
        }
    }

    /**
     * Handle Add Expense button click
     */
    @FXML
    private void handleAddExpense() {
        logger.info("Add Expense clicked");
        showExpenseDialog(null);
    }

    /**
     * Handle Edit Expense
     */
    @FXML
    private void handleEditExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an expense entry to edit.");
            return;
        }
        showExpenseDialog(selected);
    }

    /**
     * Handle Delete Expense
     */
    @FXML
    private void handleDeleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an expense entry to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Expense Entry");
        confirm.setContentText("Are you sure you want to delete this expense entry?\n\n" +
                "Vendor: " + selected.getVendor() + "\n" +
                "Amount: " + String.format("$%,.2f", selected.getAmount()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (expenseService.deleteExpense(selected.getExpenseId())) {
                expenseList.remove(selected);
                updateSummary();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense entry deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete expense entry.");
            }
        }
    }

    /**
     * Handle Refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadExpenseData();
    }

    /**
     * Show Expense Add/Edit Dialog
     */
    private void showExpenseDialog(Expense existingExpense) {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle(existingExpense == null ? "Add Expense" : "Edit Expense");
        dialog.setHeaderText(existingExpense == null ? "Enter expense details" : "Update expense details");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField vendorField = new TextField();
        vendorField.setPromptText("e.g., Amazon, Grocery Store");

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        DatePicker datePicker = new DatePicker(LocalDate.now());

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

        // Button to create new category
        Button newCategoryBtn = new Button("+ New Category");
        newCategoryBtn.setOnAction(e -> {
            Category newCategory = showNewCategoryDialog(Category.CategoryType.EXPENSE);
            if (newCategory != null) {
                expenseCategories = categoryService.getExpenseCategories(currentUserId);
                categoryCombo.setItems(FXCollections.observableArrayList(expenseCategories));
                categoryCombo.setValue(newCategory);
            }
        });

        HBox categoryBox = new HBox(5);
        categoryBox.getChildren().addAll(categoryCombo, newCategoryBtn);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional description");
        descriptionArea.setPrefRowCount(2);

        ComboBox<String> paymentMethodCombo = new ComboBox<>();
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                "Cash", "Credit Card", "Debit Card", "Bank Transfer", "PayPal", "Other"
        ));

        // Populate fields if editing
        if (existingExpense != null) {
            vendorField.setText(existingExpense.getVendor());
            amountField.setText(existingExpense.getAmount().toString());
            datePicker.setValue(existingExpense.getExpenseDate());
            descriptionArea.setText(existingExpense.getDescription());
            paymentMethodCombo.setValue(existingExpense.getPaymentMethod());

            // Find and select category
            if (existingExpense.getCategoryId() != null) {
                for (Category cat : expenseCategories) {
                    if (cat.getCategoryId().equals(existingExpense.getCategoryId())) {
                        categoryCombo.setValue(cat);
                        break;
                    }
                }
            }
        }

        // Add fields to grid
        grid.add(new Label("Vendor:"), 0, 0);
        grid.add(vendorField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryBox, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(new Label("Payment Method:"), 0, 5);
        grid.add(paymentMethodCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate
                    if (vendorField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Vendor is required.");
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

                    if (datePicker.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Date is required.");
                        return null;
                    }

                    Expense expense = existingExpense != null ? existingExpense : new Expense();
                    expense.setUserId(currentUserId);
                    expense.setVendor(vendorField.getText().trim());
                    expense.setAmount(amount);
                    expense.setExpenseDate(datePicker.getValue());
                    expense.setDescription(descriptionArea.getText().trim());
                    expense.setPaymentMethod(paymentMethodCombo.getValue());

                    if (categoryCombo.getValue() != null) {
                        expense.setCategoryId(categoryCombo.getValue().getCategoryId());
                        expense.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    return expense;
                } catch (Exception e) {
                    logger.error("Error creating expense object", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save expense: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Expense> result = dialog.showAndWait();
        result.ifPresent(expense -> {
            boolean success;
            if (existingExpense == null) {
                success = expenseService.createExpense(expense);
            } else {
                success = expenseService.updateExpense(expense);
            }

            if (success) {
                loadExpenseData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Expense " + (existingExpense == null ? "added" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingExpense == null ? "add" : "update") + " expense.");
            }
        });
    }

    /**
     * Show dialog to create a new category
     */
    private Category showNewCategoryDialog(Category.CategoryType categoryType) {
        Dialog<Category> dialog = new Dialog<>();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Create a new " + categoryType.name().toLowerCase() + " category");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Category Name");

        TextField colorField = new TextField();
        colorField.setPromptText("Color Code (e.g., #FF5733)");

        TextField iconField = new TextField();
        iconField.setPromptText("Icon Name (optional)");

        grid.add(new Label("Category Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Color Code:"), 0, 1);
        grid.add(colorField, 1, 1);
        grid.add(new Label("Icon Name:"), 0, 2);
        grid.add(iconField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String categoryName = nameField.getText().trim();
                if (categoryName.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Category name is required.");
                    return null;
                }

                // Check if category already exists
                if (categoryService.categoryNameExists(currentUserId, categoryName, categoryType)) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", 
                            "A category with this name already exists for " + categoryType.name().toLowerCase() + ".");
                    return null;
                }

                Category category = new Category();
                category.setCategoryId(UUID.randomUUID());
                category.setUserId(currentUserId);
                category.setCategoryName(categoryName);
                category.setCategoryType(categoryType);
                category.setColorCode(colorField.getText().trim().isEmpty() ? null : colorField.getText().trim());
                category.setIconName(iconField.getText().trim().isEmpty() ? null : iconField.getText().trim());
                category.setDefault(false);

                if (categoryService.createCategory(category)) {
                    return category;
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to create category.");
                    return null;
                }
            }
            return null;
        });

        Optional<Category> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Set the current user ID (called from DashboardController)
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadExpenseData();
    }
}
