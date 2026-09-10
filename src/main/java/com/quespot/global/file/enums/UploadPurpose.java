package com.quespot.global.file.enums;

public enum UploadPurpose {
    PROFILE("profiles"),
    MISSION("missions"),
    ARCHIVE("archives");

    private final String directory;

    UploadPurpose(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}
