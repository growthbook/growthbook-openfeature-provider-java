package com.github.growthbook;

import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import growthbook.sdk.java.FeatureResult;
import growthbook.sdk.java.FeatureResultSource;
import growthbook.sdk.java.multiusermode.GrowthBookMultiUser;
import growthbook.sdk.java.multiusermode.configurations.Options;
import growthbook.sdk.java.multiusermode.configurations.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GrowthBookProviderTest {

    @Mock
    private GrowthBookMultiUser mockGrowthBook;
    private GrowthBookProvider provider;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Options options = Options.builder()
                .apiHost("https://cdn.growthbook.io")
                .clientKey("test-key")
                .build();
        provider = new GrowthBookProvider(options);

        // Create an immutable context for testing
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("country", new Value("US"));
        attributes.put("version", new Value(2));
        context = new ImmutableContext("user-123", attributes);
    }

    @Test
    void initialize_shouldThrowProviderNotReadyError_whenInitializationFails() throws Exception {
        when(mockGrowthBook.initialize()).thenReturn(false);

        assertThrows(ProviderNotReadyError.class, () -> provider.initialize(context));
    }
    @Test
    void evaluateBoolean_shouldHandleBasicFlag() throws Exception {
        String key = "test-flag";
        FeatureResult<Boolean> result = FeatureResult.<Boolean>builder()
                .value(true)
                .ruleId("rule-1")
                .build();

        when(mockGrowthBook.evalFeature(eq(key), eq(Boolean.class), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(key, false, context);

        assertTrue(evaluation.getValue());
        assertEquals("rule-1", evaluation.getVariant());
        assertEquals("TARGETING_MATCH", evaluation.getReason());
    }

    @Test
    void evaluateString_shouldHandleComplexContext() throws Exception {
        String key = "string-flag";
        /*Map<String, Value> attributes = new HashMap<>();
        attributes.put("country", new Value("US"));
        attributes.put("version", new Value(2));
        attributes.put("premium", new Value(true));*/

        /*EvaluationContext complexContext = ImmutableContext.builder()
                .targetingKey("user-123")
                .attributes(attributes)
                .build();*/

        FeatureResult<String> result = FeatureResult.<String>builder()
                .value("value-a")
                .ruleId("exp-1")
                .build();

        when(mockGrowthBook.evalFeature(eq(key), eq(String.class), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<String> evaluation =
                provider.getStringEvaluation(key, "default", context);

        assertEquals("value-a", evaluation.getValue());
        assertEquals("exp-1", evaluation.getVariant());
    }

   /* @Test
    void evaluateObject_shouldHandleNullResult() throws Exception {
        String key = "object-flag";
        when(mockGrowthBook.evalFeature(eq(key), eq(Object.class), any(UserContext.class)))
                .thenReturn(null);

        Structure defaultValue = new Structure();
        ProviderEvaluation<Value> evaluation =
                provider.getObjectEvaluation(key, defaultValue, context);

        assertEquals(defaultValue, evaluation.getValue());
        assertEquals("NOT_FOUND", evaluation.getReason());
        assertEquals(ErrorCode.FLAG_NOT_FOUND, evaluation.getErrorCode());
    }*/

    @Test
    void evaluateFeature_shouldHandleInvalidKey() {
        ProviderEvaluation<String> evaluation =
                provider.getStringEvaluation(null, "default", context);

        assertEquals("default", evaluation.getValue());
        assertEquals("INVALID_KEY", evaluation.getReason());
        assertEquals(ErrorCode.INVALID_CONTEXT, evaluation.getErrorCode());
    }

    @Test
    void evaluateFeature_shouldHandleTypeMismatch() throws Exception {
        String key = "number-flag";
        FeatureResult<Integer> result = FeatureResult.<Integer>builder()
                .value(null)
                .source(FeatureResultSource.UNKNOWN_FEATURE)
                .build();

        when(mockGrowthBook.evalFeature(eq(key), eq(Integer.class), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<Integer> evaluation =
                provider.getIntegerEvaluation(key, 42, context);

        assertEquals(42, evaluation.getValue());
        assertEquals("TYPE_MISMATCH", evaluation.getReason());
        assertEquals(ErrorCode.TYPE_MISMATCH, evaluation.getErrorCode());
    }

    @Test
    void evaluateFeature_shouldHandleException() {
        String key = "error-flag";
        when(mockGrowthBook.evalFeature(eq(key), any(), any()))
                .thenThrow(new RuntimeException("Test error"));

        ProviderEvaluation<Integer> evaluation =
                provider.getIntegerEvaluation(key, 42, context);

        assertEquals(42, evaluation.getValue());
        assertEquals("ERROR", evaluation.getReason());
        assertEquals(ErrorCode.GENERAL, evaluation.getErrorCode());
        assertEquals("Test error", evaluation.getErrorMessage());
    }

    @Test
    void testMetadataName() {
        assertEquals("GrowthBook.OpenFeature.Provider.Java", provider.getMetadata().getName());
    }
}
