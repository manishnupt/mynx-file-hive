package com.fileManagement.FileManagement.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fileManagement.FileManagement.dto.FileDto;
import com.fileManagement.FileManagement.dto.FileOperationRequest;
import com.fileManagement.FileManagement.dto.FileUploadResponse;
import com.fileManagement.FileManagement.entity.FileOperationLog;
import com.fileManagement.FileManagement.exceptions.FileStorageException;
import com.fileManagement.FileManagement.service.FileStorageService;
import com.fileManagement.FileManagement.service.LoggingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins="*")

public class FileController {

    private final FileStorageService fileStorageService;
    private final LoggingService loggingService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String folderPath,
            @RequestParam(value = "username", defaultValue = "anonymous") String username) {
        
        FileUploadResponse response = fileStorageService.uploadFile(folderPath, file, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("path") String filePath,
            @RequestParam(value = "username", defaultValue = "anonymous") String username) {
        
        try {
            byte[] data = fileStorageService.downloadFile(filePath, username);
            ByteArrayResource resource = new ByteArrayResource(data);
            
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .contentLength(data.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw new FileStorageException("Failed to download file: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFile(
            @Valid @RequestBody FileOperationRequest request) {
        
        boolean result = fileStorageService.deleteFile(request.getPath(), request.getUsername());
        return result ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/rename")
    public ResponseEntity<Void> renameFile(
            @Valid @RequestBody FileOperationRequest request) {
        
        if (request.getNewName() == null || request.getNewName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean result = fileStorageService.renameFile(request.getPath(), request.getNewName(), request.getUsername());
        return result ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/move")
    public ResponseEntity<Void> moveFile(
            @Valid @RequestBody FileOperationRequest request) {
        
        if (request.getDestinationPath() == null || request.getDestinationPath().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean result = fileStorageService.moveFile(request.getPath(), request.getDestinationPath(), request.getUsername());
        return result ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileDto>> listFiles(
            @RequestParam("path") String folderPath) {
        
        List<FileDto> files = fileStorageService.listFiles(folderPath);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/info")
    public ResponseEntity<FileDto> getFileInfo(
            @RequestParam("path") String filePath) {
        
        FileDto fileDto = fileStorageService.getFileDetails(filePath);
        return ResponseEntity.ok(fileDto);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<FileOperationLog>> getFileLogs(
            @RequestParam("path") String filePath) {
        
        List<FileOperationLog> logs = loggingService.getLogsByFilePath(filePath);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/user")
    public ResponseEntity<List<FileOperationLog>> getUserLogs(
            @RequestParam("username") String username) {
        
        List<FileOperationLog> logs = loggingService.getLogsByUsername(username);
        return ResponseEntity.ok(logs);
    }
}
