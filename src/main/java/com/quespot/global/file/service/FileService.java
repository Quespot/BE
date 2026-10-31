package com.quespot.global.file.service;

import com.quespot.global.file.enums.UploadPurpose;
import com.quespot.global.file.exception.FileException;
import com.quespot.global.file.exception.code.FileErrorCode;
import com.quespot.global.file.model.PresignedUploadResult;
import com.quespot.global.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Map<String, Set<String>> ALLOWED_IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp")
    );

    private static final Set<String> ALLOWED_ROOT_DIRECTORIES = Set.of(
            UploadPurpose.PROFILE.getDirectory(),
            UploadPurpose.MISSION.getDirectory(),
            UploadPurpose.ARCHIVE.getDirectory()
    );

    private final FileStorage fileStorage;

    // 파일 업로드용 Presigned URL 생성 로직
    public PresignedUploadResult createPresignedUploadUrl(
            Long userId,
            UploadPurpose purpose,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
        long maxFileSizeBytes = fileStorage.maxFileSizeBytes();
        String normalizedContentType = normalizeContentType(contentType);
        String extension = validateAndExtractExtension(originalFilename, normalizedContentType);
        validateFileSize(fileSize, maxFileSizeBytes);

        String objectKey = createObjectKey(userId, purpose, extension);
        return fileStorage.createPresignedUploadUrl(
                objectKey,
                normalizedContentType,
                fileSize
        );
    }

    // 파일 조회용 Presigned URL 생성 로직
    public String createPresignedDownloadUrl(String objectKey) {
        validateObjectKey(objectKey);
        return fileStorage.createPresignedDownloadUrl(objectKey);
    }

    // 파일 소유자 및 업로드 용도 검증 로직
    public void validateOwnedObjectKey(
            Long userId,
            UploadPurpose purpose,
            String objectKey
    ) {
        validateObjectKey(objectKey);
        if (userId == null || userId <= 0 || purpose == null) {
            throw new FileException(FileErrorCode.INVALID_OBJECT_KEY);
        }

        String[] segments = objectKey.split("/", -1);
        if (segments.length != 3 || segments[2].isBlank()) {
            throw new FileException(FileErrorCode.INVALID_OBJECT_KEY);
        }
        if (!purpose.getDirectory().equals(segments[0])) {
            throw new FileException(FileErrorCode.OBJECT_KEY_WRONG_PURPOSE);
        }
        if (!userId.toString().equals(segments[1])) {
            throw new FileException(FileErrorCode.OBJECT_KEY_OWNER_MISMATCH);
        }
    }

    // 파일 삭제 로직
    public void deleteObject(String objectKey) {
        validateObjectKey(objectKey);
        fileStorage.deleteObject(objectKey);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new FileException(FileErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.containsKey(normalized)) {
            throw new FileException(FileErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
        return normalized;
    }

    private String validateAndExtractExtension(String originalFilename, String contentType) {
        if (originalFilename == null) {
            throw new FileException(FileErrorCode.INVALID_FILE_NAME);
        }

        String filename = originalFilename.trim();
        int extensionSeparator = filename.lastIndexOf('.');
        if (filename.isEmpty()
                || filename.length() > 255
                || filename.contains("/")
                || filename.contains("\\")
                || extensionSeparator <= 0
                || extensionSeparator == filename.length() - 1) {
            throw new FileException(FileErrorCode.INVALID_FILE_NAME);
        }

        String extension = filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.get(contentType).contains(extension)) {
            throw new FileException(FileErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    private void validateFileSize(long fileSize, long maxFileSizeBytes) {
        if (fileSize <= 0 || fileSize > maxFileSizeBytes) {
            throw new FileException(FileErrorCode.INVALID_FILE_SIZE);
        }
    }

    private String createObjectKey(Long userId, UploadPurpose purpose, String extension) {
        if (userId == null || userId <= 0 || purpose == null) {
            throw new FileException(FileErrorCode.INVALID_OBJECT_KEY);
        }
        return "%s/%d/%s.%s".formatted(
                purpose.getDirectory(),
                userId,
                UUID.randomUUID(),
                extension
        );
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null
                || objectKey.isBlank()
                || objectKey.startsWith("/")
                || objectKey.contains("..")
                || objectKey.contains("\\")) {
            throw new FileException(FileErrorCode.INVALID_OBJECT_KEY);
        }

        int separator = objectKey.indexOf('/');
        if (separator <= 0 || !ALLOWED_ROOT_DIRECTORIES.contains(objectKey.substring(0, separator))) {
            throw new FileException(FileErrorCode.INVALID_OBJECT_KEY);
        }
    }
}
