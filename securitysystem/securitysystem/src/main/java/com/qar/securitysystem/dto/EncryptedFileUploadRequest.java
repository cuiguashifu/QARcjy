package com.qar.securitysystem.dto;

public class EncryptedFileUploadRequest {
    private String encryptedData;
    private String wrappedKey;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String policy;
    private String personNo;

    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
    public String getWrappedKey() { return wrappedKey; }
    public void setWrappedKey(String wrappedKey) { this.wrappedKey = wrappedKey; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getPolicy() { return policy; }
    public void setPolicy(String policy) { this.policy = policy; }
    public String getPersonNo() { return personNo; }
    public void setPersonNo(String personNo) { this.personNo = personNo; }
}
