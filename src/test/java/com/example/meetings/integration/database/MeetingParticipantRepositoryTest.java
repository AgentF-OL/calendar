package com.example.meetings.integration.database;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("staging")
@Tag("integration-tests")
public class MeetingParticipantRepositoryTest {

    @Autowired MeetingParticipantRepository participantRepository;

    @Autowired MeetingRepository meetingRepository;

    @Autowired UserRepository userRepository;

    Clock clock = Clock.systemUTC();
    Instant start = Instant.now(clock);
    Instant end = start.plus(2, ChronoUnit.HOURS);

    @Test
    public void testSave_ShouldRejectParticipant_WhenMeetingIsNull() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        userRepository.save(organizer);
        MeetingParticipant participant = new MeetingParticipant(null, organizer, InviteStatus.ACCEPTED);
        assertThrows(DataIntegrityViolationException.class, () -> participantRepository.save(participant));
    }

    @Test
    public void testSave_ShouldRejectParticipant_WhenUserIsNull() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        userRepository.save(organizer);
        Meeting meeting = new Meeting("title", "description", start, end, organizer);
        MeetingParticipant participant = new MeetingParticipant(meeting, null, InviteStatus.ACCEPTED);
        assertThrows(InvalidDataAccessApiUsageException.class, () -> participantRepository.save(participant));
    }

    @Test
    public void testSave_ShouldRejectParticipant_WhenParticipantForMeetingAndUserAlreadyExists() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        userRepository.save(organizer);

        Meeting meeting = new Meeting("title", "description", start, end, organizer);
        MeetingParticipant participant1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant2 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant3 = new MeetingParticipant(meeting, organizer, InviteStatus.PENDING);
        MeetingParticipant participant4 = new MeetingParticipant(meeting, organizer, InviteStatus.DECLINED);

        meeting.addParticipant(participant1);
        meetingRepository.save(meeting);
        assertThrows(RuntimeException.class, () -> participantRepository.save(participant2));
        assertThrows(RuntimeException.class, () -> participantRepository.save(participant3));
        assertThrows(RuntimeException.class, () -> participantRepository.save(participant4));
    }

    @Test
    public void testFindByUserAndStatus_ShouldFindParticipant_WhenUserAndMeetingAreValid() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User accepted =  new User("AgentA", "emailA", "passwordA");
        User pending =  new User("AgentB", "emailB", "passwordB");
        User declined =  new User("AgentC", "emailC", "passwordC");
        User organizerSaved = userRepository.save(organizer);
        User acceptedSaved = userRepository.save(accepted);
        User pendingSaved = userRepository.save(pending);
        User declinedSaved = userRepository.save(declined);

        Meeting meeting = new Meeting("title", "description", start, end, organizer);

        MeetingParticipant participant1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant2 = new MeetingParticipant(meeting, accepted, InviteStatus.ACCEPTED);
        MeetingParticipant participant3 = new MeetingParticipant(meeting, pending, InviteStatus.PENDING);
        MeetingParticipant participant4 = new MeetingParticipant(meeting, declined, InviteStatus.DECLINED);

        meeting.addParticipant(participant1);
        meeting.addParticipant(participant2);
        meeting.addParticipant(participant3);
        meeting.addParticipant(participant4);

        meetingRepository.save(meeting).getId();

        assertThat(participantRepository.findByUserAndStatus(organizerSaved, InviteStatus.ACCEPTED)).hasSize(1);
        assertThat(participantRepository.findByUserAndStatus(acceptedSaved, InviteStatus.ACCEPTED)).hasSize(1);
        assertThat(participantRepository.findByUserAndStatus(pendingSaved, InviteStatus.PENDING)).hasSize(1);
        assertThat(participantRepository.findByUserAndStatus(declinedSaved, InviteStatus.DECLINED)).hasSize(1);

        assertThat(participantRepository.findByUserAndStatus(organizerSaved, InviteStatus.PENDING)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(organizerSaved, InviteStatus.DECLINED)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(acceptedSaved, InviteStatus.PENDING)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(acceptedSaved, InviteStatus.DECLINED)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(pendingSaved, InviteStatus.ACCEPTED)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(pendingSaved, InviteStatus.DECLINED)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(declinedSaved, InviteStatus.ACCEPTED)).isEmpty();
        assertThat(participantRepository.findByUserAndStatus(declinedSaved, InviteStatus.PENDING)).isEmpty();
    }

    @Test
    public void testFindByMeetingIdAndUserId_ShouldFindParticipant_WhenUserAndMeetingAreValid() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User accepted =  new User("AgentA", "emailA", "passwordA");
        User pending =  new User("AgentB", "emailB", "passwordB");
        User declined =  new User("AgentC", "emailC", "passwordC");
        Long organizerId = userRepository.save(organizer).getId();
        Long acceptedId = userRepository.save(accepted).getId();
        Long pendingId = userRepository.save(pending).getId();
        Long declinedId = userRepository.save(declined).getId();

        Meeting meeting = new Meeting("title", "description", start, end, organizer);

        MeetingParticipant participant1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant2 = new MeetingParticipant(meeting, accepted, InviteStatus.ACCEPTED);
        MeetingParticipant participant3 = new MeetingParticipant(meeting, pending, InviteStatus.PENDING);
        MeetingParticipant participant4 = new MeetingParticipant(meeting, declined, InviteStatus.DECLINED);

        meeting.addParticipant(participant1);
        meeting.addParticipant(participant2);
        meeting.addParticipant(participant3);
        meeting.addParticipant(participant4);

        Long meetingId = meetingRepository.save(meeting).getId();

        assertThat(participantRepository.findByMeetingIdAndUserId(meetingId, organizerId).isPresent()).isTrue();
        assertThat(participantRepository.findByMeetingIdAndUserId(meetingId, acceptedId).isPresent()).isTrue();
        assertThat(participantRepository.findByMeetingIdAndUserId(meetingId, pendingId).isPresent()).isTrue();
        assertThat(participantRepository.findByMeetingIdAndUserId(meetingId, declinedId).isPresent()).isTrue();
    }

    @Test
    public void testFindByMeetingIdAndUserId_ShouldNotFindParticipant_WhenUserNotInMeeting() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User accepted =  new User("AgentA", "emailA", "passwordA");
        User pending =  new User("AgentB", "emailB", "passwordB");
        User declined =  new User("AgentC", "emailC", "passwordC");
        userRepository.saveAll(List.of(organizer, accepted, pending));
        Long declinedId = userRepository.save(declined).getId();

        Meeting meeting = new Meeting("title", "description", start, end, organizer);

        MeetingParticipant participant1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant2 = new MeetingParticipant(meeting, accepted, InviteStatus.ACCEPTED);
        MeetingParticipant participant3 = new MeetingParticipant(meeting, pending, InviteStatus.PENDING);

        meeting.addParticipant(participant1);
        meeting.addParticipant(participant2);
        meeting.addParticipant(participant3);

        Long meetingId = meetingRepository.save(meeting).getId();

        assertThat(participantRepository.findByMeetingIdAndUserId(meetingId, declinedId).isPresent()).isFalse();
    }
}
