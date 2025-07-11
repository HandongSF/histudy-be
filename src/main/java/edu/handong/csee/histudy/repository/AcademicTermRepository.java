package edu.handong.csee.histudy.repository;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.TermType;
import java.util.Optional;

public interface AcademicTermRepository {
  Optional<AcademicTerm> findCurrentSemester();

  Optional<AcademicTerm> findByYearAndTerm(int year, TermType sem);

  AcademicTerm save(AcademicTerm entity);
}
