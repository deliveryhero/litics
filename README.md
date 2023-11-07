# Litics

Litics allows you to have a single source of tracking event definitions, which can then be used to generate Kotlin Multiplatform code for use across multiple platforms.

## Getting Started

### Download

The Litics API contains classes that are required by the generated code.

```kotlin
implementation("com.deliveryhero.litics:litics:0.1.2")
```

The Litics Gradle task takes your event definitions and generates Kotlin code.

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.github.deliveryhero.litics:litics-gradle-task:0.1.2")
    }
}
```

### YAML Schema

A JSON Schema file describing the structure of the YAML document can be found here. Alternatively, you can visualize the schema using an external tool like [JSON Schema Viewer](https://json-schema.app/view/%23?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdeliveryhero%2Flitics%2Fmaster%2Flitics.schema.json).

It is suggested that you import the JSON Schema into your chosen editor for auto-completion and live validation of your YAML file. For example, [here is the documentation](https://www.jetbrains.com/help/idea/json.html#ws_json_schema_add_custom) for setting it up in IntelliJ. Note that the schema has not yet been uploaded to a JSON Schema catalog, so when asked for the schema URL, it can be found [here](https://raw.githubusercontent.com/deliveryhero/litics/master/litics.schema.json).

An example tracking events definition can be found below:

```yaml
# src/main/res/events.yaml
components:
  parameters:
    order: &order_parameters
      id:
        type: string
        description: 'ID of the associated order'
        required: true
        example: 'GB430'

events:
  trackOrderAccepted:
    name: order_accepted
    description: 'Track that the order has been accepted'
    supported_platforms:
      - braze
      - firebase
    parameters:
      <<: *order_parameters
      itemCount:
        type: number
        required: false
        example: 30
  trackOrderRejected:
    …
```

### Generating the Code

In order to generate Kotlin code from your tracking event definitions, you first need to register the `EventsGeneratorTask` in your `build.gradle` and provide some required configuration.

```kotlin
val generateAnalyticsEvents by tasks.registering(com.deliveryhero.litics.EventsGeneratorTask::class) {

    // Platform type used to apply platform dependant behavior for the generated files.
    platform = Platform.JS
    
    // Package name used for the generated files.
    packageName = "com.example.analytics"

    // The yaml file that contains the tracking definitions.
    sourceFile = file("src/main/res/events.yaml")

    // The location of the generated code.
    targetDirectory = buildDir.resolve("generated/src")
}
```

We can now run this task by calling `./gradlew generateAnalyticsEvents`, and two classes will be generated: `GeneratedEventsAnalytics` and `GeneratedEventsAnalyticsImpl`. The code will be generated to your `targetDirectory` under the package defined by your `packageName`.

So in the example above, the following files will be generated:

- `${buildDir}/generated/src/com/example/analytics/GeneratedEventsAnalytics.kt`
- `${buildDir}/generated/src/com/example/analytics/GeneratedEventsAnalyticsImpl.kt`

### Creating an Event Tracker

An `EventTracker` is a web analytics service that you can track events to. For example, if one of our analytics services is Firebase, we can create an implementation of `EventTracker` to be passed to `GeneratedEventsAnalyticsImpl`.

```kotlin
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

class FirebaseEventTracker : EventTracker() {

    // This is invoked everytime you try to track an event. The supportedPlatforms 
    // are retrieved from the specific event in your YAML file. If this analytics 
    // service should track the event, return true.
    fun supportsEventTracking(supportedPlatforms: Array<String>): Boolean {
        return "firebase" in supportedPlatforms
    }

    // If supportsEventTracking(…) returned true, then we invoke this function, where 
    // we tell Firebase to log to there service.
    fun trackEvent(trackingEvent: TrackingEvent) {
        Firebase.analytics.logEvent(trackingEvent.eventName) {
            trackingEvent.parameters.forEach { (key, value) -> param(key, value) }
        }
    }
}
```

### Tracking an Event

Putting it all together, we can track our example event `order_accepted` like this:

```kotlin
val generatedEventsAnalytics: GeneratedEventsAnalytics = GeneratedEventsAnalyticsImpl(arrayOf(FirebaseEventTracker(), BrazeEventTracker()))

generatedEventsAnalytics.trackOrderAccepted(id = "US431", itemCount = 30)
```
