package org.mg.fileprocessing.security.auth.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.repository.UserRepository;
import org.mg.fileprocessing.security.auth.AuthDto;
import org.mg.fileprocessing.security.auth.UserRole;
import org.mg.fileprocessing.security.auth.jwt.dto.JwtSignInResponseDto;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @Captor private ArgumentCaptor<User> userArgumentCaptor;

    @InjectMocks private JwtAuthService jwtAuthService;

    @Test
    public void shouldSignUserUp() {
        // Given
        String email = "test@email.com";
        String password = "testpassword";
        AuthDto authDto = new AuthDto(email, password);

        given(userRepository.existsByEmail(authDto.email())).willReturn(false);

        String encodedPassword = "ENCODED_PASSWORD";
        given(passwordEncoder.encode(authDto.password())).willReturn(encodedPassword);

        // When
        jwtAuthService.signUp(authDto);

        // Then
        verify(userRepository).existsByEmail(authDto.email());
        verify(passwordEncoder).encode(authDto.password());
        verify(userRepository).save(userArgumentCaptor.capture());

        User capturedUser = userArgumentCaptor.getValue();
        assertNotNull(capturedUser);
        assertThat(capturedUser.getEmail()).isEqualTo(email);
        assertThat(capturedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(capturedUser.getRoles()).contains(UserRole.USER);
        assertThat(capturedUser.getRoles()).hasSize(1);
    }

    @Test
    public void shouldThrowExceptionWhenUserWithThatEmailAlreadyExists() {
        // Given
        String email = "test@email.com";
        AuthDto authDto = new AuthDto(email, "testpassword");

        given(userRepository.existsByEmail(email)).willReturn(true);

        // When
        // Then
        assertThatThrownBy(() -> jwtAuthService.signUp(authDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with email %s already exists".formatted(authDto.email()));
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder, never()).encode(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void shouldSignUserIn() {
        // Given
        String email = "test@email.com";
        String password = "ENCODED_PASSWORD";
        AuthDto authDto = new AuthDto(email, password);

        String expectedJwtToken = "mocked-jwt-token";
        Authentication authentication = mock(Authentication.class);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(authDto.email())
                .password(authDto.password())
                .authorities(UserRole.USER.getWithPrefix())
                .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(jwtUtil.generateToken(userDetails.getUsername())).willReturn(expectedJwtToken);

        // When
        JwtSignInResponseDto result = jwtAuthService.signIn(authDto);

        // Then
        assertNotNull(result);
        assertThat(result.jwtToken()).isEqualTo(expectedJwtToken);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidSignIn() {
        // Given
        AuthDto authDto = new AuthDto("test@user.com", "testpassword");

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willThrow(new BadCredentialsException("Invalid credentials"));

        // When
        // Then
        assertThatThrownBy(() -> jwtAuthService.signIn(authDto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }
}