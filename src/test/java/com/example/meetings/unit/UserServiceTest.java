package com.example.meetings.unit;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("dev")
@Tag("unit-tests")
public class UserServiceTest {

    UserRepository userRepository = mock(UserRepository.class);

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    UserService userService = new UserService(userRepository, passwordEncoder);

    @Test
    public void testRegister_ShouldThrowIllegalArgumentException_WhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("name42")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.register(
                "name42", "example.email123@gmail.com", "password"
        ));
        verify(userRepository).existsByUsername("name42");
    }

    @Test
    public void testRegister_ShouldReturnRegisteredUser_WhenUsernameDoesNotExist() {
        String username = "name42";
        String email = "example.email123@gmail.com";
        String password = "password";
        User expected = new User(username, email, passwordEncoder.encode(password));
        when(userRepository.existsByUsername("name42")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(expected);
        User actual = userService.register(username, email, password);
        assertEquals(expected, actual);
        assertTrue(passwordEncoder.matches(password, actual.getPasswordHash()));
        verify(userRepository).existsByUsername("name42");
        verify(userRepository).save(any());
    }

    @Test
    public void testRequireByUsername_ShouldThrowIllegalArgumentException_WhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("name42")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.requireByUsername("name42"));
        verify(userRepository).findByUsername("name42");
    }

    @Test
    public void testRequireByUsername_ShouldReturnRegisteredUser_WhenUsernameDoesNotExist() {
        String username = "name42";
        String email = "example.email123@gmail.com";
        String password = "password";
        User expected = new User(username, email, password);
        when(userRepository.findByUsername("name42")).thenReturn(Optional.of(expected));
        User actual = userService.requireByUsername(username);
        assertEquals(expected, actual);
        verify(userRepository).findByUsername("name42");
    }
}
