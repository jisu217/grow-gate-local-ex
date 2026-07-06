package com.growgate.publicdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicDataCollectRequest {

    @NotBlank(message = "HS Code는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6,10}$", message = "HS Code는 숫자 6~10자리여야 합니다.")
    private String hsCode;

    @NotBlank(message = "대상국은 필수입니다.")
    @Size(max = 40)
    private String targetCountry;

    @NotBlank(message = "제품명은 필수입니다.")
    @Size(max = 160)
    private String productName;

    @Size(max = 80)
    private String lawSearchQuery;
}
