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

    /**
     * Displays a temporary message on a label with fade animation.
     */
    public static void showMessage(Label label, String message) {
        label.getStyleClass().removeAll("error-mesage", "success-mesage");
        label.getStyleClass().add("info-message"); // CSS Class
        new LabelDuration().duration(label, message);
    }

    /**
     * Shows an error message.
     * Applies the 'error-message' CSS class.
     */
    public static void showError(Label label, String errorMessage) {
        label.getStyleClass().removeAll("info-message", "success-message");
        label.getStyleClass().add("error-message");  // CSS class
        new LabelDuration().duration(label, errorMessage);
    }

    /**
     * Shows a success message.
     * Applies the 'success-message' CSS class.
     */
    public static void showSuccess(Label label, String successMessage) {
        label.getStyleClass().removeAll("info-message", "error-message");
        label.getStyleClass().add("success-message");  // CSS class
        new LabelDuration().duration(label, successMessage);
    }
}