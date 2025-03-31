package com.fileManagement.FileManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDto {
    private String name;
    private String path;
    private long size;
    private LocalDateTime lastModified;
    private String type; // "FILE" or "FOLDER"
    private String contentType;
}
