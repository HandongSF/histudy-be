package edu.handong.csee.histudy.util;

import java.nio.charset.StandardCharsets;

public final class CourseCSVTemplate {

  private static final byte[] CONTENT = "\uFEFFtitle,code,prof\r\n".getBytes(StandardCharsets.UTF_8);

  private CourseCSVTemplate() {}

  public static byte[] content() {
    return CONTENT.clone();
  }
}
