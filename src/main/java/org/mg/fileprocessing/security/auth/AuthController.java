package org.mg.fileprocessing.security.auth;

import org.mg.fileprocessing.dto.AuthDto;
import org.springframework.http.ResponseEntity;

public interface AuthController<T> {
    ResponseEntity<Void> signUp(AuthDto authDto);
    T signIn(AuthDto authDto);
}
