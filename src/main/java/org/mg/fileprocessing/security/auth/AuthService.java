package org.mg.fileprocessing.security.auth;

public interface AuthService<T> {
    void signUp(AuthDto authDto);
    T signIn(AuthDto authDto);
}
