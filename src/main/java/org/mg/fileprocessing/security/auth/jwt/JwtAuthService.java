package org.mg.fileprocessing.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.dto.AuthDto;
import org.mg.fileprocessing.security.auth.AuthService;
import org.mg.fileprocessing.security.auth.jwt.dto.JwtSignInResponseDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtAuthService implements AuthService<JwtSignInResponseDto> {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void signUp(AuthDto authDto) {

    }

    @Override
    public JwtSignInResponseDto signIn(AuthDto authDto) {
        return null;
    }
}
