#!/usr/bin/env bash
# k6 가상 사용자(1~USER_COUNT)의 로그인 세션을 Redis에 등록
# JwtAuthenticationFilter가 토큰의 sid로 Redis 세션(quespot:auth:refresh:{sid})을 확인하기 때문에 필요
# Redis 컨테이너를 다시 만들면 세션이 사라지므로 다시 실행
set -euo pipefail

USER_COUNT="${USER_COUNT:-1000}"

for i in $(seq 1 "$USER_COUNT"); do
  echo "HSET quespot:auth:refresh:k6-session-$i userId $i"
done | docker exec -i quespot-perf-redis redis-cli > /dev/null

echo "registered sessions: $(docker exec quespot-perf-redis redis-cli --scan --pattern 'quespot:auth:refresh:k6-session-*' | wc -l | tr -d ' ')"
