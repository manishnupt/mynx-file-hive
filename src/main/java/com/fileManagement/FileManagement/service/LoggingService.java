package com.fileManagement.FileManagement.service;

import java.util.List;

import com.fileManagement.FileManagement.entity.FileOperationLog;

public interface LoggingService {
    void logOperation(String username, String operation, String filePath, String destinationPath, String status, String errorMessage);
    List<FileOperationLog> getLogsByFilePath(String filePath);
    List<FileOperationLog> getLogsByUsername(String username);
}
