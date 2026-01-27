package org.mg.fileprocessing.dto;

import jakarta.validation.constraints.NotNull;
import org.mg.fileprocessing.entity.FileVisibility;

public record UpdateFileVisibilityDto(@NotNull FileVisibility fileVisibility) {
}
