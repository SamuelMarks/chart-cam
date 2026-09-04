@file:Suppress(
    "CAST_NEVER_SUCCEEDS",
    "UNCHECKED_CAST",
)
@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
/**
 * iOS implementation of the SecureStorage interface.
 * Uses the iOS Keychain Services for storing secure data such as authentication tokens.
 */

package io.healthplatform.chartcam.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS-specific implementation of [SecureStorage] utilizing Keychain.
 */
class IosSecureStorage : SecureStorage {
    /**
     * The service name used for Keychain queries, acting as an identifier for this application's secure data.
     */
    private val serviceName = "io.healthplatform.chartcam.auth"

    /**
     * Saves a key-value pair to the iOS Keychain.
     * If the key already exists, its value is updated. Otherwise, a new item is added.
     *
     * @param key The key under which the value should be stored.
     * @param value The value to securely store.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun save(
        key: String,
        value: String,
    ) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass as platform.Foundation.NSCopyingProtocol)
        query.setObject(serviceName as NSString, forKey = kSecAttrService as platform.Foundation.NSCopyingProtocol)
        query.setObject(key as NSString, forKey = kSecAttrAccount as platform.Foundation.NSCopyingProtocol)

        val attributesToUpdate = NSMutableDictionary()
        attributesToUpdate.setObject(data, forKey = kSecValueData as platform.Foundation.NSCopyingProtocol)

        val status =
            SecItemUpdate(
                CFBridgingRetain(query) as CFDictionaryRef,
                CFBridgingRetain(attributesToUpdate) as CFDictionaryRef,
            )

        if (status != 0) {
            query.setObject(data, forKey = kSecValueData as platform.Foundation.NSCopyingProtocol)
            SecItemAdd(CFBridgingRetain(query) as CFDictionaryRef, null)
        }
    }

    /**
     * Retrieves a stored string value from the iOS Keychain.
     *
     * @param key The key associated with the value to retrieve.
     * @return The stored string value, or null if the key is not found or an error occurs.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun getString(key: String): String? {
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass as platform.Foundation.NSCopyingProtocol)
        query.setObject(serviceName as NSString, forKey = kSecAttrService as platform.Foundation.NSCopyingProtocol)
        query.setObject(key as NSString, forKey = kSecAttrAccount as platform.Foundation.NSCopyingProtocol)
        query.setObject(NSNumber(true), forKey = kSecReturnData as platform.Foundation.NSCopyingProtocol)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as platform.Foundation.NSCopyingProtocol)

        val result =
            memScoped {
                val resultPtr = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(CFBridgingRetain(query) as CFDictionaryRef, resultPtr.ptr)
                if (status == 0) resultPtr.value else null
            }

        if (result == null) return null

        val nsData = CFBridgingRelease(result) as? NSData
        return nsData?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }
    }

    /**
     * Deletes a securely stored key-value pair from the iOS Keychain.
     *
     * @param key The key of the item to delete.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun delete(key: String) {
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass as platform.Foundation.NSCopyingProtocol)
        query.setObject(serviceName as NSString, forKey = kSecAttrService as platform.Foundation.NSCopyingProtocol)
        query.setObject(key as NSString, forKey = kSecAttrAccount as platform.Foundation.NSCopyingProtocol)

        SecItemDelete(CFBridgingRetain(query) as CFDictionaryRef)
    }
}

/**
 * Creates and returns the iOS-specific instance of [SecureStorage].
 *
 * @return An instance of [IosSecureStorage].
 */
actual fun createSecureStorage(): SecureStorage = IosSecureStorage()
