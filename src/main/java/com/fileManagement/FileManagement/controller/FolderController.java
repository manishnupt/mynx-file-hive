package com.fileManagement.FileManagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fileManagement.FileManagement.dto.FileDto;
import com.fileManagement.FileManagement.dto.FileOperationRequest;
import com.fileManagement.FileManagement.dto.FolderDto;
import com.fileManagement.FileManagement.service.FolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<Void> createFolder(
            @Valid @RequestBody FileOperationRequest request) {
        
        boolean result = folderService.createFolder(request.getPath(), request.getUsername());
        return result ? ResponseEntity.status(HttpStatus.CREATED).build() : ResponseEntity.badRequest().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFolder(
            @Valid @RequestBody FileOperationRequest request) {
        
        boolean result = folderService.deleteFolder(request.getPath(), request.getUsername());
        return result ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/rename")
    public ResponseEntity<Void> renameFolder(
            @Valid @RequestBody FileOperationRequest request) {
        
        if (request.getNewName() == null || request.getNewName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean result = folderService.renameFolder(request.getPath(), request.getNewName(), request.getUsername());
        return result ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/move")
    public ResponseEntity<Void> moveFolder(
            @Valid @RequestBody FileOperationRequest request) {
        
        if (request.getDestinationPath() == null || request.getDestinationPath().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean result = folderService.moveFolder(request.getPath(), request.getDestinationPath(), request.getUsername());
        return result ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileDto>> listFolderContents(
            @RequestParam("path") String folderPath) {
        
        List<FileDto> contents = folderService.listFolderContents(folderPath);
        return ResponseEntity.ok(contents);
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<FolderDto> getFolderHierarchy(
            @RequestParam("path") String folderPath) {
        
        FolderDto hierarchy = folderService.getFolderHierarchy(folderPath);
        return ResponseEntity.ok(hierarchy);
    }
}
