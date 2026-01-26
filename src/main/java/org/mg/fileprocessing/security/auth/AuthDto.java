package org.mg.fileprocessing.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record AuthDto(@NotNull @NotBlank @Email String email, @NotNull @NotBlank @Length(min = 8, max = 128) String password) {
}
