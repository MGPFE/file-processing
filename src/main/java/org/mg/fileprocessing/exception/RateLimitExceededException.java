package org.mg.fileprocessing.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public class RateLimitExceededException extends RuntimeException {
    private final Duration retryAfter;
}
