package com.qar.securitysystem.dto;

import java.util.List;
import java.util.Map;

public class FlightXlsxPreviewResponse {
    private String fileId;
    private String originalName;
    private List<SheetPreview> sheets;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public List<SheetPreview> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetPreview> sheets) {
        this.sheets = sheets;
    }

    public static class SheetPreview {
        private String name;
        private List<String> columns;
        private List<Map<String, Object>> rows;
        private Integer totalRows;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public void setRows(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        public Integer getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(Integer totalRows) {
            this.totalRows = totalRows;
        }
    }
}

