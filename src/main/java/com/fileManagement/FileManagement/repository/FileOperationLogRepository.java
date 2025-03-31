package com.fileManagement.FileManagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fileManagement.FileManagement.entity.FileOperationLog;

@Repository
public interface FileOperationLogRepository extends JpaRepository<FileOperationLog, Long> {
    List<FileOperationLog> findByFilePathContainingOrderByTimestampDesc(String filePath);
    List<FileOperationLog> findByUsernameOrderByTimestampDesc(String username);
}
