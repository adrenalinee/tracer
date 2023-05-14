package malibu.tracer.webmvc

import malibu.tracer.EachRequestContext

internal class RequestContextHolder {
    companion object {
        private val requestContextThreadLocal = ThreadLocal<malibu.tracer.EachRequestContext>()

        fun createFrom(context: malibu.tracer.EachRequestContext) {
            requestContextThreadLocal.set(context)
        }

        fun context(): malibu.tracer.EachRequestContext {
            return requestContextThreadLocal.get()
        }

        fun contextOrNull(): malibu.tracer.EachRequestContext? {
            return requestContextThreadLocal.get()
        }

//        fun <T> getAttribute(key: String): T {
//            return requestContextThreadLocal.get()?.let {
//                it.get(key) as T
//            }?: throw RuntimeException("requestContext 가 만들어지지 않았습니다. key: $key")
//        }
//
//        fun setAttribute(key: String, value: Any) {
//            requestContextThreadLocal.get()
//                .put(key, value)
//        }

        fun remove() {
            requestContextThreadLocal.remove()
        }
    }
}
