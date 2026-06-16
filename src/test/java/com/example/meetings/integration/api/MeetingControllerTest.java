package com.example.meetings.integration.api;

import com.example.meetings.controller.MeetingController;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("dev")
@Tag("light-integration-tests")
public class MeetingControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean MeetingService meetingService;

    @MockBean UserService userService;

    @Test
    public void testProposeForm_ShouldThrowUnauthorized_WhenUserIsNotAuthorized() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testProposeForm_ShouldReturnProposePage_WhenUserIsAuthorized() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("propose"));
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testPropose_ShouldShowError_WhenMeetingServiceFails() throws Exception {
        User organizer = new User("AgentF", "emailF", "passwordF");
        String title = "title";
        String description = "description";
        String start = "2026-07-20T17:00";
        String end = "2026-07-22T17:42";
        String invitees = "AgentA,AgentB";

        when(userService.requireByUsername("AgentF")).thenReturn(organizer);
        when(meetingService.propose(
                any(User.class), anyString(), anyString(), any(Instant.class), any(Instant.class), any(List.class)
        )).thenThrow(new RuntimeException("Error!"));

        mockMvc.perform(post("/meetings/new")
                        .param("title", title)
                        .param("description", description)
                        .param("start", start)
                        .param("end", end)
                        .param("invitees", invitees)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Error!"))
                .andExpect(view().name("propose"))
                .andExpect(model().attribute("title", title))
                .andExpect(model().attribute("description", description))
                .andExpect(model().attribute("start", start))
                .andExpect(model().attribute("end", end))
                .andExpect(model().attribute("invitees", invitees));
        verify(userService).requireByUsername("AgentF");
        verify(meetingService).propose(
                any(User.class), anyString(), anyString(), any(Instant.class), any(Instant.class), any(List.class)
        );
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testPropose_ShouldCreateMeeting_WhenMeetingIsValid() throws Exception {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User participant1 = new User("AgentA", "emailA", "passwordA");
        User participant2 = new User("AgentB", "emailB", "passwordB");
        String title = "title";
        String description = "description";
        ZoneId zone = ZoneId.systemDefault();
        String start = "2026-07-20T17:00";
        Instant startInstant = LocalDateTime.parse(start).atZone(zone).toInstant();
        String end = "2026-07-22T17:42";
        Instant endInstant = LocalDateTime.parse(end).atZone(zone).toInstant();
        String invitees = "AgentA,AgentB";
        Meeting meeting = new Meeting(title, description, startInstant, endInstant, organizer);
        meeting.addParticipant(new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, participant1, InviteStatus.PENDING));
        meeting.addParticipant(new MeetingParticipant(meeting, participant2, InviteStatus.PENDING));

        when(userService.requireByUsername("AgentF")).thenReturn(organizer);
        when(meetingService.propose(
                organizer, title, description, startInstant, endInstant, List.of("AgentA", "AgentB"))
        ).thenReturn(meeting);

        mockMvc.perform(post("/meetings/new")
                        .param("title", title)
                        .param("description", description)
                        .param("start", start)
                        .param("end", end)
                        .param("invitees", invitees)
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/calendar"));

        verify(userService).requireByUsername("AgentF");
        verify(meetingService).propose(
                organizer, title, description, startInstant, endInstant, List.of("AgentA", "AgentB")
        );
        //String calendar = calendarResult.getResponse().toString();
        //assertThat(calendar).containsIgnoringCase("tentative");
        //assertThat(calendar).containsIgnoringCase(title);
        //assertThat(calendar).containsIgnoringCase(description);
        //assertThat(calendar).containsIgnoringCase(startFormatted);
        //assertThat(calendar).containsIgnoringCase(endFormatted);
        //assertThat(calendar).containsIgnoringCase("organized by " + organizer.getUsername());
        //assertThat(calendar).containsIgnoringCase(String.format("""
        //                <span>%s</span>
        //                <span>(%s)</span>
        //        """, organizer.getUsername(), "accepted"
        //));
        //assertThat(calendar).containsIgnoringCase(String.format("""
        //                <span>%s</span>
        //                <span>(%s)</span>
        //        """, participant1.getUsername(), "pending"
        //));
        //assertThat(calendar).containsIgnoringCase(String.format("""
        //                <span>%s</span>
        //                <span>(%s)</span>
        //        """, participant2.getUsername(), "pending"
        //));
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testRespond_ShouldUpdateMeetingWithAccept_WhenActionIsAccept() throws Exception {
        User f = new User("AgentF", "emailF", "passwordF");
        when(userService.requireByUsername(anyString())).thenReturn(f);
        doNothing().when(meetingService).respond(1L, f, InviteStatus.ACCEPTED);

        mockMvc.perform(post("/meetings/1/respond")
                        .param("action", "Accept")
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/calendar"));

        verify(userService).requireByUsername(anyString());
        verify(meetingService).respond(1L, f, InviteStatus.ACCEPTED);
    }

    @Test
    @WithMockUser(username = "AgentF", roles = "USER")
    public void testRespond_ShouldUpdateMeetingWithDecline_WhenActionIsAnythingOtherThanAccept() throws Exception {
        User f = new User("AgentF", "emailF", "passwordF");
        when(userService.requireByUsername(anyString())).thenReturn(f);
        doNothing().when(meetingService).respond(1L, f, InviteStatus.DECLINED);

        mockMvc.perform(post("/meetings/1/respond")
                        .param("action", "  lalala1")
                        .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/calendar"));

        verify(userService).requireByUsername(anyString());
        verify(meetingService).respond(1L, f, InviteStatus.DECLINED);
    }
}

