package com.fileManagement.FileManagement.service;

import java.util.List;

import com.fileManagement.FileManagement.dto.FileDto;
import com.fileManagement.FileManagement.dto.FolderDto;

public interface FolderService {
    boolean createFolder(String folderPath, String username);
    boolean deleteFolder(String folderPath, String username);
    boolean renameFolder(String folderPath, String newName, String username);
    boolean moveFolder(String sourcePath, String destinationPath, String username);
    List<FileDto> listFolderContents(String folderPath);
    FolderDto getFolderHierarchy(String folderPath);
}
