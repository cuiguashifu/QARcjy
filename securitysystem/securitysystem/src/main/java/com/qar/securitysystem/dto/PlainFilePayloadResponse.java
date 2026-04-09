package com.qar.securitysystem.dto;

public class PlainFilePayloadResponse {
    private String dataBase64;
    private String originalName;
    private String contentType;
    private Long sizeBytes;

    public String getDataBase64() {
        return dataBase64;
    }

    public void setDataBase64(String dataBase64) {
        this.dataBase64 = dataBase64;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}

