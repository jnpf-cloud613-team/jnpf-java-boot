package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class Chunk implements Serializable {
    /**
     * 当前文件块，从1开始
     */
    @Schema(description = "当前文件块")
    private Integer chunkNumber;
    /**
     * 分块大小
     */
    @Schema(description = "分块大小")
    private Long chunkSize;
    /**
     * 当前分块大小
     */
    @Schema(description = "当前分块大小")
    private Long currentChunkSize;
    /**
     * 总大小
     */
    @Schema(description = "总大小")
    private Long totalSize;
    /**
     * 文件标识
     */
    @Schema(description = "文件标识")
    private String identifier;
    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;
    /**
     * 相对路径
     */
    @Schema(description = "相对路径")
    private String relativePath;
    /**
     * 总块数
     */
    @Schema(description = "总块数")
    private Integer totalChunks;
    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String type;

    private String extension;

    private String fileType;
}
