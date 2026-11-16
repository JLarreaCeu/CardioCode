package server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Convierte contraseñas en hashes seguros y las comprueba.
 * Usa PBKDF2 con una sal aleatoria distinta para cada contraseña,
 * así dos usuarios con la misma contraseña tienen hashes distintos.
 * El resultado son 48 bytes: los 16 primeros son la sal y los 32 siguientes el hash.
 * Se guarda tal cual en la columna password (BLOB) de la tabla users.
 */
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;     // bytes
    private static final int HASH_LENGTH = 256;    // bits (32 bytes)
    private static final int ITERATIONS = 100_000; // más vueltas = más seguro pero más lento

    /**
     * Crea el hash de una contraseña nueva (registro o cambio de contraseña).
     */
    public static byte[] hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt);

        byte[] result = new byte[SALT_LENGTH + hash.length];
        System.arraycopy(salt, 0, result, 0, SALT_LENGTH);
        System.arraycopy(hash, 0, result, SALT_LENGTH, hash.length);
        return result;
    }

    /**
     * Comprueba si una contraseña coincide con el hash guardado (login).
     */
    public static boolean verify(String password, byte[] stored) {
        if (password == null || stored == null || stored.length <= SALT_LENGTH) {
            return false;
        }
        byte[] salt = Arrays.copyOfRange(stored, 0, SALT_LENGTH);
        byte[] expectedHash = Arrays.copyOfRange(stored, SALT_LENGTH, stored.length);
        byte[] actualHash = pbkdf2(password, salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Could not hash the password", e);
        } finally {
            spec.clearPassword();
        }
    }
}