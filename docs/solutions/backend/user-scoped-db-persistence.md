# 회원가입 및 사용자별 DB 저장 보완 내역

작성일: 2026-06-26
브랜치: `codex/user-scoped-db-signup`
PR: `https://github.com/jiyongKim615/grow_gate_local_ex/pull/31`

## 목적

회원가입 후 생성된 사용자 정보가 DB에 저장되고, 제품/마이페이지 데이터가 로그인 사용자 기준으로 분리되어 조회/저장되도록 보완했다. 기존에는 일부 화면이 mock 공유 배열을 사용해 사용자별 데이터 분리가 실제 DB 흐름까지 보장되지 않았다.

이번 수정은 다음 기준을 따른다.

- 개인 계정: `users.id` 기준으로 본인 제품만 저장/조회
- 기업 계정: `memberships.company_id` 기준으로 같은 회사 제품을 저장/조회
- 제품 기본 정보: 제품명, HS Code, 대상국, 설명을 DB row에 저장
- 프런트 마이페이지/진단 입력: mock 저장 대신 인증 토큰이 붙은 API 호출 사용

## DB 변경

`products` 테이블에 진단 입력과 저장된 제품 불러오기에 필요한 거래 기본 필드를 추가했다.

| 컬럼 | 타입 | 용도 |
| --- | --- | --- |
| `hs_code` | `VARCHAR(20)` / SQLite `TEXT` | HS Code 저장 |
| `target_country` | `VARCHAR(100)` / SQLite `TEXT` | 수출 대상국 저장 |

반영 파일:

- `database/mysql/init/01_schema.sql`
- `database/mysql/init/02_seed.sql`
- `database/mysql/verify.sql`
- `database/sqlite/schema.sql`
- `database/sqlite/seed.sql`
- `database/sqlite/verify.sql`

Seed 데이터의 리튬이온 배터리팩에도 `hs_code = 850760`, `target_country = Germany / EU`를 채웠다. DBeaver 검증용 `verify.sql`에서도 제품 HS Code와 대상국을 바로 볼 수 있게 조회 컬럼을 추가했다.

## 백엔드 변경

### 제품 엔티티 및 API 필드 확장

`Product` 엔티티에 `hsCode`, `targetCountry`를 추가했다.

반영 파일:

- `backend/BE-02/src/main/java/com/growgate/product/entity/Product.java`
- `backend/BE-02/src/main/java/com/growgate/product/dto/CreateProductRequest.java`
- `backend/BE-02/src/main/java/com/growgate/product/dto/ProductListResponse.java`
- `backend/BE-02/src/main/java/com/growgate/product/service/ProductService.java`

`POST /api/products`는 이제 아래 필드를 받을 수 있다.

```json
{
  "name": "리튬배터리 팩",
  "category": "전기전자",
  "hsCode": "850760",
  "targetCountry": "독일",
  "description": "충전식 전원 모듈"
}
```

저장 시 `owner_user_id`에는 로그인 사용자 ID가 들어가고, 기업 계정이면 `company_id`도 함께 저장된다. 제품 목록 응답에는 `hsCode`, `targetCountry`, `description`을 포함해 프런트가 저장된 제품을 다시 입력폼에 채울 수 있게 했다.

### 제품 지식 상세/수정 API 보완

제품 지식 상세 조회와 수정 흐름에서도 HS Code/대상국을 유지하도록 확장했다.

반영 파일:

- `backend/BE-02/src/main/java/com/growgate/knowledge/dto/KnowledgeDetailResponse.java`
- `backend/BE-02/src/main/java/com/growgate/knowledge/dto/UpdateKnowledgeRequest.java`
- `backend/BE-02/src/main/java/com/growgate/knowledge/service/KnowledgeService.java`

`PUT /api/products/{id}/knowledge`에서도 `hsCode`, `targetCountry` 변경을 받을 수 있고, 변경 시 audit log의 변경 필드에 포함된다.

### API 계약 문서 보완

제품 목록/생성/상세 API 계약에 `hsCode`, `targetCountry`, `description` 필드를 명시했다.

반영 파일:

- `backend/BE-01/api-contract.md`

## 프런트 변경

### 마이페이지 서비스 DB API 연동

`mypageService.js`를 mock 공유 배열 기반에서 실제 API 기반으로 교체했다.

반영 파일:

- `frontend/FE-02/src/services/mypageService.js`

주요 변경:

- `getMypageOverview()`
  - `GET /api/users/me`
  - 기업 계정이면 `GET /api/companies/me`, `GET /api/companies/me/members` 호출
- `getMypageProducts()`
  - `GET /api/products` 호출
  - 백엔드 응답을 기존 마이페이지 화면 모델로 변환
- `createMypageProduct()`
  - `POST /api/products` 호출
  - `name`, `hsCode`, `targetCountry`, `description`, `category` 저장
- `updateMypageProduct()`
  - `PUT /api/products/{id}/knowledge` 호출
- `rerunDiagnosis()`
  - `POST /api/diagnosis` 호출
  - `productId`, `targetCountry` 전달

### 진단 입력 저장 흐름 보완

진단 입력 화면에서 직접 입력한 제품을 마이페이지에 저장할 때 설명까지 DB에 저장하도록 보완했다.

반영 파일:

- `frontend/FE-02/src/pages/diagnosis/DiagnosisNew.jsx`
- `frontend/FE-02/src/pages/mypage/Mypage.jsx`
- `frontend/FE-02/src/services/productService.js`

저장된 제품을 불러올 때도 제품명, HS Code, 대상국, 설명이 함께 채워지는 방향으로 맞췄다.

## 테스트 추가

사용자별 제품 저장과 필드 보존을 검증하는 백엔드 테스트를 추가했다.

반영 파일:

- `backend/BE-02/src/test/java/com/growgate/product/service/ProductServiceTest.java`

검증 내용:

- 개인 계정 제품 생성 시 `owner_user_id` 기준으로 저장되는지
- 제품 생성 시 HS Code와 대상국이 `Product`에 저장되는지
- 제품 목록 조회 시 개인 사용자 기준 repository 조회가 호출되는지
- 목록 응답에 HS Code, 대상국, 설명이 포함되는지

## 충돌 해결 내역

최신 `origin/main` 병합 과정에서 `frontend/FE-02/src/services/mypageService.js`에 충돌이 발생했다.

충돌 원인:

- 현재 브랜치: 인증 사용자 기준 DB API 연동
- `main`: 기존 mock 배열 기반 제품 생성 로직 일부 유지

해결 기준:

- 이번 기능의 목적이 사용자별 DB 저장이므로 DB API 연동 버전을 유지
- `MYPAGE_PRODUCTS.push(...)` 기반 mock 저장 로직 제거
- 최신 `main`의 팀원 API(`/api/companies/me/members`)는 유지해 `getMypageOverview()`에 반영

해결 커밋:

- `7a78111 Merge origin/main into user-scoped DB branch`

## 검증 결과

다음 명령으로 확인했다.

```bash
cd backend/BE-02
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH ./gradlew test
```

결과: 성공

```bash
cd frontend/FE-02
npm run build
```

결과: 성공

```bash
python3 database/sqlite/build_sqlite_test_db.py
```

결과: SQLite 테스트 DB 생성 및 row count 검증 성공

```bash
git diff --check -- . ':(exclude)data/**'
```

결과: 성공

## 남은 주의사항

- 기존 DB 컨테이너를 계속 쓰는 환경에서는 Hibernate `ddl-auto=update`로 새 컬럼이 추가될 수 있다. 다만 DBeaver에서 명확히 확인하려면 `products` 테이블에 `hs_code`, `target_country` 컬럼이 있는지 확인해야 한다.
- `data/` 폴더는 이번 수정 대상에서 제외했다.
- 로컬 작업트리에 `.gstack/` untracked 디렉터리가 남아 있을 수 있으나, 커밋/PR에는 포함하지 않았다.
