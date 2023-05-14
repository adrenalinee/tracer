package malibu.tracer

class EachRequestContext(
    val url: String,
    val path: String,
    val isInclude: Boolean,
    val traceSpanId: malibu.tracer.TraceSpanId?
) {
//    lateinit var traceSpanId: TraceSpanId

//    private lateinit var error: Throwable
//
//    fun setError(error: Throwable) {
//        if (this::error.isInitialized) {
//            throw RuntimeException("error가 이미 셋팅되었습니다. 현재 error: $error")
//        }
//        this.error = error
//    }
//
//    fun getError(): Throwable? {
//        return if (this::error.isInitialized) {
//            error
//        } else {
//            null
//        }
//    }
}
