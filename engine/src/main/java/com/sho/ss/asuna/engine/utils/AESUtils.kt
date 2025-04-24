package com.sho.ss.asuna.engine.utils

import org.apaches.commons.codec.binary.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @project  启源视频
 * @author   Sho Tan.
 * @e-mail   2943343823@qq.com
 * @created 2023/7/6 11:48:37
 * @description  AES加解密工具类
 **/
object AESUtils {

    private const val AES_ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding" //相同CBC下 PKCS7 与 PKCS5 兼容
    private const val IV_LENGTH = 16 // AES 块大小

    @JvmStatic
    @JvmOverloads
    fun decrypt(cipherStr: String, key: String, ivStr: String, base64Dec: Boolean = true)
            = decrypt(cipherStr, key, ivStr.toByteArray(), base64Dec)

    @JvmStatic
    @JvmOverloads
    fun decrypt(cipherStr: String, key: String, ivBytes: ByteArray, base64Dec: Boolean = true): String? {
        val iv = IvParameterSpec(ivBytes)
        val secretKey = SecretKeySpec(key.toByteArray(), AES_ALGORITHM)
        return cipherStr.runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            //前端的密文要先base64解密
            String(
                cipher.doFinal(
                    if (base64Dec) Base64.decodeBase64(
                        this
                    ) else this.toByteArray()
                ), StandardCharsets.UTF_8
            )
        }.getOrNull()
    }

    @JvmStatic
    fun encrypt(input: String, key: String, ivStr: String) =
        encrypt(input, key, ivStr.toByteArray())

    @JvmStatic
    fun encrypt(input: String, key: String, ivBytes: ByteArray): String? {
        return input.runCatching {
            val secretKeySpec = SecretKeySpec(key.toByteArray(), AES_ALGORITHM)
            val iv = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)
            val encryptedBytes = cipher.doFinal(this.toByteArray())
            Base64.encodeBase64String(encryptedBytes)
        }.getOrNull()
    }

    @JvmStatic
    fun generateIV(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }
}