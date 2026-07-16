package edu.handong.csee.histudy.service.command;

import java.util.List;

public record ReportCommand(
    String title,
    String content,
    Long totalMinutes,
    List<Long> participantIds,
    List<String> imageUrls,
    List<Long> courseIds) {}
