package com.fileManagement.FileManagement.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fileManagement.FileManagement.dto.FileDto;
import com.fileManagement.FileManagement.dto.FolderDto;
import com.fileManagement.FileManagement.exceptions.FileStorageException;
import com.fileManagement.FileManagement.service.FileStorageService;
import com.fileManagement.FileManagement.service.FolderService;
import com.fileManagement.FileManagement.service.LoggingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderServiceImpl implements FolderService {

    private final S3Client s3Client;
    private final LoggingService loggingService;
    private final FileStorageService fileStorageService;
    
    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Override
    public boolean createFolder(String folderPath, String username) {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(normalizedFolderPath)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.empty());
            
            loggingService.logOperation(
                    username,
                    "CREATE_FOLDER",
                    normalizedFolderPath,
                    null,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to create folder: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "CREATE_FOLDER",
                    folderPath,
                    null,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to create folder: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFolder(String folderPath, String username) {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedFolderPath)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();
                
                s3Client.deleteObject(deleteRequest);
            }
            
            loggingService.logOperation(
                    username,
                    "DELETE_FOLDER",
                    normalizedFolderPath,
                    null,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete folder: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "DELETE_FOLDER",
                    folderPath,
                    null,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to delete folder: " + e.getMessage());
        }
    }

    @Override
    public boolean renameFolder(String folderPath, String newName, String username) {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);
            
            String parentPath = "";
            if (normalizedFolderPath.contains("/")) {
                parentPath = normalizedFolderPath.substring(0, normalizedFolderPath.lastIndexOf("/", normalizedFolderPath.length() - 2) + 1);
            }
            
            String newFolderPath = parentPath + newName + "/";
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedFolderPath)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                String relativePath = s3Object.key().substring(normalizedFolderPath.length());
                String newKey = newFolderPath + relativePath;
                
                CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucketName)
                        .sourceKey(s3Object.key())
                        .destinationBucket(bucketName)
                        .destinationKey(newKey)
                        .build();
                
                s3Client.copyObject(copyRequest);
                
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();
                
                s3Client.deleteObject(deleteRequest);
            }
            
            loggingService.logOperation(
                    username,
                    "RENAME_FOLDER",
                    normalizedFolderPath,
                    newFolderPath,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to rename folder: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "RENAME_FOLDER",
                    folderPath,
                    newName,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to rename folder: " + e.getMessage());
        }
    }

    @Override
    public boolean moveFolder(String sourcePath, String destinationPath, String username) {
        try {
            String normalizedSourcePath = normalizeFolderPath(sourcePath);
            String normalizedDestPath = normalizeFolderPath(destinationPath);
            
            String folderName = "";
            if (normalizedSourcePath.endsWith("/")) {
                folderName = normalizedSourcePath.substring(
                        normalizedSourcePath.lastIndexOf("/", normalizedSourcePath.length() - 2) + 1,
                        normalizedSourcePath.length() - 1
                );
            } else {
                folderName = normalizedSourcePath.substring(normalizedSourcePath.lastIndexOf("/") + 1);
            }
            
            String newFolderPath = normalizedDestPath + folderName + "/";
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedSourcePath)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                String relativePath = s3Object.key().substring(normalizedSourcePath.length());
                String newKey = newFolderPath + relativePath;
                
                CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                        .sourceBucket(bucketName)
                        .sourceKey(s3Object.key())
                        .destinationBucket(bucketName)
                        .destinationKey(newKey)
                        .build();
                
                s3Client.copyObject(copyRequest);
                
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();
                
                s3Client.deleteObject(deleteRequest);
            }
            
            loggingService.logOperation(
                    username,
                    "MOVE_FOLDER",
                    normalizedSourcePath,
                    newFolderPath,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to move folder: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "MOVE_FOLDER",
                    sourcePath,
                    destinationPath,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to move folder: " + e.getMessage());
        }
    }

    @Override
    public List<FileDto> listFolderContents(String folderPath) {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedFolderPath)
                    .delimiter("/")
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            List<FileDto> contents = new ArrayList<>();
            
            for (CommonPrefix commonPrefix : listResponse.commonPrefixes()) {
                String folderName = commonPrefix.prefix();
                folderName = folderName.substring(normalizedFolderPath.length(), folderName.length() - 1);
                
                if (!folderName.isEmpty()) {
                    contents.add(FileDto.builder()
                            .name(folderName)
                            .path(commonPrefix.prefix())
                            .size(0)
                            .lastModified(LocalDateTime.now()) 
                            .type("FOLDER")
                            .build());
                }
            }
            
            // Add files
            for (S3Object s3Object : listResponse.contents()) {
                if (!s3Object.key().equals(normalizedFolderPath)) {
                    String fileName = s3Object.key().substring(normalizedFolderPath.length());
                    
                    if (!fileName.contains("/") && !fileName.isEmpty()) {
                        contents.add(FileDto.builder()
                                .name(fileName)
                                .path(s3Object.key())
                                .size(s3Object.size())
                                .lastModified(LocalDateTime.ofInstant(s3Object.lastModified(), java.time.ZoneId.systemDefault()))
                                .type("FILE")
                                .contentType(getContentType(fileName))
                                .build());
                    }
                }
            }
            
            return contents;
        } catch (S3Exception e) {
            log.error("Failed to list folder contents: {}", e.getMessage());
            throw new FileStorageException("Failed to list folder contents: " + e.getMessage());
        }
    }

    @Override
    public FolderDto getFolderHierarchy(String folderPath) {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);
            
            String folderName = "";
            if (normalizedFolderPath.equals("")) {
                folderName = "root";
            } else if (normalizedFolderPath.endsWith("/")) {
                int lastSlashBeforeLast = normalizedFolderPath.lastIndexOf("/", normalizedFolderPath.length() - 2);
                if (lastSlashBeforeLast >= 0) {
                    folderName = normalizedFolderPath.substring(lastSlashBeforeLast + 1, normalizedFolderPath.length() - 1);
                } else {
                    folderName = normalizedFolderPath.substring(0, normalizedFolderPath.length() - 1);
                }
            } else {
                folderName = normalizedFolderPath.substring(normalizedFolderPath.lastIndexOf("/") + 1);
            }
            
            FolderDto folderDto = FolderDto.builder()
                    .name(folderName)
                    .path(normalizedFolderPath)
                    .files(new ArrayList<>())
                    .subFolders(new ArrayList<>())
                    .build();
            
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(normalizedFolderPath)
                    .build();
            
            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);
            
            Map<String, FolderDto> folderMap = new HashMap<>();
            folderMap.put(normalizedFolderPath, folderDto);
            
            for (S3Object s3Object : listObjectsResponse.contents()) {
                if (s3Object.key().equals(normalizedFolderPath)) {
                    continue;
                }
                
                String relativePath = s3Object.key().substring(normalizedFolderPath.length());
                String[] pathParts = relativePath.split("/");
                
                boolean isFolder = s3Object.key().endsWith("/");
                
                String currentPath = normalizedFolderPath;
                FolderDto currentFolder = folderDto;
                
                for (int i = 0; i < pathParts.length; i++) {
                    String part = pathParts[i];
                    
                    if (part.isEmpty()) {
                        continue;
                    }
                    
                    if (currentPath.endsWith("/")) {
                        currentPath += part;
                    } else {
                        currentPath += "/" + part;
                    }
                    
                    if (i < pathParts.length - 1 || isFolder) {
                        folderPath = currentPath + "/";
                        
                        if (!folderMap.containsKey(folderPath)) {
                            FolderDto newFolder = FolderDto.builder()
                                    .name(part)
                                    .path(folderPath)
                                    .files(new ArrayList<>())
                                    .subFolders(new ArrayList<>())
                                    .build();
                            
                            currentFolder.getSubFolders().add(newFolder);
                            folderMap.put(folderPath, newFolder);
                        }
                        
                        currentFolder = folderMap.get(folderPath);
                    } 
                    else {
                        FileDto fileDto = FileDto.builder()
                                .name(part)
                                .path(s3Object.key())
                                .size(s3Object.size())
                                .lastModified(LocalDateTime.ofInstant(s3Object.lastModified(), java.time.ZoneId.systemDefault()))
                                .type("FILE")
                                .contentType(getContentType(part))
                                .build();
                        
                        currentFolder.getFiles().add(fileDto);
                    }
                }
            }
            
            return folderDto;
        } catch (S3Exception e) {
            log.error("Failed to get folder hierarchy: {}", e.getMessage());
            throw new FileStorageException("Failed to get folder hierarchy: " + e.getMessage());
        }
    }

    private String normalizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isEmpty() || folderPath.equals("/")) {
            return "";
        }
        
        String normalizedPath = folderPath.startsWith("/") ? folderPath.substring(1) : folderPath;
        normalizedPath = normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
        
        return normalizedPath;
    }
    
    private String getContentType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls":
                return "application/vnd.ms-excel";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc":
                return "application/msword";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }
}