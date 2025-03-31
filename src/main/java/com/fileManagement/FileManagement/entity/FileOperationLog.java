package com.fileManagement.FileManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_operation_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private String filePath;

    private String destinationPath;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String status;

    @Column(length = 1000)
    private String errorMessage;
}