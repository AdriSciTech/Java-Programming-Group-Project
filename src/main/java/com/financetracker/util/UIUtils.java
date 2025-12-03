package com.financetracker.util;

import javafx.scene.control.Alert;

import java.math.BigDecimal;

/**
 * Utility class for common UI operations
 * Consolidates duplicate code from controllers
 */
public class UIUtils {

    /**
     * Show an alert dialog
     * Consolidates duplicate showAlert methods from multiple controllers
     * 
     * @param type The alert type (ERROR, WARNING, INFORMATION, etc.)
     * @param title The alert title
     * @param message The alert message
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Parse a BigDecimal from a text field, handling comma separators
     * Consolidates duplicate BigDecimal parsing code from multiple controllers
     * 
     * @param text The text to parse
     * @return The parsed BigDecimal
     * @throws NumberFormatException if the text cannot be parsed
     */
    public static BigDecimal parseBigDecimal(String text) throws NumberFormatException {
        if (text == null || text.trim().isEmpty()) {
            throw new NumberFormatException("Empty string cannot be parsed as BigDecimal");
        }
        return new BigDecimal(text.replace(",", "").trim());
    }
}

