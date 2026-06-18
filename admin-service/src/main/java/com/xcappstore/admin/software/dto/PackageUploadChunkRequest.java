package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class PackageUploadChunkRequest {
    @NotNull(message = "分片序号不能为空")
    @Min(value = 0, message = "分片序号不能小于0")
    private Integer chunkIndex;

    @NotNull(message = "分片文件不能为空")
    private MultipartFile chunkFile;

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public void setChunk_index(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public MultipartFile getChunkFile() {
        return chunkFile;
    }

    public void setChunkFile(MultipartFile chunkFile) {
        this.chunkFile = chunkFile;
    }

    public void setChunk_file(MultipartFile chunkFile) {
        this.chunkFile = chunkFile;
    }
}
