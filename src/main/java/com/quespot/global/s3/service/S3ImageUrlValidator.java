package com.quespot.global.s3.service;

import com.quespot.global.s3.config.S3Properties;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

// presigned PUT으로 올라간 이미지 URL이 우리 버킷 소유이고, 의도한 디렉터리
// 아래이고, 업로드한 본인이 맞는지 URL 문자열 패턴만으로 검증한다(#45).
// S3Service.createObjectKey가 "{purpose}/{userId}/{uuid}.{ext}" 형태로 key를
// 만들기 때문에 이 형태를 기준으로 검증한다. S3 API 호출(headObject 등)은
// 하지 않는다 — 트랜잭션 안에서 외부 호출을 피하기 위한 의도적 선택(#45 결정
// 사항, S3 오브젝트 실존 자체는 검증하지 않는다).
//
// 프로필 이미지 등 다른 UploadPurpose에도 그대로 재사용 가능하도록
// domain 패키지가 아니라 global/s3에 둔다.
@Component
@RequiredArgsConstructor
public class S3ImageUrlValidator {

    private final S3Properties properties;

    public void validate(String imageUrl, Long userId, UploadPurpose expectedPurpose) {
        URI uri = parse(imageUrl);

        String expectedHost = "%s.s3.%s.amazonaws.com".formatted(properties.bucket(), properties.region());
        if (!expectedHost.equalsIgnoreCase(uri.getHost())) {
            throw new S3Exception(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
        }

        String path = uri.getPath();
        String key = path.startsWith("/") ? path.substring(1) : path;
        String[] segments = key.split("/");
        if (segments.length != 3) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }
        if (!expectedPurpose.getDirectory().equals(segments[0])) {
            throw new S3Exception(S3ErrorCode.IMAGE_URL_WRONG_PURPOSE);
        }
        if (!userId.toString().equals(segments[1])) {
            throw new S3Exception(S3ErrorCode.IMAGE_URL_OWNER_MISMATCH);
        }
    }

    private URI parse(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new S3Exception(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new S3Exception(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
        }
    }
}
