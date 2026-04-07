package malibu.tracer.webmvc

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import malibu.tracer.io.LimitedByteArrayOutputStream
import malibu.tracer.io.toLimitedString
import org.springframework.http.MediaType
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

class TracingHttpServletResponseWrapper(
    response: HttpServletResponse,
    private val maxPayloadLength: Int
) : HttpServletResponseWrapper(response) {

    companion object {
        private const val SSE_EVENT_DELIMITER = "\n\n"
        private const val SSE_EVENT_DELIMITER_CRLF = "\r\n\r\n"
    }

    var onResponseSseEvent: ((String) -> Unit)? = null

    private val responseBodyBaos = LimitedByteArrayOutputStream(maxPayloadLength)
    private val sseEventBuffer = StringBuilder()
    private val responseCompleted = AtomicBoolean(false)

    private var captureOutputStream: ServletOutputStream? = null
    private var captureWriter: PrintWriter? = null

    override fun getOutputStream(): ServletOutputStream {
        if (captureWriter != null) {
            throw IllegalStateException("getWriter() has already been called on this response.")
        }

        return captureOutputStream ?: TeeServletOutputStream(super.getOutputStream()).also {
            captureOutputStream = it
        }
    }

    override fun getWriter(): PrintWriter {
        if (captureOutputStream != null) {
            throw IllegalStateException("getOutputStream() has already been called on this response.")
        }

        return captureWriter ?: PrintWriter(OutputStreamWriter(getOutputStream(), currentCharset()), true).also {
            captureWriter = it
        }
    }

    override fun flushBuffer() {
        captureWriter?.flush()
        captureOutputStream?.flush()
        super.flushBuffer()
    }

    fun genResponseBody(): String? {
        if (responseBodyBaos.size() <= 0) {
            return null
        }

        return responseBodyBaos.toByteArray()
            .toLimitedString(maxPayloadLength, charset = currentCharset(), truncated = responseBodyBaos.isTruncated())
    }

    fun getContentSize(): Int {
        return responseBodyBaos.size()
    }

    fun notifyResponseComplete() {
        if (responseCompleted.compareAndSet(false, true).not()) {
            return
        }

        captureWriter?.flush()
        captureOutputStream?.flush()

        if (isSseResponse()) {
            val pendingEvent = sseEventBuffer.toString().trimEnd('\r', '\n')
            if (pendingEvent.isNotBlank()) {
                onResponseSseEvent?.invoke(pendingEvent)
            }
            sseEventBuffer.setLength(0)
        }
    }

    private fun capture(bytes: ByteArray, off: Int, len: Int) {
        if (len <= 0) {
            return
        }

        responseBodyBaos.write(bytes, off, len)

        if (isSseResponse()) {
            sseEventBuffer.append(String(bytes, off, len, currentCharset()))
            emitCompletedSseEvents()
        }
    }

    private fun emitCompletedSseEvents() {
        while (true) {
            val delimiterInfo = findSseDelimiter() ?: return
            val event = sseEventBuffer.substring(0, delimiterInfo.first)
                .trimEnd('\r', '\n')

            sseEventBuffer.delete(0, delimiterInfo.second)

            if (event.isNotBlank()) {
                onResponseSseEvent?.invoke(event)
            }
        }
    }

    private fun findSseDelimiter(): Pair<Int, Int>? {
        val crlfIndex = sseEventBuffer.indexOf(SSE_EVENT_DELIMITER_CRLF)
        val lfIndex = sseEventBuffer.indexOf(SSE_EVENT_DELIMITER)

        return when {
            crlfIndex >= 0 && (lfIndex < 0 || crlfIndex <= lfIndex) -> {
                crlfIndex to crlfIndex + SSE_EVENT_DELIMITER_CRLF.length
            }
            lfIndex >= 0 -> lfIndex to lfIndex + SSE_EVENT_DELIMITER.length
            else -> null
        }
    }

    private fun isSseResponse(): Boolean {
        val contentType = contentType ?: return false
        return try {
            MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.TEXT_EVENT_STREAM)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun currentCharset(): Charset {
        val charsetName = characterEncoding?.takeIf { it.isNotBlank() } ?: Charsets.UTF_8.name()
        return Charset.forName(charsetName)
    }

    private inner class TeeServletOutputStream(
        private val delegate: ServletOutputStream
    ) : ServletOutputStream() {

        override fun isReady(): Boolean {
            return delegate.isReady
        }

        override fun setWriteListener(writeListener: WriteListener?) {
            delegate.setWriteListener(writeListener)
        }

        override fun write(b: Int) {
            delegate.write(b)
            responseBodyBaos.write(b)

            if (isSseResponse()) {
                sseEventBuffer.append(String(byteArrayOf(b.toByte()), currentCharset()))
                emitCompletedSseEvents()
            }
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            capture(b, off, len)
        }

        override fun flush() {
            delegate.flush()
        }

        override fun close() {
            delegate.close()
        }
    }
}
