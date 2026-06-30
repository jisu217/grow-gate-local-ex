# GrowGate Security & HTTPS Deployment Audit

작성일: 2026-07-01 KST  
범위: GitHub 공개 브랜치, Lightsail 배포 서버, Nginx/TLS, Spring API 노출면  
대외비 제외: AWS 계정 식별자, SSH key, DB/JWT/API key 값, 공모전 전략 문서

## 1. 조치 완료

| 항목 | 결과 |
| --- | --- |
| HTTPS 적용 | `https://growgate.54.180.56.24.sslip.io`에 Let's Encrypt 인증서 발급 |
| HTTP redirect | `http://54.180.56.24/` 포함 80번 요청을 HTTPS 도메인으로 301 redirect |
| 인증서 자동 갱신 | `certbot.timer` 활성화, `certbot renew --dry-run` 성공 |
| 보안 헤더 | HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy 적용 |
| Nginx 버전 노출 | `server_tokens off` 적용, 응답 헤더가 `Server: nginx`로 축소됨 |
| Spring API 노출면 | Spring Boot를 외부 `*:8080`이 아니라 `127.0.0.1:8080`에만 바인딩 |
| 서버 방화벽 | UFW 활성화, inbound 22/80/443만 허용 |
| Lightsail 방화벽 | 443/TCP 오픈, 80/22 유지 |

## 2. 배포 검증

| 검증 | 결과 |
| --- | --- |
| HTTPS 메인 | `200 OK` |
| HTTP IP 접속 | `301 -> https://growgate.54.180.56.24.sslip.io/` |
| HTTPS API 보호 엔드포인트 | `/api/diagnosis`가 `401` 반환, 502 없음 |
| 브라우저 렌더링 | 메인 화면 정상 렌더링 |
| 브라우저 콘솔 | CSP/JS 오류 없음 |
| 정적 자산 | JS/CSS/font 요청 정상 |
| TLS 인증서 | CN `growgate.54.180.56.24.sslip.io`, 만료 `2026-09-28` |

## 3. GitHub 노출 위험 점검

| 점검 | 결과 | 판단 |
| --- | --- | --- |
| 추적 중인 `.env`/key/pem/jks 파일 | 없음 | 양호 |
| 로컬 `.env` 파일 | `database/.env`, `frontend/FE-02/.env` 존재하나 gitignore 적용 | 양호 |
| Git history secret prefix scan | README/docs/application 예시 문구만 탐지 | 실제 credential 노출 확인 안 됨 |
| npm production audit | `found 0 vulnerabilities` | 양호 |
| GitHub Actions workflow | `.github` workflow 없음 | CI secret 노출면 없음 |
| 대외비 전략 문서 | 공개 docs에는 "대외비 제외" 명시, 전략 본문 없음 | 양호 |

## 4. 서버 노출 위험 점검

| 항목 | 결과 | 판단 |
| --- | --- | --- |
| `/etc/growgate/growgate.env` 권한 | `root:root`, `640` | 양호 |
| MySQL 바인딩 | `127.0.0.1:3306` | 양호 |
| Spring 바인딩 | `127.0.0.1:8080` | 양호 |
| Nginx 외부 포트 | 80/443 | 정상 |
| SSH 외부 포트 | 22 open to anywhere | 부분 리스크 |
| 백업 파일 | `/opt/growgate/backups/...`에 jar/dist 백업만 확인 | 비밀값 백업은 확인 안 됨 |
| 샘플 seed 계정 로그인 | `owner@growgate.local/password`, `viewer@growgate.local/password` 모두 `401` | 운영 계정 노출 확인 안 됨 |

## 5. 남은 보안 리스크

| 우선순위 | 리스크 | 근거 | 권장 조치 |
| --- | --- | --- | --- |
| P1 | SSH 22번이 전체 인터넷에 열려 있음 | Lightsail/UFW 모두 22 허용 | 관리자 IP가 고정되면 Lightsail firewall과 UFW를 해당 IP로 제한 |
| P1 | 프론트가 JWT를 `localStorage`에 저장 | `frontend/FE-02/src/services/apiClient.js` | HttpOnly Secure SameSite cookie + CSRF token 구조로 전환 |
| P2 | Spring Security에서 CSRF 비활성화 | `backend/BE-02/src/main/java/com/growgate/config/SecurityConfig.java` | cookie 기반 인증 전환 시 CSRF 재활성화 |
| P2 | 데모/standalone HTML에 시연 계정 문자열 존재 | `grow_gate_local_ex_demo/frontend/workspace.html`, `GROW_GATE_Local_EX_standalone.html` | 공개 제출물에서 데모 계정은 "로컬 시연 전용"으로 유지하거나 제거 |
| P2 | 로컬 DB seed에 알려진 bcrypt test hash 존재 | `database/mysql/init/02_seed.sql` | 운영 배포에서는 seed SQL 미적용 원칙 유지, README에 경고 추가 |
| P3 | 공공 API endpoint 중 일부가 공식 카탈로그/신청 페이지 기본값 | docs와 `application.yml` | 승인된 operation endpoint 확보 후 환경변수로 교체 |

## 6. 현재 공개 접속 주소

- HTTPS: `https://growgate.54.180.56.24.sslip.io`
- HTTP/IP 접속은 HTTPS 도메인으로 redirect된다.

## 7. 다음 권장 작업

1. 실제 보유 도메인을 연결한 뒤 `sslip.io` 임시 도메인을 정식 도메인 인증서로 교체한다.
2. SSH 접근을 현재 관리자 고정 IP로 제한한다.
3. JWT 저장소를 `localStorage`에서 HttpOnly cookie 구조로 전환한다.
4. 데모 계정 문자열과 seed 계정 안내가 공개 심사 자료에서 오해되지 않도록 문서에 "로컬 전용" 경고를 더한다.
