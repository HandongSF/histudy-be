package edu.handong.csee.histudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private String id;

    @Column(unique = true)
    private String sid;

    @Column(unique = true)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Team team;

    @OneToMany(mappedBy = "user")
    private List<Participates> participates = new ArrayList<>();

    @OneToMany(mappedBy = "sent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Friendship> sentRequests = new ArrayList<>();

    @OneToMany(mappedBy = "received", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Friendship> receivedRequests = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Choice> choices = new ArrayList<>();

    @Builder
    public User(String id, String sid, String email, String name, Role role) {
        this.id = id;
        this.sid = sid;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public void belongTo(Team team) {
        this.team = team;
        team.getUsers().add(this);
    }

    public void add(User user) {
        if (!sentRequests.isEmpty()) {
            this.sentRequests.clear();
            user.getReceivedRequests().clear();
        }
        Friendship friendship = new Friendship(this, user);
        this.sentRequests.add(friendship);
        user.getReceivedRequests().add(friendship);
    }

    public void add(List<User> users) {
        if (!sentRequests.isEmpty()) {
            this.sentRequests.clear();
            users.forEach(u -> u.receivedRequests.clear());
        }
        users
                .forEach(u -> {
                    Friendship friendship = new Friendship(this, u);
                    this.sentRequests.add(friendship);
                    u.receivedRequests.add(friendship);
                });
    }

    public void select(List<Course> courses) {
        if (!choices.isEmpty()) {
            this.choices.clear();
        }
        courses
                .forEach(c -> {
                    Choice choice = new Choice(this, c);
                    c.getChoices().add(choice);
                    this.choices.add(choice);
                });
    }

    public void removeTeam() {
        this.team = null;
    }
}
