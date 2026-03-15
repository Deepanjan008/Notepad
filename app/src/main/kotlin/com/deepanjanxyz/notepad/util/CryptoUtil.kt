package com.deepanjanxyz.notepad.util

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {

    // ─────────────── PIN hashing (SHA-256) ────────────────────────────────────

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(input: String, hashed: String): Boolean = hashPin(input) == hashed

    // ─────────────── AES-256-GCM encryption ───────────────────────────────────
    // Layout: [4B saltLen][salt][4B ivLen][iv][ciphertext + GCM tag]

    fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = randomBytes(16)
        val iv   = randomBytes(12)
        val key  = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(data)

        return ByteArrayOutputStream().apply {
            write(ByteBuffer.allocate(4).putInt(salt.size).array())
            write(salt)
            write(ByteBuffer.allocate(4).putInt(iv.size).array())
            write(iv)
            write(cipherText)
        }.toByteArray()
    }

    fun decrypt(data: ByteArray, password: String): ByteArray {
        val buf     = ByteBuffer.wrap(data)
        val salt    = ByteArray(buf.int).also { buf.get(it) }
        val iv      = ByteArray(buf.int).also { buf.get(it) }
        val cipher  = ByteArray(buf.remaining()).also { buf.get(it) }
        val key     = deriveKey(password, salt)

        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return c.doFinal(cipher)
    }

    // ─────────────── helpers ──────────────────────────────────────────────────

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec    = PBEKeySpec(password.toCharArray(), salt, 65_536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun randomBytes(size: Int) = ByteArray(size).also { SecureRandom().nextBytes(it) }
}
