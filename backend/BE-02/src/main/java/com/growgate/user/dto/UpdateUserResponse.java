package com.growgate.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateUserResponse {

    private String userId;
    private String name;
    private String updatedAt;
}
