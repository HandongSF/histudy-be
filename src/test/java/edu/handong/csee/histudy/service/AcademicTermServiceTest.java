package edu.handong.csee.histudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.dto.AcademicTermDto;
import edu.handong.csee.histudy.exception.AcademicTermNotFoundException;
import edu.handong.csee.histudy.exception.DuplicateAcademicTermException;
import edu.handong.csee.histudy.service.repository.fake.FakeAcademicTermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcademicTermServiceTest {

  private final AcademicTerm spring2024Term =
      AcademicTerm.builder().academicYear(2024).semester(TermType.SPRING).isCurrent(false).build();
  private final AcademicTerm spring2025CurrentTerm =
      AcademicTerm.builder().academicYear(2025).semester(TermType.SPRING).isCurrent(true).build();
  private final AcademicTerm fall2025Term =
      AcademicTerm.builder().academicYear(2025).semester(TermType.FALL).isCurrent(false).build();

  private FakeAcademicTermRepository academicTermRepository;
  private AcademicTermService academicTermService;

  @BeforeEach
  void setUp() {
    academicTermRepository = new FakeAcademicTermRepository();
    academicTermService = new AcademicTermService(academicTermRepository);
  }

  @Test
  void 전체_학기_목록을_조회하면_학기와_현재학기정보를_반환한다() {
    // Given
    academicTermRepository.save(spring2024Term);
    academicTermRepository.save(
        AcademicTerm.builder().academicYear(2025).semester(TermType.FALL).isCurrent(true).build());

    // When
    AcademicTermDto result = academicTermService.getAllAcademicTerms();

    // Then
    assertThat(result.academicTerms()).hasSize(2);
    assertThat(result.academicTerms().get(0).year()).isEqualTo(2025);
    assertThat(result.academicTerms().get(0).semester()).isEqualTo(TermType.FALL);
    assertThat(result.academicTerms().get(0).isCurrent()).isTrue();
    assertThat(result.academicTerms().get(1).year()).isEqualTo(2024);
  }

  @Test
  void 새로운_학기를_추가하면_기존_현재학기는_유지된다() {
    // Given
    AcademicTerm existingCurrent = academicTermRepository.save(spring2025CurrentTerm);

    // When
    academicTermService.createAcademicTerm(2025, TermType.FALL);

    // Then
    assertThat(academicTermRepository.findAll()).hasSize(2);
    assertThat(academicTermRepository.findCurrentSemester()).contains(existingCurrent);
    assertThat(
            academicTermRepository
                .findByYearAndTerm(2025, TermType.FALL)
                .orElseThrow()
                .getIsCurrent())
        .isFalse();
  }

  @Test
  void 중복된_학기를_추가하면_예외가_발생한다() {
    // Given
    academicTermRepository.save(fall2025Term);

    // When Then
    assertThatThrownBy(() -> academicTermService.createAcademicTerm(2025, TermType.FALL))
        .isInstanceOf(DuplicateAcademicTermException.class);
  }

  @Test
  void 현재_학기를_변경하면_요청한_학기만_현재_학기로_설정된다() {
    // Given
    AcademicTerm spring = academicTermRepository.save(spring2025CurrentTerm);
    AcademicTerm fall = academicTermRepository.save(fall2025Term);

    // When
    academicTermService.setCurrentTerm(fall.getAcademicTermId());

    // Then
    assertThat(spring.getIsCurrent()).isFalse();
    assertThat(fall.getIsCurrent()).isTrue();
    assertThat(academicTermRepository.findCurrentSemester()).contains(fall);
  }

  @Test
  void 존재하지_않는_학기로_현재_학기를_변경하면_예외가_발생한다() {
    // Given
    Long missingId = 999L;

    // When Then
    assertThatThrownBy(() -> academicTermService.setCurrentTerm(missingId))
        .isInstanceOf(AcademicTermNotFoundException.class);
  }
}
