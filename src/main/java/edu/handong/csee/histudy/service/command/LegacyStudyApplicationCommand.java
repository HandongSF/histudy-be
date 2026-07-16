package edu.handong.csee.histudy.service.command;

import java.util.List;

public record LegacyStudyApplicationCommand(
    List<String> friendStudentIds, List<Long> courseIds) {}
