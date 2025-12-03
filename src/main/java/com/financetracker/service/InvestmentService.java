package com.financetracker.service;

import com.financetracker.model.Investment;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Investment database operations
 */
public class InvestmentService {
    private static final Logger logger = LoggerFactory.getLogger(InvestmentService.class);
    private final SupabaseClient supabaseClient;

    public InvestmentService() {
        this.supabaseClient = SupabaseClient.getInstance();
    }

    /**
     * Create a new investment
     */
    public boolean createInvestment(Investment investment) {
        String sql = "INSERT INTO investments (investment_id, user_id, account_id, investment_name, " +
                "investment_type, symbol, quantity, purchase_price, current_price, purchase_date, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, investment.getInvestmentId());
            pstmt.setObject(2, investment.getUserId());
            pstmt.setObject(3, investment.getAccountId());
            pstmt.setString(4, investment.getInvestmentName());
            pstmt.setString(5, investment.getInvestmentType());
            pstmt.setString(6, investment.getSymbol());
            pstmt.setBigDecimal(7, investment.getQuantity());
            pstmt.setBigDecimal(8, investment.getPurchasePrice());
            pstmt.setBigDecimal(9, investment.getCurrentPrice());
            pstmt.setDate(10, investment.getPurchaseDate() != null ? Date.valueOf(investment.getPurchaseDate()) : null);
            pstmt.setString(11, investment.getDescription());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Investment created: {}", investment.getInvestmentId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error creating investment", e);
            return false;
        }
    }

    /**
     * Get all investments for a user
     */
    public List<Investment> getInvestmentsByUser(UUID userId) {
        List<Investment> investments = new ArrayList<>();
        String sql = "SELECT * FROM investments WHERE user_id = ? ORDER BY investment_name";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                investments.add(mapResultSetToInvestment(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting investments by user", e);
        }

        return investments;
    }

    /**
     * Get investment by ID
     */
    public Investment getInvestmentById(UUID investmentId) {
        String sql = "SELECT * FROM investments WHERE investment_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, investmentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToInvestment(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting investment by ID", e);
        }

        return null;
    }

    /**
     * Update an investment
     */
    public boolean updateInvestment(Investment investment) {
        String sql = "UPDATE investments SET account_id = ?, investment_name = ?, investment_type = ?, " +
                "symbol = ?, quantity = ?, purchase_price = ?, current_price = ?, purchase_date = ?, " +
                "description = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE investment_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, investment.getAccountId());
            pstmt.setString(2, investment.getInvestmentName());
            pstmt.setString(3, investment.getInvestmentType());
            pstmt.setString(4, investment.getSymbol());
            pstmt.setBigDecimal(5, investment.getQuantity());
            pstmt.setBigDecimal(6, investment.getPurchasePrice());
            pstmt.setBigDecimal(7, investment.getCurrentPrice());
            pstmt.setDate(8, investment.getPurchaseDate() != null ? Date.valueOf(investment.getPurchaseDate()) : null);
            pstmt.setString(9, investment.getDescription());
            pstmt.setObject(10, investment.getInvestmentId());

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Investment updated: {}", investment.getInvestmentId());
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error updating investment", e);
            return false;
        }
    }

    /**
     * Delete an investment
     */
    public boolean deleteInvestment(UUID investmentId) {
        String sql = "DELETE FROM investments WHERE investment_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, investmentId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Investment deleted: {}", investmentId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.error("Error deleting investment", e);
            return false;
        }
    }

    /**
     * Get portfolio summary (total value, total ROI)
     */
    public java.math.BigDecimal getPortfolioTotalValue(UUID userId) {
        List<Investment> investments = getInvestmentsByUser(userId);
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Investment inv : investments) {
            total = total.add(inv.getTotalValue());
        }
        return total;
    }

    /**
     * Get total ROI for portfolio
     */
    public java.math.BigDecimal getPortfolioTotalROI(UUID userId) {
        List<Investment> investments = getInvestmentsByUser(userId);
        java.math.BigDecimal totalROI = java.math.BigDecimal.ZERO;
        for (Investment inv : investments) {
            totalROI = totalROI.add(inv.getROI());
        }
        return totalROI;
    }

    /**
     * Map ResultSet to Investment object
     */
    private Investment mapResultSetToInvestment(ResultSet rs) throws SQLException {
        Investment investment = new Investment();
        investment.setInvestmentId((UUID) rs.getObject("investment_id"));
        investment.setUserId((UUID) rs.getObject("user_id"));

        Object accountIdObj = rs.getObject("account_id");
        if (accountIdObj != null) {
            investment.setAccountId((UUID) accountIdObj);
        }

        investment.setInvestmentName(rs.getString("investment_name"));
        investment.setInvestmentType(rs.getString("investment_type"));
        investment.setSymbol(rs.getString("symbol"));
        investment.setQuantity(rs.getBigDecimal("quantity"));
        investment.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        investment.setCurrentPrice(rs.getBigDecimal("current_price"));

        Date purchaseDate = rs.getDate("purchase_date");
        if (purchaseDate != null) {
            investment.setPurchaseDate(purchaseDate.toLocalDate());
        }

        investment.setDescription(rs.getString("description"));
        investment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        investment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return investment;
    }
}
