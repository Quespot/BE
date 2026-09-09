package com.quespot.domain.mission.service;

import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

// 아카이브 목록은 정렬 기준이 createdAt DESC, id DESC 하나뿐이고(카테고리/키워드
// 조건 없음), 서버가 항상 인증된 본인 userId로만 조회하므로 미션 목록의 커서
// (MissionCursorCodec)처럼 HMAC 서명이나 querySignature 바인딩이 필요 없다.
// 커서가 조작돼도 본인 데이터 안에서 이상한 페이지가 나올 뿐 다른 사용자
// 데이터가 새지 않는다 — 그래서 서명 없는 단순 base64만 쓴다(#45).
@Component
public class ArchiveCursorCodec {

    private static final String DELIMITER = "|";

    public String encode(ArchiveCursor cursor) {
        String payload = cursor.createdAt() + DELIMITER + cursor.photoId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public ArchiveCursor decode(String value) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 2) {
                throw invalidCursor();
            }
            LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
            long photoId = Long.parseLong(parts[1]);
            if (photoId <= 0) {
                throw invalidCursor();
            }
            return new ArchiveCursor(createdAt, photoId);
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw invalidCursor();
        }
    }

    private MissionException invalidCursor() {
        return new MissionException(MissionErrorCode.INVALID_CURSOR);
    }
}
