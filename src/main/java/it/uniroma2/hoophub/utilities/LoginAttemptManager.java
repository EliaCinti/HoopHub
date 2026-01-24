package it.uniroma2.hoophub.utilities;

import it.uniroma2.hoophub.exception.DAOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Gestisce lo stato globale dei tentativi di login falliti.
 * Mantiene lo stato (Stateful) ed è un Singleton, permettendo al LoginController
 * di rimanere Stateless.
 */
@SuppressWarnings("java:S6548") // Singleton is required
public class LoginAttemptManager {

    private static LoginAttemptManager instance;

    // Rate limiting constants
    private static final int MAX_ATTEMPTS_BEFORE_DELAY = 3;
    private static final int BASE_DELAY_SECONDS = 30;

    // State: Track failed login attempts globally
    private int globalFailedAttempts = 0;
    private Instant lastFailedAttemptTime = null;

    private LoginAttemptManager() {
        // Private constructor
    }

    public static synchronized LoginAttemptManager getInstance() {
        if (instance == null) {
            instance = new LoginAttemptManager();
        }
        return instance;
    }

    /**
     * Verifica se il rate limiting è attivo.
     * @throws DAOException se l'utente deve attendere.
     */
    public synchronized void checkRateLimit() throws DAOException {
        if (globalFailedAttempts < MAX_ATTEMPTS_BEFORE_DELAY) {
            return;
        }

        if (lastFailedAttemptTime == null) {
            return;
        }

        int delaySeconds = BASE_DELAY_SECONDS * (globalFailedAttempts - MAX_ATTEMPTS_BEFORE_DELAY + 1);
        Instant now = Instant.now();
        Duration timeSinceLastAttempt = Duration.between(lastFailedAttemptTime, now);

        if (timeSinceLastAttempt.getSeconds() < delaySeconds) {
            long remainingSeconds = delaySeconds - timeSinceLastAttempt.getSeconds();
            throw new DAOException(String.format(
                    "Too many failed login attempts. Please wait %d seconds before trying again.",
                    remainingSeconds
            ));
        }
    }

    public synchronized void recordFailedAttempt() {
        globalFailedAttempts++;
        lastFailedAttemptTime = Instant.now();
    }

    public synchronized void resetFailedAttempts() {
        globalFailedAttempts = 0;
        lastFailedAttemptTime = null;
    }
}
