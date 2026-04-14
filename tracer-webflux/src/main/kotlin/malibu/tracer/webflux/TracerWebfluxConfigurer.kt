package malibu.tracer.webflux

interface TracerWebfluxConfigurer {

    fun configureTracerWebflux(context: TracerWebfluxContextApplier)
}