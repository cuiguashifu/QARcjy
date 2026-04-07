package com.qar.securitysystem.dto;

public class EncryptedFileResponse {
    private String encryptedData;
    private String wrappedKey;
    private String originalName;
    private String contentType;
    private String policy;

    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
    public String getWrappedKey() { return wrappedKey; }
    public void setWrappedKey(String wrappedKey) { this.wrappedKey = wrappedKey; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getPolicy() { return policy; }
    public void setPolicy(String policy) { this.policy = policy; }
}
