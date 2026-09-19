#!/usr/bin/env bash
# 시드 생성·전환. 맥과 윈도우(Git Bash)에서 같은 명령으로 돈다.
#   ./seed/seed.sh init      환경 점검 + S만 만들어 파이프라인 확인
#   ./seed/seed.sh s|m|l     해당 프로파일을 쓸 수 있는 상태로 (없으면 만들고, 있으면 bench만 교체)
#   ./seed/seed.sh status    현재 상태
set -euo pipefail
cd "$(dirname "$0")"
DC="docker compose -f compose.seed.yml"
[ -f ../.env ] && set -a && . ../.env && set +a || true
: "${PGUSER:=hjo}" "${PGPASSWORD:=hjo}" "${PGPORT:=5432}"
export PGUSER PGPASSWORD PGPORT

psql_() { MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" $DC exec -T -e PGPASSWORD="$PGPASSWORD" postgres psql -U "$PGUSER" -v ON_ERROR_STOP=1 "$@"; }
die() { echo ""; echo "$1" >&2; exit 1; }
# fingerprint 대조. 세 갈래로 나뉜다.
#   기준값 없음        -> 내 값이 기준이 된다. 커밋하라고 알린다
#   입력 해시가 다름   -> 애초에 다른 입력이라 값이 다른 게 당연하다. 값 비교를 하지 않는다
#   입력은 같은데 다름 -> 진짜 문제다. 어디가 다른지 짚고 **덮어쓰지 않는다**
# 덮어쓰면 "누가 기준인지"가 사라져 매번 자기 값이 기준이 되고 영원히 "일치"한다.
fp_check() {
  local P="$1" F="fingerprints/${P}.txt" CUR="/tmp/fp-$$.txt"
  { echo "# profile=$P inputs=$INPUTS_HASH pg=$(psql_ -d postgres -tAc "SHOW server_version" | tr -d '\r' | cut -d' ' -f1)"
    psql_ -d "seed_$P" -A -t -F "  " -f /seed/verify/fingerprint.sql | tr -d '\r'
  } > "$CUR"

  if [ ! -f "$F" ]; then
    mkdir -p fingerprints; cp "$CUR" "$F"; rm -f "$CUR"
    echo ""; cat "$F"
    echo "  [기준값 없음] 이 값이 팀 기준이 됩니다. $F 를 커밋하세요."
    return 0
  fi

  local base_inputs
  base_inputs=$(sed -n '1s/.*inputs=\([0-9a-f]*\).*/\1/p' "$F")
  if [ "$base_inputs" != "$INPUTS_HASH" ]; then
    rm -f "$CUR"
    echo ""
    echo "  [기준값이 낡음] 기준값은 다른 입력으로 만들어졌습니다 ($base_inputs -> $INPUTS_HASH)."
    echo "    값이 다른 게 당연하므로 비교하지 않습니다."
    echo "    먼저 만든 사람이 기준값을 갱신해야 합니다: rm $F && bash seed/seed.sh $P"
    return 0
  fi

  if diff -q <(tail -n +2 "$F") <(tail -n +2 "$CUR") >/dev/null; then
    rm -f "$CUR"; echo "  fingerprint 기준값과 일치"
    return 0
  fi

  echo ""
  echo "  [불일치] 같은 입력인데 데이터가 다릅니다. 기준값을 덮어쓰지 않았습니다."
  diff <(tail -n +2 "$F") <(tail -n +2 "$CUR") | sed 's/^/    /'
  echo "    행 수부터 다르면 프로파일 값이, 행 수는 같고 해시만 다르면"
  echo "    DB 시간대(UTC)나 PostgreSQL 메이저 버전을 의심하세요."
  rm -f "$CUR"
  return 1
}

# 1234567 -> 1,234,567
n_() { printf '%s' "$1" | sed ':a;s/\B[0-9]\{3\}\>/,&/;ta'; }
# 초 -> "34분 21초"
el_() { local t=$1; if [ "$t" -ge 60 ]; then echo "$((t/60))분 $((t%60))초"; else echo "${t}초"; fi; }
# 프로파일별 예상 시간 (2026-09-19 실측)
eta_() { case "$1" in s) echo "$2";; m) echo "$3";; l) echo "$4";; esac; }

md5_() { { md5sum 2>/dev/null || md5; } | cut -d' ' -f1; }

# 데이터에 영향을 주는 입력만 해시한다. 바뀌면 템플릿을 다시 만들어야 하므로
# (L은 한 시간이 넘는다) 데이터를 바꾸지 않는 것은 일부러 뺀다.
#   넣는 것: 프로파일 파생값, 생성 SQL, 앱 마이그레이션, PostgreSQL 이미지 태그
#   빼는 것: verify/*.sql(데이터를 만들지 않음), seed.sh(그 결과인 파생값이 이미 들어 있음),
#            profiles/*.env 원본(값은 파생값에 반영됨)
# CR을 지우는 이유: 앱 마이그레이션은 .gitattributes에서 제외했다(Flyway 체크섬 때문).
# 윈도우에서 CRLF가 되면 맥과 해시가 달라져 서로 "낡았다"고 판단한다.
inputs_hash() {
  {
    printf '%s
' "$1"
    for f in $(ls sql/*.sql ../src/main/resources/db/migration/*.sql 2>/dev/null | sort); do
      printf '%s
' "${f##*/}"          # 파일명도 넣는다. 내용만 이으면 파일을 나눠도 합이 같다
      # 주석(--), psql 메타명령(\), 빈 줄은 뺀다. 데이터를 바꿀 수 없는 줄이다.
      # 넣으면 진행 메시지 한 줄 추가에도 L(34분)이 통째로 무효가 된다.
      tr -d '\r' < "$f" | sed -e 's/[[:space:]]*$//' -e '/^[[:space:]]*--/d' -e '/^[[:space:]]*\\/d' -e '/^$/d'
    done
    grep -m1 'image: postgres:' compose.seed.yml
  } | md5_ | cut -c1-12
}

up() {
  docker info >/dev/null 2>&1 || die "[점검 실패] Docker 엔진에 연결할 수 없습니다.
  원인: Docker Desktop이 실행 중이 아닙니다.
  조치: Docker Desktop을 실행하고 다시 시도하세요."
  $DC up -d postgres >/dev/null
  for i in $(seq 1 40); do
    $DC exec -T postgres pg_isready -U "$PGUSER" -d postgres >/dev/null 2>&1 && return 0
    sleep 2
  done
  die "[점검 실패] Postgres가 뜨지 않았습니다. '$DC logs postgres'로 확인하세요."
}

status() {
  up
  echo "--- 템플릿 ---"
  psql_ -d postgres -tAc "SELECT datname FROM pg_database WHERE datname LIKE 'seed\_%' OR datname='bench' ORDER BY 1" || true
  echo "--- bench 상태 ---"
  psql_ -d bench -tAc "SELECT 'profile='||profile||' epoch='||epoch||' pg='||pg_version FROM seed_meta" 2>/dev/null || echo "(bench 없음)"
}

build() {
  local P="$1" DB="seed_$1"
  # 프로파일 파일이 일부 값만 정의하므로, 앞 프로파일의 값이 남지 않게 먼저 지운다.
  # 한 셸에서 여러 프로파일을 다루면(seed.sh hash) 남은 POSTS_OVERRIDE가 규모를 뒤엎는다.
  unset POSTS_OVERRIDE TARGET_RATIO HOT_WINDOW_EVENTS PROFILE
  . profiles/base.env
  . "profiles/${P}.env"

  # 규모는 DB 메모리에서 역산한다 (S는 직접 지정)
  if [ -n "${POSTS_OVERRIDE:-}" ]; then POSTS=$POSTS_OVERRIDE
  else POSTS=$(awk -v m="$DB_MEM_GB" -v r="$TARGET_RATIO" -v b="$BYTES_PER_UNIT" 'BEGIN{printf "%d", m*1073741824*r/b}'); fi
  USERS_SEED=$(( POSTS / POSTS_PER_USER ));      [ "$USERS_SEED" -lt 1 ] && USERS_SEED=1
  USERS_TOTAL=$(( USERS_SEED + K6_USER_POOL ))
  COMMENTS=$(( POSTS * COMMENTS_PER_POST ))
  TOP_COMMENTS=$(awk -v c="$COMMENTS" -v r="$REPLY_RATIO" 'BEGIN{printf "%d", c*(1-r)}')
  REPLIES=$(( COMMENTS - TOP_COMMENTS ))
  VIEWS=$(( POSTS * VIEWS_PER_POST ))
  LIKES=$(( POSTS * LIKES_PER_POST ))
  LIKE_CANDIDATES=$(( LIKES * LIKE_CANDIDATE_MULT ))
  HOT_WINDOW=$HOT_WINDOW_EVENTS
  [ "$HOT_WINDOW" -gt $(( VIEWS / 2 )) ] && HOT_WINDOW=$(( VIEWS / 2 ))   # S에서 창이 전체보다 커지지 않게
  EVENT_DAYS=$(awk -v v="$VIEWS" -v h="$HOT_WINDOW" 'BEGIN{d=v/h; printf "%.4f", (d<1?1:d)}')
  POST_DAYS=365
  PARAMS=$(printf '{"posts":%d,"users_seed":%d,"k6_pool":%d,"comments":%d,"replies":%d,"views":%d,"likes":%d,"hot_window":%d,"event_days":%s,"content_bytes":%d,"zipf_s":%s,"comment_zipf_s":%s}' \
    "$POSTS" "$USERS_SEED" "$K6_USER_POOL" "$COMMENTS" "$REPLIES" "$VIEWS" "$LIKES" "$HOT_WINDOW" "$EVENT_DAYS" "$CONTENT_BYTES" "$ZIPF_S" "$COMMENT_ZIPF_S")
  INPUTS_HASH=$(inputs_hash "$PARAMS")

  local TARGET_GB
  TARGET_GB=$(awk -v m="$DB_MEM_GB" -v r="${TARGET_RATIO:-0}" 'BEGIN{printf "%.1f", m*r}')
  echo ""
  if [ -n "${POSTS_OVERRIDE:-}" ]; then
    echo "프로파일 $(echo "$P" | tr '[:lower:]' '[:upper:]') — posts $(n_ "$POSTS")  (규모 직접 지정)"
  else
    echo "프로파일 $(echo "$P" | tr '[:lower:]' '[:upper:]') — posts $(n_ "$POSTS")  (DB 메모리 ${DB_MEM_GB}GB x ${TARGET_RATIO} = ${TARGET_GB}GB 목표)"
  fi
  echo "  users $(n_ "$USERS_TOTAL") (시드 $(n_ "$USERS_SEED") + k6풀 $(n_ "$K6_USER_POOL"))"
  echo "  comments $(n_ "$COMMENTS") (최상위 $(n_ "$TOP_COMMENTS") + 대댓글 $(n_ "$REPLIES"))"
  echo "  view_events $(n_ "$VIEWS")   likes $(n_ "$LIKES")"
  echo "  24시간 창 $(n_ "$HOT_WINDOW")건   이벤트 분포 ${EVENT_DAYS}일   입력 해시 $INPUTS_HASH"
  echo ""

  # 해시만 알고 싶을 때 (seed.sh hash)
  if [ "${DRY:-0}" = "1" ]; then echo "  $P -> $INPUTS_HASH"; return 0; fi

  # 이미 최신이면 만들지 않는다
  local have
  have=$(psql_ -d "$DB" -tAc "SELECT inputs_hash FROM seed_meta" 2>/dev/null || true)
  if [ "$have" = "$INPUTS_HASH" ]; then echo "  템플릿 $DB 이미 최신 — 생성 건너뜀"; fp_check "$P"; return 0; fi
  [ -n "$have" ] && echo "  템플릿 $DB 이 낡았습니다 (입력 $have -> $INPUTS_HASH). 다시 만듭니다."

  echo "  [1/6] 스키마 — Flyway로 마이그레이션 적용$(printf '%*s' 18 '')(수 초)"
  psql_ -d postgres -c "DROP DATABASE IF EXISTS \"$DB\" WITH (FORCE)" >/dev/null
  psql_ -d postgres -c "CREATE DATABASE \"$DB\"" >/dev/null
  SEED_DB="$DB" $DC run --rm flyway >/dev/null || die "[실패] 1/6 Flyway 마이그레이션. 'SEED_DB=$DB $DC run --rm flyway'로 확인하세요."

  echo "  [2/6] 적재 — FK·인덱스를 떼고 넣는다$(printf '%*s' 20 '')($(eta_ "$P" "10초" "1분" "15분"))"
  psql_ -d "$DB" \
    -v boards="$BOARDS" -v users_total="$USERS_TOTAL" -v users_seed="$USERS_SEED" \
    -v posts="$POSTS" -v comments="$COMMENTS" -v views="$VIEWS" -v likes="$LIKES" \
    -v like_candidates="$LIKE_CANDIDATES" -v content_bytes="$CONTENT_BYTES" \
    -v top_comments="$TOP_COMMENTS" \
    -v zipf_s="$ZIPF_S" -v comment_zipf_s="$COMMENT_ZIPF_S" -v post_days="$POST_DAYS" -v event_days="$EVENT_DAYS" \
    -v event_ns="$EVENT_NS" \
    -f /seed/sql/00_helpers.sql -f /seed/sql/05_session.sql -f /seed/sql/10_prepare_load.sql \
    -f /seed/sql/20_boards.sql -f /seed/sql/21_users.sql -f /seed/sql/22_posts.sql \
    -f /seed/sql/23_comments_top.sql -f /seed/sql/24_comments_replies.sql \
    -f /seed/sql/25_post_likes.sql -f /seed/sql/26_view_events.sql >/dev/null

  echo "  [3/6] 인덱스·FK 복원 — FK 검사가 고아 행을 잡는다$(printf '%*s' 6 '')($(eta_ "$P" "3초" "20초" "8분"))"
  psql_ -d "$DB" -f /seed/sql/05_session.sql -f /seed/sql/30_restore.sql >/dev/null

  echo "  [4/6] post_stats 집계 · 시퀀스 정렬 · VACUUM ANALYZE$(printf '%*s' 3 '')($(eta_ "$P" "3초" "30초" "8분"))"
  psql_ -d "$DB" -f /seed/sql/05_session.sql -f /seed/sql/40_post_stats.sql -f /seed/sql/50_setval.sql -f /seed/sql/60_vacuum.sql >/dev/null
  psql_ -d "$DB" -v profile="$P" -v inputs_hash="$INPUTS_HASH" -v params="$PARAMS" -f /seed/sql/70_meta.sql >/dev/null

  echo "  [5/6] 검증 7개 — 하나라도 실패하면 여기서 멈춘다"
  psql_ -d "$DB" -v boards="$BOARDS" -v users_total="$USERS_TOTAL" -v posts="$POSTS" \
    -v comments="$COMMENTS" -v likes="$LIKES" -v views="$VIEWS" \
    -v hot_window="$HOT_WINDOW" -v event_ns="$EVENT_NS" -v top_comments="$TOP_COMMENTS" \
    -f /seed/verify/verify.sql

  echo "  [6/6] fingerprint — 행마다 해시를 계산해 합한다$(printf '%*s' 8 '')($(eta_ "$P" "1초" "10초" "3분"))"
  fp_check "$P"
}

switch_to() {
  local DB="seed_$1"
  local before busy
  before=$(psql_ -d bench -tAc "SELECT upper(profile) FROM seed_meta" 2>/dev/null | tr -d '
' | tr -d ' ')
  [ -z "$before" ] && before="(없음)"
  busy=$(psql_ -d postgres -tAc "SELECT count(*) FROM pg_stat_activity WHERE datname IN ('bench','$DB')" || echo 0)
  if [ "${busy:-0}" -gt 0 ]; then
    die "[전환 불가] bench 또는 $DB 에 접속 중인 세션이 ${busy}개 있습니다.
  원인: 스프링 앱이 실행 중일 가능성이 높습니다.
  조치: 앱을 중지한 뒤 다시 시도하세요."
  fi
  psql_ -d postgres -c "DROP DATABASE IF EXISTS bench WITH (FORCE)" >/dev/null
  psql_ -d postgres -c "CREATE DATABASE bench TEMPLATE \"$DB\" STRATEGY FILE_COPY" >/dev/null
  echo ""
  if [ "$before" = "$(echo "$1" | tr '[:lower:]' '[:upper:]')" ]; then
    echo "  bench : $before (그대로 — 같은 프로파일을 다시 복제했다)"
  else
    echo "  bench : $before → $(echo "$1" | tr '[:lower:]' '[:upper:]')"
  fi
}

case "${1:-status}" in
  status) status ;;
  hash)   echo "프로파일별 입력 해시"
          for p in s m l; do DRY=1 build "$p" >/dev/null 2>&1; DRY=1 build "$p" 2>/dev/null | tail -1; done ;;
  init)   up; build s; switch_to s
          echo "  [완료] S 준비됨 ($(el_ $SECONDS))"
          echo ""
          echo "  다음: bash seed/seed.sh m   (M 프로파일, 약 2분)"
          echo "        앱을 띄우면 S 규모 데이터를 봅니다. .env는 그대로 둡니다." ;;
  s|m|l)  up; build "$1"; switch_to "$1"
          echo "  [완료] $(echo "$1" | tr "[:lower:]" "[:upper:]") 준비됨 ($(el_ $SECONDS))" ;;
  *) die "사용법: seed.sh [init|s|m|l|status|hash]" ;;
esac
