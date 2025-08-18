package edu.handong.csee.histudy.repository.impl;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import edu.handong.csee.histudy.repository.jpa.JpaAcademicTermRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AcademicTermRepositoryImpl implements AcademicTermRepository {
  private final JpaAcademicTermRepository repository;

  @Override
  public Optional<AcademicTerm> findCurrentSemester() {
    return repository.findCurrentSemester();
  }

  @Override
  public Optional<AcademicTerm> findByYearAndTerm(int year, TermType sem) {
    return repository.findByYearAndTerm(year, sem);
  }

  /**
   * Persists the given AcademicTerm and returns the managed instance.
   *
   * <p>This delegates to the underlying JPA repository; the returned object is the saved (inserted or updated)
   * entity and may contain generated values (e.g. identifier).
   *
   * @param academicTerm the AcademicTerm to persist
   * @return the persisted AcademicTerm instance
   */
  @Override
  public AcademicTerm save(AcademicTerm academicTerm) {
    return repository.save(academicTerm);
  }

  /**
   * Retrieves all academic terms ordered by year in descending order.
   *
   * @return a list of AcademicTerm instances sorted by year from newest to oldest
   */
  @Override
  public List<AcademicTerm> findAllByYearDesc() {
    return repository.findAllByYearDesc();
  }

  /**
   * Finds an AcademicTerm by its primary key.
   *
   * @param id the primary key of the AcademicTerm to retrieve
   * @return an Optional containing the AcademicTerm if found, otherwise an empty Optional
   */
  @Override
  public Optional<AcademicTerm> findById(Long id) {
    return repository.findById(id);
  }

  /**
   * Marks every AcademicTerm as not current by setting their `current` flag to false.
   *
   * This operation is executed within a transaction so all changes are committed or rolled back atomically.
   */
  @Override
  @Transactional
  public void setAllCurrentToFalse() {
    repository.setAllCurrentToFalse();
  }
}
