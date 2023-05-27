package edu.handong.csee.histudy.controller.form;

import edu.handong.csee.histudy.domain.Report;
import edu.handong.csee.histudy.domain.Team;
import edu.handong.csee.histudy.domain.User;
import lombok.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportForm {

    private String title;
    private String content;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> participants;
    private List<String> images;

    public Report toEntity(Team team, List<Optional<User>> participants) {
        return Report.builder()
                .title(title)
                .content(content)
                .startTime(startTime)
                .endTime(endTime)
                .team(team)
                .participants(participants.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList())
                .images(images)
                .build();
    }
}
