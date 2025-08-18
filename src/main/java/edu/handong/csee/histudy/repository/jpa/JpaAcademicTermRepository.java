package edu.handong.csee.histudy.repository.jpa;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAcademicTermRepository extends JpaRepository<AcademicTerm, Long> {

  @Query("select a from AcademicTerm a where a.isCurrent = true")
  Optional<AcademicTerm> findCurrentSemester();

  /**
   * Finds the AcademicTerm for a given academic year and semester.
   *
   * @param year the academic year (e.g., 2024)
   * @param sem the semester term type
   * @return an Optional containing the matching AcademicTerm, or empty if no match is found
   */
  @Query("SELECT at FROM AcademicTerm at WHERE at.academicYear = :year AND at.semester = :sem")
  Optional<AcademicTerm> findByYearAndTerm(@Param("year") int year, @Param("sem") TermType sem);

  /**
   * Retrieves all AcademicTerm entities ordered by academic year in descending order.
   *
   * @return a list of AcademicTerm objects sorted by academicYear descending (most recent first)
   */
  @Query("SELECT at FROM AcademicTerm at ORDER BY at.academicYear DESC")
  List<AcademicTerm> findAllByYearDesc();

  /**
   * Marks every AcademicTerm as not current.
   *
   * Performs a bulk JPQL update setting `isCurrent` to false for all AcademicTerm records.
   */
  @Modifying
  @Query("UPDATE AcademicTerm at SET at.isCurrent = false")
  void setAllCurrentToFalse();
}
