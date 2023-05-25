package edu.handong.csee.histudy.impl;

import edu.handong.csee.histudy.domain.Friendship;
import edu.handong.csee.histudy.dto.FriendshipRequest;

public class ReceivedFriendshipRequest extends FriendshipRequest {

    public ReceivedFriendshipRequest(Friendship friendship) {
        super(friendship.getSent(), friendship.getStatus());
    }
}
