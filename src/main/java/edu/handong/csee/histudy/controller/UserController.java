package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.controller.form.BuddyForm;
import edu.handong.csee.histudy.domain.Friendship;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.FriendshipDto;
import edu.handong.csee.histudy.dto.FriendshipRequest;
import edu.handong.csee.histudy.impl.ReceivedFriendshipRequest;
import edu.handong.csee.histudy.impl.SentFriendshipRequest;
import edu.handong.csee.histudy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/me/friends")
    public FriendshipDto sendFriendRequest(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
                                           @RequestBody BuddyForm form) {

        User sender = userService.token2User(accessToken);
        List<Friendship> friendships = userService.sendRequest(sender, form);
        List<FriendshipRequest> sentRequests = friendships.stream()
                .map(SentFriendshipRequest::new)
                .collect(Collectors.toList());

        return new FriendshipDto(sentRequests);
    }

    @PatchMapping("/me/friends/{sid}")
    public FriendshipDto acceptFriendRequest(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
                                             @PathVariable String sid) {
        User user = userService.token2User(accessToken);
        userService.acceptRequest(user, sid);

        List<FriendshipRequest> receivedRequests = user.getReceivedRequests()
                .stream()
                .map(ReceivedFriendshipRequest::new)
                .collect(Collectors.toList());

        return new FriendshipDto(receivedRequests);
    }

    @DeleteMapping("/me/friends/{sid}")
    public FriendshipDto declineFriendRequest(@RequestHeader(HttpHeaders.AUTHORIZATION) String accessToken,
                                              @PathVariable String sid) {
        User user = userService.token2User(accessToken);
        userService.declineRequest(user, sid);

        List<FriendshipRequest> receivedRequests = user.getReceivedRequests()
                .stream()
                .map(ReceivedFriendshipRequest::new)
                .collect(Collectors.toList());

        return new FriendshipDto(receivedRequests);
    }
}
