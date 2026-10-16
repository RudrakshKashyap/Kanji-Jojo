@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package ua.syt0r.kanji.core

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

actual fun secureAuthBytes(): ByteArray = ByteArray(32).also { bytes ->
    bytes.usePinned { check(SecRandomCopyBytes(kSecRandomDefault, 32u, it.addressOf(0)) == errSecSuccess) }
}

actual fun sha256(bytes: ByteArray): ByteArray = ByteArray(32).also { result ->
    bytes.usePinned { source ->
        result.usePinned { destination ->
            CC_SHA256(source.addressOf(0), bytes.size.toUInt(), destination.addressOf(0).reinterpret())
        }
    }
}
