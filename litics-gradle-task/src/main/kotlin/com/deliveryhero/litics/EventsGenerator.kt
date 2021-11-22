package com.deliveryhero.litics

import com.charleskorn.kaml.Yaml
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PACKAGE_LITICS = "com.deliveryhero.litics"

private const val EVENT_TRACKER_CLASS_NAME = "EventTracker"
private const val EVENT_TRACKERS_PROPERTY_NAME = "eventTrackers"

@Serializable
data class Events(
    val components: Components = Components(),
    @SerialName("events")
    val methodNameToEvents: Map<String, Event>,
)

@Serializable
data class Components(
    val parameters: Map<String, Map<String, Event.Parameter>> = emptyMap(),
)

@Serializable
data class Event(
    val name: String,
    val description: String,
    @SerialName("supported_platforms")
    val supportedPlatforms: List<String>,
    val required: List<String> = emptyList(),
    val parameters: Map<String, Parameter> = emptyMap(),
) {

    @Serializable
    data class Parameter(
        val description: String? = null,
        val type: String,
        val default: String? = null,
        val example: String? = null,
        val enum: List<String>? = null,
    )
}

private data class EventDefinition(
    val methodName: String,
    val methodDoc: String,
    val name: String,
    val parameters: List<ParamDefinition>,
    val supportedPlatforms: List<String>,
)

private data class ParamDefinition(
    val name: String,
    val type: String,
    val isRequired: Boolean,
    val defaultValue: String?,
)

object EventsGenerator {

    fun generate(packageName: String, sourceFile: String, targetDirectory: String) {

        //import EventTracker Class
        val eventTracker = ClassName(PACKAGE_LITICS, EVENT_TRACKER_CLASS_NAME)
        val eventTrackers = ARRAY.parameterizedBy(eventTracker)

        val funSpecs = mutableListOf<FunSpec>()
        val funImplSpecs = mutableListOf<FunSpec>()

        //Make func specs for interface and Impl of that interface
        buildFunSpecs(sourceFile, funSpecs, funImplSpecs)

        //Make interface GeneratedEventsAnalytics

        val generatedEventAnalyticsAbstractClass = ClassName(packageName, "GeneratedEventsAnalytics")
        val generatedEventAnalyticsClass = ClassName(packageName, "GeneratedEventsAnalyticsImpl")

        val interfaceTypeSpec = TypeSpec.classBuilder(generatedEventAnalyticsAbstractClass)
            .addModifiers(ABSTRACT)
            .addAnnotation(ClassName("kotlin.js", "JsExport"))
            .addFunctions(funSpecs)
            .build()

        //Make class GeneratedEventsAnalyticsImpl which implements GeneratedEventsAnalytics
        val interfaceImplTypeSpec = buildInterfaceImplTypeSpec(
            generatedEventAnalyticsAbstractClass,
            generatedEventAnalyticsClass,
            eventTrackers,
            funImplSpecs,
        )

        //Make the interface file
        val interfaceFileSpec = FileSpec
            .builder(generatedEventAnalyticsAbstractClass.packageName, generatedEventAnalyticsAbstractClass.simpleName)
            .addType(interfaceTypeSpec)
            .build()

        //Make the class file
        val interfaceImplFileSpec = FileSpec
            .builder(generatedEventAnalyticsClass.packageName, generatedEventAnalyticsClass.simpleName)
            .addType(interfaceImplTypeSpec)
            .build()

        val interfaceFile = File(targetDirectory)
        val interfaceImplFile = File(targetDirectory)

        //Write files to given directory
        interfaceFileSpec.writeTo(interfaceFile)
        interfaceImplFileSpec.writeTo(interfaceImplFile)
    }

    private fun buildInterfaceImplTypeSpec(
        generatedEventAnalyticsAbstractClass: ClassName,
        generatedEventAnalyticsClass: ClassName,
        eventTrackersParameterizedTypeName: ParameterizedTypeName,
        funImplSpecs: MutableList<FunSpec>,
    ): TypeSpec {

        //Make constructor for GeneratedEventsAnalyticsImpl
        val constructorFunSpec = FunSpec.constructorBuilder()
            .addParameter(EVENT_TRACKERS_PROPERTY_NAME, eventTrackersParameterizedTypeName)
            .build()

        //Make eventTrackers property for GeneratedEventsAnalyticsImpl
        val eventTrackersPropertySpec =
            PropertySpec.builder(EVENT_TRACKERS_PROPERTY_NAME, eventTrackersParameterizedTypeName)
                .initializer(EVENT_TRACKERS_PROPERTY_NAME)
                .addModifiers(KModifier.PRIVATE)
                .build()

        //Make class GeneratedEventsAnalyticsImpl
        return TypeSpec.classBuilder(generatedEventAnalyticsClass)
            .addAnnotation(ClassName("kotlin.js", "JsExport"))
            .primaryConstructor(constructorFunSpec)
            .superclass(generatedEventAnalyticsAbstractClass)
            .addProperty(eventTrackersPropertySpec)
            .addFunctions(funImplSpecs)
            .build()
    }

    private fun buildFunSpecs(
        sourceFile: String,
        funSpec: MutableList<FunSpec>,
        funImplSpec: MutableList<FunSpec>,
    ) {
        //Read event definitions from the given sourceDirectory
        val eventsDefinitions = buildEventDefinitions(File(sourceFile))
        eventsDefinitions.forEach { eventDefinition ->
            val interfaceFunParamsSpecs = mutableListOf<ParameterSpec>()
            val implFunParamSpecs = mutableListOf<ParameterSpec>()

            //Make ParamsSpecs for each param provided by definition
            eventDefinition.parameters.forEach { paramDefinition ->
                interfaceFunParamsSpecs.add(
                    buildParamSpec(paramDefinition, canAddDefault = true)
                )
                implFunParamSpecs.add(
                    buildParamSpec(paramDefinition, canAddDefault = false)
                )
            }

            //Make fun "methodName" with given params
            funSpec.add(buildFuncSpec(eventDefinition.methodName, eventDefinition.methodDoc, interfaceFunParamsSpecs))

            //Make fun "methodName" implementation with given params
            funImplSpec.add(
                buildFuncImplSpec(
                    eventDefinition.methodName,
                    eventDefinition.name,
                    implFunParamSpecs,
                    eventDefinition.supportedPlatforms
                )
            )
        }
    }

    private fun buildFuncSpec(
        methodName: String,
        methodDoc: String,
        funParams: MutableList<ParameterSpec>,
    ): FunSpec =
        FunSpec.builder(methodName)
            .addModifiers(ABSTRACT)
            .addKdoc(methodDoc)
            .addParameters(funParams)
            .build()

    private fun buildFuncImplSpec(
        methodName: String,
        eventName: String,
        funParamsSpecs: MutableList<ParameterSpec>,
        supportedPlatforms: List<String>,
    ): FunSpec {
        val trackingEvent = ClassName(PACKAGE_LITICS, "TrackingEvent")
        val trackingEventParameter = trackingEvent.nestedClass("Parameter")

        return FunSpec.builder(methodName)
            .addModifiers(OVERRIDE)
            .addParameters(funParamsSpecs)
            .addStatement("val params = mutableListOf<%T>()", trackingEventParameter)
            .addCode(buildCodeBlock {
                funParamsSpecs.forEach {
                    if (it.type.isNullable) {
                        beginControlFlow("if (%L != null)", it.name)
                        addStatement("params += %T(%S, %L)", trackingEventParameter, it.name, it.name)
                        endControlFlow()
                    } else {
                        addStatement("params += %T(%S, %L)", trackingEventParameter, it.name, it.name)
                    }
                }
            })
            .addCode(buildCodeBlock {
                val arrayOf = MemberName("kotlin", "arrayOf")
                val paramCodeBlocks = supportedPlatforms.map { CodeBlock.of("%S", it) }
                addStatement("val supportedPlatforms = %M(%L)", arrayOf, paramCodeBlocks.joinToCode())
            })
            .addStatement("val trackingEvent = %T(%S, params.toTypedArray())", trackingEvent, eventName)
            .addStatement("eventTrackers.filter·{ it.supportsEventTracking(supportedPlatforms) }.forEach·{ it.trackEvent(trackingEvent) }")
            .build()
    }

    // The canAddDefault variable is required as overridden methods cannot have default values
    private fun buildParamSpec(paramDefinition: ParamDefinition, canAddDefault: Boolean): ParameterSpec {
        val builder = ParameterSpec
            .builder(
                name = paramDefinition.name,
                type = String::class.asTypeName().copy(nullable = !paramDefinition.isRequired)
            )

        if (paramDefinition.defaultValue != null && canAddDefault) {
            builder.defaultValue(paramDefinition.defaultValue)
        }

        return builder.build()
    }

    private fun buildEventDefinitions(file: File): List<EventDefinition> {
        return Yaml.default.decodeFromStream(Events.serializer(), file.inputStream())
            .methodNameToEvents
            .map { (methodName, event) ->
                EventDefinition(
                    methodName = methodName,
                    methodDoc = event.description,
                    name = event.name,
                    parameters = event.parameters
                        .map { (parameterName, parameter) ->
                            ParamDefinition(
                                name = parameterName,
                                type = parameter.type,
                                isRequired = parameterName in event.required,
                                defaultValue = parameter.default,
                            )
                        },
                    supportedPlatforms = event.supportedPlatforms
                )
            }
    }
}
