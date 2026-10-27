package com.example.applock.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Handles cryptographically secure hashing of PINs and Patterns using PBKDF2WithHmacSHA256
 * and random salts. Plaintext credentials are never stored or logged.
 */
object CredentialHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10_000
    private const val KEY_LENGTH = 256 // in bits
    private const val SALT_LENGTH_BYTES = 16

    data class HashResult(
        val hashHex: String,
        val saltHex: String
    )

    /**
     * Generates a secure salt if not provided, and hashes the plaintext credential using PBKDF2.
     */
    fun hashCredential(credential: String, existingSaltHex: String? = null): HashResult {
        val salt = if (existingSaltHex != null) {
            hexToBytes(existingSaltHex)
        } else {
            generateSalt()
        }

        val spec = PBEKeySpec(credential.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()

        return HashResult(
            hashHex = bytesToHex(hashBytes),
            saltHex = bytesToHex(salt)
        )
    }

    /**
     * Verifies a candidate credential against the stored hash and salt using constant-time byte comparison.
     */
    fun verifyCredential(candidate: String, storedHashHex: String, storedSaltHex: String): Boolean {
        if (candidate.isBlank() || storedHashHex.isBlank() || storedSaltHex.isBlank()) {
            return false
        }
        val computedHash = hashCredential(candidate, storedSaltHex).hashHex
        val candidateBytes = hexToBytes(computedHash)
        val storedBytes = hexToBytes(storedHashHex)

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(candidateBytes, storedBytes)
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return salt
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        check(hex.length % 2 == 0) { "Hex string length must be even" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
