/*
package com.example.meetings.integration.api;

import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.ICalService;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ICalControllerTest.class)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("dev")
@Tag("light-integration-tests")
public class ICalControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ICalService icalService;

    @MockBean MeetingService meetingService;

    @MockBean UserRepository userRepository;

    Clock clock = Clock.systemUTC();
    Instant start = Instant.now(clock);
    Instant end = start.plus(2, ChronoUnit.HOURS);

    @Test
    public void testFeed_ShouldThrowUnauthorized_WhenUserIsNotAuthorized() throws Exception {
        mockMvc.perform(get("/ical/{token}.ics", "token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testFeed_ShouldReturnNotFound_WhenDoesNotExistsUserWithIcalToken() throws Exception {
        when(userRepository.findByIcalToken(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/ical/{token}.ics", "token")
                        .with(csrf())
                        .accept(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                )
                .andExpect(status().isNotFound());

        verify(userRepository).findByIcalToken(anyString());
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testFeed_ShouldReturnCalendarPage_WhenExistsUserWithIcalToken() throws Exception {
        User f = new User("AgentF", "emailF", "passwordF");
        String fToken = f.getIcalToken();
        Meeting meeting = new Meeting("title", "description", start, end, f);

        when(userRepository.findByIcalToken(fToken)).thenReturn(Optional.of(f));
        when(meetingService.calendarFor(f)).thenReturn(List.of(meeting));
        when(icalService.render(f, List.of(meeting))).thenReturn("myIcalCalendar");

        mockMvc.perform(get("/ical/{token}.ics", fToken).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(content().contentType("text/calendar; charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string("myIcalCalendar"));

        verify(userRepository).findByIcalToken(fToken);
        verify(meetingService).calendarFor(f);
        verify(icalService).render(f, List.of(meeting));
    }
}
*/
