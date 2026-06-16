package com.example.meetings.integration.database;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@Transactional
@ActiveProfiles("staging")
@Tag("heavy-integration-tests")
public class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    @Test
    public void testSave_ShouldPersistUser_WhenAllFieldsAreFilledAndUsernameIsUnique() {
        User expected = new User("AgentF", "emailF", "passwordF");
        userRepository.save(expected);
        Optional<User> maybeActual = userRepository.findByUsername(expected.getUsername());
        assertThat(maybeActual.isPresent()).isTrue();
        User actual = maybeActual.get();
        assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
        assertThat(actual.getEmail()).isEqualTo(expected.getEmail());
        assertThat(actual.getPasswordHash()).isEqualTo(expected.getPasswordHash());
    }

    @Test
    public void testSave_ShouldRejectUser_WhenUsernameIsNull() {
        User expected = new User(null, "emailF", "passwordF");
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(expected));
    }

    @Test
    public void testSave_ShouldRejectUser_WhenEmailIsNull() {
        User expected = new User("AgentF", null, "passwordF");
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(expected));
    }

    @Test
    public void testSave_ShouldRejectUser_WhenPasswordIsNull() {
        User expected = new User("AgentF", "emailF", null);
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(expected));
    }

    @Test
    public void testSave_ShouldRejectUser_WhenUsernameAlreadyExists() {
        User first = new User("AgentF", "emailF", "passwordF");
        User second =  new User("AgentF", "emailF2", "passwordF2");
        userRepository.save(first);
        assertThat(userRepository.findByUsername(first.getUsername()).isPresent()).isTrue();
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.save(second));
    }

    @Test
    public void testFindByUsername_ShouldFindEachUser_WhenTheUsersAreInTheDatabase() {
        User f = new User("AgentF", "emailF", "passwordF");
        User a =  new User("AgentA", "emailA", "passwordA");
        User b = new User("AgentB", "emailB", "passwordB");

        userRepository.save(f);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isTrue();
        userRepository.save(a);
        assertThat(userRepository.findByUsername(a.getUsername()).isPresent()).isTrue();
        userRepository.save(b);
        assertThat(userRepository.findByUsername(b.getUsername()).isPresent()).isTrue();
    }

    @Test
    public void testFindByUsername_ShouldNotFindUser_WhenTheUserDoesNotExist() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isFalse();
        userRepository.save(f);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isTrue();
    }

    @Test
    public void testFindByUsername_ShouldNotFindUser_WhenTheUserHasBeenDeleted() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isFalse();
        User saved = userRepository.save(f);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isTrue();
        userRepository.delete(saved);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isFalse();
    }

    @Test
    public void testFindByUsername_ShouldFindUser_WhenTheUserHasBeenDeletedButAddedBack() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isFalse();
        User saved = userRepository.save(f);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isTrue();
        userRepository.delete(saved);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isFalse();
        userRepository.save(f);
        assertThat(userRepository.findByUsername(f.getUsername()).isPresent()).isTrue();
    }

    @Test
    public void testFindByIcalToken_ShouldFindEachUser_WhenTheUsersAreInTheDatabase() {
        User f = new User("AgentF", "emailF", "passwordF");
        User a =  new User("AgentA", "emailA", "passwordA");
        User b = new User("AgentB", "emailB", "passwordB");

        userRepository.save(f);
        assertThat(userRepository.findByIcalToken(f.getIcalToken()).isPresent()).isTrue();
        userRepository.save(a);
        assertThat(userRepository.findByIcalToken(a.getIcalToken()).isPresent()).isTrue();
        userRepository.save(b);
        assertThat(userRepository.findByIcalToken(b.getIcalToken()).isPresent()).isTrue();
    }

    @Test
    public void testFindByIcalToken_ShouldNotFindUser_WhenTheUserDoesNotExist() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.findByIcalToken(f.getIcalToken()).isPresent()).isFalse();
        userRepository.save(f);
        assertThat(userRepository.findByIcalToken(f.getIcalToken()).isPresent()).isTrue();
    }

    @Test
    public void testFindByIcalToken_ShouldNotFindUser_WhenTheUserHasBeenDeleted() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.findByIcalToken(f.getIcalToken()).isPresent()).isFalse();
        User saved = userRepository.save(f);
        assertThat(userRepository.findByIcalToken(f.getIcalToken()).isPresent()).isTrue();
        userRepository.delete(saved);
        assertThat(userRepository.findByIcalToken(f.getIcalToken()).isPresent()).isFalse();
    }

    @Test
    public void testExistsByUsername_ShouldFindEachUser_WhenTheUsersAreInTheDatabase() {
        User f = new User("AgentF", "emailF", "passwordF");
        User a =  new User("AgentA", "emailA", "passwordA");
        User b = new User("AgentB", "emailB", "passwordB");

        userRepository.save(f);
        assertThat(userRepository.existsByUsername(f.getUsername())).isTrue();
        userRepository.save(a);
        assertThat(userRepository.existsByUsername(a.getUsername())).isTrue();
        userRepository.save(b);
        assertThat(userRepository.existsByUsername(b.getUsername())).isTrue();
    }

    @Test
    public void testExistsByUsername_ShouldNotFindUser_WhenTheUserDoesNotExist() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.existsByUsername(f.getUsername())).isFalse();
        userRepository.save(f);
        assertThat(userRepository.existsByUsername(f.getUsername())).isTrue();
    }

    @Test
    public void testExistsByUsername_ShouldNotFindUser_WhenTheUserHasBeenDeleted() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.existsByUsername(f.getUsername())).isFalse();
        User saved = userRepository.save(f);
        assertThat(userRepository.existsByUsername(f.getUsername())).isTrue();
        userRepository.delete(saved);
        assertThat(userRepository.existsByUsername(f.getUsername())).isFalse();
    }

    @Test
    public void testExistsByUsername_ShouldFindUser_WhenTheUserHasBeenDeletedButAddedBack() {
        User f = new User("AgentF", "emailF", "passwordF");

        assertThat(userRepository.existsByUsername(f.getUsername())).isFalse();
        User saved = userRepository.save(f);
        assertThat(userRepository.existsByUsername(f.getUsername())).isTrue();
        userRepository.delete(saved);
        assertThat(userRepository.existsByUsername(f.getUsername())).isFalse();
        userRepository.save(f);
        assertThat(userRepository.existsByUsername(f.getUsername())).isTrue();
    }
}
