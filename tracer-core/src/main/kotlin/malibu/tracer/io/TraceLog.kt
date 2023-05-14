package malibu.tracer.io

//@JsonPropertyOrder("type", "traceId", "spanId", "domain", "data")
abstract class TraceLog(

    /**
     *  log type
     *  각 타입별로 추가적인 필드를 가진다.
     */
    val type: String,

    val protocol: String? = null

//    /**
//     * 각 domain 별로 추가로 관리하고 싶은 필드들을 담은 객체
//     * 어플리케이션입장에서 중요한 정보들을 담는다.
//     */
//    open var data: D? = null
)
