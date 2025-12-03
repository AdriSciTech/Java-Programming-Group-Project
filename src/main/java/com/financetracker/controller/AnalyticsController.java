package com.financetracker.controller;

import com.financetracker.service.ExpenseService;
import com.financetracker.service.IncomeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Analytics View with charts
 */
public class AnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @FXML private PieChart expenseCategoryChart;
    @FXML private LineChart<String, Number> incomeExpenseChart;
    @FXML private BarChart<String, Number> monthlyTotalsChart;

    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID currentUserId;

    @FXML
    public void initialize() {
        logger.info("Initializing AnalyticsController");

        incomeService = new IncomeService();
        expenseService = new ExpenseService();

        if (LoginController.getCurrentUser() != null) {
            currentUserId = LoginController.getCurrentUser().getUserId();
        }

        loadCharts();

        logger.info("AnalyticsController initialized");
    }

    /**
     * Load all charts with data
     */
    private void loadCharts() {
        if (currentUserId == null) {
            logger.warn("No user logged in, cannot load analytics");
            return;
        }

        try {
            loadExpenseCategoryChart();
            loadIncomeExpenseChart();
            loadMonthlyTotalsChart();
        } catch (Exception e) {
            logger.error("Error loading charts", e);
        }
    }

    /**
     * Load pie chart showing expenses by category
     */
    private void loadExpenseCategoryChart() {
        if (expenseCategoryChart == null) return;

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // Get expenses grouped by category
        List<com.financetracker.model.Expense> expenses = expenseService.getExpensesByDateRange(
                currentUserId, monthStart, monthEnd);

        // Group by category
        Map<String, BigDecimal> categoryTotals = new java.util.HashMap<>();
        for (com.financetracker.model.Expense expense : expenses) {
            if (expense != null && expense.getAmount() != null) {
                String category = expense.getCategoryName() != null ? expense.getCategoryName() : "Uncategorized";
                categoryTotals.put(category, 
                    categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(expense.getAmount()));
            }
        }

        // Create pie chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            double value = entry.getValue().doubleValue();
            if (value > 0) {
                pieChartData.add(new PieChart.Data(entry.getKey(), value));
            }
        }

        expenseCategoryChart.setData(pieChartData);
        expenseCategoryChart.setTitle("Expenses by Category (This Month)");
    }

    /**
     * Load line chart showing income vs expenses over time
     */
    private void loadIncomeExpenseChart() {
        if (incomeExpenseChart == null) return;

        LocalDate now = LocalDate.now();

        // Create series for income and expenses
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        // Get data for last 6 months
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = now.minusMonths(i).withDayOfMonth(
                now.minusMonths(i).lengthOfMonth());
            
            String monthLabel = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));

            BigDecimal income = incomeService.getTotalIncome(currentUserId, monthStart, monthEnd);
            BigDecimal expenses = expenseService.getTotalExpenses(currentUserId, monthStart, monthEnd);

            incomeSeries.getData().add(new XYChart.Data<>(monthLabel, income.doubleValue()));
            expenseSeries.getData().add(new XYChart.Data<>(monthLabel, expenses.doubleValue()));
        }

        incomeExpenseChart.getData().clear();
        incomeExpenseChart.getData().add(incomeSeries);
        incomeExpenseChart.getData().add(expenseSeries);
        incomeExpenseChart.setTitle("Income vs Expenses (Last 6 Months)");
    }

    /**
     * Load bar chart showing monthly totals
     */
    private void loadMonthlyTotalsChart() {
        if (monthlyTotalsChart == null) return;

        LocalDate now = LocalDate.now();

        // Create series for income and expenses
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        // Get data for last 6 months
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = now.minusMonths(i).withDayOfMonth(
                now.minusMonths(i).lengthOfMonth());
            
            String monthLabel = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));

            BigDecimal income = incomeService.getTotalIncome(currentUserId, monthStart, monthEnd);
            BigDecimal expenses = expenseService.getTotalExpenses(currentUserId, monthStart, monthEnd);

            incomeSeries.getData().add(new XYChart.Data<>(monthLabel, income.doubleValue()));
            expenseSeries.getData().add(new XYChart.Data<>(monthLabel, expenses.doubleValue()));
        }

        monthlyTotalsChart.getData().clear();
        monthlyTotalsChart.getData().add(incomeSeries);
        monthlyTotalsChart.getData().add(expenseSeries);
        monthlyTotalsChart.setTitle("Monthly Totals (Last 6 Months)");
    }

    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        loadCharts();
    }
}
