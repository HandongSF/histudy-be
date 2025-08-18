package edu.handong.csee.histudy.repository;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import java.util.List;
import java.util.Optional;

public interface AcademicTermRepository {
  Optional<AcademicTerm> findCurrentSemester();

  /**
 * Finds the academic term for a specific calendar year and term type.
 *
 * @param year the calendar year of the academic term (e.g., 2025)
 * @param sem  the term type (enum {@link TermType}) for that year
 * @return an {@link Optional} containing the matching {@link AcademicTerm}, or {@link Optional#empty()} if none exists
 */
Optional<AcademicTerm> findByYearAndTerm(int year, TermType sem);

  /**
 * Persists the given AcademicTerm and returns the managed instance.
 *
 * The returned instance reflects any changes applied by the persistence layer (for example, an assigned identifier or updated audit fields).
 *
 * @param entity the AcademicTerm to persist or update
 * @return the saved AcademicTerm instance
 */
AcademicTerm save(AcademicTerm entity);

  /**
 * Retrieves all AcademicTerm records ordered by year in descending order.
 *
 * @return a list of AcademicTerm entities sorted by year (newest first); may be empty if none exist
 */
List<AcademicTerm> findAllByYearDesc();

  /**
 * Finds an AcademicTerm by its identifier.
 *
 * @param id the primary key identifier of the AcademicTerm
 * @return an Optional containing the AcademicTerm if found, or an empty Optional if not present
 */
Optional<AcademicTerm> findById(Long id);

  /**
 * Clear the "current" flag on all AcademicTerm records.
 *
 * This performs a bulk update setting each AcademicTerm's current indicator to false so
 * that no term remains marked as current. Intended for use before marking a single
 * term as the current semester.
 */
void setAllCurrentToFalse();
}
