package edu.handong.csee.histudy.controller.form;

import io.jsonwebtoken.Claims;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportForm {

    private String title;

    private String content;

    private Long totalMinutes;

  /** Contains student ID(PK) */
  @Builder.Default private List<Long> participants = new ArrayList<>();

    /**
     * 이미지 URL
     *
     * @see edu.handong.csee.histudy.controller.TeamController#uploadImage(Optional, MultipartFile, Claims)
     */
    private List<String> images;

  /** Contains course ID */
  @Builder.Default private List<Long> courses = new ArrayList<>();
}
