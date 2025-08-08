package edu.handong.csee.histudy.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityMetricsDto {

  private final long studyMembers;

  private final long studyGroups;

  private final long studyHours;

  private final long reports;
}
