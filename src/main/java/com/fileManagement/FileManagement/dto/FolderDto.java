package com.fileManagement.FileManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderDto {
    private String name;
    private String path;
    private List<FileDto> files;
    private List<FolderDto> subFolders;
}
