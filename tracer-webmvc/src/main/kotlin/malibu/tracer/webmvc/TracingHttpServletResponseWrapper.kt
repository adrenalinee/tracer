package malibu.tracer.webmvc

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import malibu.tracer.io.LimitedByteArrayOutputStream
import malibu.tracer.io.toLimitedString
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

class TracingHttpServletResponseWrapper(
    response: HttpServletResponse,
    private val maxPayloadLength: Int
) : HttpServletResponseWrapper(response) {

    private val responseBodyBaos = LimitedByteArrayOutputStream(maxPayloadLength)
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
    }

    private fun capture(bytes: ByteArray, off: Int, len: Int) {
        if (len <= 0) {
            return
        }

        responseBodyBaos.write(bytes, off, len)
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
