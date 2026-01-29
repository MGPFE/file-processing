package org.mg.fileprocessing.exception;

import lombok.experimental.StandardException;

@StandardException
public class IdempotencyViolationException extends RuntimeException {
}
