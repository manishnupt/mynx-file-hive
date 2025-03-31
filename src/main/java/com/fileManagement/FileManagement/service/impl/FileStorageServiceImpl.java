package com.fileManagement.FileManagement.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fileManagement.FileManagement.dto.FileDto;
import com.fileManagement.FileManagement.dto.FileUploadResponse;
import com.fileManagement.FileManagement.exceptions.FileStorageException;
import com.fileManagement.FileManagement.service.FileStorageService;
import com.fileManagement.FileManagement.service.LoggingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final LoggingService loggingService;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Override
    public FileUploadResponse uploadFile(String folderPath, MultipartFile file, String username) {
        try {
            String filePath = normalizeFilePath(folderPath, file.getOriginalFilename());
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            loggingService.logOperation(
                    username,
                    "UPLOAD",
                    filePath,
                    null,
                    "SUCCESS",
                    null
            );

            return FileUploadResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(filePath)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .message("File uploaded successfully")
                    .build();
        } catch (IOException | S3Exception e) {
            log.error("Failed to upload file: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "UPLOAD",
                    folderPath + "/" + file.getOriginalFilename(),
                    null,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadFile(String filePath, String username) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            
            loggingService.logOperation(
                    username,
                    "DOWNLOAD",
                    filePath,
                    null,
                    "SUCCESS",
                    null
            );
            
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            log.error("Failed to download file: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "DOWNLOAD",
                    filePath,
                    null,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to download file: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String filePath, String username) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            
            loggingService.logOperation(
                    username,
                    "DELETE",
                    filePath,
                    null,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete file: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "DELETE",
                    filePath,
                    null,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public boolean renameFile(String filePath, String newName, String username) {
        try {
            String directory = filePath.substring(0, filePath.lastIndexOf('/') + 1);
            String newFilePath = directory + newName;
            
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(filePath)
                    .destinationBucket(bucketName)
                    .destinationKey(newFilePath)
                    .build();
            
            s3Client.copyObject(copyObjectRequest);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            loggingService.logOperation(
                    username,
                    "RENAME",
                    filePath,
                    newFilePath,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to rename file: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "RENAME",
                    filePath,
                    newName,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to rename file: " + e.getMessage());
        }
    }

    @Override
    public boolean moveFile(String sourcePath, String destinationPath, String username) {
        try {
            String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            String newFilePath = normalizeFilePath(destinationPath, fileName);
            
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourcePath)
                    .destinationBucket(bucketName)
                    .destinationKey(newFilePath)
                    .build();
            
            s3Client.copyObject(copyObjectRequest);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(sourcePath)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            loggingService.logOperation(
                    username,
                    "MOVE",
                    sourcePath,
                    newFilePath,
                    "SUCCESS",
                    null
            );
            
            return true;
        } catch (S3Exception e) {
            log.error("Failed to move file: {}", e.getMessage());
            
            loggingService.logOperation(
                    username,
                    "MOVE",
                    sourcePath,
                    destinationPath,
                    "FAILED",
                    e.getMessage()
            );
            
            throw new FileStorageException("Failed to move file: " + e.getMessage());
        }
    }

    @Override
    public List<FileDto> listFiles(String folderPath) {
        try {
            String prefix = folderPath.endsWith("/") ? folderPath : folderPath + "/";
            
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .delimiter("/")
                    .build();
            
            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);
            
            List<FileDto> files = new ArrayList<>();
            
            for (S3Object s3Object : listObjectsResponse.contents()) {
                if (!s3Object.key().equals(prefix)) {
                    String fileName = s3Object.key().substring(s3Object.key().lastIndexOf('/') + 1);
                    
                    if (!fileName.isEmpty()) {
                        files.add(FileDto.builder()
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
            
            return files;
        } catch (S3Exception e) {
            log.error("Failed to list files: {}", e.getMessage());
            throw new FileStorageException("Failed to list files: " + e.getMessage());
        }
    }

    @Override
    public FileDto getFileDetails(String filePath) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            
            return FileDto.builder()
                    .name(fileName)
                    .path(filePath)
                    .size(headObjectResponse.contentLength())
                    .lastModified(LocalDateTime.ofInstant(headObjectResponse.lastModified(), java.time.ZoneId.systemDefault()))
                    .type("FILE")
                    .contentType(headObjectResponse.contentType())
                    .build();
        } catch (S3Exception e) {
            log.error("Failed to get file details: {}", e.getMessage());
            throw new FileStorageException("Failed to get file details: " + e.getMessage());
        }
    }

    private String normalizeFilePath(String folderPath, String fileName) {
        if (folderPath == null || folderPath.isEmpty() || folderPath.equals("/")) {
            return fileName;
        }
        
        String normalizedPath = folderPath.startsWith("/") ? folderPath.substring(1) : folderPath;
        normalizedPath = normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
        
        return normalizedPath + fileName;
    }
    
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
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
