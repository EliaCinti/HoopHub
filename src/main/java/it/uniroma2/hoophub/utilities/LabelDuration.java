package it.uniroma2.hoophub.utilities;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class LabelDuration {

    public void duration(Label label, String message) {
        Duration fadeInDuration = Duration.millis(500);
        Duration fadeOutDuration = Duration.millis(1000);
        Duration visibleDuration = Duration.seconds(3);

        label.setText(message);

        if (label.getOpacity() == 1.0) {
            label.setOpacity(0.0);
        }

        FadeTransition fadeIn = new FadeTransition(fadeInDuration, label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = getFadeTransition(label, fadeOutDuration, visibleDuration);

        fadeIn.play();
        fadeIn.setOnFinished(event -> fadeOut.play());
    }

    private static FadeTransition getFadeTransition(Label label, Duration fadeOutDuration, Duration visibleDuration) {
        FadeTransition fadeOut = new FadeTransition(fadeOutDuration, label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_OUT);
        fadeOut.setDelay(visibleDuration);


        fadeOut.setOnFinished(event -> {
            label.setOpacity(0.0);
            label.setText(""); // Puliamo il testo per sicurezza
        });
        return fadeOut;
    }
}

