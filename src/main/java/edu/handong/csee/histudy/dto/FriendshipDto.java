package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.FriendshipStatus;
import edu.handong.csee.histudy.domain.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendshipDto {
    private List<Buddy> requests;

    public FriendshipDto(List<FriendshipRequest> requests) {
        this.requests = requests.stream()
                .map(Buddy::new)
                .toList();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Buddy {
        private String sid;
        private String name;
        private FriendshipStatus status;

        public Buddy(FriendshipRequest request) {
            User user = request.getUser();

            this.sid = user.getSid();
            this.name = user.getName();
            this.status = request.getStatus();
        }
    }
}
