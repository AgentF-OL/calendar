package com.example.meetings.integration.api;

import com.example.meetings.controller.CalendarController;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalendarController.class)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@Tag("integration-tests")
public class CalendarControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean MeetingService meetingService;

    @MockBean UserService userService;

    @Value("${app.base-url}") String baseUrl;

    Clock clock = Clock.systemUTC();
    Instant start = Instant.now(clock);
    Instant end = start.plus(2, ChronoUnit.HOURS);

    @Test
    public void testCalendar_ShouldThrowUnauthorized_WhenUserIsNotAuthorized() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testCalendar_ShouldReturnCalendarPage_WhenUserIsAuthorized() throws Exception {
        User f = new User("AgentF", "emailF", "passwordF");
        String fToken = f.getIcalToken();
        String httpUrl = baseUrl + "/ical/" + fToken + ".ics";
        String webcalUrl = httpUrl.replaceFirst("^https?://", "webcal://");

        String title = "title";
        String description = "description";
        Meeting meeting = new Meeting(title, description, start, end, f);

        when(userService.requireByUsername(f.getUsername())).thenReturn(f);
        when(meetingService.calendarFor(f)).thenReturn(List.of(meeting));
        when(meetingService.pendingInvitesFor(f)).thenReturn(List.of());

        mockMvc.perform(get("/calendar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attribute("user", f))
                .andExpect(model().attribute("meetings", List.of(meeting)))
                .andExpect(model().attribute("pendingInvites", List.of()))
                .andExpect(model().attribute("icalHttpUrl", httpUrl))
                .andExpect(model().attribute("icalWebcalUrl", webcalUrl));

        verify(userService).requireByUsername(f.getUsername());
        verify(meetingService).calendarFor(f);
        verify(meetingService).pendingInvitesFor(f);
    }
}
