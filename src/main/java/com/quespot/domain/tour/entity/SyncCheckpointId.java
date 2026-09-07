package com.quespot.domain.tour.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SyncCheckpointId implements Serializable {

    private String ldongRegnCd;
    private Integer contentTypeId;
}
