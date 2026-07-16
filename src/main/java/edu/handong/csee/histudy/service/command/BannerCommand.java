package edu.handong.csee.histudy.service.command;

public record BannerCommand(
    String label, String redirectUrl, Boolean active, BannerImage image) {}
