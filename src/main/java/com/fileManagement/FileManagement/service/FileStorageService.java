package com.fileManagement.FileManagement.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fileManagement.FileManagement.dto.FileDto;
import com.fileManagement.FileManagement.dto.FileUploadResponse;

public interface FileStorageService {
    FileUploadResponse uploadFile(String folderPath, MultipartFile file, String username);
    byte[] downloadFile(String filePath, String username);
    boolean deleteFile(String filePath, String username);
    boolean renameFile(String filePath, String newName, String username);
    boolean moveFile(String sourcePath, String destinationPath, String username);
    List<FileDto> listFiles(String folderPath);
    FileDto getFileDetails(String filePath);
}
