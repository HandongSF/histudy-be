package edu.handong.csee.histudy.user;

import edu.handong.csee.histudy.domain.Friendship;
import edu.handong.csee.histudy.domain.FriendshipStatus;
import edu.handong.csee.histudy.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional
public class FriendshipTests {

    @DisplayName("내가 친구로 등록(요청)한 상대를 알 수 있다.")
    @Test
    public void FriendshipTests_17() {
        // given
        User userA = User.builder()
                .name("userA")
                .build();
        User userB = User.builder()
                .name("userB")
                .build();

        // when
        userA.add(userB);
        Friendship friendship = userA.getSentRequests()
                .stream()
                .findAny()
                .orElseThrow();

        // then
        assertThat(friendship.getSent()).isEqualTo(userA);
        assertThat(friendship.getReceived()).isEqualTo(userB);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @DisplayName("나를 친구로 등록(요청)한 상대를 알 수 있다.")
    @Test
    public void FriendshipTests_42() {
        // given
        User userA = User.builder()
                .name("userA")
                .build();
        User userB = User.builder()
                .name("userB")
                .build();

        // when
        userA.add(userB);
        Friendship friendship = userB.getReceivedRequests()
                .stream()
                .findAny()
                .orElseThrow();

        // then
        assertThat(friendship.getSent()).isEqualTo(userA);
        assertThat(friendship.getReceived()).isEqualTo(userB);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @DisplayName("등록(요청)한 상대에 대해 요청상태(대기, 수락, 거절) 등을 알 수 있다: 수락")
    @Test
    public void FriendshipTests_66() {
        // given
        User userA = User.builder()
                .name("userA")
                .build();
        User userB = User.builder()
                .name("userB")
                .build();

        // when
        userA.add(userB);
        userB.getReceivedRequests()
                .stream()
                .filter(friendship -> friendship.getSent().equals(userA))
                .findAny()
                .ifPresent((Friendship::accept));

        Friendship friendship = userB.getReceivedRequests()
                .stream()
                .findAny()
                .orElseThrow();

        // then
        assertThat(friendship.getSent()).isEqualTo(userA);
        assertThat(friendship.getReceived()).isEqualTo(userB);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @DisplayName("등록(요청)한 상대에 대해 요청상태(대기, 수락, 거절) 등을 알 수 있다: 거절")
    @Test
    public void FriendshipTests_96() {
        // given
        User userA = User.builder()
                .name("userA")
                .build();
        User userB = User.builder()
                .name("userB")
                .build();

        // when
        userA.add(userB);
        userB.getReceivedRequests()
                .stream()
                .filter(friendship -> friendship.getSent().equals(userA))
                .findAny()
                .ifPresent(Friendship::decline);

        // then
        assertThat(userB.getReceivedRequests()).isEmpty();
        assertThat(userA.getSentRequests()).isEmpty();
    }
}
