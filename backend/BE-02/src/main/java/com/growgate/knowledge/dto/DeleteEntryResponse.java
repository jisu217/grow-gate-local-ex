package com.growgate.knowledge.dto;

// 지식 하위 항목(인증서/FTO/바이어요구사항/출처) 삭제 응답 DTO
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteEntryResponse {

    private String message;
    private String id;
}
