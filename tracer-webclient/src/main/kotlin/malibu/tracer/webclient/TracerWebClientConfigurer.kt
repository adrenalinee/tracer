package malibu.tracer.webclient

interface TracerWebClientConfigurer {

    fun configureTracerWebClient(context: TracerWebClientContextApplyer)
}