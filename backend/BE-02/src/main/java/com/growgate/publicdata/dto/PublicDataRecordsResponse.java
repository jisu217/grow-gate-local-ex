package com.growgate.publicdata.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicDataRecordsResponse {

    private List<PublicDataRecordResponse> records;
}
