package jnpf.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jnpf.base.ActionResult;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.vo.DownloadVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.model.*;
import jnpf.service.FileService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * 通用控制器
 *
 * @author JNPF开发平台组
 * @version V1.2.191207
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Slf4j
@Tag(name = "公共", description = "file")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class UtilsController {


    private final ConfigValueUtil configValueUtil;

    private final RedisUtil redisUtil;

    private final DictionaryDataService dictionaryDataService;

    private final FileService fileService;



    /**
     * 图形验证码
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "图形验证码")
    @GetMapping("/ImageCode/{timestamp}")
    @Parameter(name = "timestamp", description = "时间戳", required = true)
    public void imageCode(@PathVariable("timestamp") String timestamp) {
        DownUtil.downCode(null);
        redisUtil.insert(timestamp, ServletUtil.getSession().getAttribute(CodeUtil.RANDOMCODEKEY), 120);
    }

    /**
     * 获取全部下载文件链接（打包下载）
     *
     * @return
     */
    @NoDataSourceBind
    @Operation(summary = "获取全部下载文件链接（打包下载）")
    @PostMapping("/PackDownload/{type}")
    public ActionResult<Object> packDownloadUrl(@PathVariable("type") String type, @RequestBody List<Map<String, String>> fileInfoList) throws IOException {
        type = XSSEscape.escape(type);
        if (fileInfoList == null || fileInfoList.isEmpty()) {
            return ActionResult.fail(MsgCode.FA047.get());
        }

        StringBuilder zipTempFilePath = null;
        String zipFileId = RandomUtil.uuId() + ".zip";
        List<String> repeatName = new ArrayList<>();
        for (Map<String, String> fileInfoMap : fileInfoList) {
            String fileId = XSSEscape.escape( fileInfoMap.get("fileId")).trim();
            String fileName = XSSEscape.escape( fileInfoMap.get("fileName")).trim();
            if (repeatName.contains(fileName)) {
                fileName = fileName.substring(0, fileName.lastIndexOf(".")) + "副本" + UUID.randomUUID().toString().substring(0, 5) + fileName.substring(fileName.lastIndexOf("."));
            } else {
                repeatName.add(fileName);
            }
            if (StringUtil.isEmpty(fileId) || StringUtil.isEmpty(fileName)) {
                continue;
            }
            FileParameter fileParameter = new FileParameter(type, fileId);
            if (FileUploadUtils.exists(fileParameter)) {
                if (zipTempFilePath == null) {
                    zipTempFilePath = new StringBuilder(FileUploadUtils.getLocalBasePath() + FilePathUtil.getFilePath(FileTypeConstant.FILEZIPDOWNTEMPPATH));
                    if (!new File(zipTempFilePath.toString()).exists()) {
                        new File(zipTempFilePath.toString()).mkdirs();
                    }
                    zipTempFilePath = zipTempFilePath.append(zipFileId);
                }
                String finalZipTempFilePath = String.valueOf(zipTempFilePath);
                String finalFileName = fileName;
                FileUploadUtils.downloadFile(fileParameter, inputStream -> {
                    try {
                        ZipUtil.fileAddToZip(finalZipTempFilePath, inputStream, finalFileName);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                });
            }
        }
        //将文件上传到默认文件服务器
        String newFileId = zipFileId;
        if (!"local".equals(FileUploadUtils.getDefaultPlatform())) { //不是本地，说明是其他文件服务器，将zip文件上传到其他服务器里，方便下载
            File zipFile = new File(String.valueOf(zipTempFilePath));
            FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(FilePathUtil.getFilePath(FileTypeConstant.FILEZIPDOWNTEMPPATH), zipFileId), zipFile);
            Files.delete(zipFile.toPath());
            FileUtils.deleteQuietly(zipFile);
            newFileId = fileInfo.getFilename();
        }
        jnpf.base.vo.DownloadVO vo = DownloadVO.builder().name(zipFileId).url(UploaderUtil.uploaderFile(newFileId + "#" + FileTypeConstant.FILEZIPDOWNTEMPPATH)).build();
        Map<String, Object> map = new HashMap<>();
        map.put("downloadVo", vo);
        map.put("downloadName", "文件" + zipFileId);
        return ActionResult.success(map);
    }

    /**
     * 上传文件/图片
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "上传文件/图片")
    @PostMapping(value = "/Uploader/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Parameter(name = "type", description = "类型", required = true)
    public ActionResult<UploaderVO> uploader(@PathVariable("type") String type, MultipartFile file, MergeChunkDto mergeChunkDto, HttpServletRequest httpServletRequest) {
        mergeChunkDto.setType(type);
        return ActionResult.success(fileService.uploadFile(mergeChunkDto, file));
    }


    /**
     * 图片转成base64
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "图片转成base64")
    @PostMapping(value = "/Uploader/imgToBase64", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> imgToBase64(@RequestParam("file") MultipartFile file) throws IOException {
        String encode = cn.hutool.core.codec.Base64.encode(file.getBytes());
        Object base64Name = "";
        if (StringUtil.isNotEmpty(encode)) {
            base64Name = "data:image/jpeg;base64,"+encode;
        }
        return ActionResult.success(base64Name);
    }


    /**
     * 获取下载文件链接
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "获取下载文件链接")
    @GetMapping("/Download/{type}/{fileName}")
    @Parameter(name = "type", description = "类型", required = true)
    @Parameter(name = "fileName", description = "文件名称", required = true)
    public ActionResult<Object> downloadUrl(@PathVariable("type") String type, @PathVariable("fileName") String fileName) {
        boolean exists = FileUploadUtils.exists(new FileParameter(type, fileName));
        if (exists) {
            DownloadVO vo = DownloadVO.builder().name(fileName).url(UploaderUtil.uploaderFile(fileName + "#" + type)).build();
            return ActionResult.success(vo);
        }
        return ActionResult.fail(MsgCode.FA018.get());
    }

    /**
     * 下载文件链接
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "下载文件链接")
    @GetMapping("/Download")
    public void downloadFile(@RequestParam("encryption") String encryption, @RequestParam("name") String downName) throws DataException {
        fileService.downloadFile(encryption, downName);
    }



    /**
     * 获取图片
     *
     * @param fileName
     * @param type
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "获取图片")
    @GetMapping("/Image/{type}/{fileName}")
    @Parameter(name = "type", description = "类型", required = true)
    @Parameter(name = "fileName", description = "名称", required = true)
    public void downLoadImg(@PathVariable("type") String type, @PathVariable("fileName") String fileName, @RequestParam(name = "s", required = false) String securityKey, @RequestParam(name = "t", required = false) String t) throws DataException {
        fileService.flushFile(type, fileName, securityKey, StringUtil.isEmpty(t));
    }


    /**
     * app启动获取信息
     *
     * @param appName
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "app启动获取信息")
    @GetMapping("/AppStartInfo/{appName}")
    @Parameter(name = "appName", description = "名称", required = true)
    public ActionResult<Object> getAppStartInfo(@PathVariable("appName") String appName) {
        JSONObject object = new JSONObject();
        object.put("AppVersion", configValueUtil.getAppVersion());
        object.put("AppUpdateContent", configValueUtil.getAppUpdateContent());
        return ActionResult.success(object);
    }

    //----------大屏图片下载---------
    @NoDataSourceBind()
    @Operation(summary = "获取图片")
    @GetMapping("/VisusalImg/{bivisualpath}/{type}/{fileName}")
    @Parameter(name = "type", description = "类型", required = true)
    @Parameter(name = "bivisualpath", description = "路径", required = true)
    @Parameter(name = "fileName", description = "名称", required = true)
    public void downVisusalImg(@PathVariable("type") String type, @PathVariable("bivisualpath") String bivisualpath, @PathVariable("fileName") String fileName) {
        fileName = XSSEscape.escape(fileName);
        String filePath = configValueUtil.getBiVisualPath();
        String finalFileName = fileName;
        FileUploadUtils.downloadFile(new FileParameter(filePath + type + "/", fileName), inputStream ->
            FileDownloadUtil.flushFile(inputStream, finalFileName));
    }

    //----------------------

    @NoDataSourceBind()
    @Operation(summary = "预览文件")
    @GetMapping("/Uploader/Preview")
    public ActionResult<Object> preview(PreviewParams previewParams) {
        return ActionResult.success(MsgCode.SU000.get(), fileService.previewFile(previewParams));
    }


    @Operation(summary = "分片上传获取")
    @GetMapping("/chunk")
    public ActionResult<ChunkRes> checkChunk(Chunk chunk) {
        return ActionResult.success(fileService.checkChunk(chunk));
    }


    @Operation(summary = "分片上传附件")
    @PostMapping("/chunk")
    public ActionResult<ChunkRes> upload(Chunk chunk, MultipartFile file) {
        return ActionResult.success(fileService.uploadChunk(chunk, file));
    }

    @Operation(summary = "分片组装")
    @PostMapping("/merge")
    public ActionResult<UploaderVO> merge(MergeChunkDto mergeChunkDto) {
        return ActionResult.success(fileService.mergeChunk(mergeChunkDto));
    }

}
