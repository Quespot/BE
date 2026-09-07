package com.quespot.domain.tour.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiExceptionTest {

    @Test
    void parsesReasonCodeFromXmlBodyAndDetectsQuotaExceeded() {
        String xml = """
                <OpenAPI_ServiceResponse>
                    <cmmMsgHeader>
                        <returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</returnAuthMsg>
                        <returnReasonCode>22</returnReasonCode>
                    </cmmMsgHeader>
                </OpenAPI_ServiceResponse>
                """;

        TourApiException exception = TourApiException.fromXml(xml);

        assertThat(exception.isQuotaExceeded()).isTrue();
    }

    @Test
    void nonQuotaXmlReasonCodeIsNotQuotaExceeded() {
        String xml = "<OpenAPI_ServiceResponse><cmmMsgHeader><returnReasonCode>30</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>";

        TourApiException exception = TourApiException.fromXml(xml);

        assertThat(exception.isQuotaExceeded()).isFalse();
    }

    @Test
    void missingReasonCodeInXmlYieldsNotQuotaExceeded() {
        String xml = "<OpenAPI_ServiceResponse><cmmMsgHeader></cmmMsgHeader></OpenAPI_ServiceResponse>";

        TourApiException exception = TourApiException.fromXml(xml);

        assertThat(exception.isQuotaExceeded()).isFalse();
    }

    @Test
    void fromResultCodeIsNeverQuotaExceeded() {
        TourApiException exception = TourApiException.fromResultCode("0001", "APPLICATION ERROR");

        assertThat(exception.isQuotaExceeded()).isFalse();
    }
}
