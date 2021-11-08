package com.deliveryhero.litics

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock.Builder
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import org.yaml.snakeyaml.Yaml
import java.io.File
import javax.inject.Inject

private const val PACKAGE_LITICS = "com.deliveryhero.litics"

private const val KOTLIN_COLLECTIONS_PACKAGE_NAME = "kotlin.collections"
private const val ARRAY_LIST_CLASS_NAME = "ArrayList"

private const val GENERATED_EVENT_ANALYTICS_PACKAGE_NAME = "com.deliveryhero.rps.analytics.generated"
private const val GENERATED_EVENT_ANALYTICS_INTERFACE_NAME = "GeneratedEventsAnalytics"
private const val GENERATED_EVENT_ANALYTICS_CLASS_NAME = "GeneratedEventsAnalyticsImpl"

private const val EVENTS_ANALYTICS_PACKAGE_NAME = "com.deliveryhero.rps.analytics.events"
private const val EVENT_ANALYTICS_PROPERTY_NAME = "eventAnalytics"

private const val EVENT_ANALYTICS_CLASS_NAME = "EventsAnalytics"

private const val TRACKING_EVENT_CLASS_NAME = "TrackingEvent"

private const val ANALYTICS_PLATFORM_CLASS_NAME = "AnalyticsPlatform"
private const val ANALYTICS_PLATFORM_PACKAGE_NAME = "$PACKAGE_LITICS.$ANALYTICS_PLATFORM_CLASS_NAME"

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

private const val INDENT = "    "

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

    fun generate(sourceDirectory: String, targetDirectory: String) {

        //import EventAnalytics Class
        val eventAnalytics = ClassName(EVENTS_ANALYTICS_PACKAGE_NAME, EVENT_ANALYTICS_CLASS_NAME)

        //import TrackingEvent Class
        val trackingEvent = ClassName(PACKAGE_LITICS, TRACKING_EVENT_CLASS_NAME)

        val funSpecs = mutableListOf<FunSpec>()
        val funImplSpecs = mutableListOf<FunSpec>()

        //Make func specs for interface and Impl of that interface
        buildFunSpecs(sourceDirectory, trackingEvent, funSpecs, funImplSpecs)

        //Make interface GeneratedEventsAnalytics

        val interfaceTypeSpec = buildInterfaceTypeSpec(funSpecs)

        //Make class GeneratedEventsAnalyticsImpl which implements GeneratedEventsAnalytics
        val interfaceImplTypeSpec = buildInterfaceImplTypeSpec(eventAnalytics, funImplSpecs)

        //Make the interface file
        val interfaceFileSpec = buildFileSpec(
            fileName = GENERATED_EVENT_ANALYTICS_INTERFACE_NAME,
            typeSpec = interfaceTypeSpec
        )

        //Make the class file
        val interfaceImplFileSpec = buildFileSpec(
            fileName = GENERATED_EVENT_ANALYTICS_CLASS_NAME,
            typeSpec = interfaceImplTypeSpec
        )

        val interfaceFile = File(targetDirectory)
        val interfaceImplFile = File(targetDirectory)

        //Write files to given directory
        interfaceFileSpec.writeTo(interfaceFile)
        interfaceImplFileSpec.writeTo(interfaceImplFile)
    }

    private fun buildInterfaceImplTypeSpec(eventAnalyticsClassName: ClassName, funImplSpecs: MutableList<FunSpec>): TypeSpec {

        //Make constructor for GeneratedEventsAnalyticsImpl
        val constructorFunSpec = buildConstructorFunSpec(eventAnalyticsClassName)

        //Make eventAnalytics property for GeneratedEventsAnalyticsImpl
        val eventAnalyticsPropertySpec = PropertySpec.builder(EVENT_ANALYTICS_PROPERTY_NAME, eventAnalyticsClassName)
            .initializer(EVENT_ANALYTICS_PROPERTY_NAME)
            .addModifiers(KModifier.PRIVATE)
            .build()

        //Make class GeneratedEventsAnalyticsImpl
        return TypeSpec.classBuilder(GENERATED_EVENT_ANALYTICS_CLASS_NAME)
            .primaryConstructor(constructorFunSpec)
            .addSuperinterface(ClassName(GENERATED_EVENT_ANALYTICS_PACKAGE_NAME, GENERATED_EVENT_ANALYTICS_INTERFACE_NAME))
            .addProperty(eventAnalyticsPropertySpec)
            .addFunctions(funImplSpecs)
            .build()
    }

    private fun buildConstructorFunSpec(eventAnalyticsClassName: ClassName): FunSpec =
        FunSpec.constructorBuilder()
            .addAnnotation(Inject::class)
            .addParameter(EVENT_ANALYTICS_PROPERTY_NAME, eventAnalyticsClassName)
            .build()

    private fun buildInterfaceTypeSpec(funSpecs: MutableList<FunSpec>): TypeSpec =
        TypeSpec.interfaceBuilder(GENERATED_EVENT_ANALYTICS_INTERFACE_NAME)
            .addFunctions(funSpecs)
            .build()

    private fun buildFileSpec(fileName: String, typeSpec: TypeSpec): FileSpec =
        FileSpec.builder(
            packageName = GENERATED_EVENT_ANALYTICS_PACKAGE_NAME,
            fileName = fileName
        )
            .addType(typeSpec)
            .indent(INDENT)
            .build()

    private fun buildFunSpecs(
        source: String,
        trackingEventClassName: ClassName,
        funSpec: MutableList<FunSpec>,
        funImplSpec: MutableList<FunSpec>,
    ) {
        //Read event definitions from the given sourceDirectory
        val eventsDefinitions = getEventDefinitions(source)
        eventsDefinitions.forEach {
            val eventName = it.eventName
            val methodName = it.methodName
            val methodDoc = it.methodDoc
            val eventParams = it.eventParams
            val supportedPlatforms = it.supportedPlatforms

            val interfaceFunParamsSpecs = mutableListOf<ParameterSpec>()
            val implFunParamSpecs = mutableListOf<ParameterSpec>()

            //Make ParamsSpecs for each param provided by definition
            eventParams.forEach { paramDefinition ->
                interfaceFunParamsSpecs.add(
                    buildParamSpec(paramDefinition, canAddDefault = true)
                )
                implFunParamSpecs.add(
                    buildParamSpec(paramDefinition, canAddDefault = false)
                )
            }

            //Make fun "methodName" with given params
            funSpec.add(buildFuncSpec(methodName, methodDoc, interfaceFunParamsSpecs))

            //Make fun "methodName" implementation with given params
            funImplSpec.add(
                buildFuncImplSpec(methodName, eventName, implFunParamSpecs, trackingEventClassName, supportedPlatforms)
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
        trackingEventClassName: ClassName,
        supportedPlatforms: List<String>,
    ): FunSpec =
        FunSpec.builder(methodName)
            .addModifiers(OVERRIDE)
            .addParameters(funParamsSpecs)
            .addStatement("val params = mutableMapOf<String, String>()")
            .addCode(buildCodeBlock {
                funParamsSpecs.forEach {
                    if (it.type.isNullable) {
                        addStatement("if (%L != null)", it.name)
                        addStatement("    params[%S] = %L", it.name, it.name)
                    } else {
                        addStatement("params[%S] = %L", it.name, it.name)
                    }
                }
            })
            .addCode(buildCodeBlock {
                val platformAnalytics = ClassName(PACKAGE_LITICS, ANALYTICS_PLATFORM_CLASS_NAME)
                val arrayList = ClassName(KOTLIN_COLLECTIONS_PACKAGE_NAME, ARRAY_LIST_CLASS_NAME)
                val arrayListOfAnalyticsPlatform = arrayList.parameterizedBy(platformAnalytics)
                addStatement("val supportedPlatforms = %T()", arrayListOfAnalyticsPlatform)
                supportedPlatforms.forEach { addAnalyticsPlatform(it) }
            })
            .addStatement("val trackingEvent = %T(%S, params, supportedPlatforms)", trackingEventClassName, eventName)
            .addStatement("eventAnalytics.trackEvent(trackingEvent)")
            .build()

    private fun Builder.addAnalyticsPlatform(it: String) {
        val analyticsPlatform = ClassName(ANALYTICS_PLATFORM_PACKAGE_NAME, it)
        addStatement("supportedPlatforms += %T", analyticsPlatform)
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
        println("buildEventDefinition for file -> $file")
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
        readParamsFromMap(eventProperties, requiredItems) { eventParams += it }

        //Go to base params file and read properties if any
        if (baseEventParams != null) {
            resolveBaseParams(file, baseEventParams as EventPropertiesMap, requiredItems) { eventParams += it }
        }

        //Go to base order related event file and read properties if any
        if (baseOrderEventParams != null) {
            resolveBaseParams(file, baseOrderEventParams as EventPropertiesMap, requiredItems) { eventParams += it }
        }

        //Go to base vendor check-in related event file and read properties if any
        if (baseVendorCheckInParams != null) {
            resolveBaseParams(file, baseVendorCheckInParams as EventPropertiesMap, requiredItems) { eventParams += it }
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
        callback: (ParamDefinition) -> Unit,
    ) {
        val value = file.resolveSibling(baseParams.values.first() as String)
        val baseEventDetails = Yaml().load(value.inputStream()) as EventPropertiesMap
        val baseEventProperties = getEventProperties(baseEventDetails)
        readParamsFromMap(baseEventProperties, requiredParams, callback)
    }

    private fun readParamsFromMap(
        properties: EventPropertiesMap,
        requiredParams: List<String>,
        callback: (ParamDefinition) -> Unit,
    ) {
        val params = properties[PARAMS] as? EventPropertiesMap

        params?.forEach { (key, value) ->
            val paramName = key as String
            val paramTypeMap = value as EventPropertiesMap
            val paramType = paramTypeMap[TYPE] as String
            val defaultValue = paramTypeMap[DEFAULT] as String?
            val paramDefinition = ParamDefinition(
                paramName = paramName,
                paramType = paramType,
                isRequired = requiredParams.contains(paramName),
                defaultValue = defaultValue,
            )
            callback.invoke(paramDefinition)
        }
    }

    private fun getEventProperties(eventDetails: EventPropertiesMap) = (eventDetails.values.first() as EventPropertiesMap)[PROPERTIES] as EventPropertiesMap

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
