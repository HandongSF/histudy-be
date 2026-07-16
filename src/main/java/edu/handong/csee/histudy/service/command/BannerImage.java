package edu.handong.csee.histudy.service.command;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public record BannerImage(String originalFilename, String contentType, byte[] content) {

  public BannerImage {
    content = content == null ? new byte[0] : content.clone();
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  public long size() {
    return content.length;
  }

  public boolean isEmpty() {
    return content.length == 0;
  }

  public InputStream inputStream() {
    return new ByteArrayInputStream(content);
  }
}
