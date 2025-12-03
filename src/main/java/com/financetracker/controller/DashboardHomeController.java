package com.financetracker.controller;

import com.financetracker.model.Bill;
import com.financetracker.service.AccountService;
import com.financetracker.service.BillService;
import com.financetracker.service.ExpenseService;
import com.financetracker.service.IncomeService;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Dashboard Home View
 */
public class DashboardHomeController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardHomeController.class);

    @FXML private Label totalBalanceLabel;
    @FXML private Label monthlyIncomeLabel;
    @FXML private Label monthlyExpenseLabel;
    @FXML private Label monthlySavingsLabel;
    @FXML private Label nextBillLabel;
    @FXML private PieChart expensePieChart;

    private IncomeService incomeService;
    private ExpenseService expenseService;
    private BillService billService;
    private AccountService accountService;
    private UUID currentUserId;

    @FXML
    public void initialize() {
        logger.info("Initializing DashboardHomeController");

        incomeService = new IncomeService();
        expenseService = new ExpenseService();
        billService = new BillService();
        accountService = new AccountService();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        loadDashboardData();

        logger.info("DashboardHomeController initialized");
    }

    /**
     * Load all dashboard data
     */
    private void loadDashboardData() {
        if (currentUserId == null) {
            logger.warn("No user logged in, cannot load dashboard data");
            return;
        }

        try {
            loadSummaryCards();
            loadNextBill();
            loadExpensePieChart();
        } catch (Exception e) {
            logger.error("Error loading dashboard data", e);
        }
    }

    /**
     * Load summary cards with monthly totals
     */
    private void loadSummaryCards() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // Calculate monthly income
        BigDecimal monthlyIncome = incomeService.getTotalIncome(currentUserId, monthStart, monthEnd);
        if (monthlyIncomeLabel != null) {
            monthlyIncomeLabel.setText(String.format("$%,.2f", monthlyIncome));
        }

        // Calculate monthly expenses
        BigDecimal monthlyExpenses = expenseService.getTotalExpenses(currentUserId, monthStart, monthEnd);
        if (monthlyExpenseLabel != null) {
            monthlyExpenseLabel.setText(String.format("$%,.2f", monthlyExpenses));
        }

        // Calculate monthly savings
        BigDecimal monthlySavings = monthlyIncome.subtract(monthlyExpenses);
        if (monthlySavingsLabel != null) {
            monthlySavingsLabel.setText(String.format("$%,.2f", monthlySavings));
        }

        // Calculate total balance from all accounts
        List<com.financetracker.model.Account> accounts = accountService.getAccountsByUser(currentUserId);
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (com.financetracker.model.Account account : accounts) {
            if (account.isActive()) {
                totalBalance = totalBalance.add(account.getBalance());
            }
        }
        if (totalBalanceLabel != null) {
            totalBalanceLabel.setText(String.format("$%,.2f", totalBalance));
        }
    }

    /**
     * Load next upcoming bill
     */
    private void loadNextBill() {
        if (nextBillLabel == null) return;

        try {
            List<Bill> upcomingBills = billService.getUpcomingBills(currentUserId, 30);
            if (upcomingBills != null && !upcomingBills.isEmpty()) {
                Bill nextBill = upcomingBills.get(0);
                LocalDate nextPaymentDate = nextBill.getNextPaymentDate();
                if (nextPaymentDate != null) {
                    nextBillLabel.setText(nextBill.getName() + " - " + 
                        String.format("$%,.2f", nextBill.getAmount()) + 
                        " due " + nextPaymentDate.format(
                            java.time.format.DateTimeFormatter.ofPattern("MMM dd")));
                } else {
                    nextBillLabel.setText(nextBill.getName() + " - " + 
                        String.format("$%,.2f", nextBill.getAmount()) + " (ended)");
                }
            } else {
                nextBillLabel.setText("No upcoming bills");
            }
        } catch (Exception e) {
            logger.error("Error loading next bill", e);
            nextBillLabel.setText("Unable to load bills");
        }
    }

    /**
     * Load mini pie chart for expenses by category
     */
    private void loadExpensePieChart() {
        if (expensePieChart == null) return;

        try {
            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);
            LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

            List<com.financetracker.model.Expense> expenses = expenseService.getExpensesByDateRange(
                    currentUserId, monthStart, monthEnd);

            // Group by category
            java.util.Map<String, BigDecimal> categoryTotals = new java.util.HashMap<>();
            for (com.financetracker.model.Expense expense : expenses) {
                if (expense != null && expense.getAmount() != null) {
                    String category = expense.getCategoryName() != null ? expense.getCategoryName() : "Uncategorized";
                    categoryTotals.put(category, 
                        categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(expense.getAmount()));
                }
            }

            // Create pie chart data (top 5 categories)
            expensePieChart.getData().clear();
            categoryTotals.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    double value = entry.getValue().doubleValue();
                    if (value > 0) {
                        expensePieChart.getData().add(
                            new javafx.scene.chart.PieChart.Data(entry.getKey(), value));
                    }
                });
        } catch (Exception e) {
            logger.error("Error loading expense pie chart", e);
        }
    }

    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadDashboardData();
    }
}
