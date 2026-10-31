package com.quespot.domain.notification.dto.req;

import com.quespot.domain.notification.enums.DeviceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterFcmTokenRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsLatitudeWithoutLongitude() {
        RegisterFcmTokenRequestDTO dto = new RegisterFcmTokenRequestDTO(
                "t", DeviceType.ANDROID, new BigDecimal("37.5"), null);

        Set<ConstraintViolation<RegisterFcmTokenRequestDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("locationComplete"));
    }

    @Test
    void acceptsBothOrNeither() {
        assertThat(validator.validate(new RegisterFcmTokenRequestDTO("t", DeviceType.ANDROID, null, null))).isEmpty();
        assertThat(validator.validate(new RegisterFcmTokenRequestDTO(
                "t", DeviceType.ANDROID, new BigDecimal("37.5"), new BigDecimal("127.0")))).isEmpty();
    }

    @Test
    void rejectsOutOfRangeLatitude() {
        RegisterFcmTokenRequestDTO dto = new RegisterFcmTokenRequestDTO(
                "t", DeviceType.ANDROID, new BigDecimal("91"), new BigDecimal("127.0"));

        assertThat(validator.validate(dto)).isNotEmpty();
    }
}
