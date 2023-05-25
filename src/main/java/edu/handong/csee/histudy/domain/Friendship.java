package edu.handong.csee.histudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User sent;

    @ManyToOne(fetch = FetchType.LAZY)
    private User received;

    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    public Friendship(User sent, User received) {
        this.sent = sent;
        this.received = received;
        this.status = FriendshipStatus.PENDING;
    }

    public void accept() {
        if (this.status == FriendshipStatus.PENDING) {
            this.status = FriendshipStatus.ACCEPTED;
        }
    }

    public void decline() {
        if (this.status == FriendshipStatus.PENDING) {
            sent.getSentRequests().remove(this);
            received.getReceivedRequests().remove(this);
        }
    }
}
