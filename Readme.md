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


** 기능 정의
    - 랜딩 페이지
        레포지토리 별 가장 최근 배포 정보 출력
        -> GitHub Actions에서 배포가 끝날 때마다 웹훅을 발생시키고 백엔드에서 이를 받아 배포 히스토리를 모두 DB에 저장
        -> 랜딩 페이지 조회 API(/api/repos)
            결과 형태
            data: [
                {
                    "name": "cicd_frontend",
                    "tag": "v1.1.0 | COMMIT_HASH"
                    "commit_msg": "커밋 메세지",
                    "deployed_at": "2025-11-20 11:11:11.000"
                },
                {
                    "name": "cicd_backend",
                    "tag": "v1.1.0 | COMMIT_HASH"
                    "commit_msg": "커밋 메세지",
                    "deployed_at": "2025-11-20 11:11:11.000"
                }
            ]
    - 각 Step 별 로그 출력
        - 프론트엔드 에서 JobID를 Request로 받아 GitHub Rest API를 이용해 로그를 SSE로 전달
        - 
    - 각 Step 별 성공 실패 및 결과 출력
        - GitHub Actions job이 끝날 때마다 job_id를 백엔드에 호출(작업이 끝났음을 알림)
        - 백엔드에서는 job_id를 이용해 작업의 결과를 프론트엔드에 전달 및 DB 저장(이 떄도 SSE를 이용해야하는가?)
    - GitHub Action 동작 시 runId를 포함시켜 백엔드 서버로 웹훅 발송을 통해 DB에 데이터 저장
        -> 이후 각 Job이 끝날 때마다 runId 와 jobId를 페이로드에 포함시켜 백엔드 서버로 웹훅 발송
        -> 백엔드에서는 jobId를 이용해 작업의 결과를 조회해 DB에 저장
    - ECR 데이터 조회
        - 배포가 완료된 상태의 이미지들을 조회해 각 태그별 데이터 
    - ECS 배포 실행
        - 배포 버튼을 눌렀을 때 repo 및 cluster 정보를 가지고, ECS에 새로운 배포 실행
    - ECS 롤백 실행
        - 배포가 완료된 상태에서 롤백 버튼 클릭 시 가장 최근에 배포된 버전을 확인하고 해당 버전으로 롤백
    - 스케줄러를 이용해 특정 시간마다 CloudWatch에서 ECS 데이터를 조회해온다.
        - 조회한 데이터는 DB에 저장해두고 프론트엔드에서 보일 수 있도록 API 추가


## tb_project에서 프로젝트의 클러스터 이름과, 서비스 이름을 가지고 있고 이 값을 프론트엔드에 뿌려준다.
    - 동기화 버튼을 만들어 동기화시킬 수 있도록 한다.