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

    private static final String INFO = "info";
    private static final String ERROR = "error";
    private static final String SUCCESS = "success";
    private static final String TITLE = "title";

    /**
     * Displays a persistent title message (white text, no fade out).
     * Used as initial page title that gets replaced by other messages.
     */
    public static void showTitle(Label label, String message) {
        label.getStyleClass().removeAll(INFO, ERROR, SUCCESS, TITLE);
        label.getStyleClass().add(TITLE);
        label.setText(message);
        label.setOpacity(1.0);
    }

    /**
     * Displays a temporary message on a label with fade animation.
     */
    public static void showMessage(Label label, String message) {
        label.getStyleClass().removeAll(ERROR, SUCCESS);
        label.getStyleClass().add(INFO);
        new LabelDuration().duration(label, message);
    }

    /**
     * Shows an error message.
     * Applies the 'error' CSS class.
     */
    public static void showError(Label label, String errorMessage) {
        label.getStyleClass().removeAll(INFO, SUCCESS);
        label.getStyleClass().add(ERROR);
        new LabelDuration().duration(label, errorMessage);
    }

    /**
     * Shows a success message.
     * Applies the 'success' CSS class.
     */
    public static void showSuccess(Label label, String successMessage) {
        label.getStyleClass().removeAll(INFO, ERROR);
        label.getStyleClass().add(SUCCESS);
        new LabelDuration().duration(label, successMessage);
    }
}