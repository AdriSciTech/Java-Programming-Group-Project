package com.financetracker.service;

import com.financetracker.model.Bill;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Bill and Subscription database operations
 */
public class BillService {
    private static final Logger logger = LoggerFactory.getLogger(BillService.class);
    private final SupabaseClient supabaseClient;

    public BillService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new bill/subscription
     */
    public boolean createBill(Bill bill) {
        // Calculate next payment date
        calculateNextPaymentDate(bill);

        String sql = "INSERT INTO bills_subscriptions (bill_id, user_id, category_id, name, amount, " +
                "billing_cycle, due_day, start_date, end_date, is_active, reminder_days, " +
                "next_payment_date, description, vendor) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, bill.getBillId());
            pstmt.setObject(2, bill.getUserId());
            pstmt.setObject(3, bill.getCategoryId());
            pstmt.setString(4, bill.getName());
            pstmt.setBigDecimal(5, bill.getAmount());
            pstmt.setString(6, bill.getBillingCycle());
            pstmt.setInt(7, bill.getDueDay());
            pstmt.setDate(8, Date.valueOf(bill.getStartDate()));
            pstmt.setDate(9, bill.getEndDate() != null ? Date.valueOf(bill.getEndDate()) : null);
            pstmt.setBoolean(10, bill.isActive());
            pstmt.setInt(11, bill.getReminderDays());
            pstmt.setDate(12, bill.getNextPaymentDate() != null ? Date.valueOf(bill.getNextPaymentDate()) : null);
            pstmt.setString(13, bill.getDescription());
            pstmt.setString(14, bill.getVendor());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill created: {}", bill.getBillId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating bill", e);
            return false;
        }
    }

    /**
     * Get all bills for a user
     */
    public List<Bill> getBillsByUser(UUID userId) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.next_payment_date ASC NULLS LAST";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting bills by user", e);
        }

        return bills;
    }

    /**
     * Get active bills for a user
     */
    public List<Bill> getActiveBills(UUID userId) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.is_active = true " +
                "ORDER BY b.next_payment_date ASC NULLS LAST";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting active bills", e);
        }

        return bills;
    }

    /**
     * Get upcoming bills (next payment date within next 7 days)
     */
    public List<Bill> getUpcomingBills(UUID userId, int daysAhead) {
        List<Bill> bills = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysAhead);

        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.is_active = true " +
                "AND b.next_payment_date BETWEEN ? AND ? " +
                "ORDER BY b.next_payment_date ASC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(today));
            pstmt.setDate(3, Date.valueOf(futureDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting upcoming bills", e);
        }

        return bills;
    }

    /**
     * Get bill by ID
     */
    public Bill getBillById(UUID billId) {
        String sql = "SELECT b.*, c.category_name " +
                "FROM bills_subscriptions b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, billId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToBill(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting bill by ID", e);
        }

        return null;
    }

    /**
     * Update a bill
     */
    public boolean updateBill(Bill bill) {
        // Recalculate next payment date
        calculateNextPaymentDate(bill);

        String sql = "UPDATE bills_subscriptions SET category_id = ?, name = ?, amount = ?, " +
                "billing_cycle = ?, due_day = ?, start_date = ?, end_date = ?, is_active = ?, " +
                "reminder_days = ?, next_payment_date = ?, description = ?, vendor = ?, " +
                "updated_at = CURRENT_TIMESTAMP " +
                "WHERE bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, bill.getCategoryId());
            pstmt.setString(2, bill.getName());
            pstmt.setBigDecimal(3, bill.getAmount());
            pstmt.setString(4, bill.getBillingCycle());
            pstmt.setInt(5, bill.getDueDay());
            pstmt.setDate(6, Date.valueOf(bill.getStartDate()));
            pstmt.setDate(7, bill.getEndDate() != null ? Date.valueOf(bill.getEndDate()) : null);
            pstmt.setBoolean(8, bill.isActive());
            pstmt.setInt(9, bill.getReminderDays());
            pstmt.setDate(10, bill.getNextPaymentDate() != null ? Date.valueOf(bill.getNextPaymentDate()) : null);
            pstmt.setString(11, bill.getDescription());
            pstmt.setString(12, bill.getVendor());
            pstmt.setObject(13, bill.getBillId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill updated: {}", bill.getBillId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating bill", e);
            return false;
        }
    }

    /**
     * Delete a bill
     */
    public boolean deleteBill(UUID billId) {
        String sql = "DELETE FROM bills_subscriptions WHERE bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, billId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill deleted: {}", billId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting bill", e);
            return false;
        }
    }

    /**
     * Cancel a subscription (set is_active to false)
     */
    public boolean cancelBill(UUID billId) {
        String sql = "UPDATE bills_subscriptions SET is_active = false, updated_at = CURRENT_TIMESTAMP " +
                "WHERE bill_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, billId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Bill cancelled: {}", billId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error cancelling bill", e);
            return false;
        }
    }

    /**
     * Calculate next payment date based on billing cycle and due day
     */
    private void calculateNextPaymentDate(Bill bill) {
        if (bill.getStartDate() == null || bill.getBillingCycle() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate nextDate = bill.getStartDate();
        int dueDay = bill.getDueDay();

        // Find the next payment date based on billing cycle
        while (nextDate.isBefore(today) || nextDate.equals(today)) {
            switch (bill.getBillingCycle()) {
                case "DAILY":
                    nextDate = nextDate.plusDays(1);
                    break;
                case "WEEKLY":
                    nextDate = nextDate.plusWeeks(1);
                    break;
                case "MONTHLY":
                    nextDate = nextDate.plusMonths(1);
                    // Adjust to due day of month
                    if (dueDay > 0 && dueDay <= 28) {
                        nextDate = nextDate.withDayOfMonth(Math.min(dueDay, nextDate.lengthOfMonth()));
                    }
                    break;
                case "QUARTERLY":
                    nextDate = nextDate.plusMonths(3);
                    if (dueDay > 0 && dueDay <= 28) {
                        nextDate = nextDate.withDayOfMonth(Math.min(dueDay, nextDate.lengthOfMonth()));
                    }
                    break;
                case "YEARLY":
                    nextDate = nextDate.plusYears(1);
                    if (dueDay > 0 && dueDay <= 28) {
                        nextDate = nextDate.withDayOfMonth(Math.min(dueDay, nextDate.lengthOfMonth()));
                    }
                    break;
                default:
                    nextDate = nextDate.plusMonths(1);
                    break;
            }

            // Check if we've exceeded end date
            if (bill.getEndDate() != null && nextDate.isAfter(bill.getEndDate())) {
                nextDate = null;
                break;
            }
        }

        bill.setNextPaymentDate(nextDate);
    }

    /**
     * Map ResultSet to Bill object
     */
    private Bill mapResultSetToBill(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId((UUID) rs.getObject("bill_id"));
        bill.setUserId((UUID) rs.getObject("user_id"));

        Object categoryIdObj = rs.getObject("category_id");
        if (categoryIdObj != null) {
            bill.setCategoryId((UUID) categoryIdObj);
        }

        bill.setName(rs.getString("name"));
        bill.setAmount(rs.getBigDecimal("amount"));
        bill.setBillingCycle(rs.getString("billing_cycle"));
        bill.setDueDay(rs.getInt("due_day"));
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) {
            bill.setStartDate(startDate.toLocalDate());
        }

        Date endDate = rs.getDate("end_date");
        if (endDate != null) {
            bill.setEndDate(endDate.toLocalDate());
        }

        bill.setActive(rs.getBoolean("is_active"));
        bill.setReminderDays(rs.getInt("reminder_days"));

        Date lastPaymentDate = rs.getDate("last_payment_date");
        if (lastPaymentDate != null) {
            bill.setLastPaymentDate(lastPaymentDate.toLocalDate());
        }

        Date nextPaymentDate = rs.getDate("next_payment_date");
        if (nextPaymentDate != null) {
            bill.setNextPaymentDate(nextPaymentDate.toLocalDate());
        }

        bill.setDescription(rs.getString("description"));
        bill.setVendor(rs.getString("vendor"));
        bill.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        bill.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        // Joined field
        try {
            bill.setCategoryName(rs.getString("category_name"));
        } catch (SQLException e) {
            // Field not present in query
        }

        return bill;
    }
}
