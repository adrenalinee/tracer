package malibu.tracer.webclient

import org.springframework.web.reactive.function.client.ClientResponse
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class ClientResponseAndBodyHolder(
    originResponse: ClientResponse,

    /**
     * body cache 할지 여부
     */
    bodyHold: Boolean
) {

    /**
     * 400이상의 응답을 받을때 response body doOnComplete() 이벤트가 3번 실행되어서
     * logging 을 한번만 하기 위해서 최초 이벤트 발생인지 알려주는 역할을 함.
     */
    private var beforeDoOnComplate = true

    lateinit var onResponseBodyReadComplate: () -> Unit

    /**
     * 복사된 response body
     */
    private val responseBodyBaos = ByteArrayOutputStream()

    val clientResponse: ClientResponse

    /**
     *
     */
    init {
        clientResponse = originResponse.mutate()
            .body { bodyFlux ->
                if (bodyHold) {
                    bodyFlux.doOnNext { dataBuffer ->
                        Channels.newChannel(responseBodyBaos)
                            .write(dataBuffer.asByteBuffer())
                    }
                } else {
                    bodyFlux
                }
//                .doOnSubscribe { println(it) }
//                .doOnTerminate { println("doOnTerminate") }
                .doOnComplete {
                    if (beforeDoOnComplate) {
                        beforeDoOnComplate = false
                        onResponseBodyReadComplate()
                    }
                }
            }
            .build()
    }

    /**
     *
     */
    fun genResponseBody(): String? {
        return if (responseBodyBaos.size() <= 0) {
            null
        } else {
            String(responseBodyBaos.toByteArray())
        }
    }
}