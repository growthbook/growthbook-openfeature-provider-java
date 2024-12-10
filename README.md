# GrowthBook Provider for OpenFeature

This is an OpenFeature provider implementation for GrowthBook's Java SDK. It allows you to use GrowthBook as a feature flag provider with the OpenFeature SDK.

## Growth Book Overview
[GrowthBook](https://www.growthbook.io) is an open source Feature Flagging and Experimentation platform built for data teams, engineers, and product managers. [Get Started](https://www.growthbook.io/get-started) using GrowthBook today. 

## Supported Java Versions
This version of the GrowthBook provider works with Java 8 and above.

## Getting Started

First, install the Growthbook OpenFeature provider for Java as a dependency in your application.

```xml
<dependency>
  <groupId>com.github.growthbook</groupId>
  <artifactId>growthbook-openfeature-provider-java</artifactId>
  <version>0.0.1</version> <!-- use current version number -->
</dependency>
```

```
implementation group: 'com.github.growthbook', name: 'growthbook-openfeature-provider-java', version: '0.0.1'
// Use current version number in place of 0.0.1.
```

## Dependencies
Uses the following dependencies:

```yaml
dev.openfeature:sdk:1.12.2

io.growthbook:growthbook-sdk-java:1.1.0
```

## Example Usage

```java
// Configure GrowthBook
Options options = Options.builder()
    .apiHost("https://cdn.growthbook.io")
    .clientKey("YOUR_CLIENT_KEY")
    .build();

// Initialize provider
GrowthBookProvider provider = new GrowthBookProvider(options);

// Set the provider in OpenFeature
OpenFeatureAPI.getInstance().setProvider(provider);

// Get a client
Client client = OpenFeatureAPI.getInstance().getClient();

// Create context with user attributes
EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-123")
    .set("country", "US")
    .build();

// Evaluate features
boolean enabled = client.getBooleanValue("my-feature", false, context);

// Clean up
provider.shutdown();

```

See `GrowthBookExample.java` for a complete example of how to use the provider. For information on using the OpenFeature client please refer to the [OpenFeature Documentation](https://openfeature.dev/docs/reference/concepts/evaluation-api/).

## Features

- Supports all OpenFeature evaluation types (boolean, string, integer, double, object)
- Full integration with GrowthBook's targeting and feature evaluation capability
- Thread-safe evaluation

### Context Conversion

The provider automatically converts OpenFeature's `EvaluationContext` to GrowthBook's `UserContext`:

- `targetingKey` becomes the user ID
- Context attributes are converted to GrowthBook attributes
- Supports all value types (boolean, number, string, object, array)

## Thread Safety

The provider is thread-safe and can be used in multi-threaded environments.

## Best Practices

1. Initialize the provider early in your application lifecycle
2. Reuse the OpenFeature client instance
3. Include relevant user attributes in the context
4. Handle potential exceptions during evaluation
5. Clean up resources using the shutdown method

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.