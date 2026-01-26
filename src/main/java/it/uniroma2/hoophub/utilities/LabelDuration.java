package it.uniroma2.hoophub.utilities;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Utility class for label animations.
 * Provides fade-in/fade-out effects for temporary messages.
 */
public class LabelDuration {

    private static final Duration FADE_IN_DURATION = Duration.millis(500);
    private static final Duration FADE_OUT_DURATION = Duration.millis(1000);
    private static final Duration VISIBLE_DURATION = Duration.seconds(3);

    /**
     * Shows a temporary message that fades out completely.
     *
     * @param label   The label to animate
     * @param message The message to display
     */
    public void duration(Label label, String message) {
        label.setText(message);

        if (label.getOpacity() == 1.0) {
            label.setOpacity(0.0);
        }

        FadeTransition fadeIn = createFadeIn(label);
        FadeTransition fadeOut = createFadeOut(label);

        fadeOut.setOnFinished(event -> {
            label.setOpacity(0.0);
            label.setText("");
        });

        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();
    }

    /**
     * Shows a temporary message that returns to the original title after fading.
     *
     * @param label         The label to animate
     * @param message       The temporary message to display
     * @param originalTitle The title to return to after the message fades
     * @param titleStyleClass The CSS class for the title style
     */
    public void durationThenResetToTitle(Label label, String message, String originalTitle, String titleStyleClass) {
        label.setText(message);

        if (label.getOpacity() == 1.0) {
            label.setOpacity(0.0);
        }

        FadeTransition fadeIn = createFadeIn(label);
        FadeTransition fadeOut = createFadeOut(label);

        fadeOut.setOnFinished(event -> {
            // Reset to title style and text
            label.getStyleClass().removeAll("info", "error", "success", "warning");
            label.getStyleClass().add(titleStyleClass);
            label.setText(originalTitle);

            // Fade the title back in
            FadeTransition titleFadeIn = createFadeIn(label);
            titleFadeIn.play();
        });

        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();
    }

    /**
     * Creates a fade-in transition.
     */
    private FadeTransition createFadeIn(Label label) {
        FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_IN);
        return fadeIn;
    }

    /**
     * Creates a fade-out transition with delay.
     */
    private FadeTransition createFadeOut(Label label) {
        FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_OUT);
        fadeOut.setDelay(VISIBLE_DURATION);
        return fadeOut;
    }
}