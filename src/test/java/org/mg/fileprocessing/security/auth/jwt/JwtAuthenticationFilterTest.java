package org.mg.fileprocessing.security.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.security.auth.UserRole;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.http.MediaTypeAssert;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldAuthenticateWhenValidHeader() throws ServletException, IOException {
        // Given
        String jwtToken = "valid.jwt.token";
        String username = "test@email.com";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Authorization", "Bearer " + jwtToken);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        UserDetails userDetails = User.builder()
                .username(username)
                .password("testpwd")
                .authorities(UserRole.USER.getWithPrefix())
                .build();

        given(jwtUtil.parseJwt(anyString())).willReturn(Optional.of(jwtToken));
        given(jwtUtil.isValid(jwtToken)).willReturn(true);
        given(jwtUtil.getUsernameFromToken(jwtToken)).willReturn(username);
        given(userDetailsService.loadUserByUsername(username)).willReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(mockHttpServletRequest, mockHttpServletResponse, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertThat(auth.getName()).isEqualTo(username);
        verify(filterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void shouldNotAuthenticateWhenInvalidRequest() throws ServletException, IOException {
        // Given
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(jwtUtil.parseJwt(any())).willReturn(Optional.empty());

        // When
        jwtAuthenticationFilter.doFilterInternal(mockHttpServletRequest, mockHttpServletResponse, filterChain);

        // Then
        verify(jwtUtil, never()).isValid(anyString());
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidJwtToken() {
        // Given
        String jwtToken = "valid.jwt.token";

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Authorization", "Bearer " + jwtToken);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        given(jwtUtil.parseJwt(anyString())).willReturn(Optional.of(jwtToken));
        given(jwtUtil.isValid(jwtToken)).willReturn(false);

        // When
        // Then
        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(mockHttpServletRequest, mockHttpServletResponse, filterChain))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessage("Failed to authenticate user")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}