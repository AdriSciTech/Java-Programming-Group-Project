package com.financetracker.service;

import com.financetracker.model.Transfer;
import com.financetracker.util.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for Transfer database operations
 */
public class TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);
    private final SupabaseClient supabaseClient;
    private final AccountService accountService;

    public TransferService() {
        this.supabaseClient = SupabaseClient.getInstance();
        this.accountService = new AccountService();
    }

    /**
     * Create a new transfer and update account balances
     */
    public boolean createTransfer(Transfer transfer) {
        String sql = "INSERT INTO transfers (transfer_id, user_id, from_account_id, to_account_id, " +
                "amount, transfer_date, description, transfer_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = supabaseClient.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Insert transfer
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setObject(1, transfer.getTransferId());
                    pstmt.setObject(2, transfer.getUserId());
                    pstmt.setObject(3, transfer.getFromAccountId());
                    pstmt.setObject(4, transfer.getToAccountId());
                    pstmt.setBigDecimal(5, transfer.getAmount());
                    pstmt.setDate(6, Date.valueOf(transfer.getTransferDate()));
                    pstmt.setString(7, transfer.getDescription());
                    pstmt.setString(8, transfer.getTransferType());

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected <= 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // Update account balances within the same transaction
                if (!updateAccountBalances(conn, transfer)) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                logger.info("Transfer created: {}", transfer.getTransferId());
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Error creating transfer", e);
            return false;
        }
    }

    /**
     * Update account balances after transfer (within transaction)
     */
    private boolean updateAccountBalances(Connection conn, Transfer transfer) throws SQLException {
        // Get accounts using the same connection
        com.financetracker.model.Account fromAccount = accountService.getAccountById(conn, transfer.getFromAccountId());
        com.financetracker.model.Account toAccount = accountService.getAccountById(conn, transfer.getToAccountId());

        if (fromAccount == null || toAccount == null) {
            logger.error("One or both accounts not found for transfer");
            return false;
        }

        if (transfer.getAmount() == null || fromAccount.getBalance() == null || toAccount.getBalance() == null) {
            logger.error("Transfer amount or account balance is null");
            return false;
        }

        // Calculate new balances
        java.math.BigDecimal newFromBalance = fromAccount.getBalance().subtract(transfer.getAmount());
        java.math.BigDecimal newToBalance = toAccount.getBalance().add(transfer.getAmount());

        // Update balances using the same connection
        boolean fromUpdated = accountService.updateAccountBalance(conn, transfer.getFromAccountId(), newFromBalance);
        boolean toUpdated = accountService.updateAccountBalance(conn, transfer.getToAccountId(), newToBalance);

        return fromUpdated && toUpdated;
    }

    /**
     * Get all transfers for a user
     */
    public List<Transfer> getTransfersByUser(UUID userId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT t.*, a1.account_name as from_account_name, a2.account_name as to_account_name " +
                "FROM transfers t " +
                "LEFT JOIN accounts a1 ON t.from_account_id = a1.account_id " +
                "LEFT JOIN accounts a2 ON t.to_account_id = a2.account_id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.transfer_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transfers.add(mapResultSetToTransfer(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting transfers by user", e);
        }

        return transfers;
    }

    /**
     * Get transfers by date range
     */
    public List<Transfer> getTransfersByDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT t.*, a1.account_name as from_account_name, a2.account_name as to_account_name " +
                "FROM transfers t " +
                "LEFT JOIN accounts a1 ON t.from_account_id = a1.account_id " +
                "LEFT JOIN accounts a2 ON t.to_account_id = a2.account_id " +
                "WHERE t.user_id = ? AND t.transfer_date BETWEEN ? AND ? " +
                "ORDER BY t.transfer_date DESC";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, userId);
            pstmt.setDate(2, Date.valueOf(startDate));
            pstmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transfers.add(mapResultSetToTransfer(rs));
            }

        } catch (SQLException e) {
            logger.error("Error getting transfers by date range", e);
        }

        return transfers;
    }

    /**
     * Get transfer by ID
     */
    public Transfer getTransferById(UUID transferId) {
        String sql = "SELECT t.*, a1.account_name as from_account_name, a2.account_name as to_account_name " +
                "FROM transfers t " +
                "LEFT JOIN accounts a1 ON t.from_account_id = a1.account_id " +
                "LEFT JOIN accounts a2 ON t.to_account_id = a2.account_id " +
                "WHERE t.transfer_id = ?";

        try (Connection conn = supabaseClient.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, transferId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTransfer(rs);
            }

        } catch (SQLException e) {
            logger.error("Error getting transfer by ID", e);
        }

        return null;
    }

    /**
     * Update a transfer
     */
    public boolean updateTransfer(Transfer transfer) {
        // Get old transfer to reverse balance changes
        Transfer oldTransfer = getTransferById(transfer.getTransferId());
        if (oldTransfer == null) {
            logger.error("Transfer not found for update: {}", transfer.getTransferId());
            return false;
        }

        String sql = "UPDATE transfers SET from_account_id = ?, to_account_id = ?, amount = ?, " +
                "transfer_date = ?, description = ?, transfer_type = ? " +
                "WHERE transfer_id = ?";

        try (Connection conn = supabaseClient.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Reverse old transfer balances first
                if (!reverseTransferBalances(conn, oldTransfer)) {
                    conn.rollback();
                    return false;
                }

                // Update transfer record
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setObject(1, transfer.getFromAccountId());
                    pstmt.setObject(2, transfer.getToAccountId());
                    pstmt.setBigDecimal(3, transfer.getAmount());
                    pstmt.setDate(4, Date.valueOf(transfer.getTransferDate()));
                    pstmt.setString(5, transfer.getDescription());
                    pstmt.setString(6, transfer.getTransferType());
                    pstmt.setObject(7, transfer.getTransferId());

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected <= 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // Update account balances with new transfer
                if (!updateAccountBalances(conn, transfer)) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                logger.info("Transfer updated: {}", transfer.getTransferId());
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Error updating transfer", e);
            return false;
        }
    }

    /**
     * Reverse transfer balances (for updates/deletes) within transaction
     */
    private boolean reverseTransferBalances(Connection conn, Transfer transfer) throws SQLException {
        // Get accounts using the same connection
        com.financetracker.model.Account fromAccount = accountService.getAccountById(conn, transfer.getFromAccountId());
        com.financetracker.model.Account toAccount = accountService.getAccountById(conn, transfer.getToAccountId());

        if (fromAccount == null || toAccount == null) {
            logger.error("One or both accounts not found for transfer reversal");
            return false;
        }

        if (transfer.getAmount() == null || fromAccount.getBalance() == null || toAccount.getBalance() == null) {
            logger.error("Transfer amount or account balance is null");
            return false;
        }

        // Calculate reversed balances (add back to source, deduct from destination)
        java.math.BigDecimal newFromBalance = fromAccount.getBalance().add(transfer.getAmount());
        java.math.BigDecimal newToBalance = toAccount.getBalance().subtract(transfer.getAmount());

        // Update balances using the same connection
        boolean fromUpdated = accountService.updateAccountBalance(conn, transfer.getFromAccountId(), newFromBalance);
        boolean toUpdated = accountService.updateAccountBalance(conn, transfer.getToAccountId(), newToBalance);

        return fromUpdated && toUpdated;
    }

    /**
     * Delete a transfer and reverse account balances
     */
    public boolean deleteTransfer(UUID transferId) {
        Transfer transfer = getTransferById(transferId);
        if (transfer == null) {
            logger.error("Transfer not found for deletion: {}", transferId);
            return false;
        }

        String sql = "DELETE FROM transfers WHERE transfer_id = ?";

        try (Connection conn = supabaseClient.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Reverse account balances first
                if (!reverseTransferBalances(conn, transfer)) {
                    conn.rollback();
                    return false;
                }

                // Delete transfer record
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setObject(1, transferId);
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected <= 0) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();
                logger.info("Transfer deleted: {}", transferId);
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Error deleting transfer", e);
            return false;
        }
    }

    /**
     * Map ResultSet to Transfer object
     */
    private Transfer mapResultSetToTransfer(ResultSet rs) throws SQLException {
        Transfer transfer = new Transfer();
        transfer.setTransferId((UUID) rs.getObject("transfer_id"));
        transfer.setUserId((UUID) rs.getObject("user_id"));
        transfer.setFromAccountId((UUID) rs.getObject("from_account_id"));
        transfer.setToAccountId((UUID) rs.getObject("to_account_id"));
        transfer.setAmount(rs.getBigDecimal("amount"));
        
        Date transferDate = rs.getDate("transfer_date");
        if (transferDate != null) {
            transfer.setTransferDate(transferDate.toLocalDate());
        }
        
        transfer.setDescription(rs.getString("description"));
        transfer.setTransferType(rs.getString("transfer_type"));
        transfer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Joined fields
        try {
            transfer.setFromAccountName(rs.getString("from_account_name"));
            transfer.setToAccountName(rs.getString("to_account_name"));
        } catch (SQLException e) {
            // Fields not present in query
        }

        return transfer;
    }
}
