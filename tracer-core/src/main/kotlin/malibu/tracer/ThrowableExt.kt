package malibu.tracer

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.traceToString(): String {
    val sw = StringWriter()
    this.printStackTrace(PrintWriter(sw))

    return sw.toString()
}