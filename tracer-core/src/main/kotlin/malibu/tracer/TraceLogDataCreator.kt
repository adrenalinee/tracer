package malibu.tracer

/**
 * @param D - 로그 환경에 따라서 context 객체는 바뀐다.
 */
interface TraceLogDataCreator<D> {
    fun create(logContext: Any): D?
}