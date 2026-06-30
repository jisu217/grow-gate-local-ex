package com.growgate.publicdata.dto;

import com.growgate.publicdata.service.PublicDataCollectionResult;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicDataCollectResponse {

    private PublicDataRunResponse run;
    private List<PublicDataRecordResponse> records;

    public static PublicDataCollectResponse from(PublicDataCollectionResult result) {
        return PublicDataCollectResponse.builder()
                .run(PublicDataRunResponse.from(result.getRun()))
                .records(result.getRecords().stream().map(PublicDataRecordResponse::from).toList())
                .build();
    }
}
