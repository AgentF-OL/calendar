package com.example.meetings.integration.api;

import com.example.meetings.controller.AuthController;
import com.example.meetings.model.User;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@Tag("integration-tests")
public class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;

    String passwordHash = new BCryptPasswordEncoder().encode("password");

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testRegisterForm_ShouldReturnUnfilledRegisterPage_Always() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testRegister_ShouldShowError_WhenUserAlreadyExists() throws Exception {
        when(userService.register(anyString(), anyString(), anyString())).thenThrow(
                new IllegalArgumentException("Username already exists")
        );
        mockMvc.perform(post("/register")
                        .param("username", "AgentF")
                        .param("email", "email.example123@gmail.com")
                        .param("password", "password")
                        .with(csrf())
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Username already exists"))
                .andExpect(model().attribute("username", is("AgentF")))
                .andExpect(model().attribute("email", is("email.example123@gmail.com")));
        verify(userService).register(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testRegister_ShouldFillUserInfo_WhenUserDoesNotExist() throws Exception {
        when(userService.register(anyString(), anyString(), anyString())).thenReturn(
                new User("AgentF", "email.example123@gmail.com", passwordHash)
        );
        mockMvc.perform(post("/register")
                        .param("username", "AgentF")
                        .param("email", "email.example123@gmail.com")
                        .param("password", "password")
                        .with(csrf())
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login?registered"))
                .andExpect(model().attributeDoesNotExist("error"));
        verify(userService).register(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testRoot_ShouldRedirectToCalendar_WhenUserIsAuthorized() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/calendar"));
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testLogin_ShouldReturnLoginPage_Always() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }
}
