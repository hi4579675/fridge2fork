# 🍴 fridge2fork

> **재료를 넣으면, 레시피가 나온다.**
> 1인 가구를 위한 스프링 부트 기반 냉장고 레시피 추천 서비스

---

## 📌 프로젝트 소개

**fridge2fork**는 혼자 사는 사람들을 위한 레시피 추천 서비스입니다.
냉장고에 있는 재료를 등록하면, 지금 당장 만들 수 있는 레시피를 추천해드려요.

- 🥦 보유 재료 기반 레시피 매칭
- 🔥 공공 API 연동 칼로리 · 영양정보 자동 계산
- 📊 Redis 기반 실시간 인기 레시피 랭킹
- 📬 Kafka 기반 유통기한 알림

---

## 🚀 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA + QueryDSL |
| Database | MySQL 8 |
| Cache | Redis |
| Message Queue | Kafka |
| 인증 | Spring Security + JWT |
| API 문서 | Swagger (SpringDoc) |
| 외부 API | 식품안전처 공공데이터 API |
| 빌드 | Gradle |
| 인프라 | AWS EC2 + RDS + S3 |
| CI/CD | GitHub Actions + Docker |

---

## ✨ 주요 기능

### ✅ 핵심 기능
- 이메일 회원가입 / 로그인 (JWT Access + Refresh Token)
- 냉장고 재료 등록 · 수정 · 삭제
- **보유 재료 기반 레시피 추천** (재료 매칭률 순 정렬)
- 레시피 CRUD — 제목, 조리 순서, 재료, 썸네일
- 공공 API 연동 칼로리 · 영양정보 자동 계산

### ⭐ 차별화 기능
- 레시피 댓글 · 별점
- 좋아요 토글
- **실시간 인기 레시피 랭킹** (Redis Sorted Set)
- 오늘 먹은 것 기록 + 일일 칼로리 합산

### 💡 추가 기능
- **유통기한 알림** (Kafka 이벤트 기반)
- 부족한 재료 알려주기 — "이것만 사면 만들 수 있어요"
- 1주일 식단 추천

---

## 🗄 ERD

> 9개 테이블 · `USERS`와 `RECIPES` 중심 설계

```
USERS
 ├── USER_INGREDIENTS  (냉장고)
 ├── RECIPES
 │    ├── RECIPE_INGREDIENTS
 │    ├── RECIPE_STEPS
 │    ├── COMMENTS
 │    ├── LIKES
 │    └── MEAL_LOGS
 └── (INGREDIENTS 재료 마스터)
```

---

## 🏗 아키텍처

```
GitHub Actions
  └── Docker 빌드 → Docker Hub Push → EC2 배포

클라이언트
  └── ALB / Nginx
        └── EC2 (Spring Boot)
              ├── RDS MySQL   (메인 데이터)
              ├── Redis       (랭킹, 세션, JWT 블랙리스트)
              └── Kafka       (유통기한 이벤트 브로커)
```

---

## 📁 패키지 구조

```
fridge2fork
├── domain
│   ├── auth          # JWT, 로그인, 회원가입
│   ├── user          # 회원 정보
│   ├── ingredient    # 재료 마스터
│   ├── fridge        # 냉장고 (user_ingredients)
│   ├── recipe        # 레시피 CRUD + 추천
│   ├── comment       # 댓글 · 별점
│   ├── like          # 좋아요
│   ├── ranking       # Redis 랭킹
│   ├── meallog       # 식사 기록
│   └── notification  # Kafka 유통기한 알림
└── global
    ├── config        # Security, Redis, Kafka, Swagger 설정
    ├── exception     # 공통 예외 처리
    └── util          # JWT 유틸, 공통 응답 포맷
```

---

## 🔑 핵심 구현

### 레시피 추천 로직
냉장고에 있는 재료를 기반으로 **QueryDSL**로 레시피를 매칭합니다.
`매칭률 = (보유 재료 수 / 전체 필요 재료 수) × 100` 순으로 정렬합니다.

```java
// 보유 재료와 1개 이상 겹치는 레시피 조회
SELECT r, COUNT(ri) as matchCount
FROM Recipe r
JOIN RecipeIngredient ri ON ri.recipe = r
WHERE ri.ingredient.id IN :ownedIngredientIds
GROUP BY r
ORDER BY matchCount DESC
```

### Redis 인기 랭킹
**Redis Sorted Set**으로 실시간 인기 레시피를 랭킹합니다.
점수 = `조회수 + (좋아요 수 × 2)`

```java
redisTemplate.opsForZSet().incrementScore("recipe:ranking", recipeId, score);
```

### Kafka 유통기한 알림
매일 스케줄러가 실행되어 유통기한 D-3 이내 재료를 감지하고 이벤트를 발행합니다.

```
Producer: ExpiryScheduler → Topic: ingredient-expiry
Consumer: NotificationConsumer → 유저에게 알림 전송
```

---

## 🌐 API 명세

Base URL: `https://api.fridge2fork.com/api/v1`

| 메서드 | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| POST | `/auth/register` | 회원가입 | ✗ |
| POST | `/auth/login` | 로그인 | ✗ |
| GET | `/fridge` | 냉장고 재료 조회 | ✓ |
| POST | `/fridge` | 재료 등록 | ✓ |
| GET | `/recipes/recommend` | 재료 기반 추천 | ✓ |
| POST | `/recipes` | 레시피 등록 | ✓ |
| GET | `/recipes/{id}/missing` | 부족한 재료 조회 | ✓ |
| POST | `/recipes/{id}/likes` | 좋아요 토글 | ✓ |
| GET | `/ranking/recipes` | 인기 랭킹 조회 | ✗ |
| POST | `/meal-logs` | 식사 기록 | ✓ |

> 전체 API 명세는 Swagger UI에서 확인: `/swagger-ui/index.html`

---

## ⚙️ 로컬 실행 방법

### 요구사항
- Java 17+
- Docker & Docker Compose

### 실행

```bash
git clone https://github.com/hi4579675/fridge2fork.git
cd fridge2fork

# 인프라 실행 (MySQL, Redis, Kafka)
docker-compose up -d

# 애플리케이션 실행
./gradlew bootRun
```

### 환경변수

```env
DB_URL=jdbc:mysql://localhost:3306/fridge2fork
DB_USERNAME=root
DB_PASSWORD=your_password

JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=3600000

REDIS_HOST=localhost
REDIS_PORT=6379

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

PUBLIC_API_KEY=your_public_api_key
```

---

## 🗓 개발 로드맵

| 기간 | 목표 |
|------|------|
| 1~2주차 | 프로젝트 세팅, ERD 확정, JWT 인증 구현 |
| 3~4주차 | 냉장고 재료 관리, 레시피 CRUD |
| 5~6주차 | 재료 기반 추천 엔진, 공공 API 연동 |
| 7~8주차 | 댓글, 좋아요, Redis 랭킹, 식사 기록 |
| 9~10주차 | Kafka 알림, 식단 추천, 부족 재료 기능 |
| 11~12주차 | 테스트 코드, AWS 배포, README 정리 |

---

## 🐛 트러블슈팅

> *(개발하면서 겪은 문제와 해결 과정을 여기에 채워나갑니다)*

---

## 📄 라이선스

MIT License © 2025 fridge2fork
