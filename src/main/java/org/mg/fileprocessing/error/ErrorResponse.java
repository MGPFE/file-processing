package org.mg.fileprocessing.error;

import java.time.Instant;

public record ErrorResponse(
        Integer code,
        String reason,
        Instant timestamp
) {
}
