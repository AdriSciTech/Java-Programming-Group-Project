package com.financetracker.controller;

import com.financetracker.model.Bill;
import com.financetracker.service.AccountService;
import com.financetracker.service.BillService;
import com.financetracker.service.ExpenseService;
import com.financetracker.service.IncomeService;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
            // Still show default values even if not logged in
            if (totalBalanceLabel != null) totalBalanceLabel.setText("$0.00");
            if (monthlyIncomeLabel != null) monthlyIncomeLabel.setText("$0.00");
            if (monthlyExpenseLabel != null) monthlyExpenseLabel.setText("$0.00");
            if (monthlySavingsLabel != null) monthlySavingsLabel.setText("$0.00");
            if (nextBillLabel != null) nextBillLabel.setText("No upcoming bills");
            return;
        }

        // Load all data in background threads for better performance
        loadSummaryCards();
        loadNextBill();
        loadExpensePieChart();
    }

    /**
     * Load summary cards with monthly totals
     */
    private void loadSummaryCards() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // Run all database operations in parallel background threads
        Task<BigDecimal> incomeTask = new Task<BigDecimal>() {
            @Override
            protected BigDecimal call() throws Exception {
                return incomeService.getTotalIncome(currentUserId, monthStart, monthEnd);
            }
        };

        Task<BigDecimal> expenseTask = new Task<BigDecimal>() {
            @Override
            protected BigDecimal call() throws Exception {
                return expenseService.getTotalExpenses(currentUserId, monthStart, monthEnd);
            }
        };

        Task<List<com.financetracker.model.Account>> accountTask = new Task<List<com.financetracker.model.Account>>() {
            @Override
            protected List<com.financetracker.model.Account> call() throws Exception {
                return accountService.getAccountsByUser(currentUserId);
            }
        };

        incomeTask.setOnSucceeded(e -> {
            BigDecimal monthlyIncome = incomeTask.getValue();
            if (monthlyIncome == null) monthlyIncome = BigDecimal.ZERO;
            final BigDecimal finalIncome = monthlyIncome;
            Platform.runLater(() -> {
                if (monthlyIncomeLabel != null) {
                    monthlyIncomeLabel.setText(String.format("$%,.2f", finalIncome));
                }
                // Calculate savings if expenses are already loaded
                if (expenseTask.isDone() && expenseTask.getValue() != null) {
                    BigDecimal monthlySavings = finalIncome.subtract(expenseTask.getValue());
                    if (monthlySavingsLabel != null) {
                        monthlySavingsLabel.setText(String.format("$%,.2f", monthlySavings));
                    }
                } else if (expenseTask.isDone()) {
                    BigDecimal monthlySavings = finalIncome.subtract(BigDecimal.ZERO);
                    if (monthlySavingsLabel != null) {
                        monthlySavingsLabel.setText(String.format("$%,.2f", monthlySavings));
                    }
                }
            });
        });
        
        incomeTask.setOnFailed(e -> {
            logger.error("Error loading income", incomeTask.getException());
            Platform.runLater(() -> {
                if (monthlyIncomeLabel != null) {
                    monthlyIncomeLabel.setText("$0.00");
                }
            });
        });

        expenseTask.setOnSucceeded(e -> {
            BigDecimal monthlyExpenses = expenseTask.getValue();
            if (monthlyExpenses == null) monthlyExpenses = BigDecimal.ZERO;
            final BigDecimal finalExpenses = monthlyExpenses;
            Platform.runLater(() -> {
                if (monthlyExpenseLabel != null) {
                    monthlyExpenseLabel.setText(String.format("$%,.2f", finalExpenses));
                }
                // Calculate savings when both are ready
                if (incomeTask.isDone() && incomeTask.getValue() != null) {
                    BigDecimal monthlySavings = incomeTask.getValue().subtract(finalExpenses);
                    if (monthlySavingsLabel != null) {
                        monthlySavingsLabel.setText(String.format("$%,.2f", monthlySavings));
                    }
                } else if (incomeTask.isDone()) {
                    // If income is done but null, use zero
                    BigDecimal monthlySavings = BigDecimal.ZERO.subtract(finalExpenses);
                    if (monthlySavingsLabel != null) {
                        monthlySavingsLabel.setText(String.format("$%,.2f", monthlySavings));
                    }
                }
            });
        });
        
        expenseTask.setOnFailed(e -> {
            logger.error("Error loading expenses", expenseTask.getException());
            Platform.runLater(() -> {
                if (monthlyExpenseLabel != null) {
                    monthlyExpenseLabel.setText("$0.00");
                }
            });
        });

        accountTask.setOnSucceeded(e -> {
            List<com.financetracker.model.Account> accounts = accountTask.getValue();
            BigDecimal totalBalance = BigDecimal.ZERO;
            if (accounts != null) {
                for (com.financetracker.model.Account account : accounts) {
                    if (account != null && account.isActive() && account.getBalance() != null) {
                        totalBalance = totalBalance.add(account.getBalance());
                    }
                }
            }
            final BigDecimal finalBalance = totalBalance;
            Platform.runLater(() -> {
                if (totalBalanceLabel != null) {
                    totalBalanceLabel.setText(String.format("$%,.2f", finalBalance));
                }
            });
        });
        
        accountTask.setOnFailed(e -> {
            logger.error("Error loading accounts", accountTask.getException());
            Platform.runLater(() -> {
                if (totalBalanceLabel != null) {
                    totalBalanceLabel.setText("$0.00");
                }
            });
        });

        // Start all tasks in parallel
        new Thread(incomeTask).start();
        new Thread(expenseTask).start();
        new Thread(accountTask).start();
    }

    /**
     * Load next upcoming bill
     */
    private void loadNextBill() {
        if (nextBillLabel == null) return;

        Task<List<Bill>> billTask = new Task<List<Bill>>() {
            @Override
            protected List<Bill> call() throws Exception {
                return billService.getUpcomingBills(currentUserId, 30);
            }
        };

        billTask.setOnSucceeded(e -> {
            List<Bill> upcomingBills = billTask.getValue();
            Platform.runLater(() -> {
                if (upcomingBills != null && !upcomingBills.isEmpty()) {
                    Bill nextBill = upcomingBills.get(0);
                    LocalDate nextPaymentDate = nextBill.getNextPaymentDate();
                    BigDecimal amount = nextBill.getAmount();
                    if (amount == null) amount = BigDecimal.ZERO;
                    
                    if (nextPaymentDate != null) {
                        nextBillLabel.setText(nextBill.getName() + " - " + 
                            String.format("$%,.2f", amount) + 
                            " due " + nextPaymentDate.format(
                                java.time.format.DateTimeFormatter.ofPattern("MMM dd")));
                    } else {
                        nextBillLabel.setText(nextBill.getName() + " - " + 
                            String.format("$%,.2f", amount) + " (ended)");
                    }
                } else {
                    nextBillLabel.setText("No upcoming bills");
                }
            });
        });

        billTask.setOnFailed(e -> {
            logger.error("Error loading next bill", billTask.getException());
            Platform.runLater(() -> {
                nextBillLabel.setText("Unable to load bills");
            });
        });

        new Thread(billTask).start();
    }

    /**
     * Load mini pie chart for expenses by category
     */
    private void loadExpensePieChart() {
        if (expensePieChart == null) return;

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        Task<List<com.financetracker.model.Expense>> expenseTask = new Task<List<com.financetracker.model.Expense>>() {
            @Override
            protected List<com.financetracker.model.Expense> call() throws Exception {
                return expenseService.getExpensesByDateRange(currentUserId, monthStart, monthEnd);
            }
        };

        expenseTask.setOnSucceeded(e -> {
            List<com.financetracker.model.Expense> expenses = expenseTask.getValue();
            
            // Group by category
            java.util.Map<String, BigDecimal> categoryTotals = new java.util.HashMap<>();
            for (com.financetracker.model.Expense expense : expenses) {
                if (expense != null && expense.getAmount() != null) {
                    String category = expense.getCategoryName() != null ? expense.getCategoryName() : "Uncategorized";
                    categoryTotals.put(category, 
                        categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(expense.getAmount()));
                }
            }

            // Create pie chart data (top 5 categories) on JavaFX thread
            Platform.runLater(() -> {
                expensePieChart.getData().clear();
                if (categoryTotals.isEmpty()) {
                    // Show empty state message
                    expensePieChart.setTitle("No expenses this month");
                } else {
                    expensePieChart.setTitle(null);
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
                }
            });
        });

        expenseTask.setOnFailed(e -> {
            logger.error("Error loading expense pie chart", expenseTask.getException());
        });

        new Thread(expenseTask).start();
    }

    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadDashboardData();
    }
}
