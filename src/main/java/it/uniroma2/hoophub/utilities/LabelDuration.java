package it.uniroma2.hoophub.utilities;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * LabelDuration provides utility methods to apply fade transitions to labels in a JavaFX application.
 * This class is designed to enhance the user interface by allowing messages on labels to fade in and fade out.
 */
public class LabelDuration {
    /**
     * Applies a fade-in and fade-out effect to a label with a specified message.
     * The message appears with a fade-in effect, remains visible for the duration of the transition, and then fades out.
     *
     * @param label    The label to which the fade transitions will be applied.
     * @param message  The message to display on the label during the transition.
     */
    public void duration(Label label, String message) {
        Duration fadeInDuration = Duration.millis(1000);  // Durata dell'effetto di fade-in
        Duration fadeOutDuration = Duration.millis(1000);  // Durata dell'effetto di fade-out
        Duration visibleDuration = Duration.seconds(3);  // Durata della visibilità del messaggio

        label.setText(message);

        FadeTransition fadeIn = new FadeTransition(fadeInDuration, label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(fadeOutDuration, label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_OUT);
        fadeOut.setDelay(visibleDuration);  // Imposta il ritardo prima del fade-out

        fadeIn.play();
        fadeIn.setOnFinished(event -> fadeOut.play());
    }
}
