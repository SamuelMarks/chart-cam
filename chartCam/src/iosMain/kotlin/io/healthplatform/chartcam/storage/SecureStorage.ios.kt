package io.healthplatform.chartcam.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Foundation.CFBridgingRetain
import platform.Foundation.CFBridgingRelease

class IosSecureStorage : SecureStorage {
    
    private val serviceName = "io.healthplatform.chartcam.auth"

    @OptIn(ExperimentalForeignApi::class)
    override fun save(key: String, value: String) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass as platform.Foundation.NSCopyingProtocol)
        query.setObject(serviceName as NSString, forKey = kSecAttrService as platform.Foundation.NSCopyingProtocol)
        query.setObject(key as NSString, forKey = kSecAttrAccount as platform.Foundation.NSCopyingProtocol)

        val attributesToUpdate = NSMutableDictionary()
        attributesToUpdate.setObject(data, forKey = kSecValueData as platform.Foundation.NSCopyingProtocol)
        
        val status = SecItemUpdate(
            CFBridgingRetain(query) as CFDictionaryRef,
            CFBridgingRetain(attributesToUpdate) as CFDictionaryRef
        )
        
        if (status != 0) {
           query.setObject(data, forKey = kSecValueData as platform.Foundation.NSCopyingProtocol)
           SecItemAdd(CFBridgingRetain(query) as CFDictionaryRef, null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getString(key: String): String? {
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass as platform.Foundation.NSCopyingProtocol)
        query.setObject(serviceName as NSString, forKey = kSecAttrService as platform.Foundation.NSCopyingProtocol)
        query.setObject(key as NSString, forKey = kSecAttrAccount as platform.Foundation.NSCopyingProtocol)
        query.setObject(NSNumber(true), forKey = kSecReturnData as platform.Foundation.NSCopyingProtocol)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as platform.Foundation.NSCopyingProtocol)

        val result = memScoped {
            val resultPtr = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(CFBridgingRetain(query) as CFDictionaryRef, resultPtr.ptr)
            if (status == 0) resultPtr.value else null
        }
        
        if (result == null) return null
        
        val nsData = CFBridgingRelease(result) as? NSData
        return nsData?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun delete(key: String) {
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass as platform.Foundation.NSCopyingProtocol)
        query.setObject(serviceName as NSString, forKey = kSecAttrService as platform.Foundation.NSCopyingProtocol)
        query.setObject(key as NSString, forKey = kSecAttrAccount as platform.Foundation.NSCopyingProtocol)
        
        SecItemDelete(CFBridgingRetain(query) as CFDictionaryRef)
    }
}

actual fun createSecureStorage(): SecureStorage = IosSecureStorage()
