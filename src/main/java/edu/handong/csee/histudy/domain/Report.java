package edu.handong.csee.histudy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private LocalTime startTime;
    private LocalTime endTime;
    private long totalMinutes;
    @Embedded
    private BaseTime baseTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<Participates> participants = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<Image> images = new ArrayList<>();

    @Builder
    public Report(String title,
                  String content,
                  LocalTime startTime,
                  LocalTime endTime,
                  Team team,
                  List<User> participants,
                  List<String> images) {
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;

        totalMinutes = Duration.between(startTime, endTime).toMinutes();
        writtenBy(team);
        addParticipants(participants);
        insert(images);
        team.increase(totalMinutes);
    }

    public void addParticipants(List<User> users) {
        List<Participates> participates = users.stream()
                .map(user -> new Participates(user, this))
                .toList();
        this.participants.addAll(participates);
    }

    public void writtenBy(Team team) {
        this.team = team;
        team.getReports().add(this);
    }

    public void insert(List<String> images) {
        if (images == null) {
            return;
        }
        List<Image> paths = images.stream()
                .map(img -> new Image(img, this))
                .toList();
        this.images.addAll(paths);
    }
}
