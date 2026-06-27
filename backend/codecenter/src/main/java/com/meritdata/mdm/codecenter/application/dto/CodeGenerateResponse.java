package com.meritdata.mdm.codecenter.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeGenerateResponse {
    private Boolean success;
    private String code;
    private Long sequenceNum;
    private String allocationId;
    private String errorMessage;

    public static CodeGenerateResponse success(String code, Long seq, String allocId) {
        return CodeGenerateResponse.builder()
                .success(true)
                .code(code)
                .sequenceNum(seq)
                .allocationId(allocId)
                .build();
    }
    public static CodeGenerateResponse fail(String msg) {
        return CodeGenerateResponse.builder()
                .success(false)
                .errorMessage(msg)
                .build();
    }
}
