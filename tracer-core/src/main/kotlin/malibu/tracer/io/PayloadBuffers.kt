package malibu.tracer.io

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset

private const val TRUNCATED_SUFFIX = "…[truncated]"

/**
 * [OutputStream] implementation that keeps at most [maxSize] bytes in memory.
 * Additional bytes are silently discarded while [isTruncated] is marked so
 * callers can append a truncation hint to logs.
 */
class LimitedByteArrayOutputStream(private val maxSize: Int) : OutputStream() {

    private val delegate = ByteArrayOutputStream()

    private val enforceLimit = maxSize > 0

    @Volatile
    private var truncated: Boolean = false

    override fun write(b: Int) {
        if (!enforceLimit || delegate.size() < maxSize) {
            delegate.write(b)
        } else {
            truncated = true
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (!enforceLimit) {
            delegate.write(b, off, len)
            return
        }

        if (len <= 0) {
            return
        }

        val spaceLeft = maxSize - delegate.size()
        if (spaceLeft <= 0) {
            truncated = true
            return
        }

        if (len <= spaceLeft) {
            delegate.write(b, off, len)
        } else {
            delegate.write(b, off, spaceLeft)
            truncated = true
        }
    }

    fun toByteArray(): ByteArray = delegate.toByteArray()

    fun size(): Int = delegate.size()

    fun isTruncated(): Boolean = truncated
}

/**
 * Convert [ByteArray] to a UTF-8 [String] while guarding against log spam by
 * limiting the number of characters written. When truncation happens a suffix
 * is appended so operators can tell that the payload was cut off.
 */
fun ByteArray.toLimitedString(maxLength: Int, charset: Charset = Charsets.UTF_8, truncated: Boolean = false): String {
    if (isEmpty()) {
        return ""
    }

    if (maxLength <= 0) {
        return String(this, charset)
    }

    val safeLength = minOf(size, maxLength)
    val base = String(this, 0, safeLength, charset)
    val needsSuffix = truncated || size > safeLength
    return if (needsSuffix) {
        base + TRUNCATED_SUFFIX
    } else {
        base
    }
}
