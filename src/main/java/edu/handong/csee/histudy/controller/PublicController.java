package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.dto.ActivityMetricsDto;
import edu.handong.csee.histudy.dto.ActivityTerm;
import edu.handong.csee.histudy.dto.TeamRankDto;
import edu.handong.csee.histudy.service.ActivityMetricsService;
import edu.handong.csee.histudy.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {

  private final TeamService teamService;
  private final ActivityMetricsService activityMetricsService;

  @GetMapping("/teams")
  public TeamRankDto getTeams() {
    return teamService.getAllTeams();
  }

  @GetMapping("/activity")
  public ActivityMetricsDto getActivityMetrics(@RequestParam(defaultValue = "all") String term) {
    ActivityTerm activityTerm = ActivityTerm.fromString(term);
    return activityMetricsService.getActivityMetrics(activityTerm);
  }
}
