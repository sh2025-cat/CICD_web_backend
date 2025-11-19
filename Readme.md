# CI/CD 자동화 백엔드 서버

## 1. 개요

본 프로젝트는 GitHub Actions 및 AWS ECS와 연동하여 CI/CD 파이프라인을 관리하고 배포를 자동화하는 백엔드 서버입니다. GitHub의 워크플로우를 모니터링하고 제어하며, 빌드된 Docker 이미지를 AWS ECS에 배포하는 기능을 API로 제공합니다.

## 2. 주요 기능

### 2.1. GitHub 파이프라인 관리

- **파이프라인 조회**: 특정 GitHub 리포지토리의 워크플로우 실행 목록 및 상태를 조회합니다.
- **파이프라인 상세 조회**: 특정 워크플로우 실행(Run)에 포함된 Job 목록 및 상세 정보를 조회합니다.
- **로그 조회**: 특정 Job의 실행 로그를 실시간으로 확인합니다.
- **파이프라인 제어**:
    - 특정 워크플로우를 수동으로 실행합니다. (`Dispatch`)
    - 실행 중인 워크플로우를 중지(Cancel)하거나 재실행(Rerun)합니다.

### 2.2. AWS ECS 배포

- **ECS 서비스 배포**: 지정된 ECS 클러스터의 서비스에 새로운 Docker 이미지를 배포합니다.
- CI 단계에서 ECR에 새로운 이미지가 푸시된 후, 이 API를 호출하여 ECS 서비스가 새 이미지를 사용하도록 업데이트할 수 있습니다.

## 3. API 엔드포인트

API 명세는 Swagger UI를 통해 확인할 수 있습니다.

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

| 기능                 | Method | URI                               | 설명                                     |
| -------------------- | ------ | --------------------------------- | ---------------------------------------- |
| **Pipeline**         |        |                                   |                                          |
| 파이프라인 목록 조회 | `GET`  | `/api/pipelines`                  | GitHub 리포지토리의 워크플로우 실행 목록 조회 |
| Job 목록 조회        | `GET`  | `/api/pipelines/{runId}/jobs`     | 특정 파이프라인의 Job 목록 조회          |
| Job 상세 조회        | `GET`  | `/api/pipelines/job/{jobId}`      | 특정 Job의 상세 정보 조회                |
| Job 로그 조회        | `GET`  | `/api/pipelines/{jobId}/jobs/logs`| 특정 Job의 실행 로그 조회                |
| 파이프라인 실행      | `POST` | `/api/pipelines/{workflowId}/start` | 특정 워크플로우 실행                     |
| 파이프라인 중지      | `POST` | `/api/pipelines/{runId}/cancel`   | 실행 중인 워크플로우 중지                |
| 파이프라인 재실행    | `POST` | `/api/pipelines/{runId}/rerun`    | 완료된 워크플로우 재실행                 |
| **Deployment**       |        |                                   |                                          |
| ECS 서비스 배포      | `POST` | `/api/deployments/ecs`            | AWS ECS 서비스에 새로운 이미지 배포      |

## 4. 기술 스택

- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build Tool**: Gradle
- **API-Docs**: SpringDoc (Swagger UI)
- **Libraries**:
    - Spring Web
    - Lombok
    - RestClient (for GitHub API)
    - AWS SDK for Java v2 (for ECS)

## 5. 시작 가이드

### 5.1. 환경 설정

`application.yml` 파일에 아래와 같이 GitHub 및 AWS 관련 설정을 추가해야 합니다.

```yaml
# GitHub API Access Token
github:
  access-token: "your_github_personal_access_token"

# AWS ECR Repository URI
aws:
  ecr:
    repository-uri: "your_aws_ecr_repository_uri"

# AWS Credentials
# AWS Access Key와 Secret Key는 환경 변수 또는 ~/.aws/credentials 파일을 통해 설정하는 것을 권장합니다.
# AWS_ACCESS_KEY_ID=...
# AWS_SECRET_ACCESS_KEY=...
```

### 5.2. 빌드 및 실행

1.  **Gradle 빌드**
    ```bash
    ./gradlew build
    ```

2.  **애플리케이션 실행**
    ```bash
    java -jar build/libs/cicd_backend-0.0.1-SNAPSHOT.jar
    ```

서버가 실행되면 `http://localhost:8080` 에서 접속할 수 있습니다.
