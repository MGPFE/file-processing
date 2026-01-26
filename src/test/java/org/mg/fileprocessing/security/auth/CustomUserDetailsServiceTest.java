package org.mg.fileprocessing.security.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    @Test
    public void shouldLoadUserByUsername() {
        // Given
        String username = "test@username.com";
        String password = "testpwd";
        User user = User.builder()
                .email(username)
                .roles(List.of(UserRole.USER))
                .password(password)
                .build();

        given(userRepository.findByEmail(username)).willReturn(Optional.of(user));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(username);

        // Then
        assertNotNull(result);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(UserRole.USER.getWithPrefix());
        assertThat(result.getPassword()).isEqualTo(password);
    }

    @Test
    public void shouldThrowExceptionWhenUserDoesNotExist() {
        // Given
        String username = "DoesNotExist";

        given(userRepository.findByEmail(username)).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User does not exist");
    }
}