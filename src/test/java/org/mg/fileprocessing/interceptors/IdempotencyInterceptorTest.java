package org.mg.fileprocessing.interceptors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.exception.IdempotencyViolationException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private IdempotencyInterceptor idempotencyInterceptor;
    private IdempotencyInterceptorProperties idempotencyInterceptorProperties;

    @BeforeEach
    void setUp() {
        idempotencyInterceptorProperties = new IdempotencyInterceptorProperties();

        idempotencyInterceptor = new IdempotencyInterceptor(redisTemplate, idempotencyInterceptorProperties);
    }

    @Test
    public void shouldAllowRequestIfNotPost() {
        // Given
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("GET");
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        // When
        boolean result = idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

        // Then
        assertTrue(result);
        verify(redisTemplate, never()).opsForValue();
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(valueOps, never()).get(anyString());
    }

    @Test
    public void shouldThrowExceptionWhenIdempotencyKeyNotPassed() {
        // Given
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        // When
        // Then
        assertThatThrownBy(() -> idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null))
                .isInstanceOf(IdempotencyViolationException.class)
                .hasMessage("Idempotency-Key header is mandatory");
        verify(redisTemplate, never()).opsForValue();
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(valueOps, never()).get(anyString());
    }

    @Test
    public void shouldThrowExceptionWhenKeyIsBlank() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, "  ");
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        // When
        // Then
        assertThatThrownBy(() -> idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null))
                .isInstanceOf(IdempotencyViolationException.class)
                .hasMessage("Idempotency-Key header is mandatory");
        verify(redisTemplate, never()).opsForValue();
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(valueOps, never()).get(anyString());
    }

    @Test
    public void shouldAllowRequestIfIsFirst() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";
        final String idempotencyKey = "DUMMY_KEY";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, idempotencyKey);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(idempotencyKey, "Processing", idempotencyInterceptorProperties.getKeyExpiration())).willReturn(Boolean.TRUE);

        // When
        boolean result = idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

        // Then
        assertTrue(result);
        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        verify(redisTemplate).opsForValue();
        verify(valueOps).setIfAbsent(eq(idempotencyKey), eq("Processing"), eq(idempotencyInterceptorProperties.getKeyExpiration()));
        verify(valueOps, never()).get(idempotencyKey);
    }

    @Test
    public void shouldReturnTrueWhenSetIfAbsentReturnsNull() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";
        final String idempotencyKey = "DUMMY_KEY";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, idempotencyKey);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(idempotencyKey, "Processing", idempotencyInterceptorProperties.getKeyExpiration())).willReturn(null);

        // When
        boolean result = idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

        // Then
        assertTrue(result);
        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        verify(redisTemplate).opsForValue();
        verify(valueOps).setIfAbsent(eq(idempotencyKey), eq("Processing"), eq(idempotencyInterceptorProperties.getKeyExpiration()));
    }

    @Test
    public void shouldNotAllowRequestAndReturn425WhenNotFirstRequestAndStatusIsProcessing() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";
        final String idempotencyKey = "DUMMY_KEY";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, idempotencyKey);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(idempotencyKey, "Processing", idempotencyInterceptorProperties.getKeyExpiration())).willReturn(Boolean.FALSE);
        given(valueOps.get(idempotencyKey)).willReturn("Processing");

        // When
        boolean result = idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

        // Then
        assertFalse(result);
        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(425);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOps).setIfAbsent(eq(idempotencyKey), eq("Processing"), eq(idempotencyInterceptorProperties.getKeyExpiration()));
        verify(valueOps).get(idempotencyKey);
    }

    @Test
    public void shouldNotAllowRequestAndReturn425WhenNotFirstRequestAndStatusIsCompleted() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";
        final String idempotencyKey = "DUMMY_KEY";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, idempotencyKey);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(idempotencyKey, "Processing", idempotencyInterceptorProperties.getKeyExpiration())).willReturn(Boolean.FALSE);
        given(valueOps.get(idempotencyKey)).willReturn("Completed");

        // When
        boolean result = idempotencyInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

        // Then
        assertFalse(result);
        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(209);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOps).setIfAbsent(eq(idempotencyKey), eq("Processing"), eq(idempotencyInterceptorProperties.getKeyExpiration()));
        verify(valueOps).get(idempotencyKey);
    }

    @Test
    public void shouldDeleteKeyAfterCompletionIfExceptionOccurred() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";
        final String idempotencyKey = "DUMMY_KEY";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, idempotencyKey);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        // When
        idempotencyInterceptor.afterCompletion(mockHttpServletRequest, mockHttpServletResponse, null, new RuntimeException("DUMMY"));

        // Then
        verify(redisTemplate).delete(idempotencyKey);
        verify(redisTemplate, never()).opsForValue();
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    public void shouldSetKeyStatusToCompletedAfterCompletion() {
        // Given
        final String idempotencyKeyHeader = "Idempotency-Key";
        final String idempotencyKey = "DUMMY_KEY";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        mockHttpServletRequest.addHeader(idempotencyKeyHeader, idempotencyKey);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(redisTemplate.opsForValue()).willReturn(valueOps);

        // When
        idempotencyInterceptor.afterCompletion(mockHttpServletRequest, mockHttpServletResponse, null, null);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOps).set(eq(idempotencyKey), eq("Completed"), eq(idempotencyInterceptorProperties.getKeyExpiration()));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    public void shouldSkipAfterCompletionIfKeyIsNull() {
        // Given
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setMethod("POST");
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        // When
        idempotencyInterceptor.afterCompletion(mockHttpServletRequest, mockHttpServletResponse, null, null);

        // Then
        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).opsForValue();
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }
}