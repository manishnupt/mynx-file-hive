package com.fileManagement.FileManagement.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fileManagement.FileManagement.entity.FileOperationLog;
import com.fileManagement.FileManagement.repository.FileOperationLogRepository;
import com.fileManagement.FileManagement.service.LoggingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoggingServiceImpl implements LoggingService {

    private final FileOperationLogRepository logRepository;

    @Override
    @Async
    public void logOperation(String username, String operation, String filePath, String destinationPath, String status, String errorMessage) {
        FileOperationLog log = FileOperationLog.builder()
                .username(username != null ? username : "anonymous")
                .operation(operation)
                .filePath(filePath)
                .destinationPath(destinationPath)
                .timestamp(LocalDateTime.now())
                .status(status)
                .errorMessage(errorMessage)
                .build();
        
        logRepository.save(log);
    }

    @Override
    public List<FileOperationLog> getLogsByFilePath(String filePath) {
        return logRepository.findByFilePathContainingOrderByTimestampDesc(filePath);
    }

    @Override
    public List<FileOperationLog> getLogsByUsername(String username) {
        return logRepository.findByUsernameOrderByTimestampDesc(username);
    }
}
