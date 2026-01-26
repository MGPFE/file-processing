package org.mg.fileprocessing.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.repository.UserRepository;
import org.mg.fileprocessing.security.auth.AuthDto;
import org.mg.fileprocessing.security.auth.AuthService;
import org.mg.fileprocessing.security.auth.UserRole;
import org.mg.fileprocessing.security.auth.jwt.dto.JwtSignInResponseDto;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Primary
@Service
@RequiredArgsConstructor
public class JwtAuthService implements AuthService<JwtSignInResponseDto> {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void signUp(AuthDto authDto) {
        if (userRepository.existsByEmail(authDto.email())) {
            throw new IllegalArgumentException("User with email %s already exists".formatted(authDto.email()));
        }

        User newUser = User.builder()
                .email(authDto.email())
                .password(passwordEncoder.encode(authDto.password()))
                .roles(List.of(UserRole.USER))
                .build();

        userRepository.save(newUser);
    }

    @Override
    public JwtSignInResponseDto signIn(AuthDto authDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authDto.email(),
                        authDto.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return new JwtSignInResponseDto(jwtUtil.generateToken(userDetails.getUsername()));
    }
}
