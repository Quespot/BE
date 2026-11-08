package com.quespot.domain.spot.service;

import java.util.List;

public record RefinementSummary(
        int created,
        int updated,
        int skippedInvalidCoordinates,
        int skippedUnchanged,
        int failed
) {

    public static RefinementSummary of(List<RefinementResult> results, int failed) {
        int created = 0;
        int updated = 0;
        int skippedInvalidCoordinates = 0;
        int skippedUnchanged = 0;

        for (RefinementResult result : results) {
            switch (result) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case SKIPPED_INVALID_COORDINATES -> skippedInvalidCoordinates++;
                case SKIPPED_UNCHANGED -> skippedUnchanged++;
            }
        }

        return new RefinementSummary(created, updated, skippedInvalidCoordinates, skippedUnchanged, failed);
    }

    public RefinementSummary withAdditionalUnchanged(int unchanged) {
        return new RefinementSummary(created, updated, skippedInvalidCoordinates, skippedUnchanged + unchanged, failed);
    }

    public static RefinementSummary combine(List<RefinementSummary> summaries) {
        int created = 0;
        int updated = 0;
        int skippedInvalidCoordinates = 0;
        int skippedUnchanged = 0;
        int failed = 0;

        for (RefinementSummary summary : summaries) {
            created += summary.created();
            updated += summary.updated();
            skippedInvalidCoordinates += summary.skippedInvalidCoordinates();
            skippedUnchanged += summary.skippedUnchanged();
            failed += summary.failed();
        }

        return new RefinementSummary(created, updated, skippedInvalidCoordinates, skippedUnchanged, failed);
    }
}
