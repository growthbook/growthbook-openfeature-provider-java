# GrowthBook Provider for OpenFeature

This is an OpenFeature provider implementation for GrowthBook's Java SDK. It allows you to use GrowthBook as a feature flag provider with the OpenFeature SDK.

## Dependencies
Uses the following dependencies:

```yaml
        dev.openfeature:sdk:1.12.2
    
        io.growthbook:growthbook-sdk-java:1.1.0
```

## Quick Start

```java
// Configure GrowthBook
Options options = Options.builder()
    .apiHost("https://cdn.growthbook.io")
    .clientKey("YOUR_CLIENT_KEY")
    .build();

// Initialize provider
GrowthBookProvider provider = new GrowthBookProvider(options);
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
```

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

## Example Usage

See `GrowthBookExample.java` for a complete example of how to use the provider.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.