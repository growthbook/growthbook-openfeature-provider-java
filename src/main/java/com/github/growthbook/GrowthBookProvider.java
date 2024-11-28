package com.github.growthbook;

import com.google.gson.*;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import growthbook.sdk.java.FeatureResult;
import growthbook.sdk.java.multiusermode.GrowthBookMultiUser;
import growthbook.sdk.java.multiusermode.configurations.Options;
import growthbook.sdk.java.multiusermode.configurations.UserContext;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An OpenFeature {@link FeatureProvider} implementation for GrowthBook's Java SDK.
 */
public class GrowthBookProvider implements FeatureProvider {

    private static final Logger LOG = Logger.getLogger(GrowthBookProvider.class.getName());
    private final String name = "GrowthBook.OpenFeature.Provider.Java";
    private final GrowthBookMultiUser growthBookMultiUser;

    @Override
    public Metadata getMetadata() {
        return () -> name;
    }

    public GrowthBookProvider() {
        this(Options.builder().build());
    }

    public GrowthBookProvider(Options options) {
        this.growthBookMultiUser = new GrowthBookMultiUser(options);
    }

    @Override
    public void initialize(EvaluationContext evaluationContext) throws Exception {
        FeatureProvider.super.initialize(evaluationContext);
        // Client has to be using in the synchronized mode!
        try {
            boolean isReady = this.growthBookMultiUser.initialize();
            if (!isReady) {
                String errorMsg = "Failed to initialize GrowthBook instance.";
                LOG.severe(errorMsg);
                throw new ProviderNotReadyError(errorMsg);
            }
            LOG.info("GrowthBook provider initialized successfully");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error initializing GrowthBook provider", e);
            throw new ProviderNotReadyError("Error initializing GrowthBook provider", e);
        }
    }

    private JsonElement convertValueToJsonElement(Value value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }

        if (value.isBoolean()) {
            return new JsonPrimitive(value.asBoolean());
        }
        if (value.isNumber()) {
            return new JsonPrimitive(value.asDouble());
        }
        if (value.isString()) {
            return new JsonPrimitive(value.asString());
        }
        if (value.isStructure()) {
            JsonObject obj = new JsonObject();
            value.asStructure().asMap().forEach((k, v) -> obj.add(k, convertValueToJsonElement(v)));
            return obj;
        }
        if (value.isList()) {
            JsonArray array = new JsonArray();
            value.asList().forEach(v -> array.add(convertValueToJsonElement(v)));
            return array;
        }

        return new JsonPrimitive(value.asString());
    }

    // Convert OpenFeature's EvaluationContext to GrowthBook's UserContext
    private UserContext convertContext(EvaluationContext context) {
        UserContext.UserContextBuilder builder = UserContext.builder();

        if (context == null) {
            return builder.build();
        }

        JsonObject userAttributes = new JsonObject();

        // Convert attributes
        Map<String, Value> attributes = context.asMap();
        attributes.forEach((k, v) -> userAttributes.add(k, convertValueToJsonElement(v)));

        // Handle targeting attributes if present
        String targetingKey = context.getTargetingKey();
        if (targetingKey != null && !targetingKey.isEmpty()) {
            userAttributes.addProperty("id", targetingKey);
        }

        String url = attributes.getOrDefault("url", new Value("")).asString();
        if (!url.isEmpty()) {
            builder.url(url);
        }

        return builder.attributes(userAttributes).build();
    }

    private <T> ProviderEvaluation<T> evaluateFeature(String key, T defaultValue, EvaluationContext context, Class<T> type) {
        try {

            if (key == null || key.trim().isEmpty()) {
                LOG.warning("Feature key is null or empty");
                return ProviderEvaluation.<T>builder()
                        .value(defaultValue)
                        .reason("INVALID_KEY")
                        .errorCode(ErrorCode.INVALID_CONTEXT)
                        .build();
            }

            FeatureResult<T> result = growthBookMultiUser.evalFeature(key, type, convertContext(context));

            if (result == null) {
                return ProviderEvaluation.<T>builder()
                        .value(defaultValue)
                        .reason("NOT_FOUND")
                        .errorCode(ErrorCode.FLAG_NOT_FOUND)
                        .build();
            }

            if (!type.isInstance(result.getValue())) {
                return ProviderEvaluation.<T>builder()
                        .value(defaultValue)
                        .reason("TYPE_MISMATCH")
                        .errorCode(ErrorCode.TYPE_MISMATCH)
                        .build();
            }

            return ProviderEvaluation.<T>builder()
                    .value(type.cast(result.getValue()))
                    .variant(result.getRuleId())
                    .reason("TARGETING_MATCH")
                    .build();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while evaluating feature: " + key, e);
            return ProviderEvaluation.<T>builder()
                    .value(defaultValue)
                    .reason("ERROR")
                    .errorCode(ErrorCode.GENERAL)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }


    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultValue, EvaluationContext ctx) {
        return evaluateFeature(key, defaultValue, ctx, Boolean.class);
    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultValue, EvaluationContext ctx) {
        return evaluateFeature(key, defaultValue, ctx, String.class);
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultValue, EvaluationContext ctx) {
        return evaluateFeature(key, defaultValue, ctx, Integer.class);
    }

    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultValue, EvaluationContext ctx) {
        return evaluateFeature(key, defaultValue, ctx, Double.class);
    }

    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultValue, EvaluationContext ctx) {
        try {
            FeatureResult<Object> result = growthBookMultiUser.evalFeature(key, Object.class, convertContext(ctx));

            if (result == null) {
                return ProviderEvaluation.<Value>builder()
                        .value(defaultValue)
                        .reason("NOT_FOUND")
                        .errorCode(ErrorCode.FLAG_NOT_FOUND)
                        .build();
            }

            Object value = result.getValue();
            if (value == null) {
                return ProviderEvaluation.<Value>builder()
                        .value(defaultValue)
                        .reason("NULL_VALUE")
                        .build();
            }

            return ProviderEvaluation.<Value>builder()
                    .value(Value.objectToValue(result.getValue()))
                    .variant(result.getRuleId())
                    .reason("TARGETING_MATCH")
                    .build();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error evaluating object feature: " + key, e);
            return ProviderEvaluation.<Value>builder()
                    .value(defaultValue)
                    .reason("ERROR")
                    .errorCode(ErrorCode.GENERAL)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public void shutdown() {
        try {
            // Call any necessary cleanup methods on growthBookMultiUser
            // growthBookMultiUser.destroy();
            LOG.info("GrowthBook provider shutdown successfully");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error during GrowthBook provider shutdown", e);
        } finally {
            FeatureProvider.super.shutdown();
        }
    }
}
