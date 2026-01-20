package org.mg.fileprocessing.security.auth;

import org.mg.fileprocessing.dto.AuthDto;

public interface AuthService<T> {
    void signUp(AuthDto authDto);
    T signIn(AuthDto authDto);
}
