package malibu.tracer.webmvc

interface TracerWebMvcConfigurer {

    fun configureTracerWebflux(context: TracerWebMvcContextApplyer)
}