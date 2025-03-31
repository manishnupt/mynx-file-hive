package com.fileManagement.FileManagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileOperationRequest {
    @NotBlank(message = "Path is required")
    private String path;
    
    private String destinationPath;
    private String newName;
    private String username;
}