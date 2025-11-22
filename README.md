# CICD Web Backend

CI/CD 파이프라인 관리를 위한 백엔드 서비스

## 환경 설정

### Spring Profiles

| Profile | 용도 | 파일 |
|---------|------|------|
| default | 로컬 개발 환경 | `application.yml` |
| prod | 운영 환경 (ECS) | `application-prod.yml` |

### 운영 환경 (prod) 설정

운영 환경에서는 AWS Secrets Manager를 통해 환경 변수가 주입됩니다.

#### 필수 환경 변수

| 환경 변수 | 설명 |
|----------|------|
| `DB_HOST` | 데이터베이스 호스트 |
| `DB_PORT` | 데이터베이스 포트 |
| `DB_NAME` | 데이터베이스 이름 |
| `DB_USERNAME` | 데이터베이스 사용자명 |
| `DB_PASSWORD` | 데이터베이스 비밀번호 |
| `GITHUB_TOKEN` | GitHub 액세스 토큰 |
| `ECR_REPOSITORY_URI` | ECR 저장소 URI |

#### 프로필 활성화

ECS Task Definition에서 환경 변수 설정:
```json
{
  "name": "SPRING_PROFILES_ACTIVE",
  "value": "prod"
}
```

또는 실행 시:
```bash
java -jar app.jar --spring.profiles.active=prod
```

## API 문서

- Swagger UI: `/swagger-ui.html`
- API Docs: `/api-docs`
