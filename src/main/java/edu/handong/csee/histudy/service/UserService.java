package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.controller.form.BuddyForm;
import edu.handong.csee.histudy.domain.Friendship;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public List<Friendship> sendRequest(User sender, BuddyForm form) {
        userRepository
                .findAllById(form.getBuddies())
                .forEach(sender::add);

        return sender.getSentRequests();
    }

    public User token2User(String accessToken) {
        return userRepository
                .findUserByAccessToken(accessToken)
                .orElseThrow();
    }
}
