package malibu.tracer.io

/**
 * 미리 정의된 trace log type
 */
object DefinedTraceLogType {

    const val START = "START"

    const val FINISH = "FINISH"

    /**
     * 요청을 받은 것에 대한 정보를 로깅함
     */
    const val REQUEST = "REQUEST"

    /**
     * 요청에 대한 응답 정보를 로깅함
     */
    const val RESPONSE = "RESPONSE"

    /**
     * 외부 서버 연결에 대한 정보를 로깅함.
     */
    const val CONNECT = "CONNECT"
}