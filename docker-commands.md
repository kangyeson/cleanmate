# cleanmate 컨테이너 실행 명령 (Docker CLI, Compose 사용 안 함)

- 모든 명령은 **저장소 루트(C:\cleanmate, VM에서는 clone한 폴더)** 에서 실행한다.
- 명령은 한 줄로 적어 PowerShell과 bash에서 똑같이 쓸 수 있게 했다.
- 도커 허브 아이디는 `kangyeson`(이미지 이름 앞부분). 보고서·영상의 명령도 이 파일과 같아야 한다.
- 비밀번호는 `env/*.env`에만 둔다(Git·이미지에 포함하지 않음). 처음이면 `env/*.env.example`을 복사해 만든다.

| 이름 | 값 |
|---|---|
| 네트워크 | cleanmate-front (프록시·화면·서버), cleanmate-back (서버·DB·Redis) |
| 볼륨 | cleanmate-db-data, cleanmate-proxy-data, cleanmate-proxy-letsencrypt |
| 호스트 포트 | 80(서비스), 81(NPM 관리 화면, 설정할 때만) |
| 내부 포트 | web 80, auth 8081, duty 8082, notify 3000, Redis 6379, MySQL 3306 |
| 당번 서버 별칭 | duty_pool (front·back 양쪽) |

---

## 0. (개발 PC에서 한 번) 개발용 컨테이너 정리

IntelliJ에서 실행 중인 auth·duty 서버를 먼저 끈다. 개발용 컨테이너가 80·81 포트를 쓰고 있으므로 지운다(개발용 볼륨은 남는다).

```
docker rm -f cm-proxy cm-web cm-mysql cm-redis
```

## 1. jar 빌드 (Spring Boot 서버 2개)

```
./gradlew :auth:bootJar :duty:bootJar
```
Windows PowerShell에서는 `.\gradlew.bat :auth:bootJar :duty:bootJar`. 테스트는 미리 `./gradlew test`로 확인한다.

## 2. 이미지 빌드 (직접 만든 이미지 5개, 빌드 컨텍스트 = 저장소 루트)

```
docker build -f Dockerfile.db -t kangyeson/cleanmate-db:v1 .
docker build -f Dockerfile.auth -t kangyeson/cleanmate-auth:v1 .
docker build -f Dockerfile.duty -t kangyeson/cleanmate-duty:v1 .
docker build -f Dockerfile.web -t kangyeson/cleanmate-web:v1 .
docker build -f Dockerfile.notify -t kangyeson/cleanmate-notify:v1 .
```

## 3. 네트워크와 볼륨

```
docker network create cleanmate-front
docker network create cleanmate-back
docker volume create cleanmate-db-data
docker volume create cleanmate-proxy-data
docker volume create cleanmate-proxy-letsencrypt
```

## 4. DB와 Redis (back 네트워크에만, 호스트 포트 없음)

```
docker run -d --name cleanmate-db --network cleanmate-back --restart unless-stopped --env-file env/db.env -v cleanmate-db-data:/var/lib/mysql kangyeson/cleanmate-db:v1
docker run -d --name cleanmate-session --network cleanmate-back --restart unless-stopped redis:7
```

DB 준비 확인: `docker logs cleanmate-db` 마지막에 `ready for connections ... port: 3306`이 보이면 다음 단계로 간다(볼륨이 비어 있는 첫 실행은 초기화 때문에 20~40초 걸림).

## 5. 인증 서버 (back으로 실행 후 front에도 연결)

```
docker run -d --name cleanmate-auth --network cleanmate-back --restart on-failure --env-file env/auth.env -e DB_HOST=cleanmate-db -e DB_USER=auth_user -e REDIS_HOST=cleanmate-session -e DUTY_URL=http://duty_pool:8082 kangyeson/cleanmate-auth:v1
docker network connect cleanmate-front cleanmate-auth
```

## 6. 당번 서버 2대 (같은 이미지, INSTANCE_ID만 다름. 별칭 duty_pool을 front·back 양쪽에)

```
docker run -d --name cleanmate-duty-1 --network cleanmate-back --network-alias duty_pool --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-1 kangyeson/cleanmate-duty:v1
docker network connect --alias duty_pool cleanmate-front cleanmate-duty-1
docker run -d --name cleanmate-duty-2 --network cleanmate-back --network-alias duty_pool --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-2 kangyeson/cleanmate-duty:v1
docker network connect --alias duty_pool cleanmate-front cleanmate-duty-2
```
- 응답 헤더 `X-Served-By`(duty-1·duty-2)와 `X-App-Version`(기본 v1)으로 어느 서버·버전이 처리했는지 보인다.
- 정기 작업(월요일 0시, 서버 시작 때)은 Redis 잠금 `lock:weekly-job`을 얻은 한 대만 실행한다. `docker logs`에 한 대는 "정기 작업 실행 완료", 다른 한 대는 "건너뜀"이 보인다.
- 인증·알림 서버는 별칭 `duty_pool`로 당번 서버를 부른다(프록시를 거치지 않는 내부 호출).

## 6-2. 알림 서버 (Node.js, back으로 실행 후 front에도 연결)

```
docker run -d --name cleanmate-notify --network cleanmate-back --restart on-failure --env-file env/notify.env -e DB_HOST=cleanmate-db -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e DUTY_URL=http://duty_pool:8082 kangyeson/cleanmate-notify:v1
docker network connect cleanmate-front cleanmate-notify
```

## 7. 화면과 프록시 (front 네트워크에만, 프록시만 호스트 포트)

```
docker run -d --name cleanmate-web --network cleanmate-front --restart unless-stopped kangyeson/cleanmate-web:v1
docker run -d --name cleanmate-proxy --network cleanmate-front --restart unless-stopped -p 80:80 -p 81:81 -v cleanmate-proxy-data:/data -v cleanmate-proxy-letsencrypt:/etc/letsencrypt jc21/nginx-proxy-manager:latest
```

## 8. NPM 설정 (관리 화면 http://localhost:81, 프록시 볼륨에 저장되어 한 번만)

1. 처음 로그인 후 관리자 이메일·비밀번호를 바꾼다.
2. PC의 hosts 파일에 `127.0.0.1 cleanmate.com`을 추가한다(VM이면 VM IP).
3. Hosts → Proxy Hosts → Add: Domain `cleanmate.com`, Forward `http` / `cleanmate-web` / `80`.
4. Custom Locations (Forward Hostname 끝에 슬래시를 붙이지 않는다 — 붙이면 `/api/...` 접두어가 잘림):

| Location | Forward Hostname | Port |
|---|---|---|
| /api/auth | cleanmate-auth | 8081 |
| /api/rooms | cleanmate-auth | 8081 |
| /api/members | cleanmate-auth | 8081 |
| /api/zones | cleanmate-auth | 8081 |
| /api/duty | duty_pool | 8082 |
| /api/stats | duty_pool | 8082 |
| /api/history | duty_pool | 8082 |
| /api/notify | cleanmate-notify | 3000 |

`/internal/...`은 등록하지 않는다(서버 간 호출 전용).

저장한 뒤 NPM이 만든 설정 파일로 경로를 확인한다. 모든 location이 `/`로 시작해야 한다(11/17: `api/duty`처럼 `/`가 빠지면 그 경로가 화면 컨테이너로 가서 404).
```
docker exec cleanmate-proxy grep -n "location" /data/nginx/proxy_host/1.conf
``` 11/19에 당번 서버 경로를 upstream(duty_backend) 방식으로 바꾼다.

## 8-2. 당번 서버 분산을 upstream 방식으로
```
docker exec cleanmate-proxy mkdir -p /data/nginx/custom
docker cp deploy/npm/http_top.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
```
1. NPM 관리 화면 → `cleanmate.com` → Edit → Custom Locations에서 `/api/duty`, `/api/stats`, `/api/history` 세 줄을 지운다.
2. Advanced 탭의 Custom Nginx Configuration에 `deploy/npm/advanced.conf` 내용을 붙여 넣고 Save.
3. 확인: `docker exec cleanmate-proxy nginx -t` → `syntax is ok`, `test is successful`

주의: upstream에 적힌 duty-1·duty-2 컨테이너가 하나라도 없으면 nginx 설정 검사가 실패한다. 서버를 빼거나 바꿀 때는 `http_top.conf`를 먼저 고치고 다시 넣은 뒤 `docker exec cleanmate-proxy nginx -s reload`.

## 9. 확인

```
docker ps
docker network inspect cleanmate-front
docker network inspect cleanmate-back
docker exec cleanmate-auth curl -s localhost:8081/health
docker exec cleanmate-duty-1 curl -s localhost:8082/health
docker exec cleanmate-notify curl -s localhost:3000/health
docker exec cleanmate-proxy getent hosts duty_pool
```
`duty_pool`에 IP가 2개(duty-1, duty-2) 나와야 한다.
브라우저에서 `http://cleanmate.com` → 로그인(예시 데이터: 예손 / 1234) → 현황.

## 시험 (측정 스크립트: 시험할 때만 띄우는 일회성 curl 컨테이너)

```
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 40
```
(VM bash에서도 같은 명령. 결과의 "서버별 처리"가 duty-1·duty-2로 고르게 나뉘면 분산 정상)

① **부하 분산:** 위 명령 그대로.

② **앱만 멈춘 서버:** duty-1을 back 네트워크에서 끊어 "컨테이너는 살아 있고 DB에 못 가서 503"을 만든다.
```
docker network disconnect cleanmate-back cleanmate-duty-1
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 40
docker network connect --alias duty_pool cleanmate-back cleanmate-duty-1
```
별칭 방식(8단계까지)에서 한 번, upstream 방식(8-2단계 뒤)에서 한 번 실행해 실패 수를 비교한다. 기대: 별칭은 약 절반 실패, upstream은 처음 몇 건 뒤 실패 없이 duty-2로만.

③ **정기 작업 한 번만:** 시험용으로 매분 실행하도록 두 서버를 다시 만든다(끝나면 6단계 명령으로 원래대로).
```
docker rm -f cleanmate-duty-1 cleanmate-duty-2
```
6단계의 duty-1·duty-2 `docker run`에 `-e WEEKLY_CRON="0 * * * * *"`을 추가해 실행하고 `network connect` 두 줄도 다시 실행한다. 8-2단계(upstream)를 적용했다면 두 대가 모두 뜬 뒤 `docker exec cleanmate-proxy nginx -s reload`로 새 컨테이너 IP를 다시 읽게 한다(컨테이너를 새로 만들면 IP가 바뀔 수 있음). 1분 이상 지나서:
```
docker logs cleanmate-duty-1 2>&1 | Select-String "정기 작업"
docker logs cleanmate-duty-2 2>&1 | Select-String "정기 작업"
```
(VM bash는 `Select-String` 대신 `grep`) 매분 한 대만 "실행 완료", 다른 한 대는 "다른 인스턴스가 실행 중"으로 건너뛰어야 한다. 끝나면 두 서버를 지우고 6단계 명령(WEEKLY_CRON 없이)으로 다시 실행한 뒤 다시 `nginx -s reload`.

## 시험 ④ 카나리 배포 (v2를 요청의 약 20%에만)

v2 이미지 만들기 (v1 이미지는 다시 빌드하지 않는다. 롤백할 때 돌아갈 버전이다):
```
.\gradlew :duty:bootJar
docker build -f Dockerfile.duty --build-arg APP_VERSION=v2 -t kangyeson/cleanmate-duty:v2 .
```
1. v2 서버 한 대 실행. 별칭 `duty_pool`은 주지 않는다(인증·알림 서버의 내부 호출은 아직 v1로만, 사용자 요청만 비율대로 v2로).
```
docker run -d --name cleanmate-duty-3 --network cleanmate-back --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-3 kangyeson/cleanmate-duty:v2
docker network connect cleanmate-front cleanmate-duty-3
```
2. 20초쯤 뒤 v2 단독 확인 (프록시를 거치지 않고 컨테이너 안에서):
```
docker exec cleanmate-duty-3 curl -s localhost:8082/health
docker exec cleanmate-duty-3 curl -s -i localhost:8082/api/duty/week
```
`X-App-Version: v2` 헤더와 본문의 `"doneCount"`, `"total"`을 확인한다.
3. upstream에 v2를 weight 1로 추가 (v1 두 대는 weight 2 → v2 약 20%):
```
docker cp deploy/npm/http_top.canary.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -t
docker exec cleanmate-proxy nginx -s reload
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 50
```
기대: 실패 0, 버전별 처리 v1=40 v2=10, 서버별 duty-1=20 duty-2=20 duty-3=10.
4. 문제가 보이면 카나리 중단(롤백): 원래 설정으로 되돌리고 v2를 지운다.
```
docker cp deploy/npm/http_top.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -s reload
docker rm -f cleanmate-duty-3
```
(순서 주의: upstream에서 먼저 빼고 reload한 뒤 컨테이너를 지운다. 반대로 하면 reload가 실패한다.)

## 시험 ⑤ 무중단 전환 (v1 → v2, 한 대씩)

카나리(시험 ④)가 문제없을 때 이어서 한다. duty-3(v2)은 전환이 끝날 때까지 그대로 두어, 한 대를 교체하는 동안에도 두 대가 요청을 받게 한다.
단계별 upstream 설정은 `deploy/npm/rolling/`에 있다(`down` = 그 서버로 보내지 않음).

**PowerShell 창을 하나 더 열어** `C:\cleanmate`에서 측정을 먼저 시작한다(약 5분, 끝나면 결과 출력):
```
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 1200 -s 0.2
```
첫 창에서 순서대로:

1. duty-1을 upstream에서 빼고 v2로 교체
```
docker cp deploy/npm/rolling/1-duty1-out.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -t
docker exec cleanmate-proxy nginx -s reload
```
5초 기다린 뒤(이미 받은 요청이 끝나도록) 교체:
```
docker stop cleanmate-duty-1
docker rm cleanmate-duty-1
docker run -d --name cleanmate-duty-1 --network cleanmate-back --network-alias duty_pool --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-1 kangyeson/cleanmate-duty:v2
docker network connect --alias duty_pool cleanmate-front cleanmate-duty-1
docker exec cleanmate-duty-1 curl -s localhost:8082/health
```
`/health`가 `"success":true`가 될 때까지(20초쯤) 기다린다. 아직 upstream에서 빠져 있으므로 준비 중에도 사용자 요청은 오지 않는다.

2. duty-1을 다시 넣고, duty-2를 빼고 v2로 교체
```
docker cp deploy/npm/rolling/2-duty2-out.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -t
docker exec cleanmate-proxy nginx -s reload
```
5초 기다린 뒤(이미 받은 요청이 끝나도록) 교체:
```
docker stop cleanmate-duty-2
docker rm cleanmate-duty-2
docker run -d --name cleanmate-duty-2 --network cleanmate-back --network-alias duty_pool --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-2 kangyeson/cleanmate-duty:v2
docker network connect --alias duty_pool cleanmate-front cleanmate-duty-2
docker exec cleanmate-duty-2 curl -s localhost:8082/health
```
`/health`가 `"success":true`가 될 때까지(20초쯤) 기다린다. 아직 upstream에서 빠져 있으므로 준비 중에도 사용자 요청은 오지 않는다.

3. duty-2를 다시 넣고 duty-3을 뺀 뒤 정리 (최종: duty-1·duty-2 모두 v2)
```
docker cp deploy/npm/http_top.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -t
docker exec cleanmate-proxy nginx -s reload
docker stop cleanmate-duty-3
docker rm cleanmate-duty-3
```
측정이 끝나면 기대: 실패 0, 버전별 처리에 v1과 v2가 섞여 있고(전환 중), 끝 무렵은 v2만. 끝난 뒤 한 번 더:
```
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 40
```
기대: 실패 0, duty-1=20 duty-2=20, v2=40.

주의: 컨테이너를 지운 상태에서는 reload하지 않는다(nginx가 이름을 찾지 못해 실패). 항상 "upstream에서 빼고 reload → 교체 → 확인 → 다음 파일로 다시 넣고 reload" 순서.

## 시험 ⑥ 롤백 (v2 → v1)

같은 순서로, 이미지 태그만 v1으로 되돌린다(v1 이미지는 다시 빌드하지 않고 남아 있던 것을 쓴다). 다른 창에서 측정을 다시 시작:
```
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 1200 -s 0.2
```
1. duty-1 교체
```
docker cp deploy/npm/rolling/3-rollback-duty1-out.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -t
docker exec cleanmate-proxy nginx -s reload
```
5초 기다린 뒤(이미 받은 요청이 끝나도록) 교체:
```
docker stop cleanmate-duty-1
docker rm cleanmate-duty-1
docker run -d --name cleanmate-duty-1 --network cleanmate-back --network-alias duty_pool --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-1 kangyeson/cleanmate-duty:v1
docker network connect --alias duty_pool cleanmate-front cleanmate-duty-1
docker exec cleanmate-duty-1 curl -s localhost:8082/health
```
`/health`가 `"success":true`가 될 때까지(20초쯤) 기다린다. 아직 upstream에서 빠져 있으므로 준비 중에도 사용자 요청은 오지 않는다.

2. duty-2 교체
```
docker cp deploy/npm/rolling/4-rollback-duty2-out.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -t
docker exec cleanmate-proxy nginx -s reload
```
5초 기다린 뒤(이미 받은 요청이 끝나도록) 교체:
```
docker stop cleanmate-duty-2
docker rm cleanmate-duty-2
docker run -d --name cleanmate-duty-2 --network cleanmate-back --network-alias duty_pool --restart on-failure --env-file env/duty.env -e DB_HOST=cleanmate-db -e DB_USER=duty_user -e REDIS_HOST=cleanmate-session -e AUTH_URL=http://cleanmate-auth:8081 -e INSTANCE_ID=duty-2 kangyeson/cleanmate-duty:v1
docker network connect --alias duty_pool cleanmate-front cleanmate-duty-2
docker exec cleanmate-duty-2 curl -s localhost:8082/health
```
`/health`가 `"success":true`가 될 때까지(20초쯤) 기다린다. 아직 upstream에서 빠져 있으므로 준비 중에도 사용자 요청은 오지 않는다.

3. 원래 설정으로
```
docker cp deploy/npm/http_top.conf cleanmate-proxy:/data/nginx/custom/http_top.conf
docker exec cleanmate-proxy nginx -s reload
docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 40
```
기대: 측정 중 실패 0, 끝난 뒤 v1=40.

## 10. 도커 허브 업로드

```
docker login
docker push kangyeson/cleanmate-db:v1
docker push kangyeson/cleanmate-auth:v1
docker push kangyeson/cleanmate-duty:v1
docker push kangyeson/cleanmate-web:v1
docker push kangyeson/cleanmate-notify:v1
```

## 참고: 전체 삭제 후 다시 시작

```
docker rm -f cleanmate-proxy cleanmate-web cleanmate-notify cleanmate-duty-1 cleanmate-duty-2 cleanmate-auth cleanmate-session cleanmate-db
docker volume rm cleanmate-db-data
```
DB 볼륨을 지우면 다음 실행 때 초기화 스크립트가 다시 돈다. 프록시 볼륨을 지우면 NPM 설정을 다시 해야 한다.
