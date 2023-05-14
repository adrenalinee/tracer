package malibu.tracer.webclient

import malibu.tracer.TracerContext
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

/**
 * requestContext 를 subscriberContext 에 셋팅해준다.
 * 이걸 사용하지 않으면 webmvc 환경에서 traceId를 불러오지 못한다.
 * 수동으로 각 프로젝트에서 하드 코딩해도 된다.
 *
 * 이 필터를 TracerExchangeFilter 보다 먼저 등록해야 한다.
 * ex)
 * <code>
 * WebClient.builder()
 *   .filter(tracerWebMvcSupportFilter)
 *   .filter(tracerExchangeFilter)
 * </code>
 *
 */
class TracerWebMvcSupportFilter: ExchangeFilterFunction {

    /**
     * tracer webMvc module 이 참조되지 않았을 경우에 WebMvcTracerLogger class
     * 선언자체가 로딩시 에러를 발생시킨다. 그래서 Any로 일단 정의 해둠.
     */
    lateinit var webMvcTracerLogger: Any

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(request)
//            .subscriberContext { context ->
//                context.putNonNull(
//                    TracerContext.ATTR_REQUEST_CONTEXT,
//                    (webMvcTracerLogger as malibu.tracer.webmvc.WebMvcTracerLogger).getRequestContext())
//            }
            .contextWrite { context ->
                context.putNonNull(
                    TracerContext.ATTR_REQUEST_CONTEXT,
                    (webMvcTracerLogger as malibu.tracer.webmvc.WebMvcTracerLogger).getRequestContext()
                )
            }
    }
}