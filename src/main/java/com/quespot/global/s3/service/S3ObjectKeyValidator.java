package com.quespot.global.s3.service;

import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import org.springframework.stereotype.Component;

// 업로드 완료 후 저장 API에 제출되는 objectKey가 우리가 발급한 형태와 일치하는지
// 검증한다(#45). S3 버킷은 비공개(Public Access Block 유지, 원래 이슈 #42 완료
// 조건)라 클라이언트가 임의로 만든 URL을 신뢰할 수 없다 — presigned PUT/GET만
// 쓴다는 전제 그대로, 저장도 objectKey로 받는다(URL이 아님).
//
// S3Service.createObjectKey가 "{purpose}/{userId}/{uuid}.{ext}" 형태로 key를
// 만들기 때문에 이 형태(정확히 3세그먼트)를 기준으로 검증한다. S3 API 호출
// (headObject 등)은 하지 않는다 — 트랜잭션 안에서 외부 호출을 피하기 위한
// 의도적 선택(#45 결정 사항, S3 오브젝트 실존 자체는 검증하지 않는다).
//
// 프로필 이미지 등 다른 UploadPurpose에도 그대로 재사용 가능하도록
// domain 패키지가 아니라 global/s3에 둔다.
@Component
public class S3ObjectKeyValidator {

    public void validate(String objectKey, Long userId, UploadPurpose expectedPurpose) {
        if (objectKey == null) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }

        // split(regex)는 뒤쪽 빈 문자열을 조용히 버린다 — "missions/1/"처럼
        // 파일명이 빈 key가 길이 2로 정확히 걸러지도록 limit=-1로 트레일링
        // 빈 세그먼트도 그대로 살린다.
        String[] segments = objectKey.split("/", -1);
        if (segments.length != 3 || segments[2].isBlank()) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }
        if (!expectedPurpose.getDirectory().equals(segments[0])) {
            throw new S3Exception(S3ErrorCode.OBJECT_KEY_WRONG_PURPOSE);
        }
        if (!userId.toString().equals(segments[1])) {
            throw new S3Exception(S3ErrorCode.OBJECT_KEY_OWNER_MISMATCH);
        }
    }
}
