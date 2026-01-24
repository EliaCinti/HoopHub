package it.uniroma2.hoophub.utilities;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for secure password operations including hashing and verification.
 * This class uses BCrypt algorithm which automatically includes salt in the hash.
 */
public class PasswordUtils {

    private PasswordUtils() {
        /* no instance */
    }

    // Default cost factor (work factor) for BCrypt
    // Higher values are more secure but slower
    private static final int DEFAULT_COST = 12;

    /**
     * Hashes a password using BCrypt with a randomly generated salt.
     *
     * @param plainTextPassword The password to hash
     * @return The hashed password (includes the salt)
     */
    public static String hashPassword(String plainTextPassword) {
        // Generate a salt with the specified cost factor
        String salt = BCrypt.gensalt(DEFAULT_COST);

        // Hash the password with the generated salt
        return BCrypt.hashpw(plainTextPassword, salt);
    }

    /**
     * Verifies a plaintext password against a hashed password.
     *
     * @param plainTextPassword The plaintext password to check
     * @param hashedPassword The hashed password to check against
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        // BCrypt will extract the salt from the stored hash,
        // hash the plaintext password with that salt,
        // and compare the result with the stored hash
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}
