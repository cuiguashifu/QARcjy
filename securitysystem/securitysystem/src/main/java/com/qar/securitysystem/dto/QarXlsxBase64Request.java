package com.qar.securitysystem.dto;

public class QarXlsxBase64Request {
    private String filename;
    private String dataBase64;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDataBase64() {
        return dataBase64;
    }

    public void setDataBase64(String dataBase64) {
        this.dataBase64 = dataBase64;
    }
}

