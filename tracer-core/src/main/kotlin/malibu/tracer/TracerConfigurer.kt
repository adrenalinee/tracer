package malibu.tracer

interface TracerConfigurer {
    fun configureTracer(context: TracerContextApplyer) {}
    fun configureDataCreator(registry: TraceDataCreatorRegistry) {}
}