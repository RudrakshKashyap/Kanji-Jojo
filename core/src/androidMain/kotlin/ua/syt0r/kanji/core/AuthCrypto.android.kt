package ua.syt0r.kanji.core

import java.security.MessageDigest
import java.security.SecureRandom

actual fun secureAuthBytes(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
actual fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
