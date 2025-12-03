package com.financetracker.controller;

import com.financetracker.model.Bill;
import com.financetracker.model.Category;
import com.financetracker.service.BillService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for Bills & Subscriptions Management View
 */
public class BillController {
    private static final Logger logger = LoggerFactory.getLogger(BillController.class);

    @FXML private TableView<Bill> billTable;
    @FXML private TableColumn<Bill, String> nameColumn;
    @FXML private TableColumn<Bill, String> amountColumn;
    @FXML private TableColumn<Bill, String> cycleColumn;
    @FXML private TableColumn<Bill, String> nextPaymentColumn;
    @FXML private TableColumn<Bill, String> statusColumn;

    private BillService billService;
    private CategoryService categoryService;
    private ObservableList<Bill> billList;
    private UUID currentUserId;
    private List<Category> expenseCategories;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        logger.info("Initializing BillController");

        billService = new BillService();
        categoryService = new CategoryService();
        billList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        loadBillData();

        logger.info("BillController initialized");
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            return new SimpleStringProperty(String.format("$%,.2f", amount));
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        cycleColumn.setCellValueFactory(new PropertyValueFactory<>("billingCycle"));

        nextPaymentColumn.setCellValueFactory(cellData -> {
            LocalDate next = cellData.getValue().getNextPaymentDate();
            return new SimpleStringProperty(next != null ? next.format(DATE_FORMATTER) : "N/A");
        });

        statusColumn.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().isActive();
            return new SimpleStringProperty(active ? "Active" : "Inactive");
        });

        billTable.setItems(billList);

        billTable.setRowFactory(tv -> {
            TableRow<Bill> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditBill();
                }
            });
            return row;
        });
    }

    private void loadBillData() {
        if (currentUserId == null) return;

        try {
            expenseCategories = categoryService.getExpenseCategories(currentUserId);
            List<Bill> bills = billService.getBillsByUser(currentUserId);
            billList.setAll(bills);
        } catch (Exception e) {
            logger.error("Error loading bill data", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load bill data: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddBill() {
        showBillDialog(null);
    }

    @FXML
    private void handleEditBill() {
        Bill selected = billTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to edit.");
            return;
        }
        showBillDialog(selected);
    }

    @FXML
    private void handleDeleteBill() {
        Bill selected = billTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Bill");
        confirm.setContentText("Are you sure you want to delete this bill?\n\n" +
                "Name: " + selected.getName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (billService.deleteBill(selected.getBillId())) {
                billList.remove(selected);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Bill deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete bill.");
            }
        }
    }

    @FXML
    private void handleCancelBill() {
        Bill selected = billTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a bill to cancel.");
            return;
        }

        if (billService.cancelBill(selected.getBillId())) {
            selected.setActive(false);
            billTable.refresh();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Bill cancelled successfully.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to cancel bill.");
        }
    }

    private void showBillDialog(Bill existingBill) {
        Dialog<Bill> dialog = new Dialog<>();
        dialog.setTitle(existingBill == null ? "Add Bill" : "Edit Bill");
        dialog.setHeaderText(existingBill == null ? "Enter bill details" : "Update bill details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Bill Name");

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

        ComboBox<String> cycleCombo = new ComboBox<>();
        cycleCombo.setItems(FXCollections.observableArrayList("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"));
        cycleCombo.setValue("MONTHLY");

        TextField dueDayField = new TextField("1");
        dueDayField.setPromptText("Day of month (1-28)");

        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker();

        TextField reminderField = new TextField("3");
        reminderField.setPromptText("Days before due date");

        TextField vendorField = new TextField();
        vendorField.setPromptText("Vendor name");

        if (existingBill != null) {
            nameField.setText(existingBill.getName());
            amountField.setText(existingBill.getAmount().toString());
            cycleCombo.setValue(existingBill.getBillingCycle());
            dueDayField.setText(String.valueOf(existingBill.getDueDay()));
            startDatePicker.setValue(existingBill.getStartDate());
            endDatePicker.setValue(existingBill.getEndDate());
            reminderField.setText(String.valueOf(existingBill.getReminderDays()));
            vendorField.setText(existingBill.getVendor());

            if (existingBill.getCategoryId() != null) {
                for (Category cat : expenseCategories) {
                    if (cat.getCategoryId().equals(existingBill.getCategoryId())) {
                        categoryCombo.setValue(cat);
                        break;
                    }
                }
            }
        }

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryCombo, 1, 1);
        grid.add(new Label("Amount:"), 0, 2);
        grid.add(amountField, 1, 2);
        grid.add(new Label("Billing Cycle:"), 0, 3);
        grid.add(cycleCombo, 1, 3);
        grid.add(new Label("Due Day:"), 0, 4);
        grid.add(dueDayField, 1, 4);
        grid.add(new Label("Start Date:"), 0, 5);
        grid.add(startDatePicker, 1, 5);
        grid.add(new Label("End Date (optional):"), 0, 6);
        grid.add(endDatePicker, 1, 6);
        grid.add(new Label("Reminder Days:"), 0, 7);
        grid.add(reminderField, 1, 7);
        grid.add(new Label("Vendor:"), 0, 8);
        grid.add(vendorField, 1, 8);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (nameField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Name is required.");
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

                    int dueDay;
                    try {
                        dueDay = Integer.parseInt(dueDayField.getText());
                        if (dueDay < 1 || dueDay > 28) {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", "Due day must be between 1 and 28.");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid due day format.");
                        return null;
                    }

                    int reminderDays;
                    try {
                        String reminderText = reminderField.getText().trim();
                        if (reminderText.isEmpty()) {
                            reminderDays = 3; // Default
                        } else {
                            reminderDays = Integer.parseInt(reminderText);
                            if (reminderDays < 0) {
                                showAlert(Alert.AlertType.ERROR, "Validation Error", "Reminder days must be 0 or greater.");
                                return null;
                            }
                        }
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid reminder days format.");
                        return null;
                    }

                    if (startDatePicker.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Start date is required.");
                        return null;
                    }

                    if (cycleCombo.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Billing cycle is required.");
                        return null;
                    }

                    Bill bill = existingBill != null ? existingBill : new Bill();
                    bill.setUserId(currentUserId);
                    bill.setName(nameField.getText().trim());
                    bill.setAmount(amount);
                    bill.setBillingCycle(cycleCombo.getValue());
                    bill.setDueDay(dueDay);
                    bill.setStartDate(startDatePicker.getValue());
                    bill.setEndDate(endDatePicker.getValue());
                    bill.setReminderDays(reminderDays);
                    bill.setVendor(vendorField.getText().trim());

                    if (categoryCombo.getValue() != null) {
                        bill.setCategoryId(categoryCombo.getValue().getCategoryId());
                        bill.setCategoryName(categoryCombo.getValue().getCategoryName());
                    }

                    return bill;
                } catch (Exception e) {
                    logger.error("Error creating bill object", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save bill: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Bill> result = dialog.showAndWait();
        result.ifPresent(bill -> {
            boolean success;
            if (existingBill == null) {
                success = billService.createBill(bill);
            } else {
                success = billService.updateBill(bill);
            }

            if (success) {
                loadBillData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Bill " + (existingBill == null ? "added" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingBill == null ? "add" : "update") + " bill.");
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
        loadBillData();
    }
}
