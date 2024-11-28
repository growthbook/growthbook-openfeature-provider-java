package com.github.growthbook;

import dev.openfeature.sdk.*;
import growthbook.sdk.java.multiusermode.configurations.Options;

import java.util.HashMap;
import java.util.Map;

public class GrowthBookExample {

    public static void main(String[] args) {
        // Configure GrowthBook
        Options options = Options.builder()
                .apiHost("https://cdn.growthbook.io")
                .clientKey("YOUR_CLIENT_KEY")
                .build();

        // Create and initialize the provider
        GrowthBookProvider provider = new GrowthBookProvider(options);

        // Set the provider in OpenFeature
        OpenFeatureAPI.getInstance().setProvider(provider);

        // Create a client
        Client client = OpenFeatureAPI.getInstance().getClient();

        // Create evaluation context with user attributes
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("country", new Value("US"));
        attributes.put("device", new Value("mobile"));
        EvaluationContext context = new ImmutableContext("user-123", attributes);

        // Evaluate different types of features
        boolean showNewFeature = client.getBooleanValue("new-feature-flag", false, context);
        String greeting = client.getStringValue("welcome-message", "Hello", context);
        int maxAttempts = client.getIntegerValue("max-attempts", 3, context);

        System.out.println("showNewFeature: " + showNewFeature);
        System.out.println("greeting: " + greeting);
        System.out.println("You have " + maxAttempts + " attempts remaining.");

        // Clean up
        provider.shutdown();
    }
}
