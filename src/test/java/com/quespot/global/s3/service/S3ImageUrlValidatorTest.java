package com.quespot.global.s3.service;

import com.quespot.global.s3.config.S3Properties;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ImageUrlValidatorTest {

    private S3ImageUrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new S3ImageUrlValidator(new S3Properties("test-bucket", "ap-northeast-2", 300, 10_485_760));
    }

    @Test
    void acceptsUrlMatchingOurBucketPurposeAndOwner() {
        assertThatCode(() -> validator.validate(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/abc.jpg", 1L, UploadPurpose.MISSION
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsUrlFromDifferentHost() {
        assertThatThrownBy(() -> validator.validate(
                "https://evil-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/abc.jpg", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
    }

    @Test
    void rejectsUrlWithWrongPurposeDirectory() {
        assertThatThrownBy(() -> validator.validate(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/1/abc.jpg", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_WRONG_PURPOSE);
    }

    @Test
    void rejectsUrlUploadedByDifferentUser() {
        assertThatThrownBy(() -> validator.validate(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/2/abc.jpg", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_OWNER_MISMATCH);
    }

    @Test
    void rejectsUrlWithWrongKeyShape() {
        assertThatThrownBy(() -> validator.validate(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/extra/abc.jpg", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> validator.validate(
                "not-a-url", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
    }

    @Test
    void rejectsNonHttpsScheme() {
        assertThatThrownBy(() -> validator.validate(
                "http://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/abc.jpg", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
    }
}
