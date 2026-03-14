package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.FileParameter;
import jnpf.enums.FilePreviewTypeEnum;
import jnpf.model.FileListVO;
import jnpf.model.YozoFileParams;
import jnpf.model.YozoParams;
import jnpf.util.*;
import jnpf.utils.SplicingUrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档在线预览
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@NoDataSourceBind()
@Tag(name = "文档在线预览", description = "DocumentPreview")
@RestController
@RequestMapping("/api/extend/DocumentPreview")
@RequiredArgsConstructor
public class DocumentPreviewController {

    private final YozoParams yozoParams;

    /**
     * 永中文件预览
     *
     * @param fileId      文件主键
     * @param params      永中模型
     * @param previewType 类型
     * @return
     */
    @Operation(summary = "文件预览")
    @GetMapping("/{fileId}/Preview")
    @Parameter(name = "fileId", description = "文件主键", required = true)
    @Parameter(name = "previewType", description = "类型")
    @SaCheckPermission("extend.documentPreview")
    public ActionResult<Object> filePreview(@PathVariable("fileId") String fileId, YozoFileParams params, @RequestParam("previewType") String previewType) {
        List<FileListVO> fileList = FileUploadUtils.getFileList(new FileParameter().setRemotePath(FileTypeConstant.DOCUMENTPREVIEW).setRecursive(true));
        if (fileList.isEmpty() || Integer.parseInt(fileId) >= fileList.size()) {
            return ActionResult.fail(MsgCode.ETD111.get());
        }
        FileListVO fileListVO = fileList.get(Integer.parseInt(fileId));
        String fileName = fileListVO.getFileName();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        String url = yozoParams.getJnpfDomain() + "/api/file/Image/" + FileTypeConstant.DOCUMENTPREVIEW + "/" + fileName + "?fullfilename=" + fileName + "&s=" + UserProvider.getUser().getSecurityKey();
        String urlPath;
        if (previewType.equals(FilePreviewTypeEnum.YOZO_ONLINE_PREVIEW.getType())) {
            params.setUrl(url);
            urlPath = SplicingUrlUtil.getPreviewUrl(params);
            return ActionResult.success("success", XSSEscape.escape(urlPath));
        }
        return ActionResult.success("success", url);
    }

    /**
     * 列表
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "获取文档列表")
    @GetMapping
    @SaCheckPermission("extend.documentPreview")
    public ActionResult<List<FileListVO>> list(Page page) {
        List<FileListVO> fileList = FileUploadUtils.getFileList(new FileParameter().setRemotePath(FileTypeConstant.DOCUMENTPREVIEW).setRecursive(true));
        fileList.stream().forEach(t -> {
            if (t.getFileName() != null) {
                String[] split = t.getFileName().split("/");
                if (split.length > 0) {
                    t.setFileName(split[split.length - 1]);
                }
            }
        });
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            fileList = fileList.stream().filter(t -> t.getFileName().contains(page.getKeyword())).collect(Collectors.toList());
        }
        return ActionResult.success(fileList);
    }

}
