package org.mg.fileprocessing.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.dto.AuthDto;
import org.mg.fileprocessing.security.auth.AuthController;
import org.mg.fileprocessing.security.auth.AuthService;
import org.mg.fileprocessing.security.auth.jwt.dto.JwtSignInResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class JwtAuthController implements AuthController<JwtSignInResponseDto> {
    private final AuthService<JwtSignInResponseDto> jwtAuthService;

    @Override
    public ResponseEntity<Void> signUp(AuthDto authDto) {
        jwtAuthService.signUp(authDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public JwtSignInResponseDto signIn(AuthDto authDto) {
        return jwtAuthService.signIn(authDto);
    }
}
