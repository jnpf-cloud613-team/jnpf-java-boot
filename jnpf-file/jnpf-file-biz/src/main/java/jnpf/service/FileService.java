package jnpf.service;

import jnpf.model.*;
import org.springframework.web.multipart.MultipartFile;


public interface FileService {

    /**
     * 获取上传分类文件夹名称
     * @param type
     * @return
     */
    String getPath(String type);

    /**
     * 获取本地仓储路径
     * @return
     */
    String getLocalBasePath();
    /**
     * 上传文件
     * @param mergeChunkDto
     * @param file
     * @return
     */
    UploaderVO uploadFile(MergeChunkDto mergeChunkDto, MultipartFile file);

    /**
     * 分片上传检查
     */
    ChunkRes checkChunk(Chunk chunk);

    /**
     * 上传分片文件
     * @param chunk
     * @param file
     */
    ChunkRes uploadChunk(Chunk chunk, MultipartFile file);

    /**
     * 合并分片文件
     */
    UploaderVO mergeChunk(MergeChunkDto mergeChunkDto);

    /**
     * 下载文件
     * @param encryption
     * @param downName
     */
    void downloadFile(String encryption, String downName);

    /**
     * 文件是否存在
     * @param path 文件夹路径
     * @param fileName
     * @return
     */
    boolean fileExists(String path, String fileName);

    /**
     * 获取文件预览地址
     * @param previewParams
     * @return
     */
    String previewFile(PreviewParams previewParams);

    /**
     * 输出文件流
     * @param type 图片分类
     * @param fileName 文件名
     * @param securityKey
     * @param redirect
     */
    void flushFile(String type, String fileName, String securityKey, boolean redirect);

}
