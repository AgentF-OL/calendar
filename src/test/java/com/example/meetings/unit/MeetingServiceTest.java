package com.example.meetings.unit;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("dev")
@Tag("unit-tests")
public class MeetingServiceTest {

    @Mock MeetingRepository meetingRepository;

    @Mock MeetingParticipantRepository participantRepository;

    @Mock UserRepository userRepository;

    @InjectMocks MeetingService meetingService;

    private static User user;

    @BeforeAll
    public static void init() {
        user = new User("name42", "example.email123@gmail.com", "password");
    }

    @Test
    public void testPropose_ShouldThrowIllegalArgumentException_WhenMeetingEndDoesNotComeAfterStart() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.minus(1, ChronoUnit.HOURS);
        List<String> inviteeUsernames = List.of();
        assertThrows(IllegalArgumentException.class, () -> meetingService.propose(
                user, title, description, start, end, inviteeUsernames
        ));
    }

    @Test
    public void testPropose_ShouldAutoAcceptOrganizer_WhenOrganizerProposesMeetingAndInviteesListIsEmpty() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        List<String> inviteeUsernames = List.of();
        Meeting expected = new Meeting(title, description, start, end, user);
        expected.addParticipant(new MeetingParticipant(expected, user, InviteStatus.ACCEPTED));
        when(meetingRepository.save(expected)).thenReturn(expected);
        Meeting actual = meetingService.propose(user, title, description, start, end, inviteeUsernames);
        assertEquals(1, actual.getParticipants().size());
        assertEquals(InviteStatus.ACCEPTED, expected.getParticipants().stream().toList().get(0).getStatus());
        assertEquals(expected, actual);
        verify(meetingRepository).save(expected);
    }

    @Test
    public void testPropose_ShouldInviteNoOneElse_WhenInviteeNamesAreNullOrWhiteSpace() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        List<String> inviteeUsernames = new ArrayList<>();
        inviteeUsernames.add("     ");
        inviteeUsernames.add(null);
        Meeting expected = new Meeting(title, description, start, end, user);
        expected.addParticipant(new MeetingParticipant(expected, user, InviteStatus.ACCEPTED));
        when(meetingRepository.save(expected)).thenReturn(expected);
        Meeting actual = meetingService.propose(user, title, description, start, end, inviteeUsernames);
        assertEquals(1, actual.getParticipants().size());
        assertEquals(InviteStatus.ACCEPTED, expected.getParticipants().stream().toList().get(0).getStatus());
        assertEquals(expected, actual);
        verify(meetingRepository).save(expected);
    }

    @Test
    public void testPropose_ShouldThrowIllegalArgumentException_WhenInviteeNameDoesNotExist() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        List<String> inviteeUsernames = List.of("     ", "name!&%");
        when(userRepository.findByUsername("name!&%")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> meetingService.propose(user, title, description, start, end, inviteeUsernames));
        verify(userRepository).findByUsername("name!&%");
    }

    @Test
    public void testPropose_ShouldMarkInviteesInviteStatusAsPending_WhenAllInviteeNamesAreValid() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        List<String> inviteeUsernames = List.of("     ", "name!&%", "name123");
        User invitee1 = new User("name!&%", "email1", "password1");
        User invitee2 = new User("name123", "email2", "password2");
        Meeting expected = new Meeting(title, description, start, end, user);
        expected.addParticipant(new MeetingParticipant(expected, user, InviteStatus.ACCEPTED));
        expected.addParticipant(new MeetingParticipant(expected, invitee1, InviteStatus.PENDING));
        expected.addParticipant(new MeetingParticipant(expected, invitee2, InviteStatus.PENDING));
        when(userRepository.findByUsername("name!&%")).thenReturn(Optional.of(invitee1));
        when(userRepository.findByUsername("name123")).thenReturn(Optional.of(invitee2));
        when(meetingRepository.save(expected)).thenReturn(expected);
        Meeting actual = meetingService.propose(user, title, description, start, end, inviteeUsernames);
        assertTrue(actual.getParticipants()
                .stream()
                .allMatch(p -> p.getUser().equals(user) ?
                        p.getStatus().equals(InviteStatus.ACCEPTED) :
                        p.getStatus().equals(InviteStatus.PENDING)
                )
        );
        assertEquals(expected, actual);
        verify(userRepository).findByUsername("name!&%");
        verify(userRepository).findByUsername("name123");
        verify(meetingRepository).save(expected);
    }

    @Test
    public void testRespond_ShouldThrowIllegalArgumentException_WhenInviteStatusIsPending() {
        assertThrows(IllegalArgumentException.class, () -> meetingService.respond(1L, user, InviteStatus.PENDING));
    }

    @Test
    public void testRespond_ShouldThrowIllegalArgumentException_WhenUserWasNotInvitedToTheMeeting() {
        when(participantRepository.findByMeetingIdAndUserId(anyLong(), any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> meetingService.respond(1L, user, InviteStatus.ACCEPTED));
        verify(participantRepository).findByMeetingIdAndUserId(anyLong(), any());
    }

    @Test
    public void testRespond_ShouldUpdateRepositoryUserStatus_WhenUserRespondedByAcceptingTheMeeting() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        Meeting meeting = new Meeting(title, description, start, end, user);
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(anyLong(), any())).thenReturn(Optional.of(participant));
        assertDoesNotThrow(() -> meetingService.respond(1L, user, InviteStatus.ACCEPTED));
        assertEquals(InviteStatus.ACCEPTED, participant.getStatus());
        verify(participantRepository).findByMeetingIdAndUserId(anyLong(), any());
    }

    @Test
    public void testRespond_ShouldUpdateRepositoryUserStatus_WhenUserRespondedByDecliningTheMeeting() {
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        Meeting meeting = new Meeting(title, description, start, end, user);
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(anyLong(), any())).thenReturn(Optional.of(participant));
        assertDoesNotThrow(() -> meetingService.respond(1L, user, InviteStatus.DECLINED));
        assertEquals(InviteStatus.DECLINED, participant.getStatus());
        verify(participantRepository).findByMeetingIdAndUserId(anyLong(), any());
    }

    @Test
    public void testCopyFromDiscovered_ShouldAutoAcceptUserAndConfirmMeeting_Always() {
        String source = "source";
        String externalId = "externalId";
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        String url = "url";
        String venue = "venue";
        DiscoveredEvent event = new DiscoveredEvent(
                source, externalId, title, description, start, end, url, venue
        );
        String meetingDescription = String.format("%s\n\nVenue: %s\nSource: source (%s)", description, venue, url);
        Meeting expected = new Meeting(title, meetingDescription, start, end, user);
        expected.addParticipant(new MeetingParticipant(expected, user, InviteStatus.ACCEPTED));
        when(meetingRepository.save(expected)).thenReturn(expected);
        Meeting actual = meetingService.copyFromDiscovered(user, event);
        assertEquals(InviteStatus.ACCEPTED, actual.getParticipants().stream().toList().get(0).getStatus());
        assertTrue(actual.isConfirmed());
        assertEquals(expected, actual);
        verify(meetingRepository).save(expected);
    }

    @Test
    public void testCopyFromDiscovered_ShouldAutoGenerateEndTime_WhenEventDoesNotIncludeEndTime() {
        String source = "source";
        String externalId = "externalId";
        String title = "title";
        String description = "description";
        Instant start = Instant.now();
        Instant end = start.plus(Duration.ofHours(2));
        String url = "url";
        String venue = "venue";
        DiscoveredEvent event = new DiscoveredEvent(
                source, externalId, title, description, start, null, url, venue
        );
        String meetingDescription = String.format("%s\n\nVenue: %s\nSource: source (%s)", description, venue, url);
        Meeting expected = new Meeting(title, meetingDescription, start, end, user);
        expected.addParticipant(new MeetingParticipant(expected, user, InviteStatus.ACCEPTED));
        when(meetingRepository.save(expected)).thenReturn(expected);
        Meeting actual = meetingService.copyFromDiscovered(user, event);
        assertNotNull(actual.getEndTime());
        assertEquals(end, actual.getEndTime());
        assertEquals(expected, actual);
        verify(meetingRepository).save(expected);
    }

    @Test
    public void testCalendarForIcalToken_ShouldThrowIllegalArgumentException_WhenNoUserHasTheIcalToken() {
        when(userRepository.findByIcalToken("token")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> meetingService.calendarForIcalToken("token"));
        verify(userRepository).findByIcalToken("token");
    }

    @Test
    public void testCalendarForIcalToken_ShouldReturnUserCalendar_WhenAUserExistsWithTheIcalToken() {
        String title1 = "title1";
        String title2 = "title2";
        String description1 = "description1";
        String description2 = "description2";
        Instant start1 = Instant.now();
        Instant start2 = start1.plus(4, ChronoUnit.HOURS);
        Instant end1 = start1.plus(1, ChronoUnit.HOURS);
        Instant end2 = start2.plus(3, ChronoUnit.HOURS);
        Meeting meeting1 = new Meeting(title1, description1, start1, end1, user);
        Meeting meeting2 = new Meeting(title2, description2, start2, end2, user);
        List<Meeting> expected = List.of(meeting1, meeting2);
        when(userRepository.findByIcalToken("token")).thenReturn(Optional.of(user));
        when(meetingRepository.findCalendarMeetings(user)).thenReturn(expected);
        List<Meeting> actual = meetingService.calendarForIcalToken("token");
        assertEquals(2, actual.size());
        assertTrue(actual.stream().allMatch(m -> m.getOrganizer().equals(user)));
        assertEquals(expected, actual);
        verify(userRepository).findByIcalToken("token");
        verify(meetingRepository).findCalendarMeetings(user);
    }
}
