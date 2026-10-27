package com.example.applock.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialHasherTest {

    @Test
    fun test4DigitPinHashAndVerify() {
        val pin = "1234"
        val result = CredentialHasher.hashCredential(pin)

        assertTrue(result.hashHex.isNotBlank())
        assertTrue(result.saltHex.isNotBlank())

        val isValid = CredentialHasher.verifyCredential(pin, result.hashHex, result.saltHex)
        assertTrue("4-digit PIN verification should succeed for correct PIN", isValid)
    }

    @Test
    fun test6DigitPinHashAndVerify() {
        val pin = "123456"
        val result = CredentialHasher.hashCredential(pin)

        assertTrue(result.hashHex.isNotBlank())
        assertTrue(result.saltHex.isNotBlank())

        val isValid = CredentialHasher.verifyCredential(pin, result.hashHex, result.saltHex)
        assertTrue("6-digit PIN verification should succeed for correct PIN", isValid)

        val isInvalid = CredentialHasher.verifyCredential("123455", result.hashHex, result.saltHex)
        assertFalse("6-digit PIN verification should fail for wrong PIN", isInvalid)
    }

    @Test
    fun testVerifyCredentialFailsForWrongPin() {
        val pin = "1234"
        val wrongPin = "4321"
        val result = CredentialHasher.hashCredential(pin)

        val isValid = CredentialHasher.verifyCredential(wrongPin, result.hashHex, result.saltHex)
        assertFalse("PIN verification should fail for wrong PIN", isValid)
    }

    @Test
    fun testDifferentSaltsProduceDifferentHashes() {
        val pin = "123456"
        val result1 = CredentialHasher.hashCredential(pin)
        val result2 = CredentialHasher.hashCredential(pin)

        assertNotEquals("Hashes with different salts should not match", result1.hashHex, result2.hashHex)
        assertNotEquals("Generated salts should be unique", result1.saltHex, result2.saltHex)
    }

    @Test
    fun testPatternVerification() {
        val pattern = "0-1-2-5-8"
        val result = CredentialHasher.hashCredential(pattern)

        assertTrue(CredentialHasher.verifyCredential("0-1-2-5-8", result.hashHex, result.saltHex))
        assertFalse(CredentialHasher.verifyCredential("0-1-2-3-4", result.hashHex, result.saltHex))
    }
}
