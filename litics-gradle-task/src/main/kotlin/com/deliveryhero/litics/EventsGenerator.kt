package com.deliveryhero.litics

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
import org.yaml.snakeyaml.Yaml

private const val PACKAGE_LITICS = "com.deliveryhero.litics"

private const val EVENT_TRACKER_CLASS_NAME = "EventTracker"
private const val EVENT_TRACKERS_PROPERTY_NAME = "eventTrackers"

private const val DESCRIPTION = "description"
private const val SUPPORTED_PLATFORMS = "supported_platforms"
private const val PROPERTIES = "properties"
private const val REQUIRED = "required"
private const val TYPE = "type"
private const val DEFAULT = "default"
private const val PARAMS = "params"
private const val BASE_EVENT_PARAMS = "base_event_params"
private const val BASE_ORDER_EVENT_PARAMS = "base_order_event_params"
private const val BASE_VENDOR_CHECK_IN_EVENT_PARAMS = "base_vendor_checkin_event_params"

private typealias EventPropertiesMap = Map<*, *>

private data class EventDefinition(
    val methodName: String,
    val methodDoc: String = "",
    val eventName: String,
    val eventParams: List<ParamDefinition>,
    val supportedPlatforms: List<String>,
)

private data class ParamDefinition(
    val paramName: String,
    val paramType: String,
    val isRequired: Boolean,
    val defaultValue: String?,
)

object EventsGenerator {

    fun generate(packageName: String, sourceDirectory: String, targetDirectory: String) {

        //import EventTracker Class
        val eventTracker = ClassName(PACKAGE_LITICS, EVENT_TRACKER_CLASS_NAME)
        val eventTrackers = ARRAY.parameterizedBy(eventTracker)

        val funSpecs = mutableListOf<FunSpec>()
        val funImplSpecs = mutableListOf<FunSpec>()

        //Make func specs for interface and Impl of that interface
        buildFunSpecs(sourceDirectory, funSpecs, funImplSpecs)

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
        source: String,
        funSpec: MutableList<FunSpec>,
        funImplSpec: MutableList<FunSpec>,
    ) {
        //Read event definitions from the given sourceDirectory
        val eventsDefinitions = getEventDefinitions(source)
        eventsDefinitions.forEach { eventDefinition ->
            val interfaceFunParamsSpecs = mutableListOf<ParameterSpec>()
            val implFunParamSpecs = mutableListOf<ParameterSpec>()

            //Make ParamsSpecs for each param provided by definition
            eventDefinition.eventParams.forEach { paramDefinition ->
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
                    eventDefinition.eventName,
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
                val listOf = MemberName("kotlin", "arrayOf")
                val paramCodeBlocks = supportedPlatforms.map { CodeBlock.of("%S", it) }
                addStatement("val supportedPlatforms = %M(%L)", listOf, paramCodeBlocks.joinToCode())
            })
            .addStatement("val trackingEvent = %T(%S, params.toTypedArray())", trackingEvent, eventName)
            .addStatement("eventTrackers.filter·{ it.supportsEventTracking(supportedPlatforms) }.forEach·{ it.trackEvent(trackingEvent) }")
            .build()
    }

    // The canAddDefault variable is required as overridden methods cannot have default values
    private fun buildParamSpec(paramDefinition: ParamDefinition, canAddDefault: Boolean): ParameterSpec {
        val builder = ParameterSpec
            .builder(
                name = paramDefinition.paramName,
                type = String::class.asTypeName().copy(nullable = !paramDefinition.isRequired)
            )

        if (paramDefinition.defaultValue != null && canAddDefault) {
            builder.defaultValue(paramDefinition.defaultValue)
        }

        return builder.build()
    }

    private fun getEventDefinitions(source: String): List<EventDefinition> =
        File(source).listFiles()?.map(this::buildEventDefinition) ?: emptyList()

    private fun buildEventDefinition(file: File): EventDefinition {
        //Load event definition as a map
        val eventDetails = Yaml().load(file.inputStream()) as EventPropertiesMap

        //First key is the methodName
        val methodName = eventDetails.keys.first() as String

        //Get event properties key form the map
        val methodDoc = getEventDescription(eventDetails)

        //Get event properties key form the map
        val eventProperties = getEventProperties(eventDetails)

        //Get required items for the event
        val requiredItems = getEventRequiredProperties(eventDetails)

        //Get support analytics platforms for the event
        val supportedPlatforms = getEventSupportedPlatforms(eventDetails)

        //First key of properties is the eventName
        val eventName = eventProperties.keys.first() as String

        //Get base params that need to be sent for each event
        val baseEventParams = eventProperties[BASE_EVENT_PARAMS]

        //Get base params that need to be sent for each order related event
        val baseOrderEventParams = eventProperties[BASE_ORDER_EVENT_PARAMS]

        //Get base params that need to be sent for each vendor check-in related event
        val baseVendorCheckInParams = eventProperties[BASE_VENDOR_CHECK_IN_EVENT_PARAMS]

        val eventParams = mutableListOf<ParamDefinition>()

        //Read event properties
        eventParams += readParamsFromMap(eventProperties, requiredItems)

        //Go to base params file and read properties if any
        if (baseEventParams != null) {
            eventParams += resolveBaseParams(file, baseEventParams as EventPropertiesMap, requiredItems)
        }

        //Go to base order related event file and read properties if any
        if (baseOrderEventParams != null) {
            eventParams += resolveBaseParams(file, baseOrderEventParams as EventPropertiesMap, requiredItems)
        }

        //Go to base vendor check-in related event file and read properties if any
        if (baseVendorCheckInParams != null) {
            eventParams += resolveBaseParams(file, baseVendorCheckInParams as EventPropertiesMap, requiredItems)
        }

        return EventDefinition(
            methodName = methodName,
            methodDoc = methodDoc,
            eventName = eventName,
            eventParams = eventParams,
            supportedPlatforms = supportedPlatforms
        )
    }

    private fun resolveBaseParams(
        file: File,
        baseParams: EventPropertiesMap,
        requiredParams: List<String>,
    ): List<ParamDefinition> {
        val value = file.resolveSibling(baseParams.values.first() as String)
        val baseEventDetails = Yaml().load(value.inputStream()) as EventPropertiesMap
        val baseEventProperties = getEventProperties(baseEventDetails)
        return readParamsFromMap(baseEventProperties, requiredParams)
    }

    private fun readParamsFromMap(
        properties: EventPropertiesMap,
        requiredParams: List<String>,
    ): List<ParamDefinition> {
        return (properties[PARAMS] as? EventPropertiesMap)
            .orEmpty()
            .map { (key, value) ->
                val paramName = key as String
                val paramTypeMap = value as EventPropertiesMap
                val paramType = paramTypeMap[TYPE] as String
                val defaultValue = paramTypeMap[DEFAULT] as String?
                ParamDefinition(
                    paramName = paramName,
                    paramType = paramType,
                    isRequired = requiredParams.contains(paramName),
                    defaultValue = defaultValue,
                )
            }
    }

    private fun getEventProperties(eventDetails: EventPropertiesMap) =
        (eventDetails.values.first() as EventPropertiesMap)[PROPERTIES] as EventPropertiesMap

    private fun getEventDescription(eventDetails: EventPropertiesMap) =
        (eventDetails.values.first() as EventPropertiesMap)[DESCRIPTION] as? String ?: ""

    @Suppress("UNCHECKED_CAST")
    private fun getEventRequiredProperties(eventDetails: EventPropertiesMap) =
        (eventDetails.values.first() as EventPropertiesMap)[REQUIRED]
            ?.let { it as List<String> } ?: listOf()

    @Suppress("UNCHECKED_CAST")
    private fun getEventSupportedPlatforms(eventDetails: EventPropertiesMap): List<String> =
        (eventDetails.values.first() as EventPropertiesMap)[SUPPORTED_PLATFORMS] as List<String>
}
