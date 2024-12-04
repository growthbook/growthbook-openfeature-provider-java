package com.github.growthbook;

import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import growthbook.sdk.java.FeatureResult;
import growthbook.sdk.java.FeatureResultSource;
import growthbook.sdk.java.multiusermode.GrowthBookClient;
import growthbook.sdk.java.multiusermode.configurations.Options;
import growthbook.sdk.java.multiusermode.configurations.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static dev.openfeature.sdk.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GrowthBookProviderTest {

    @Mock
    private GrowthBookClient mockGrowthBookClient;
    private GrowthBookProvider provider;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        try (MockedStatic<GrowthBookClientFactory> mockedClientFactory = mockStatic(GrowthBookClientFactory.class)){
            mockedClientFactory.when(() -> GrowthBookClientFactory.instance(any()))
                    .thenReturn(mockGrowthBookClient);
            mockedClientFactory.when(GrowthBookClientFactory::instance).thenReturn(mockGrowthBookClient);

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
    }

    @Test
    void initialize_shouldThrowProviderNotReadyError_whenInitializationFails() {
        when(mockGrowthBookClient.initialize()).thenReturn(false);
        ProviderNotReadyError err = assertThrows(ProviderNotReadyError.class, () -> provider.initialize(context));
        assertEquals("Error initializing GrowthBook provider", err.getMessage());
    }

    @Test
    void initialize_success() throws Exception {
        when(mockGrowthBookClient.initialize()).thenReturn(true);
        provider.initialize(context);
    }

    @Test
    void evaluateBoolean_shouldHandleBasicFlag() {
        String key = "test-flag";
        FeatureResult<Boolean> result = FeatureResult.<Boolean>builder()
                .value(true)
                .ruleId("rule-1")
                .build();

        when(mockGrowthBookClient.evalFeature(eq(key), eq(Boolean.class), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(key, false, context);

        assertTrue(evaluation.getValue());
        assertEquals("rule-1", evaluation.getVariant());
        assertEquals("TARGETING_MATCH", evaluation.getReason());
    }

    @Test
    void evaluateBoolean_shouldReturnInvalidContext_whenKeyIsNull() {
        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(null, false, context);
        assertFalse(evaluation.getValue());
        assertEquals("INVALID_KEY", evaluation.getReason());
        assertEquals(INVALID_CONTEXT, evaluation.getErrorCode());
    }

    @Test
    void evaluateBoolean_shouldReturnInvalidContext_whenKeyIsEmpty() {
        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation("", false, context);
        assertFalse(evaluation.getValue());
        assertEquals("INVALID_KEY", evaluation.getReason());
        assertEquals(INVALID_CONTEXT, evaluation.getErrorCode());
    }

    @Test
    void evaluateBoolean_shouldReturnNotFound_whenFlagNotFound() {
        String key = "test-flag";

        when(mockGrowthBookClient.evalFeature(eq(key), eq(Boolean.class), any(UserContext.class)))
                .thenReturn(null);

        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(key, false, context);

        assertFalse(evaluation.getValue());
        assertEquals("NOT_FOUND", evaluation.getReason());
        assertEquals(FLAG_NOT_FOUND, evaluation.getErrorCode());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void evaluateBoolean_shouldReturnTypeMismatch_whenResultTypeDoesNotMatch() {
        String key = "test-flag";
        FeatureResult result = FeatureResult.builder()
                .value("true")
                .ruleId("rule-1")
                .build();

        when(mockGrowthBookClient.evalFeature(eq(key), any(), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(key, false, context);

        assertFalse(evaluation.getValue());
        assertEquals("TYPE_MISMATCH", evaluation.getReason());
        assertEquals(TYPE_MISMATCH, evaluation.getErrorCode());
    }

    @Test
    void evaluateBoolean_shouldReturnGeneralError_whenException() {
        String key = "test-flag";

        when(mockGrowthBookClient.evalFeature(eq(key), any(), any(UserContext.class)))
                .thenThrow(new RuntimeException("General Exception"));

        ProviderEvaluation<Boolean> evaluation = provider.getBooleanEvaluation(key, false, context);

        assertFalse(evaluation.getValue());
        assertEquals("ERROR", evaluation.getReason());
        assertEquals(GENERAL, evaluation.getErrorCode());
        assertEquals("General Exception", evaluation.getErrorMessage());
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

        when(mockGrowthBookClient.evalFeature(eq(key), eq(String.class), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<String> evaluation =
                provider.getStringEvaluation(key, "default", context);

        assertEquals("value-a", evaluation.getValue());
        assertEquals("exp-1", evaluation.getVariant());
    }

//    @Test
//    void evaluateObject_shouldHandleNullResult() throws Exception {
//        String key = "object-flag";
//        when(mockGrowthBookClient.evalFeature(eq(key), eq(Object.class), any(UserContext.class)))
//                .thenReturn(null);
//
//        Structure defaultValue = new Structure();
//        ProviderEvaluation<Value> evaluation =
//                provider.getObjectEvaluation(key, defaultValue, context);
//
//        assertEquals(defaultValue, evaluation.getValue());
//        assertEquals("NOT_FOUND", evaluation.getReason());
//        assertEquals(ErrorCode.FLAG_NOT_FOUND, evaluation.getErrorCode());
//    }

    @Test
    void evaluateFeature_shouldHandleInvalidKey() {
        ProviderEvaluation<String> evaluation =
                provider.getStringEvaluation(null, "default", context);

        assertEquals("default", evaluation.getValue());
        assertEquals("INVALID_KEY", evaluation.getReason());
        assertEquals(INVALID_CONTEXT, evaluation.getErrorCode());
    }

    @Test
    void evaluateFeature_shouldHandleTypeMismatch() throws Exception {
        String key = "number-flag";
        FeatureResult<Integer> result = FeatureResult.<Integer>builder()
                .value(null)
                .source(FeatureResultSource.UNKNOWN_FEATURE)
                .build();

        when(mockGrowthBookClient.evalFeature(eq(key), eq(Integer.class), any(UserContext.class)))
                .thenReturn(result);

        ProviderEvaluation<Integer> evaluation =
                provider.getIntegerEvaluation(key, 42, context);

        assertEquals(42, evaluation.getValue());
        assertEquals("TYPE_MISMATCH", evaluation.getReason());
        assertEquals(TYPE_MISMATCH, evaluation.getErrorCode());
    }

    @Test
    void evaluateFeature_shouldHandleException() {
        String key = "error-flag";
        when(mockGrowthBookClient.evalFeature(eq(key), any(), any()))
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
