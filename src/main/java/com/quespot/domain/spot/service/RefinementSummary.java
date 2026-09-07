package com.quespot.domain.spot.service;

import java.util.List;

public record RefinementSummary(
        int created,
        int updated,
        int skippedInvalidCoordinates,
        int skippedUnchanged,
        int failed
) {

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
