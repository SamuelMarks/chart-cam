import okio.ByteString.Companion.toByteString

fun testBase64(bytes: ByteArray): String {
    return bytes.toByteString().base64()
}
