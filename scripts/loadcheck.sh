#!/bin/sh
# =====================================================================
# loadcheck.sh — 프록시를 거쳐 같은 요청을 반복해 보내고 결과를 집계한다.
#
# 실습 11의 요청 반복 + 컨테이너별 처리 건수 집계 스크립트를 넓힌 것.
# 응답마다 상태 코드, 처리한 서버(X-Served-By), 버전(X-App-Version), 응답 시간을 기록해
#   - 부하 분산: 서버별 처리 수가 고르게 나뉘는지
#   - 장애·배포 중: 실패한 요청이 몇 건인지
#   - 카나리: 버전별 비율
# 을 숫자로 보여 준다.
#
# 실행 (시험할 때만 띄우는 일회성 curl 컨테이너, front 네트워크에서 프록시로 요청):
#   docker run --rm --network cleanmate-front -v ${PWD}/scripts:/scripts curlimages/curl:8.10.1 sh /scripts/loadcheck.sh -n 100
#
# 옵션
#   -n 요청 수        (기본 50)
#   -p 경로           (기본 /api/duty/week, 로그인 없이 되는 조회)
#   -s 요청 간격(초)  (기본 0.1, 배포 중 관찰은 0.2~0.5로 길게)
#   -c 쿠키           (예: "CMSESSION=..." 로그인이 필요한 /api/duty/mine 시험용)
#   -v                요청마다 한 줄씩 출력
# =====================================================================

COUNT=50
REQ_PATH=/api/duty/week
SLEEP=0.1
COOKIE=""
VERBOSE=0
TARGET=${TARGET:-http://cleanmate-proxy}   # 프록시 컨테이너 이름 (front 네트워크 안)
HOST=${HOST:-cleanmate.com}                # NPM Proxy Host 도메인

while getopts "n:p:s:c:v" opt; do
  case $opt in
    n) COUNT=$OPTARG ;;
    p) REQ_PATH=$OPTARG ;;
    s) SLEEP=$OPTARG ;;
    c) COOKIE=$OPTARG ;;
    v) VERBOSE=1 ;;
    *) echo "사용법: loadcheck.sh [-n 요청수] [-p 경로] [-s 간격] [-c 쿠키] [-v]"; exit 1 ;;
  esac
done

echo "대상: $TARGET$REQ_PATH (Host: $HOST), 요청 $COUNT회, 간격 ${SLEEP}초"
RESULT=/tmp/loadcheck.$$
: > "$RESULT"

i=1
while [ "$i" -le "$COUNT" ]; do
  # 한 줄 = "상태코드|응답시간|처리서버|버전" (헤더가 없으면 '-')
  LINE=$(curl -s -o /dev/null --max-time 5 \
          -H "Host: $HOST" ${COOKIE:+-H "Cookie: $COOKIE"} \
          -w '%{http_code}|%{time_total}|%header{x-served-by}|%header{x-app-version}' \
          "$TARGET$REQ_PATH")
  # '|'로 나눠야 헤더가 비어 있어도 칸이 밀리지 않는다
  OLDIFS=$IFS; IFS='|'; set -- $LINE; IFS=$OLDIFS
  CODE=${1:-000}; TIME=${2:-0}; SERVER=${3:--}; VERSION=${4:--}
  [ -z "$SERVER" ] && SERVER=-
  [ -z "$VERSION" ] && VERSION=-
  echo "$CODE $TIME $SERVER $VERSION" >> "$RESULT"
  [ "$VERBOSE" -eq 1 ] && echo "#$i $CODE ${TIME}s $SERVER $VERSION"
  i=$((i + 1))
  sleep "$SLEEP"
done

# 집계: 성공(2xx·3xx·4xx는 서버가 정상 응답한 것) / 실패(000=연결 실패·시간 초과, 5xx=서버 오류)
awk '
{
  total++
  if ($1 == "000" || $1 >= 500) { fail++; failcode[$1]++ } else { ok++ }
  server[$3]++
  version[$4]++
  sum += $2; if ($2 > max) max = $2
}
END {
  printf "\n=== 결과 ===\n"
  printf "요청 %d / 성공 %d / 실패 %d (%.1f%%)\n", total, ok, fail, (total ? fail * 100 / total : 0)
  for (c in failcode) printf "  실패 코드 %s: %d건\n", c, failcode[c]
  printf "서버별 처리:"
  for (s in server) printf "  %s=%d", s, server[s]
  printf "\n버전별 처리:"
  for (v in version) printf "  %s=%d", v, version[v]
  printf "\n응답 시간: 평균 %.3f초, 최대 %.3f초\n", (total ? sum / total : 0), max
}' "$RESULT"

rm -f "$RESULT"
