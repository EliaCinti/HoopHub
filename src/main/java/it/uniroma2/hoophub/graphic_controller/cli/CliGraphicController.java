package it.uniroma2.hoophub.graphic_controller.cli;

/**
 * Abstract base class for all CLI graphic controllers.
 * <p>
 * This class defines the common contract for CLI boundary classes.
 * All CLI graphic controllers must extend this class and implement the {@link #execute()} method.
 * </p>
 * <p>
 * <strong>Design:</strong> Concrete subclasses use {@link CliUtils} directly for CLI I/O operations.
 * CliUtils is package-private, ensuring that only classes in this package can perform console I/O.
 * </p>
 */
public abstract class CliGraphicController {

    /**
     * Executes the graphic controller logic.
     * Subclasses must implement this method to define their specific behavior.
     */
    public abstract void execute();
}
