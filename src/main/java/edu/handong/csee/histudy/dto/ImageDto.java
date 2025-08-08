package edu.handong.csee.histudy.dto;

import edu.handong.csee.histudy.domain.ReportImage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageDto {

  public ImageDto(ReportImage entity) {
    this.id = entity.getReportImageId();
    this.url = entity.getPath();
  }

  private long id;

  private String url;
}
