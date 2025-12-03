package com.financetracker.controller;

import com.financetracker.model.Account;
import com.financetracker.model.Transfer;
import com.financetracker.service.AccountService;
import com.financetracker.service.TransferService;
import com.financetracker.util.UIUtils;
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
 * Controller for Transfer Management View
 */
public class TransferController {
    private static final Logger logger = LoggerFactory.getLogger(TransferController.class);

    @FXML private TableView<Transfer> transferTable;
    @FXML private TableColumn<Transfer, String> dateColumn;
    @FXML private TableColumn<Transfer, String> fromColumn;
    @FXML private TableColumn<Transfer, String> toColumn;
    @FXML private TableColumn<Transfer, String> amountColumn;
    @FXML private TableColumn<Transfer, String> descriptionColumn;

    private TransferService transferService;
    private AccountService accountService;
    private ObservableList<Transfer> transferList;
    private ObservableList<Account> accountList;
    private UUID currentUserId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    public void initialize() {
        logger.info("Initializing TransferController");

        transferService = new TransferService();
        accountService = new AccountService();
        transferList = FXCollections.observableArrayList();
        accountList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        loadTransferData();
        loadAccounts();

        logger.info("TransferController initialized");
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getTransferDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "");
        });

        fromColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getFromAccountName();
            return new SimpleStringProperty(name != null ? name : "N/A");
        });

        toColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getToAccountName();
            return new SimpleStringProperty(name != null ? name : "N/A");
        });

        amountColumn.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getAmount();
            return new SimpleStringProperty(String.format("$%,.2f", amount));
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        transferTable.setItems(transferList);

        transferTable.setRowFactory(tv -> {
            TableRow<Transfer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditTransfer();
                }
            });
            return row;
        });
    }

    private void loadTransferData() {
        if (currentUserId == null) return;

        try {
            List<Transfer> transfers = transferService.getTransfersByUser(currentUserId);
            transferList.setAll(transfers);
        } catch (Exception e) {
            logger.error("Error loading transfer data", e);
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load transfer data: " + e.getMessage());
        }
    }

    private void loadAccounts() {
        if (currentUserId == null) return;

        try {
            List<Account> accounts = accountService.getActiveAccounts(currentUserId);
            accountList.setAll(accounts);
        } catch (Exception e) {
            logger.error("Error loading accounts", e);
        }
    }

    @FXML
    private void handleAddTransfer() {
        showTransferDialog(null);
    }

    @FXML
    private void handleEditTransfer() {
        Transfer selected = transferTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a transfer to edit.");
            return;
        }
        showTransferDialog(selected);
    }

    @FXML
    private void handleDeleteTransfer() {
        Transfer selected = transferTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a transfer to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Transfer");
        confirm.setContentText("Are you sure you want to delete this transfer?\n\n" +
                "This will reverse the account balance changes.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (transferService.deleteTransfer(selected.getTransferId())) {
                transferList.remove(selected);
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Transfer deleted successfully.");
            } else {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete transfer.");
            }
        }
    }

    private void showTransferDialog(Transfer existingTransfer) {
        Dialog<Transfer> dialog = new Dialog<>();
        dialog.setTitle(existingTransfer == null ? "Add Transfer" : "Edit Transfer");
        dialog.setHeaderText(existingTransfer == null ? "Enter transfer details" : "Update transfer details");
        
        // Set owner window to prevent black tab glitch
        if (transferTable.getScene() != null && transferTable.getScene().getWindow() != null) {
            dialog.initOwner(transferTable.getScene().getWindow());
        }
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        
        // Apply stylesheet
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Account> fromAccountCombo = new ComboBox<>();
        fromAccountCombo.setItems(accountList);
        fromAccountCombo.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                return account != null ? account.getAccountName() : "";
            }
            @Override
            public Account fromString(String string) {
                return null;
            }
        });

        ComboBox<Account> toAccountCombo = new ComboBox<>();
        toAccountCombo.setItems(accountList);
        toAccountCombo.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                return account != null ? account.getAccountName() : "";
            }
            @Override
            public Account fromString(String string) {
                return null;
            }
        });

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional description");
        descriptionArea.setPrefRowCount(2);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList("INTERNAL", "EXTERNAL"));
        typeCombo.setValue("INTERNAL");

        if (existingTransfer != null) {
            amountField.setText(existingTransfer.getAmount().toString());
            datePicker.setValue(existingTransfer.getTransferDate());
            descriptionArea.setText(existingTransfer.getDescription());
            typeCombo.setValue(existingTransfer.getTransferType());

            for (Account acc : accountList) {
                if (acc.getAccountId().equals(existingTransfer.getFromAccountId())) {
                    fromAccountCombo.setValue(acc);
                }
                if (acc.getAccountId().equals(existingTransfer.getToAccountId())) {
                    toAccountCombo.setValue(acc);
                }
            }
        }

        grid.add(new Label("From Account:"), 0, 0);
        grid.add(fromAccountCombo, 1, 0);
        grid.add(new Label("To Account:"), 0, 1);
        grid.add(toAccountCombo, 1, 1);
        grid.add(new Label("Amount:"), 0, 2);
        grid.add(amountField, 1, 2);
        grid.add(new Label("Date:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(new Label("Transfer Type:"), 0, 5);
        grid.add(typeCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (fromAccountCombo.getValue() == null || toAccountCombo.getValue() == null) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select both accounts.");
                        return null;
                    }

                    if (fromAccountCombo.getValue().getAccountId().equals(toAccountCombo.getValue().getAccountId())) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "From and To accounts must be different.");
                        return null;
                    }

                    BigDecimal amount;
                    try {
                        amount = UIUtils.parseBigDecimal(amountField.getText());
                        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                            UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be greater than 0.");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid amount format.");
                        return null;
                    }

                    if (datePicker.getValue() == null) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Date is required.");
                        return null;
                    }

                    if (typeCombo.getValue() == null) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Transfer type is required.");
                        return null;
                    }

                    Transfer transfer = existingTransfer != null ? existingTransfer : new Transfer();
                    transfer.setUserId(currentUserId);
                    transfer.setFromAccountId(fromAccountCombo.getValue().getAccountId());
                    transfer.setToAccountId(toAccountCombo.getValue().getAccountId());
                    transfer.setAmount(amount);
                    transfer.setTransferDate(datePicker.getValue());
                    transfer.setDescription(descriptionArea.getText().trim());
                    transfer.setTransferType(typeCombo.getValue());

                    return transfer;
                } catch (Exception e) {
                    logger.error("Error creating transfer object", e);
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to save transfer: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Transfer> result = dialog.showAndWait();
        result.ifPresent(transfer -> {
            boolean success;
            if (existingTransfer == null) {
                success = transferService.createTransfer(transfer);
            } else {
                success = transferService.updateTransfer(transfer);
            }

            if (success) {
                loadTransferData();
                loadAccounts(); // Refresh accounts to show updated balances
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Transfer " + (existingTransfer == null ? "added" : "updated") + " successfully.");
            } else {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingTransfer == null ? "add" : "update") + " transfer.");
            }
        });
    }


    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadTransferData();
        loadAccounts();
    }
}
