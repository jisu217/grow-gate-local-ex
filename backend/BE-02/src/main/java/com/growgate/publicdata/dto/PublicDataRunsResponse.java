package com.growgate.publicdata.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicDataRunsResponse {

    private List<PublicDataRunResponse> runs;
}
