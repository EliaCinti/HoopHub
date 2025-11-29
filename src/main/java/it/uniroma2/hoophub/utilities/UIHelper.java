package it.uniroma2.hoophub.utilities;

import javafx.scene.control.Label;

/**
 * Utility class for common UI operations.
 * Works with CSS stylesheets for visual styling.
 */
public class UIHelper {

    private UIHelper() {
        // Utility class
    }

    private static final String INFO_MESSAGE = "info-message";
    private static final String ERROR_MESSAGE = "error-message";
    private static final String SUCCESS_MESSAGE = "success-message";

    /**
     * Displays a temporary message on a label with fade animation.
     */
    public static void showMessage(Label label, String message) {
        label.getStyleClass().removeAll(ERROR_MESSAGE, SUCCESS_MESSAGE);
        label.getStyleClass().add(INFO_MESSAGE); // CSS Class
        new LabelDuration().duration(label, message);
    }

    /**
     * Shows an error message.
     * Applies the 'error-message' CSS class.
     */
    public static void showError(Label label, String errorMessage) {
        label.getStyleClass().removeAll(INFO_MESSAGE, SUCCESS_MESSAGE);
        label.getStyleClass().add(ERROR_MESSAGE);  // CSS class
        new LabelDuration().duration(label, errorMessage);
    }

    /**
     * Shows a success message.
     * Applies the 'success-message' CSS class.
     */
    public static void showSuccess(Label label, String successMessage) {
        label.getStyleClass().removeAll(INFO_MESSAGE, ERROR_MESSAGE);
        label.getStyleClass().add(SUCCESS_MESSAGE);  // CSS class
        new LabelDuration().duration(label, successMessage);
    }
}