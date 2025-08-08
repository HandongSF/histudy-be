package edu.handong.csee.histudy.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademicTerm extends BaseTime {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long academicTermId;

  private Integer academicYear;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TermType semester = TermType.NONE;

  @Builder.Default private Boolean isCurrent = false;
}
