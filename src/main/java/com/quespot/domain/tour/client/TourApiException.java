package com.quespot.domain.tour.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TourApiException extends RuntimeException {

    private static final Pattern REASON_CODE_PATTERN = Pattern.compile("<returnReasonCode>(\\d+)</returnReasonCode>");
    private static final String QUOTA_EXCEEDED_CODE = "22";

    private final String returnReasonCode;

    private TourApiException(String message, String returnReasonCode) {
        super(message);
        this.returnReasonCode = returnReasonCode;
    }

    public static TourApiException fromXml(String xmlBody) {
        Matcher matcher = REASON_CODE_PATTERN.matcher(xmlBody);
        String reasonCode = matcher.find() ? matcher.group(1) : null;
        return new TourApiException("TourAPI 게이트웨이 오류 (returnReasonCode=" + reasonCode + ")", reasonCode);
    }

    public static TourApiException fromResultCode(String resultCode, String resultMsg) {
        return new TourApiException(
                "TourAPI 응답 오류 (resultCode=" + resultCode + ", resultMsg=" + resultMsg + ")",
                resultCode
        );
    }

    public boolean isQuotaExceeded() {
        return QUOTA_EXCEEDED_CODE.equals(returnReasonCode);
    }
}
