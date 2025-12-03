package com.financetracker.controller;

import com.financetracker.model.Account;
import com.financetracker.model.Investment;
import com.financetracker.service.AccountService;
import com.financetracker.service.InvestmentService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for Investment Management View
 */
public class InvestmentController {
    private static final Logger logger = LoggerFactory.getLogger(InvestmentController.class);

    @FXML private Label portfolioValueLabel;
    @FXML private Label totalROILabel;
    @FXML private TableView<Investment> investmentTable;
    @FXML private TableColumn<Investment, String> nameColumn;
    @FXML private TableColumn<Investment, String> typeColumn;
    @FXML private TableColumn<Investment, String> symbolColumn;
    @FXML private TableColumn<Investment, String> quantityColumn;
    @FXML private TableColumn<Investment, String> purchasePriceColumn;
    @FXML private TableColumn<Investment, String> currentPriceColumn;
    @FXML private TableColumn<Investment, String> roiColumn;

    private InvestmentService investmentService;
    private AccountService accountService;
    private ObservableList<Investment> investmentList;
    private ObservableList<Account> accountList;
    private UUID currentUserId;

    @FXML
    public void initialize() {
        logger.info("Initializing InvestmentController");

        investmentService = new InvestmentService();
        accountService = new AccountService();
        investmentList = FXCollections.observableArrayList();
        accountList = FXCollections.observableArrayList();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        setupTable();
        loadInvestmentData();
        loadAccounts();
        updateSummary();

        logger.info("InvestmentController initialized");
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("investmentName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("investmentType"));
        symbolColumn.setCellValueFactory(new PropertyValueFactory<>("symbol"));

        quantityColumn.setCellValueFactory(cellData -> {
            BigDecimal qty = cellData.getValue().getQuantity();
            return new SimpleStringProperty(String.format("%.2f", qty));
        });
        quantityColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        purchasePriceColumn.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getPurchasePrice();
            return new SimpleStringProperty(String.format("$%,.2f", price));
        });
        purchasePriceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        currentPriceColumn.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getCurrentPrice();
            return new SimpleStringProperty(String.format("$%,.2f", price));
        });
        currentPriceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        roiColumn.setCellValueFactory(cellData -> {
            BigDecimal roi = cellData.getValue().getROI();
            return new SimpleStringProperty(String.format("$%,.2f", roi));
        });
        roiColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        investmentTable.setItems(investmentList);

        investmentTable.setRowFactory(tv -> {
            TableRow<Investment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditInvestment();
                }
            });
            return row;
        });
    }

    private void loadInvestmentData() {
        if (currentUserId == null) return;

        try {
            List<Investment> investments = investmentService.getInvestmentsByUser(currentUserId);
            investmentList.setAll(investments);
            updateSummary();
        } catch (Exception e) {
            logger.error("Error loading investment data", e);
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load investment data: " + e.getMessage());
        }
    }

    private void loadAccounts() {
        if (currentUserId == null) return;

        try {
            List<Account> accounts = accountService.getAccountsByUser(currentUserId);
            accountList.setAll(accounts);
        } catch (Exception e) {
            logger.error("Error loading accounts", e);
        }
    }

    private void updateSummary() {
        if (currentUserId == null) return;

        BigDecimal totalValue = investmentService.getPortfolioTotalValue(currentUserId);
        BigDecimal totalROI = investmentService.getPortfolioTotalROI(currentUserId);

        if (portfolioValueLabel != null) {
            portfolioValueLabel.setText(String.format("$%,.2f", totalValue));
        }
        if (totalROILabel != null) {
            totalROILabel.setText(String.format("$%,.2f", totalROI));
        }
    }

    @FXML
    private void handleAddInvestment() {
        showInvestmentDialog(null);
    }

    @FXML
    private void handleEditInvestment() {
        Investment selected = investmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an investment to edit.");
            return;
        }
        showInvestmentDialog(selected);
    }

    @FXML
    private void handleDeleteInvestment() {
        Investment selected = investmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an investment to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Investment");
        confirm.setContentText("Are you sure you want to delete this investment?\n\n" +
                "Name: " + selected.getInvestmentName());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (investmentService.deleteInvestment(selected.getInvestmentId())) {
                investmentList.remove(selected);
                updateSummary();
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Investment deleted successfully.");
            } else {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete investment.");
            }
        }
    }

    private void showInvestmentDialog(Investment existingInvestment) {
        Dialog<Investment> dialog = new Dialog<>();
        dialog.setTitle(existingInvestment == null ? "Add Investment" : "Edit Investment");
        dialog.setHeaderText(existingInvestment == null ? "Enter investment details" : "Update investment details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Investment Name");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(
                "STOCK", "BOND", "MUTUAL_FUND", "ETF", "CRYPTO", "REAL_ESTATE", "RETIREMENT", "OTHER"
        ));
        typeCombo.setValue("STOCK");

        TextField symbolField = new TextField();
        symbolField.setPromptText("Symbol (e.g., AAPL)");

        TextField quantityField = new TextField();
        quantityField.setPromptText("0.00");

        TextField purchasePriceField = new TextField();
        purchasePriceField.setPromptText("0.00");

        TextField currentPriceField = new TextField();
        currentPriceField.setPromptText("0.00");

        DatePicker purchaseDatePicker = new DatePicker(LocalDate.now());

        ComboBox<Account> accountCombo = new ComboBox<>();
        accountCombo.setItems(accountList);
        accountCombo.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                return account != null ? account.getAccountName() : "";
            }
            @Override
            public Account fromString(String string) {
                return null;
            }
        });

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Optional description");
        descriptionArea.setPrefRowCount(2);

        if (existingInvestment != null) {
            nameField.setText(existingInvestment.getInvestmentName());
            typeCombo.setValue(existingInvestment.getInvestmentType());
            symbolField.setText(existingInvestment.getSymbol());
            quantityField.setText(existingInvestment.getQuantity().toString());
            purchasePriceField.setText(existingInvestment.getPurchasePrice().toString());
            currentPriceField.setText(existingInvestment.getCurrentPrice().toString());
            purchaseDatePicker.setValue(existingInvestment.getPurchaseDate());
            descriptionArea.setText(existingInvestment.getDescription());

            if (existingInvestment.getAccountId() != null) {
                for (Account acc : accountList) {
                    if (acc.getAccountId().equals(existingInvestment.getAccountId())) {
                        accountCombo.setValue(acc);
                        break;
                    }
                }
            }
        }

        grid.add(new Label("Investment Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Symbol:"), 0, 2);
        grid.add(symbolField, 1, 2);
        grid.add(new Label("Quantity:"), 0, 3);
        grid.add(quantityField, 1, 3);
        grid.add(new Label("Purchase Price:"), 0, 4);
        grid.add(purchasePriceField, 1, 4);
        grid.add(new Label("Current Price:"), 0, 5);
        grid.add(currentPriceField, 1, 5);
        grid.add(new Label("Purchase Date:"), 0, 6);
        grid.add(purchaseDatePicker, 1, 6);
        grid.add(new Label("Account:"), 0, 7);
        grid.add(accountCombo, 1, 7);
        grid.add(new Label("Description:"), 0, 8);
        grid.add(descriptionArea, 1, 8);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (nameField.getText().isEmpty()) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Investment name is required.");
                        return null;
                    }

                    BigDecimal quantity, purchasePrice, currentPrice;
                    try {
                        quantity = UIUtils.parseBigDecimal(quantityField.getText());
                        purchasePrice = UIUtils.parseBigDecimal(purchasePriceField.getText());
                        currentPrice = UIUtils.parseBigDecimal(currentPriceField.getText());
                        if (quantity.compareTo(BigDecimal.ZERO) <= 0 || 
                            purchasePrice.compareTo(BigDecimal.ZERO) <= 0 ||
                            currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                            UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "All values must be greater than 0.");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid number format.");
                        return null;
                    }

                    if (typeCombo.getValue() == null) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Validation Error", "Investment type is required.");
                        return null;
                    }

                    Investment investment = existingInvestment != null ? existingInvestment : new Investment();
                    investment.setUserId(currentUserId);
                    investment.setInvestmentName(nameField.getText().trim());
                    investment.setInvestmentType(typeCombo.getValue());
                    investment.setSymbol(symbolField.getText().trim());
                    investment.setQuantity(quantity);
                    investment.setPurchasePrice(purchasePrice);
                    investment.setCurrentPrice(currentPrice);
                    // Purchase date is optional, so null is allowed
                    investment.setPurchaseDate(purchaseDatePicker.getValue());
                    investment.setDescription(descriptionArea.getText().trim());

                    if (accountCombo.getValue() != null) {
                        investment.setAccountId(accountCombo.getValue().getAccountId());
                    }

                    return investment;
                } catch (Exception e) {
                    logger.error("Error creating investment object", e);
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to save investment: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Investment> result = dialog.showAndWait();
        result.ifPresent(investment -> {
            boolean success;
            if (existingInvestment == null) {
                success = investmentService.createInvestment(investment);
            } else {
                success = investmentService.updateInvestment(investment);
            }

            if (success) {
                loadInvestmentData();
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Investment " + (existingInvestment == null ? "added" : "updated") + " successfully.");
            } else {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to " + (existingInvestment == null ? "add" : "update") + " investment.");
            }
        });
    }


    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadInvestmentData();
        loadAccounts();
    }
}
