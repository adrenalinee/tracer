package malibu.tracer.io

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("runId", "traceId", "spanId", "domain")
data class Trace(
    /**
     * trace log 들을 하나의 group으로 묶기 위한 키
     * request 에서부터 response 까지를 하나의
     */
    val traceId: String?,

    /**
     * log가 발생한 도메인
     * 카테고리의 역할을 한다.
     */
    val domain: String?,

    /**
     *
     */
    val spanId: Int?,

    /**
     *
     */
    val runId: String? = null
)
