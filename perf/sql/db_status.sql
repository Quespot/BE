-- 동기화 전후 MySQL 누적 실행 횟수 스냅샷
-- 동기화 직전·직후에 한 번씩 실행해 차이로 실행한 쿼리·트랜잭션 수를 구한다
SHOW GLOBAL STATUS
 WHERE Variable_name IN (
       'Com_select', 'Com_insert', 'Com_update', 'Com_delete',
       'Com_commit', 'Com_rollback', 'Questions'
 );
