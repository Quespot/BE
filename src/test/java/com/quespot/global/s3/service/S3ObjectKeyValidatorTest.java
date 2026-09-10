package com.quespot.global.s3.service;

import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ObjectKeyValidatorTest {

    private final S3ObjectKeyValidator validator = new S3ObjectKeyValidator();

    @Test
    void acceptsKeyMatchingPurposeAndOwner() {
        assertThatCode(() -> validator.validate("missions/1/abc.jpg", 1L, UploadPurpose.MISSION))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsKeyWithWrongPurposeDirectory() {
        assertThatThrownBy(() -> validator.validate("profiles/1/abc.jpg", 1L, UploadPurpose.MISSION))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.OBJECT_KEY_WRONG_PURPOSE);
    }

    @Test
    void rejectsKeyUploadedByDifferentUser() {
        assertThatThrownBy(() -> validator.validate("missions/2/abc.jpg", 1L, UploadPurpose.MISSION))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.OBJECT_KEY_OWNER_MISMATCH);
    }

    @Test
    void rejectsKeyWithExtraSegment() {
        assertThatThrownBy(() -> validator.validate("missions/1/extra/abc.jpg", 1L, UploadPurpose.MISSION))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    void rejectsKeyWithTrailingSlashAndNoFilename() {
        assertThatThrownBy(() -> validator.validate("missions/1/", 1L, UploadPurpose.MISSION))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    void rejectsKeyWithTrailingSlashAfterFilename() {
        assertThatThrownBy(() -> validator.validate("missions/1/abc.jpg/", 1L, UploadPurpose.MISSION))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    void rejectsNullKey() {
        assertThatThrownBy(() -> validator.validate(null, 1L, UploadPurpose.MISSION))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    void rejectsFullUrlInsteadOfBareKey() {
        assertThatThrownBy(() -> validator.validate(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/abc.jpg", 1L, UploadPurpose.MISSION
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_OBJECT_KEY);
    }
}
