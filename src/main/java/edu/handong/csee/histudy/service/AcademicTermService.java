package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.controller.form.AcademicTermForm;
import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.dto.AcademicTermDto;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicTermService {

  private final AcademicTermRepository academicTermRepository;

  /**
   * Creates and persists a new AcademicTerm using values from the given form.
   *
   * The created term will have its "current" flag set to false and is saved via the repository.
   *
   * @param form an AcademicTermForm containing the academic year and semester to persist
   */
  @Transactional
  public void createAcademicTerm(AcademicTermForm form) {
    AcademicTerm academicTerm =
        AcademicTerm.builder()
            .academicYear(form.getYear())
            .semester(form.getTerm())
            .isCurrent(false)
            .build();

    academicTermRepository.save(academicTerm);
  }

  /**
   * Retrieves all academic terms and returns them as an AcademicTermDto.
   *
   * <p>Fetches AcademicTerm entities ordered by year descending, maps each to an
   * AcademicTermForm (year and semester), and wraps the resulting list in an
   * AcademicTermDto.
   *
   * @return an AcademicTermDto containing the list of AcademicTermForm objects
   */
  @Transactional(readOnly = true)
  public AcademicTermDto getAllAcademicTerms() {
    List<AcademicTerm> terms = academicTermRepository.findAllByYearDesc();

    List<AcademicTermForm> termForms =
        terms.stream()
            .map(term -> new AcademicTermForm(term.getAcademicYear(), term.getSemester()))
            .toList();

    return new AcademicTermDto(termForms);
  }

  /**
   * Marks the academic term identified by the given id as the current term.
   *
   * Clears the current flag on all terms and sets the specified term's current flag to true.
   * The change is executed within a transaction and will be persisted on commit.
   *
   * @param id the id of the academic term to mark as current
   * @throws IllegalArgumentException if no academic term exists with the given id
   */
  @Transactional
  public void setCurrentTerm(Long id) {
    AcademicTerm targetTerm =
        academicTermRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Academic term not found"));
    academicTermRepository.setAllCurrentToFalse();
    targetTerm.setCurrent(true);
  }
}
