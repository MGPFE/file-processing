package org.mg.fileprocessing.security.auth.jwt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.security.auth.AuthDto;
import org.mg.fileprocessing.security.auth.AuthController;
import org.mg.fileprocessing.security.auth.AuthService;
import org.mg.fileprocessing.security.auth.jwt.dto.JwtSignInResponseDto;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Primary
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class JwtAuthController implements AuthController<JwtSignInResponseDto> {
    private final AuthService<JwtSignInResponseDto> jwtAuthService;

    @Override
    @PostMapping("/sign-up")
    public ResponseEntity<Void> signUp(@RequestBody @Valid AuthDto authDto) {
        jwtAuthService.signUp(authDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/sign-in")
    public JwtSignInResponseDto signIn(@RequestBody AuthDto authDto) {
        return jwtAuthService.signIn(authDto);
    }
}
