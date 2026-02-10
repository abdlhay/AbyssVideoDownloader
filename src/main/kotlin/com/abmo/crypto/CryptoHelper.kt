package com.abmo.crypto

import org.koin.core.component.KoinComponent
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoHelper : KoinComponent {

    private fun initCipher(mode: Int, key: String): Cipher {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val iv = keyBytes.sliceArray(0 until 16)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(mode, secretKey, ivSpec)
        return cipher
    }

    /**
     * Encrypts the given data using AES in CTR mode.
     *
     * @param data The plaintext data to be encrypted. If null, encryption will not be performed.
     * @param key The secret key used for encryption. It must be 16 bytes long (128 bits) for AES.
     * @return The encrypted data as a string encoded in ISO-8859-1.
     * @throws Exception If an error occurs during encryption.
     */
    fun encryptAESCTR(data: String?, key: String): String {
        val cipher = initCipher(Cipher.ENCRYPT_MODE, key)
        val dataBytes = data?.toByteArray(StandardCharsets.UTF_8)
        val encryptedBytes = cipher.doFinal(dataBytes)
        return String(encryptedBytes, Charsets.ISO_8859_1)
    }

    /**
     * Decrypts the given byte array using AES in CTR mode.
     *
     * @param data The encrypted data as a byte array to be decrypted.
     * @param key The secret key used for decryption. It must be 16 bytes long (128 bits) for AES.
     * @return The decrypted plaintext data as a byte array.
     * @throws Exception If an error occurs during decryption.
     */
    fun decryptAESCTR(data: ByteArray, key: String): ByteArray {
        val cipher = initCipher(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun decryptString(cipherText: String, key: ByteArray): String {
        val decryptedBytes = decryptStringToBytes(cipherText, key)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun decryptStringToBytes(str: String, key: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        val counter = key.copyOfRange(0, 16)
        val bytes = ByteArray(str.length) { index ->
            str[index].code.toByte()
        }

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(counter))

        return cipher.doFinal(bytes)
    }

    fun getKey(value: Any?): String {
        val bytes = when (value) {
            is Number -> {
                value.toString().map { char ->
                    if (char.isDigit()) {
                        char.digitToInt().toByte()
                    } else {
                        char.code.toByte()
                    }
                }.toByteArray()
            }
            else -> {
                value?.toString()?.toByteArray(Charsets.UTF_8) ?: byteArrayOf()
            }
        }

        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

}