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

  /**
   * Sets whether this academic term is the current active term.
   *
   * This updates the entity's internal `isCurrent` flag; callers may pass
   * `true`, `false`, or `null` (to unset the flag).
   *
   * @param current true if this term should be marked current, false otherwise, or null to unset
   */
  public void setCurrent(Boolean current) {
    isCurrent = current;
  }
}
