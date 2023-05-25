package edu.handong.csee.histudy.impl;

import edu.handong.csee.histudy.domain.Friendship;
import edu.handong.csee.histudy.dto.FriendshipRequest;

public class SentFriendshipRequest extends FriendshipRequest {

    public SentFriendshipRequest(Friendship friendship) {
        super(friendship.getReceived(), friendship.getStatus());
    }
}
