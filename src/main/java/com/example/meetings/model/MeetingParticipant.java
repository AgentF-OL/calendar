package com.example.meetings.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "user_id"}))
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status;

    protected MeetingParticipant() {}

    public MeetingParticipant(Meeting meeting, User user, InviteStatus status) {
        this.meeting = meeting;
        this.user = user;
        this.status = status;
    }

    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public User getUser() { return user; }
    public InviteStatus getStatus() { return status; }

    public void setStatus(InviteStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MeetingParticipant that)) return false;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getMeeting().getId(), that.getMeeting().getId())
                && Objects.equals(getUser(), that.getUser())
                && getStatus() == that.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getMeeting().getId(), getUser(), getStatus());
    }
}
