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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("staging")
@Tag("integration-tests")
public class MeetingRepositoryTest {

    @Autowired MeetingRepository meetingRepository;

    @Autowired UserRepository userRepository;

    Clock clock = Clock.systemUTC();
    Instant start = Instant.now(clock);
    Instant middle = start.plus(1, ChronoUnit.HOURS);
    Instant end = start.plus(2, ChronoUnit.HOURS);
    Instant beforeStart = start.minus(1, ChronoUnit.HOURS);
    Instant afterEnd = end.plus(1, ChronoUnit.HOURS);

    @Test
    public void testSave_ShouldPersistMeeting_WhenMeetingIsValid() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        Meeting expected = new Meeting("title", "description", start, end, organizer);
        User savedOrganizer = userRepository.save(organizer);
        Meeting saved = meetingRepository.save(expected);
        Optional<Meeting> maybeMeeting = meetingRepository.findById(saved.getId());
        assertThat(maybeMeeting).isPresent();
        Meeting actual = maybeMeeting.get();
        assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getStartTime().getEpochSecond()).isEqualTo(expected.getStartTime().getEpochSecond());
        assertThat(actual.getEndTime().getEpochSecond()).isEqualTo(expected.getEndTime().getEpochSecond());
        assertThat(actual.getOrganizer()).isEqualTo(savedOrganizer);
        assertThat(actual.getParticipants()).isEmpty();
    }

    @Test
    public void testSave_ShouldRejectMeeting_WhenTitleIsNull() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        Meeting meeting = new Meeting(null, "description", start, end, organizer);
        userRepository.save(organizer);
        assertThrows(DataIntegrityViolationException.class, () -> meetingRepository.save(meeting));
    }

    @Test
    public void testSave_ShouldPersistMeeting_WhenDescriptionIsNull() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        Meeting meeting = new Meeting("title", null, start, end, organizer);
        userRepository.save(organizer);
        Meeting saved = meetingRepository.save(meeting);
        assertThat(meetingRepository.findById(saved.getId())).isPresent();
    }

    @Test
    public void testSave_ShouldRejectMeeting_WhenStartIsNull() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        Meeting meeting = new Meeting("title", "description", null, end, organizer);
        userRepository.save(organizer);
        assertThrows(DataIntegrityViolationException.class, () -> meetingRepository.save(meeting));
    }

    @Test
    public void testSave_ShouldRejectMeeting_WhenEndIsNull() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        Meeting meeting = new Meeting("title", "description", start, null, organizer);
        userRepository.save(organizer);
        assertThrows(DataIntegrityViolationException.class, () -> meetingRepository.save(meeting));
    }

    @Test
    public void testSave_ShouldRejectMeeting_WhenOrganizerIsNull() {
        Meeting meeting = new Meeting("title", "description", start, end, null);
        assertThrows(DataIntegrityViolationException.class, () -> meetingRepository.save(meeting));
    }

    @Test
    public void testSave_ShouldRejectMeeting_WhenOrganizerDoesNotExist() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        Meeting meeting = new Meeting("title", "description", start, end, organizer);
        assertThrows(InvalidDataAccessApiUsageException.class, () -> meetingRepository.save(meeting));
    }

    @Test
    public void testSave_ShouldPersistMeeting_WhenItHasMultipleParticipantsWithAnyStatus() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User accept1 = new User("AgentA", "emailA", "passwordA");
        User accept2 = new User("AgentB", "emailB", "passwordB");
        User pending1 = new User("AgentC", "emailC", "passwordC");
        User pending2 = new User("AgentD", "emailD", "passwordD");
        User declined1 = new User("AgentE", "emailE", "passwordE");
        User declined2 = new User("AgentG", "emailG", "passwordG");
        userRepository.saveAll(List.of(organizer, accept1, accept2, pending1, pending2, declined1, declined2));
        User savedOrganizer = userRepository.findByUsername("AgentF").get();

        Meeting expected = new Meeting("title", "description", start, end, organizer);

        MeetingParticipant participant1 = new MeetingParticipant(expected, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant2 = new MeetingParticipant(expected, accept1, InviteStatus.ACCEPTED);
        MeetingParticipant participant3 = new MeetingParticipant(expected, accept2, InviteStatus.ACCEPTED);
        MeetingParticipant participant4 = new MeetingParticipant(expected, pending1, InviteStatus.PENDING);
        MeetingParticipant participant5 = new MeetingParticipant(expected, pending2, InviteStatus.PENDING);
        MeetingParticipant participant6 = new MeetingParticipant(expected, declined1, InviteStatus.DECLINED);
        MeetingParticipant participant7 = new MeetingParticipant(expected, declined2, InviteStatus.DECLINED);

        expected.addParticipant(participant1);
        expected.addParticipant(participant2);
        expected.addParticipant(participant3);
        expected.addParticipant(participant4);
        expected.addParticipant(participant5);
        expected.addParticipant(participant6);
        expected.addParticipant(participant7);

        Meeting saved = meetingRepository.save(expected);
        Optional<Meeting> maybeMeeting = meetingRepository.findById(saved.getId());
        assertThat(maybeMeeting).isPresent();
        Meeting actual = maybeMeeting.get();
        assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getStartTime().getEpochSecond()).isEqualTo(expected.getStartTime().getEpochSecond());
        assertThat(actual.getEndTime().getEpochSecond()).isEqualTo(expected.getEndTime().getEpochSecond());
        assertThat(actual.getOrganizer()).isEqualTo(savedOrganizer);
        assertThat(actual.getParticipants()).hasSize(7);
    }

    @Test
    public void testSave_ShouldPersistMeetings_WhenAllMeetingsAreTheSame() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User accept1 = new User("AgentA", "emailA", "passwordA");
        User pending1 = new User("AgentC", "emailC", "passwordC");
        User declined1 = new User("AgentE", "emailE", "passwordE");
        userRepository.saveAll(List.of(organizer, accept1, pending1, declined1));
        User savedOrganizer = userRepository.findByUsername("AgentF").get();

        Meeting expected1 = new Meeting("title", "description", start, end, organizer);
        Meeting expected2 = new Meeting("title", "description", start, end, organizer);
        Meeting expected3 = new Meeting("title", "description", start, end, organizer);

        MeetingParticipant participant11 = new MeetingParticipant(expected1, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant12 = new MeetingParticipant(expected1, accept1, InviteStatus.ACCEPTED);
        MeetingParticipant participant13 = new MeetingParticipant(expected1, pending1, InviteStatus.PENDING);
        MeetingParticipant participant14 = new MeetingParticipant(expected1, declined1, InviteStatus.DECLINED);
        MeetingParticipant participant21 = new MeetingParticipant(expected2, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant22 = new MeetingParticipant(expected2, accept1, InviteStatus.ACCEPTED);
        MeetingParticipant participant23 = new MeetingParticipant(expected2, pending1, InviteStatus.PENDING);
        MeetingParticipant participant24 = new MeetingParticipant(expected2, declined1, InviteStatus.DECLINED);
        MeetingParticipant participant31 = new MeetingParticipant(expected3, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant32 = new MeetingParticipant(expected3, accept1, InviteStatus.ACCEPTED);
        MeetingParticipant participant33 = new MeetingParticipant(expected3, pending1, InviteStatus.PENDING);
        MeetingParticipant participant34 = new MeetingParticipant(expected3, declined1, InviteStatus.DECLINED);

        expected1.addParticipant(participant11);
        expected1.addParticipant(participant12);
        expected1.addParticipant(participant13);
        expected1.addParticipant(participant14);
        expected2.addParticipant(participant21);
        expected2.addParticipant(participant22);
        expected2.addParticipant(participant23);
        expected2.addParticipant(participant24);
        expected3.addParticipant(participant31);
        expected3.addParticipant(participant32);
        expected3.addParticipant(participant33);
        expected3.addParticipant(participant34);

        Meeting saved1 = meetingRepository.save(expected1);
        Optional<Meeting> maybeMeeting1 = meetingRepository.findById(saved1.getId());
        assertThat(maybeMeeting1).isPresent();
        Meeting actual1 = maybeMeeting1.get();
        assertThat(actual1.getTitle()).isEqualTo(expected1.getTitle());
        assertThat(actual1.getDescription()).isEqualTo(expected1.getDescription());
        assertThat(actual1.getStartTime().getEpochSecond()).isEqualTo(expected1.getStartTime().getEpochSecond());
        assertThat(actual1.getEndTime().getEpochSecond()).isEqualTo(expected1.getEndTime().getEpochSecond());
        assertThat(actual1.getOrganizer()).isEqualTo(savedOrganizer);
        assertThat(actual1.getParticipants()).hasSize(4);

        Meeting saved2 = meetingRepository.save(expected2);
        Optional<Meeting> maybeMeeting2 = meetingRepository.findById(saved2.getId());
        assertThat(maybeMeeting2).isPresent();
        Meeting actual2 = maybeMeeting2.get();
        assertThat(actual2.getTitle()).isEqualTo(expected2.getTitle());
        assertThat(actual2.getDescription()).isEqualTo(expected2.getDescription());
        assertThat(actual2.getStartTime().getEpochSecond()).isEqualTo(expected2.getStartTime().getEpochSecond());
        assertThat(actual2.getEndTime().getEpochSecond()).isEqualTo(expected2.getEndTime().getEpochSecond());
        assertThat(actual2.getOrganizer()).isEqualTo(savedOrganizer);
        assertThat(actual2.getParticipants()).hasSize(4);

        Meeting saved3 = meetingRepository.save(expected3);
        Optional<Meeting> maybeMeeting3 = meetingRepository.findById(saved3.getId());
        assertThat(maybeMeeting3).isPresent();
        Meeting actual3 = maybeMeeting3.get();
        assertThat(actual3.getTitle()).isEqualTo(expected3.getTitle());
        assertThat(actual3.getDescription()).isEqualTo(expected3.getDescription());
        assertThat(actual3.getStartTime().getEpochSecond()).isEqualTo(expected3.getStartTime().getEpochSecond());
        assertThat(actual3.getEndTime().getEpochSecond()).isEqualTo(expected3.getEndTime().getEpochSecond());
        assertThat(actual3.getOrganizer()).isEqualTo(savedOrganizer);
        assertThat(actual3.getParticipants()).hasSize(4);
    }

    @Test
    public void testFindCalendarMeetings_ShouldNotReturnDeclinedMeeting_WhenUserDeclined() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User declined = new User("AgentA", "emailA", "passwordA");
        userRepository.saveAll(List.of(organizer, declined));

        Meeting expected1 = new Meeting("title1", "description", start, end, organizer);
        Meeting expected2 = new Meeting("title2", "description", start, end, organizer);

        MeetingParticipant participant11 = new MeetingParticipant(expected1, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant12 = new MeetingParticipant(expected1, declined, InviteStatus.ACCEPTED);
        MeetingParticipant participant21 = new MeetingParticipant(expected2, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant22 = new MeetingParticipant(expected2, declined, InviteStatus.DECLINED);

        expected1.addParticipant(participant11);
        expected1.addParticipant(participant12);
        expected2.addParticipant(participant21);
        expected2.addParticipant(participant22);
        meetingRepository.saveAll(List.of(expected1, expected2));

        List<Meeting> meetingsOfDeclinedUser = meetingRepository.findCalendarMeetings(declined);
        assertThat(meetingsOfDeclinedUser).hasSize(1);
        assertThat(meetingsOfDeclinedUser.get(0).getTitle()).isEqualTo(expected1.getTitle());
    }

    @Test
    public void testFindCalendarMeetings_ShouldReturnEmpty_WhenUserHasNoMeetings() {
        User f = new User("AgentF", "emailF", "passwordF");
        userRepository.save(f);
        assertThat(meetingRepository.findCalendarMeetings(f)).isEmpty();
    }

    @Test
    public void testFindOverlapping_ShouldNotReturnMeeting_WhenUserDeclinedOrMeetingIsOutOfTimeFrame() {
        User organizer = new User("AgentF", "emailF", "passwordF");
        User declined = new User("AgentA", "emailA", "passwordA");
        userRepository.saveAll(List.of(organizer, declined));

        Meeting expected1 = new Meeting("title1", "description", beforeStart, middle, organizer);
        Meeting expected2 = new Meeting("title2", "description", beforeStart, middle, organizer);
        Meeting expected3 = new Meeting("title3", "description", end, afterEnd, organizer);

        MeetingParticipant participant11 = new MeetingParticipant(expected1, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant12 = new MeetingParticipant(expected1, declined, InviteStatus.ACCEPTED);
        MeetingParticipant participant21 = new MeetingParticipant(expected2, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant22 = new MeetingParticipant(expected2, declined, InviteStatus.DECLINED);
        MeetingParticipant participant31 = new MeetingParticipant(expected3, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant participant32 = new MeetingParticipant(expected3, declined, InviteStatus.PENDING);

        expected1.addParticipant(participant11);
        expected1.addParticipant(participant12);
        expected2.addParticipant(participant21);
        expected2.addParticipant(participant22);
        expected3.addParticipant(participant31);
        expected3.addParticipant(participant32);
        meetingRepository.saveAll(List.of(expected1, expected2, expected3));

        List<Meeting> meetingsOfDeclinedUser = meetingRepository.findOverlapping(declined, start, end);
        assertThat(meetingsOfDeclinedUser).hasSize(1);
        assertThat(meetingsOfDeclinedUser.get(0).getTitle()).isEqualTo(expected1.getTitle());
    }

    @Test
    public void testFindOverlapping_ShouldReturnEmpty_WhenUserHasNoMeetingsInTimeFrame() {
        User f = new User("AgentF", "emailF", "passwordF");
        User user = new User("AgentA", "emailA", "passwordA");
        userRepository.saveAll(List.of(f, user));

        Meeting outOfFrame = new Meeting("title", "description", beforeStart, start, f);

        MeetingParticipant participant1 = new MeetingParticipant(outOfFrame, f, InviteStatus.ACCEPTED);
        MeetingParticipant participant2 = new MeetingParticipant(outOfFrame, user, InviteStatus.ACCEPTED);

        outOfFrame.addParticipant(participant1);
        outOfFrame.addParticipant(participant2);

        assertThat(meetingRepository.findOverlapping(user, start, end)).isEmpty();
    }
}
