package it.uniroma2.hoophub.graphic_controller.cli;

/**
 * Abstract base class for all CLI graphic controllers.
 * <p>
 * This class provides protected methods for CLI I/O operations by wrapping {@link CliUtils}.
 * It serves as a controlled gateway to CLI utilities, ensuring that:
 * <ul>
 *     <li>System.out/in usage is centralized in {@link CliUtils}</li>
 *     <li>Only CLI graphic controllers can access CLI I/O methods</li>
 *     <li>SonarQube compliance is maintained (no System.out outside CliUtils)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Design Pattern:</strong> Template Method pattern - subclasses implement {@link #execute()}
 * while inheriting common CLI I/O capabilities.
 * </p>
 */
public abstract class CliGraphicController {

    // ========== Abstract Method ==========

    /**
     * Executes the graphic controller logic.
     * Subclasses must implement this method to define their specific behavior.
     */
    public abstract void execute();

    // ========== Protected I/O Methods (Wrapper for CliUtils) ==========

    /**
     * Displays a regular message.
     *
     * @param message The message to display
     */
    protected void showMessage(String message) {
        CliUtils.print(message);
    }

    /**
     * Displays a success message in green color.
     *
     * @param message The success message to display
     */
    protected void showSuccess(String message) {
        CliUtils.printSuccess(message);
    }

    /**
     * Displays an error message in red color.
     *
     * @param message The error message to display
     */
    protected void showError(String message) {
        CliUtils.printError(message);
    }

    /**
     * Displays an informational message in cyan color.
     *
     * @param message The info message to display
     */
    protected void showInfo(String message) {
        CliUtils.printInfo(message);
    }

    /**
     * Displays a warning message in yellow color.
     *
     * @param message The warning message to display
     */
    protected void showWarning(String message) {
        CliUtils.printWarning(message);
    }

    /**
     * Prints a blank line for spacing.
     */
    protected void newLine() {
        CliUtils.printNewLine();
    }

    /**
     * Prints a horizontal separator line.
     */
    protected void showSeparator() {
        CliUtils.printSeparator();
    }

    /**
     * Displays a title with a decorative box border.
     *
     * @param title The title text to display
     */
    protected void showTitle(String title) {
        CliUtils.printTitle(title);
    }

    /**
     * Displays a menu with numbered options.
     *
     * @param title   The menu title
     * @param options The menu options (variable arguments)
     */
    protected void showMenu(String title, String... options) {
        CliUtils.printMenu(title, options);
    }

    /**
     * Displays a prompt and reads user input.
     *
     * @param prompt The prompt text to display
     * @return The user's input as a String, trimmed
     */
    protected String readInput(String prompt) {
        return CliUtils.readInput(prompt);
    }

    /**
     * Displays a prompt for password input.
     * Note: Input is still visible in basic CLI. For hidden input, use Console.readPassword().
     *
     * @param prompt The prompt text to display
     * @return The user's password input as a String, trimmed
     */
    protected String readPassword(String prompt) {
        return CliUtils.readPassword(prompt);
    }
}
