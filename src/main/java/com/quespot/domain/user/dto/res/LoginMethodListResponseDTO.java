package com.quespot.domain.user.dto.res;

import java.util.List;

public record LoginMethodListResponseDTO(
        List<LoginMethodResponseDTO> loginMethods
) {
}
