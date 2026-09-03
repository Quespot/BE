package com.quespot.domain.item.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum ItemSuccessCode implements BaseCode {

    SHOP_ITEMS_FOUND(HttpStatus.OK,
            "ITEM_200_001",
            "상점 아이템 목록을 조회했습니다."),

    MY_ITEMS_FOUND(HttpStatus.OK,
            "ITEM_200_002",
            "보유 아이템 목록을 조회했습니다."),

    ITEM_EQUIPPED(HttpStatus.OK,
            "ITEM_200_003",
            "아이템이 장착되었습니다."),

    ITEM_UNEQUIPPED(HttpStatus.OK,
            "ITEM_200_004",
            "아이템이 해제되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ItemSuccessCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public SuccessReasonDTO getReason() {
        return new SuccessReasonDTO(null, true, code, message);
    }

    @Override
    public SuccessReasonDTO getReasonHttpStatus() {
        return new SuccessReasonDTO(httpStatus, true, code, message);
    }
}
