package com.financetracker.controller;

import com.financetracker.model.Account;
import com.financetracker.service.AccountService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for Account Management View
 */
public class AccountController {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @FXML private TableView<Account> accountTable;
    @FXML private TableColumn<Account, String> nameColumn;
    @FXML private TableColumn<Account, String> typeColumn;
    @FXML private TableColumn<Account, String> balanceColumn;
    @FXML private TableColumn<Account, String> currencyColumn;
    @FXML private TableColumn<Account, String> institutionColumn;
    @FXML private TableColumn<Account, String> statusColumn;

    private AccountService accountService;
    private ObservableList<Account> accountList;
    private UUID currentUserId;

    @FXML
    public void initialize() {
        logger.info("Initializing AccountController");

        accountService = new AccountService();
        accountList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        loadAccountData();

        logger.info("AccountController initialized");
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));

        balanceColumn.setCellValueFactory(cellData -> {
            BigDecimal balance = cellData.getValue().getBalance();
            return new SimpleStringProperty(String.format("$%,.2f", balance));
        });
        balanceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        currencyColumn.setCellValueFactory(new PropertyValueFactory<>("currency"));
        institutionColumn.setCellValueFactory(new PropertyValueFactory<>("institutionName"));

        statusColumn.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().isActive();
            return new SimpleStringProperty(active ? "Active" : "Inactive");
        });

        accountTable.setItems(accountList);

        accountTable.setRowFactory(tv -> {
            TableRow<Account> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditAccount();
                }
            });
            return row;
        });
    }

    private void loadAccountData() {
        if (currentUserId == null) return;

        try {
            List<Account> accounts = accountService.getAccountsByUser(currentUserId);
            accountList.setAll(accounts);
        } catch (Exception e) {
            logger.error("Error loading account data", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load account data: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddAccount() {
        showAccountDialog(null);
    }

    @FXML
    private void handleEditAccount() {
        Account selected = accountTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account to edit.");
            return;
        }
        showAccountDialog(selected);
    }

    @FXML
    private void handleDeleteAccount() {
        Account selected = accountTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Account");
        confirm.setContentText("Are you sure you want to delete this account?\n\n" +
                "Name: " + selected.getAccountName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (accountService.deleteAccount(selected.getAccountId())) {
                accountList.remove(selected);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete account.");
            }
        }
    }

    @FXML
    private void handleToggleActive() {
        Account selected = accountTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an account.");
            return;
        }

        selected.setActive(!selected.isActive());
        if (accountService.updateAccount(selected)) {
            accountTable.refresh();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Account status updated.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update account status.");
        }
    }

    private void showAccountDialog(Account existingAccount) {
        Dialog<Account> dialog = new Dialog<>();
        dialog.setTitle(existingAccount == null ? "Add Account" : "Edit Account");
        dialog.setHeaderText(existingAccount == null ? "Enter account details" : "Update account details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Account Name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(
                "CHECKING", "SAVINGS", "CREDIT_CARD", "CASH", "INVESTMENT", "OTHER"
        ));
        typeCombo.setValue("CHECKING");

        TextField balanceField = new TextField("0.00");
        balanceField.setPromptText("0.00");

        TextField currencyField = new TextField("USD");
        currencyField.setPromptText("USD");

        TextField institutionField = new TextField();
        institutionField.setPromptText("Bank/Institution Name");

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Account Number (optional)");

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);

        if (existingAccount != null) {
            nameField.setText(existingAccount.getAccountName());
            typeCombo.setValue(existingAccount.getAccountType());
            balanceField.setText(existingAccount.getBalance().toString());
            currencyField.setText(existingAccount.getCurrency());
            institutionField.setText(existingAccount.getInstitutionName());
            accountNumberField.setText(existingAccount.getAccountNumber());
            activeCheck.setSelected(existingAccount.isActive());
        }

        grid.add(new Label("Account Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Account Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Balance:"), 0, 2);
        grid.add(balanceField, 1, 2);
        grid.add(new Label("Currency:"), 0, 3);
        grid.add(currencyField, 1, 3);
        grid.add(new Label("Institution:"), 0, 4);
        grid.add(institutionField, 1, 4);
        grid.add(new Label("Account Number:"), 0, 5);
        grid.add(accountNumberField, 1, 5);
        grid.add(activeCheck, 0, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (nameField.getText().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Account name is required.");
                        return null;
                    }

                    BigDecimal balance;
                    try {
                        balance = new BigDecimal(balanceField.getText().replace(",", ""));
                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid balance format.");
                        return null;
                    }

                    if (typeCombo.getValue() == null) {
                        showAlert(Alert.AlertType.ERROR, "Validation Error", "Account type is required.");
                        return null;
                    }

                    Account account = existingAccount != null ? existingAccount : new Account();
                    account.setUserId(currentUserId);
                    account.setAccountName(nameField.getText().trim());
                    account.setAccountType(typeCombo.getValue());
                    account.setBalance(balance);
                    account.setCurrency(currencyField.getText().trim());
                    account.setInstitutionName(institutionField.getText().trim());
                    account.setAccountNumber(accountNumberField.getText().trim());
                    account.setActive(activeCheck.isSelected());

                    return account;
                } catch (Exception e) {
                    logger.error("Error creating account object", e);
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save account: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Account> result = dialog.showAndWait();
        result.ifPresent(account -> {
            boolean success;
            if (existingAccount == null) {
                success = accountService.createAccount(account);
            } else {
                success = accountService.updateAccount(account);
            }

            if (success) {
                loadAccountData();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Account " + (existingAccount == null ? "added" : "updated") + " successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingAccount == null ? "add" : "update") + " account.");
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
        loadAccountData();
    }
}
