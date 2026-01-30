package org.mg.fileprocessing.interceptors;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.exception.RateLimitExceededException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {
    @Mock private ProxyManager<byte[]> buckets;
    @Mock private RemoteBucketBuilder<byte[]> remoteBucketBuilder;
    @Mock private BucketProxy bucketProxy;
    @Mock private ConsumptionProbe consumptionProbe;

    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        RateLimitInterceptorProperties rateLimitInterceptorProperties = new RateLimitInterceptorProperties();

        rateLimitInterceptor = new RateLimitInterceptor(buckets, rateLimitInterceptorProperties);
    }

    @Test
    public void shouldReturnTrueWhenRequestIsAllowed() {
        // Given
        String remoteAddr = "127.0.0.1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(buckets.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(eq(remoteAddr.getBytes(StandardCharsets.UTF_8)), any(Supplier.class))).willReturn(bucketProxy);
        given(bucketProxy.tryConsumeAndReturnRemaining(1)).willReturn(consumptionProbe);
        given(consumptionProbe.isConsumed()).willReturn(Boolean.TRUE);

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        // Then
        assertTrue(result);
        verify(buckets).builder();
        verify(remoteBucketBuilder).build(eq(remoteAddr.getBytes(StandardCharsets.UTF_8)), any(Supplier.class));
        verify(bucketProxy).tryConsumeAndReturnRemaining(1);
        verify(consumptionProbe).isConsumed();
    }

    @Test
    public void shouldThrowExceptionWhenRequestRateLimited() {
        // Given
        String remoteAddr = "127.0.0.1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(buckets.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(eq(remoteAddr.getBytes(StandardCharsets.UTF_8)), any(Supplier.class))).willReturn(bucketProxy);
        given(bucketProxy.tryConsumeAndReturnRemaining(1)).willReturn(consumptionProbe);
        given(consumptionProbe.isConsumed()).willReturn(Boolean.FALSE);
        given(consumptionProbe.getNanosToWaitForRefill()).willReturn(10000L);

        // When
        // Then
        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class);
        verify(buckets).builder();
        verify(remoteBucketBuilder).build(eq(remoteAddr.getBytes(StandardCharsets.UTF_8)), any(Supplier.class));
        verify(bucketProxy).tryConsumeAndReturnRemaining(1);
        verify(consumptionProbe).isConsumed();
    }
}