package org.mg.fileprocessing.error;

import java.time.Instant;

public record ErrorResponse(
        int code,
        String reason,
        Instant timestamp
) {
}
