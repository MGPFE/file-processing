package org.mg.fileprocessing.entity;

public enum ScanStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUCCESS,
    FAILURE_RETRIABLE,
    RETRYING;
}
