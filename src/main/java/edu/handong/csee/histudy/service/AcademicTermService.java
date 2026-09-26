package edu.handong.csee.histudy.service;

import static edu.handong.csee.histudy.dto.AcademicTermDto.*;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.dto.AcademicTermDto;
import edu.handong.csee.histudy.exception.AcademicTermNotFoundException;
import edu.handong.csee.histudy.exception.DuplicateAcademicTermException;
import edu.handong.csee.histudy.exception.MissingParameterException;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import edu.handong.csee.histudy.observability.audit.*;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicTermService {

  private static final String MESSAGE_MISSING_ACADEMIC_TERM =
      "연도와 학기는 필수 입력값입니다.";

  private final AcademicTermRepository academicTermRepository;
  private final ApplicationEventPublisher publisher;

  @Transactional
  public void createAcademicTerm(Integer year, TermType semester, AuditContext context) {
    Objects.requireNonNull(context, "context must not be null");
    if (year == null || semester == null) {
      throw new MissingParameterException(MESSAGE_MISSING_ACADEMIC_TERM);
    }

    academicTermRepository
        .findByYearAndTerm(year, semester)
        .ifPresent(
            existing -> {
              throw new DuplicateAcademicTermException(year, semester);
            });

    AcademicTerm academicTerm =
        AcademicTerm.builder()
            .academicYear(year)
            .semester(semester)
            .isCurrent(false)
            .build();

    AcademicTerm saved = academicTermRepository.save(academicTerm);
    publisher.publishEvent(new AcademicTermCreatedEvent(context, saved.getAcademicTermId()));
  }

  @Transactional(readOnly = true)
  public AcademicTermDto getAllAcademicTerms() {
    List<AcademicTerm> terms = academicTermRepository.findAllByYearDescAndSemesterDesc();
    List<AcademicTermItem> items =
        terms.stream()
            .map(
                term ->
                    new AcademicTermItem(
                        term.getAcademicTermId(),
                        term.getAcademicYear(),
                        term.getSemester(),
                        term.getIsCurrent()))
            .toList();

    return new AcademicTermDto(items);
  }

  @Transactional
  public void setCurrentTerm(Long id, AuditContext context) {
    Objects.requireNonNull(context, "context must not be null");
    AcademicTerm targetTerm =
        academicTermRepository.findById(id).orElseThrow(AcademicTermNotFoundException::new);

    if (targetTerm.getIsCurrent()) {
      publisher.publishEvent(new CurrentTermChangedEvent(context, id, id, false));
      return;
    }
    AcademicTerm previous = academicTermRepository.findCurrentSemester().orElse(null);
    Long previousId = previous == null ? null : previous.getAcademicTermId();
    if (previous != null) {
      previous.setCurrent(false);
    }
    targetTerm.setCurrent(true);
    publisher.publishEvent(new CurrentTermChangedEvent(context, previousId, id, true));
  }
}
