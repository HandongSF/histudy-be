# HIStudy (API Server)

HIStudy는 한동대학교 온라인 스터디그룹 관리 서비스입니다. 학생들은 희망하는 과목의 우선순위에 따라 스터디그룹에 배정되며,
배정된 스터디그룹에서 스터디 활동을 기록하고 인증할 수 있습니다. 2023년 1학기 소프트웨어공학 수업 팀 프로젝트로 시작하여,
2023년 2학기부터 전산전자공학부 학생들을 대상으로 서비스를 제공하고 있습니다.

## Table of Contents

- [Getting Started](#getting-started)
- [Contributing](#contributing)
- [License](#license)

## Getting Started

- 자바 17 버전 이상이 설치되어 있어야 합니다.
- 이 저장소를 clone 한 뒤 IDE에서 `HistudyApplication` 클래스를 실행하거나, 터미널에서 다음 명령어를 실행하여 서버를 실행할 수 있습니다.

    ```shell
    $ ./gradlew bootRun
    ```

- 서버의 Base URL은 `http://localhost:8080` 입니다.
- API 문서를 확인하려면 서버를 실행한 뒤 http://localhost:8080/swagger-ui/index.html 로 접속하세요.
- H2DB (기본값)를 사용하는 경우 http://localhost:8080/h2-console 로 접속하여 데이터베이스 콘솔 화면에 접근 가능합니다.
- **`application.yml` 파일의 변경사항을 커밋하지 마세요. `application-local.yml` 파일을 만들어서 개인 설정을 저장하는 것을 권장합니다.**
  - 예를 들어, DB 접속 정보를 MySQL로 바꾸려면 `application-local.yml` 파일을 만들고 다음과 같이 작성하세요.

    ```yaml
      # src/main/resources/application-local.yml
      spring:
          datasource: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/testdb
          username: root
          password: notverysecure
    ```
  - 이후 `application.yml` 파일에서 다음과 같이 `application-local.yml` 파일을 참조하도록 설정하세요.

    ```yaml
      # src/main/resources/application.yml
      spring:
          profiles:
              include: local
    ```

## Contributing

- 새로운 기능이나 개선사항을 제안하려면 Github Issue를 작성해주세요.
- 또는 이 저장소를 fork 하고 Pull Request를 보내서 기여할 수 있습니다. 단, 작업규모가 크다고 판단되는 경우에는 먼저 Issue를 작성해주세요.

## License

HIStudy는 Apache License 2.0을 따릅니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.
