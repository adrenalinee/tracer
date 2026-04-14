package malibu.tracer.webmvc

interface TracerWebMvcConfigurer {

    fun configureTracerWebMvc(context: TracerWebMvcContextApplier)
}