package com.example.meetings.unit;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.AppUserDetailsService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("dev")
@Tag("unit-tests")
public class AppUserDetailsServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks AppUserDetailsService appUserDetailsService;

    @Test
    public void testLoadUserByUsername_ShouldThrowUserNotFoundException_WhenUserDoesNotExistInTheRepository() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> appUserDetailsService.loadUserByUsername("AgentF"));
        verify(userRepository).findByUsername(anyString());
    }

    @Test
    public void testLoadUserByUsername_ShouldReturnUserDetails_WhenUserExistsInTheRepository() {
        User user = new User("AgentF", "email.example123@gmail.com", "#he293fhhash");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        UserDetails expected = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails actual = appUserDetailsService.loadUserByUsername("AgentF");
        assertEquals(expected, actual);
        verify(userRepository).findByUsername(anyString());
    }
}
