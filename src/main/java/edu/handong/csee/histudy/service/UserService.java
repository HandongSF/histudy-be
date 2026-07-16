package edu.handong.csee.histudy.service;

import edu.handong.csee.histudy.domain.*;
import edu.handong.csee.histudy.dto.ApplyFormDto;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.*;
import edu.handong.csee.histudy.repository.*;
import edu.handong.csee.histudy.repository.StudyApplicantRepository;
import edu.handong.csee.histudy.service.command.LegacyStudyApplicationCommand;
import edu.handong.csee.histudy.service.command.SignUpCommand;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final StudyGroupRepository studyGroupRepository;
  private final AcademicTermRepository academicTermRepository;
  private final StudyApplicantRepository studyApplicantRepository;

  public List<User> search(Optional<String> keyword) {
    if (keyword.isEmpty() || keyword.get().isBlank()) {
      return userRepository.findAll(Sort.by(Sort.Direction.ASC, "sid"));
    }
    return userRepository.findUserByNameOrSidOrEmail(keyword.get());
  }

  public ApplyFormDto apply(LegacyStudyApplicationCommand command, String email) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);

    removeFormHistoryIfExists(user, currentTerm);

    List<User> partners =
        command.friendStudentIds().stream()
            .map(sid -> userRepository.findUserBySid(sid).orElseThrow(UserNotFoundException::new))
            .toList();

    List<Course> courses =
        command.courseIds().stream()
            .map(id -> courseRepository.findById(id).orElseThrow(CourseNotFoundException::new))
            .toList();

    StudyApplicant applicant = StudyApplicant.of(currentTerm, user, partners, courses);

    partners.forEach(
        partner ->
            studyApplicantRepository
                .findByUserAndTerm(partner, currentTerm)
                .ifPresent(
                    partnerApplication -> {
                      partnerApplication.changeStatusIfReceivedBy(
                          user, StudyPartnerRequest::accept);

                      applicant.changeStatusIfReceivedBy(partner, StudyPartnerRequest::accept);
                    }));
    studyApplicantRepository.save(applicant);
    return new ApplyFormDto(applicant);
  }

  public StudyApplicant apply(List<Long> friendsIds, List<Long> courseIds, String email) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);

    removeFormHistoryIfExists(user, currentTerm);

    List<User> partners =
        friendsIds.stream()
            .map(id -> userRepository.findById(id).orElseThrow(UserNotFoundException::new))
            .toList();

    List<Course> courses =
        courseIds.stream()
            .map(id -> courseRepository.findById(id).orElseThrow(CourseNotFoundException::new))
            .toList();

    StudyApplicant applicant = StudyApplicant.of(currentTerm, user, partners, courses);

    partners.forEach(
        partner ->
            studyApplicantRepository
                .findByUserAndTerm(partner, currentTerm)
                .ifPresent(
                    partnerApplication -> {
                      partnerApplication.changeStatusIfReceivedBy(
                          user, StudyPartnerRequest::accept);

                      applicant.changeStatusIfReceivedBy(partner, StudyPartnerRequest::accept);
                    }));
    return studyApplicantRepository.save(applicant);
  }

  private void removeFormHistoryIfExists(User user, AcademicTerm currentTerm) {
    studyApplicantRepository
        .findByUserAndTerm(user, currentTerm)
        .ifPresent(
            applicant -> {
              if (applicant.hasStudyGroup()) {
                throw new IllegalStateException("그룹이 이미 배정된 신청서는 삭제할 수 없습니다.");
              }
              List<User> receivers = applicant.getRequestedUsers();

              for (User receiver : receivers) {
                studyApplicantRepository
                    .findByUserAndTerm(receiver, currentTerm)
                    .ifPresent(
                        partnerApplication ->
                            partnerApplication.changeStatusIfReceivedBy(
                                user, StudyPartnerRequest::unfriend));
              }
              studyApplicantRepository.delete(applicant);
            });
  }

  public void signUp(SignUpCommand command) {
    userRepository
        .findUserBySub(command.sub())
        .ifPresentOrElse(
            __ -> {
              throw new UserAlreadyExistsException();
            },
            () ->
                userRepository.save(
                    User.builder()
                        .sid(command.sid())
                        .email(command.email())
                        .name(command.name())
                        .sub(command.sub())
                        .role(Role.USER)
                        .build()));
  }

  public User getUser(Optional<String> subOr) {
    String sub = subOr.orElseThrow(MissingSubException::new);
    return userRepository.findUserBySub(sub).orElseThrow(UserNotFoundException::new);
  }

  public List<UserDto.UserInfo> getAppliedUsers() {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyApplicant> allForms = studyApplicantRepository.findAllByTerm(currentTerm);
    return getInfoFromUser(allForms);
  }

  public List<UserDto.UserInfo> getInfoFromUser(List<StudyApplicant> forms) {
    return forms.stream().map(form -> new UserDto.UserInfo(form.getUser(), form)).toList();
  }

  public Optional<StudyApplicant> getUserInfo(String email) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    return studyApplicantRepository.findByUserAndTerm(user, currentTerm);
  }

  public UserDto.UserMe getUserMe(Optional<String> emailOr) {
    String email = emailOr.orElseThrow(MissingEmailException::new);
    User user = userRepository.findUserByEmail(email).orElseThrow(UserNotFoundException::new);
    return new UserDto.UserMe(user);
  }

  public List<UserDto.UserInfo> getUnmatchedUsers() {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);

    List<User> allUsers = userRepository.findAll(Sort.by(Sort.Direction.ASC, "sid"));
    List<User> assignedApplicantForms =
        studyApplicantRepository.findAssignedApplicants(currentTerm).stream()
            .map(StudyApplicant::getUser)
            .toList();

    return allUsers.stream()
        .filter(user -> !assignedApplicantForms.contains(user))
        .map(
            user -> {
              Optional<StudyApplicant> formOr =
                  studyApplicantRepository.findByUserAndTerm(user, currentTerm);

              return formOr
                  .map(form -> new UserDto.UserInfo(user, form))
                  .orElse(new UserDto.UserInfo(user));
            })
        .toList();
  }

  public void deleteUserForm(String sid) {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    User user = userRepository.findUserBySid(sid).orElseThrow(UserNotFoundException::new);
    removeFormHistoryIfExists(user, currentTerm);
  }

  public void editUser(UserDto.UserEdit form) {
    User user = userRepository.findById(form.getId()).orElseThrow(UserNotFoundException::new);
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    Optional<StudyApplicant> applicantOr =
        studyApplicantRepository.findByUserAndTerm(user, currentTerm);

    user.edit(form.getSid(), form.getName());

    Optional.ofNullable(form.getTeam())
        .ifPresentOrElse(
            tag -> {
              StudyApplicant applicant = applicantOr.orElseThrow(NoStudyApplicationFound::new);
              studyGroupRepository
                  .findByTagAndAcademicTerm(tag, currentTerm)
                  .ifPresentOrElse(
                      existingGroup -> existingGroup.addMember(applicant),
                      () -> {
                        StudyGroup newGroup = StudyGroup.of(tag, currentTerm, List.of(applicant));
                        studyGroupRepository.save(newGroup);
                      });
            },
            () -> applicantOr.ifPresent(StudyApplicant::leaveStudyGroup));

    studyGroupRepository
        .findAllEmptyByAcademicTerm(currentTerm)
        .forEach(
            group ->
                /*
                 TODO: Need to check there are associated reports
                 Currently deleting a group with no members but with reports
                 will cause FK constraint violation
                */
                studyGroupRepository.deleteById(group.getStudyGroupId()));
  }

  public List<UserDto.UserInfo> getAppliedWithoutGroup() {
    AcademicTerm currentTerm =
        academicTermRepository.findCurrentSemester().orElseThrow(NoCurrentTermFoundException::new);
    List<StudyApplicant> unassignedApplicants =
        studyApplicantRepository.findUnassignedApplicants(currentTerm);
    return getInfoFromUser(unassignedApplicants);
  }
}
