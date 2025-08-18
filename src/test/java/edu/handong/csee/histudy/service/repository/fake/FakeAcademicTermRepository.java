package edu.handong.csee.histudy.service.repository.fake;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import edu.handong.csee.histudy.repository.AcademicTermRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class FakeAcademicTermRepository implements AcademicTermRepository {

  private final List<AcademicTerm> store = new ArrayList<>();
  private Long sequence = 1L;

  @Override
  public Optional<AcademicTerm> findCurrentSemester() {
    return store.stream().filter(AcademicTerm::getIsCurrent).findFirst();
  }

  @Override
  public Optional<AcademicTerm> findByYearAndTerm(int year, TermType sem) {
    return store.stream()
        .filter(cal -> cal.getAcademicYear().equals(year) && cal.getSemester().equals(sem))
        .findFirst();
  }

  /**
   * Saves the given AcademicTerm into the in-memory store.
   *
   * If the provided entity has a null academicTermId, a new sequential id is assigned (via ReflectionTestUtils).
   * If an entry with the same academicTermId already exists in the store, it is removed (update semantics) before
   * adding the provided entity.
   *
   * @param entity the AcademicTerm to save or update
   * @return the saved AcademicTerm (the same instance passed in, with an id assigned if it was previously null)
   */
  @Override
  public AcademicTerm save(AcademicTerm entity) {
    if (entity.getAcademicTermId() == null) {
      ReflectionTestUtils.setField(entity, "academicTermId", sequence++);
    }

    // Remove existing entity if updating
    store.removeIf(existing -> existing.getAcademicTermId().equals(entity.getAcademicTermId()));
    store.add(entity);
    return entity;
  }

  /**
   * Returns all stored academic terms sorted by academic year in descending order.
   *
   * The returned list is a copy and modifications to it do not affect the repository's internal store.
   *
   * @return a new List of AcademicTerm ordered from newest to oldest academic year
   */
  @Override
  public List<AcademicTerm> findAllByYearDesc() {
    List<AcademicTerm> result = new ArrayList<>(store);
    result.sort(Comparator.comparing(AcademicTerm::getAcademicYear).reversed());
    return result;
  }

  /**
   * Returns a copy of all stored academic terms.
   *
   * @return a new List containing every AcademicTerm currently in the repository (modifications to the returned list do not affect the repository)
   */
  public List<AcademicTerm> findAll() {
    return new ArrayList<>(store);
  }

  /**
   * Finds an academic term by its unique database identifier.
   *
   * @param id the academicTermId to look up
   * @return an Optional containing the matching AcademicTerm if found, otherwise Optional.empty()
   */
  @Override
  public Optional<AcademicTerm> findById(Long id) {
    return store.stream().filter(term -> term.getAcademicTermId().equals(id)).findFirst();
  }

  /**
   * Sets every stored AcademicTerm's `isCurrent` flag to false.
   *
   * This mutates the in-memory store in-place by updating each AcademicTerm instance's
   * private `isCurrent` field (via reflection). Useful for resetting repository state
   * in tests.
   */
  @Override
  public void setAllCurrentToFalse() {
    for (AcademicTerm term : store) {
      ReflectionTestUtils.setField(term, "isCurrent", false);
    }
  }

  /**
   * Clears the in-memory repository and resets the ID sequence to 1.
   *
   * Removes all stored AcademicTerm entries and resets the internal sequence counter
   * so subsequent saved entities will be assigned IDs beginning at 1.
   */
  public void clear() {
    store.clear();
    sequence = 1L;
  }
}
