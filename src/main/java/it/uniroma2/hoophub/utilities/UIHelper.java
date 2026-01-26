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
     * Message disappears after animation completes.
     */
    public static void showMessage(Label label, String message) {
        label.getStyleClass().removeAll(ERROR, SUCCESS, TITLE);
        label.getStyleClass().add(INFO);
        new LabelDuration().duration(label, message);
    }

    /**
     * Shows an error message that disappears after animation.
     * Applies the 'error' CSS class.
     */
    public static void showError(Label label, String errorMessage) {
        label.getStyleClass().removeAll(INFO, SUCCESS, TITLE);
        label.getStyleClass().add(ERROR);
        new LabelDuration().duration(label, errorMessage);
    }

    /**
     * Shows a success message that disappears after animation.
     * Applies the 'success' CSS class.
     */
    public static void showSuccess(Label label, String successMessage) {
        label.getStyleClass().removeAll(INFO, ERROR, TITLE);
        label.getStyleClass().add(SUCCESS);
        new LabelDuration().duration(label, successMessage);
    }

    // ==================== METHODS THAT RESET TO TITLE ====================

    /**
     * Displays a temporary info message, then returns to the page title.
     *
     * @param label     The label to use
     * @param message   The temporary message
     * @param pageTitle The title to return to
     */
    public static void showMessageThenTitle(Label label, String message, String pageTitle) {
        label.getStyleClass().removeAll(ERROR, SUCCESS, TITLE);
        label.getStyleClass().add(INFO);
        new LabelDuration().durationThenResetToTitle(label, message, pageTitle, TITLE);
    }

    /**
     * Shows an error message, then returns to the page title.
     *
     * @param label     The label to use
     * @param message   The error message
     * @param pageTitle The title to return to
     */
    public static void showErrorThenTitle(Label label, String message, String pageTitle) {
        label.getStyleClass().removeAll(INFO, SUCCESS, TITLE);
        label.getStyleClass().add(ERROR);
        new LabelDuration().durationThenResetToTitle(label, message, pageTitle, TITLE);
    }

    /**
     * Shows a success message, then returns to the page title.
     *
     * @param label     The label to use
     * @param message   The success message
     * @param pageTitle The title to return to
     */
    public static void showSuccessThenTitle(Label label, String message, String pageTitle) {
        label.getStyleClass().removeAll(INFO, ERROR, TITLE);
        label.getStyleClass().add(SUCCESS);
        new LabelDuration().durationThenResetToTitle(label, message, pageTitle, TITLE);
    }
}