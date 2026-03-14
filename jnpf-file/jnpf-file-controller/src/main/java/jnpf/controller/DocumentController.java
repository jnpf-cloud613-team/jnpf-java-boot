package jnpf.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.controller.SuperController;
import jnpf.base.util.OptimizeUtil;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.DocumentEntity;
import jnpf.entity.DocumentLogEntity;
import jnpf.entity.DocumentShareEntity;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.extend.service.DocumentApi;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.model.task.FileModel;
import jnpf.model.MergeChunkDto;
import jnpf.model.UploaderVO;
import jnpf.model.document.*;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.service.DocumentLogService;
import jnpf.service.DocumentService;
import jnpf.service.FileService;
import jnpf.util.*;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import jnpf.workflow.service.WorkFlowApi;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档管理
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Slf4j
@Tag(name = "知识管理", description = "Document")
@RestController
@RequestMapping("/api/file/Document")
@RequiredArgsConstructor
public class DocumentController extends SuperController<DocumentService, DocumentEntity> implements DocumentApi {


    private final DocumentService documentService;

    private final ConfigValueUtil configValueUtil;

    private final UserService userService;


    private final DocumentLogService documentLogService;

    private final WorkFlowApi workFlowApi;

    private final FileService fileService;

    /**
     * 列表
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "列表")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<DocumentInfoVO> info(@PathVariable("id") String id) throws DataException {
        DocumentEntity entity = documentService.getInfo(id);
        if (!Objects.equals(entity.getCreatorUserId(), UserProvider.getLoginUserId())) {
            return ActionResult.fail(MsgCode.AD104.get());
        }
        DocumentInfoVO vo = JsonUtil.getJsonToBean(entity, DocumentInfoVO.class);
        //截取后缀
        if (Objects.equals(vo.getType(), 1) && vo.getFullName().contains(".")) {
            vo.setFullName(vo.getFullName().substring(0, vo.getFullName().lastIndexOf(".")));
        }
        return ActionResult.success(vo);
    }

    /**
     * 新建
     *
     * @param documentCrForm 新建模型
     * @return
     */
    @Operation(summary = "新建")
    @PostMapping
    @Parameter(name = "documentCrForm", description = "知识模型", required = true)
    public ActionResult<Object> create(@RequestBody @Valid DocumentCrForm documentCrForm) {
        DocumentEntity entity = JsonUtil.getJsonToBean(documentCrForm, DocumentEntity.class);
        if (documentService.isExistByFullName(documentCrForm.getFullName(), entity.getId(), documentCrForm.getParentId())) {
            return ActionResult.fail(MsgCode.EXIST004.get());
        }
        entity.setEnabledMark(1);
        documentService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @param id             主键
     * @param documentUpForm 修改模型
     * @return
     */
    @Operation(summary = "修改")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "documentUpForm", description = "知识模型", required = true)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid DocumentUpForm documentUpForm) {
        DocumentEntity entity = JsonUtil.getJsonToBean(documentUpForm, DocumentEntity.class);
        if (documentService.isExistByFullName(documentUpForm.getFullName(), id, documentUpForm.getParentId())) {
            return ActionResult.fail(MsgCode.EXIST004.get());
        }
        DocumentEntity info = documentService.getInfo(id);
        //获取后缀名
        if (Objects.equals(info.getType(), 1) && StringUtil.isNotEmpty(info.getFileExtension())) {
            entity.setFullName(entity.getFullName() + StrPool.DOT + info.getFileExtension());
        }
        if (!Objects.equals(info.getCreatorUserId(), UserProvider.getLoginUserId())) {
            return ActionResult.fail(MsgCode.AD104.get());
        }

        boolean flag = documentService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        DocumentEntity entity = documentService.getInfo(id);
        if (entity != null) {
            List<DocumentEntity> allList = documentService.getAllList(entity.getId());
            if (!allList.isEmpty()) {
                return ActionResult.fail(MsgCode.FA016.get());
            }
            if (!Objects.equals(entity.getCreatorUserId(), UserProvider.getLoginUserId())) {
                return ActionResult.fail(MsgCode.AD104.get());
            }
            documentService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 列表
     *
     * @return
     */
    @Operation(summary = "获取知识管理列表（文件夹树）")
    @PostMapping("/FolderTree")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<ListVO<DocumentFolderTreeVO>> folderTree(@RequestBody DocumentShareForm form) {
        List<DocumentEntity> data = documentService.getFolderList();
        if (StringUtil.isNotEmpty(form.getIds())) {
            form.getIds().forEach(t -> data.remove(documentService.getInfo(t)));
        }
        List<DocumentFolderTreeModel> treeList = new ArrayList<>();
        DocumentFolderTreeModel model = new DocumentFolderTreeModel();
        model.setId("-1");
        model.setFullName("全部文档");
        model.setParentId("0");
        model.setIcon("0");
        treeList.add(model);
        for (DocumentEntity entity : data) {
            DocumentFolderTreeModel treeModel = new DocumentFolderTreeModel();
            treeModel.setId(entity.getId());
            treeModel.setFullName(entity.getFullName());
            treeModel.setParentId(entity.getParentId());
            treeModel.setIcon("fa fa-folder");
            treeList.add(treeModel);
        }
        List<SumTree<DocumentFolderTreeModel>> trees = TreeDotUtils.convertListToTreeDotFilter(treeList);
        List<DocumentFolderTreeVO> listVO = JsonUtil.getJsonToList(trees, DocumentFolderTreeVO.class);
        ListVO<DocumentFolderTreeVO> vo = new ListVO<>();
        vo.setList(listVO);
        return ActionResult.success(vo);
    }

    /**
     * 列表（全部文档）
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "获取知识管理列表（全部文档）")
    @GetMapping
    public ActionResult<ListVO<DocumentListVO>> allList(PageDocument page) {
        List<DocumentEntity> data = documentService.getAllList(page.getParentId());
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            data = documentService.getSearchAllList(page.getKeyword());
        }
        List<DocumentListVO> list = JsonUtil.getJsonToList(data, DocumentListVO.class);
        //读取允许文件预览类型
        String allowPreviewType = configValueUtil.getAllowPreviewFileType();
        String[] fileType = allowPreviewType.split(",");
        for (DocumentListVO documentListVO : list) {
            //文件预览类型检验
            String s = Arrays.stream(fileType).filter(type -> type.equals(documentListVO.getFileExtension())).findFirst().orElse(null);
            documentListVO.setIsPreview(s);
        }

        ListVO<DocumentListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 列表（我的分享）
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "知识管理（我的共享列表）")
    @GetMapping("/Share")
    public ActionResult<ListVO<DocumentListVO>> shareOutList(PageDocument page) {
        List<DocumentEntity> data = documentService.getShareOutList();
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            List<DocumentEntity> dataSearch = new ArrayList<>();
            for (DocumentEntity datum : data) {
                if (Objects.equals(datum.getType(), 0)) {
                    List<DocumentEntity> childList = new ArrayList<>();
                    documentService.getChildSrcList(datum.getId(), childList, 1);
                    List<DocumentEntity> collect = childList.stream().filter(t -> !Objects.equals(t.getType(), 0)).collect(Collectors.toList());
                    dataSearch.addAll(collect);
                } else {
                    dataSearch.add(datum);
                }

            }
            data = dataSearch.stream().distinct().filter(t -> t.getFullName().contains(page.getKeyword())).collect(Collectors.toList());
        } else if (StringUtil.isNotEmpty(page.getParentId()) && !"0".equals(page.getParentId())) {
            data = documentService.getAllList(page.getParentId());
        }
        List<DocumentListVO> list = JsonUtil.getJsonToList(data, DocumentListVO.class);
        ListVO<DocumentListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 列表（共享给我）
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "获取知识管理列表（共享给我）")
    @GetMapping("/ShareTome")
    public ActionResult<Object> shareTomeList(PageDocument page) {
        List<DocumentShareEntity> shareTomeList = documentService.getShareTomeList();
        List<String> ids = shareTomeList.stream().map(DocumentShareEntity::getDocumentId).collect(Collectors.toList());
        List<DocumentEntity> list = documentService.getInfoByIds(ids);

        List<String> userIds = list.stream().map(DocumentEntity::getCreatorUserId).collect(Collectors.toList());
        List<UserEntity> userNames = userService.getUserName(userIds);
        List<DocumentListVO> dataRes = new ArrayList<>();
        if (StringUtil.isNotEmpty(page.getParentId()) && !"0".equals(page.getParentId())) {
            List<DocumentListVO> listVOS = documentService.getChildListUserName(page.getParentId(), true);
            DocumentShareEntity documentShareEntity = documentService.getShareByParentId(page.getParentId());
            for (DocumentListVO item : listVOS) {
                if (documentShareEntity != null) {
                    item.setShareTime(documentShareEntity.getShareTime());
                    UserEntity userEntity = userNames.stream().filter(t -> t.getId().equals(documentShareEntity.getCreatorUserId())).findFirst().orElse(null);
                    item.setCreatorUserId(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
                }
                String creatorUserName = item.getCreatorUserName();
                String creatorUserAccount = item.getCreatorUserAccount();
                item.setCreatorUserId(creatorUserName + "/" + creatorUserAccount);
                dataRes.add(item);
            }
        } else {
            for (DocumentEntity datum : list) {
                if (StringUtil.isNotEmpty(page.getKeyword())) {
                    DocumentShareEntity documentShareEntity = shareTomeList.stream().filter(t -> t.getDocumentId().equals(datum.getId())).findFirst().orElse(null);
                    if (documentShareEntity != null && Objects.equals(datum.getType(), 0)) {

                        List<DocumentEntity> childList = new ArrayList<>();
                        documentService.getChildSrcList(datum.getId(), childList, 1);
                        for (DocumentEntity item : childList) {
                            DocumentListVO documentListVO = BeanUtil.copyProperties(item, DocumentListVO.class);
                            if (item.getFullName().contains(page.getKeyword()) && Objects.equals(item.getEnabledMark(), 1) && !Objects.equals(item.getType(), 0)) {
                                documentListVO.setShareTime(documentShareEntity.getShareTime());
                                UserEntity userEntity = userNames.stream().filter(t -> t.getId().equals(documentShareEntity.getCreatorUserId())).findFirst().orElse(null);
                                documentListVO.setCreatorUserId(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
                                dataRes.add(documentListVO);
                            }
                        }
                    }
                } else {
                    DocumentShareEntity documentShareEntity = shareTomeList.stream().filter(t -> t.getDocumentId().equals(datum.getId())).findFirst().orElse(null);
                    if (documentShareEntity != null) {
                        DocumentListVO documentListVO = BeanUtil.copyProperties(datum, DocumentListVO.class);
                        documentListVO.setShareTime(documentShareEntity.getShareTime());
                        UserEntity userEntity = userNames.stream().filter(t -> t.getId().equals(documentShareEntity.getCreatorUserId())).findFirst().orElse(null);
                        documentListVO.setCreatorUserId(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
                        dataRes.add(documentListVO);
                    }
                }
            }
        }

        ListVO<DocumentListVO> vo = new ListVO<>();
        vo.setList(dataRes);
        return ActionResult.success(vo);
    }

    /**
     * 列表（回收站）
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "获取知识管理列表（回收站）")
    @GetMapping("/Trash")
    public ActionResult<ListVO<DocumentTrashListVO>> trashList(Page page) {
        List<DocumentTrashListVO> data = documentService.getTrashList(page.getKeyword());
        ListVO<DocumentTrashListVO> vo = new ListVO<>();
        vo.setList(data);
        return ActionResult.success(vo);
    }

    /**
     * 列表（共享人员）
     *
     * @param documentId 文档主键
     * @return
     */
    @Operation(summary = "获取知识管理列表（共享人员）")
    @GetMapping("/ShareUser/{documentId}")
    @Parameter(name = "documentId", description = "文档主键", required = true)
    public ActionResult<ListVO<DocumentSuserListVO>> shareUserList(@PathVariable("documentId") String documentId) {
        List<DocumentShareEntity> data = documentService.getShareUserList(documentId);
        List<DocumentSuserListVO> list = JsonUtil.getJsonToList(data, DocumentSuserListVO.class);
        ListVO<DocumentSuserListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * app上传文件
     *
     * @param documentUploader 上传模型
     * @return
     */
    @Operation(summary = "知识管理上传文件")
    @PostMapping("/Uploader")
    public ActionResult<Object> uploader(DocumentUploader documentUploader) throws DataException {
        String fileType = UpUtil.getFileType(documentUploader.getFile());
        //验证类型
        if (!OptimizeUtil.fileType(configValueUtil.getAllowUploadFileType(), fileType)) {
            return ActionResult.fail(MsgCode.FA017.get());
        }

        //上传
        uploaderVO(documentUploader);
        return ActionResult.success(MsgCode.SU015.get());
    }

    /**
     * 分片组装
     *
     * @param mergeChunkDto 合并模型
     * @return
     */
    @Operation(summary = "分片组装")
    @PostMapping("/merge")
    public ActionResult<Object> merge(MergeChunkDto mergeChunkDto) {
        String identifier = XSSEscape.escapePath(mergeChunkDto.getIdentifier());
        String path = FileUploadUtils.getLocalBasePath() + configValueUtil.getTemporaryFilePath();
        String filePath = XSSEscape.escapePath(path + identifier);
        String partFile = XSSEscape.escapePath(path + mergeChunkDto.getFileName());
        try {
            @Cleanup FileOutputStream destTempfos = new FileOutputStream(partFile, true);
            List<File> mergeFileList = FileUtil.getFile(new File(filePath));

            for (int i = 0; i < mergeFileList.size(); i++) {
                String chunkName = identifier.concat("-") + (i + 1);
                File files = new File(filePath, chunkName);
                if (files.exists()) {
                    FileUtils.copyFile(files, destTempfos);
                }
            }
            File partFiles = new File(partFile);
            if (partFiles.exists()) {
                MultipartFile multipartFile = FileUtil.createFileItem(partFiles);
                uploaderVO(new DocumentUploader(multipartFile, mergeChunkDto.getParentId()));
                FileUtil.deleteTmp(multipartFile);
            }
            FileUtils.deleteQuietly(new File(filePath));
            FileUtils.deleteQuietly(new File(partFile));
        } catch (Exception e) {
            log.error("合并分片失败: {}", e.getMessage());
            throw new DataException(MsgCode.FA033.get());
        }
        return ActionResult.success(MsgCode.SU015.get());
    }

    @Operation(summary = "流程归档文件上传接口")
    @PostMapping("/UploadBlob")
    public ActionResult<Object> uploadBlob(DocumentUploader dub) throws DataException {
        uploaderVO(dub);
        workFlowApi.updateIsFile(dub.getTaskId());
        return ActionResult.success(MsgCode.SU015.get());
    }

    /**
     * 获取下载文件链接
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取下载文件链接")
    @PostMapping("/Download/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object> download(@PathVariable("id") String id) {
        DocumentEntity entity = documentService.getInfo(id);
        if (entity != null) {
            String name = entity.getFilePath();
            String fileName = name + "#" + FileTypeConstant.DOCUMENT + "#" + entity.getFullName() + "." + entity.getFileExtension();
            DownloadVO vo = DownloadVO.builder().name(entity.getFullName()).url(UploaderUtil.uploaderFile(fileName)).build();
            return ActionResult.success(vo);
        }
        return ActionResult.fail(MsgCode.FA018.get());
    }

    /**
     * 获取全部下载文件链接（打包下载）
     *
     * @return
     */
    @Operation(summary = "打包下载")
    @PostMapping("/PackDownload")
    public ActionResult<Object> packDownloadUrl(@RequestBody DocumentShareForm obj) {
        //单个文件直接下载
        if (obj.getIds().size() == 1) {
            DocumentEntity entity = documentService.getInfo(obj.getIds().get(0));
            if (entity != null && !Objects.equals(entity.getType(), 0)) {
                String name = entity.getFilePath();
                String fileName = name + "#" + FileTypeConstant.DOCUMENT + "#" + entity.getFullName() + "." + entity.getFileExtension();
                DownloadVO vo = DownloadVO.builder().name(entity.getFullName()).url(UploaderUtil.uploaderFile(fileName)).build();
                return ActionResult.success(vo);
            }
        }
        String servicePath = FilePathUtil.getFilePath(FileTypeConstant.FILEZIPDOWNTEMPPATH);
        String tempFilePath = FileUploadUtils.getLocalBasePath() + FilePathUtil.getFilePath(FileTypeConstant.FILEZIPDOWNTEMPPATH);
        String zipFileSrc = "Package_" + RandomUtil.uuId();
        String zipFileName = zipFileSrc + ".zip";
        String mainPath = servicePath + zipFileSrc + File.separator;
        String absMainPath = tempFilePath + zipFileSrc;
        new File(absMainPath).mkdirs();
        //递归生成文件夹下的文件
        createdFiles(obj.getIds(), mainPath);

        String filePath = tempFilePath + zipFileName;
        //打包
        FileUtil.toZip(filePath, true, tempFilePath + zipFileSrc);
        //删除源文件
        FileUtil.deleteFileAll(new File(tempFilePath + zipFileSrc));

        //上传压缩包到服务器
        MultipartFile multipartFile = FileUtil.createFileItem(new File(XSSEscape.escapePath(filePath)));
        FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(FilePathUtil.getFilePath(FileTypeConstant.FILEZIPDOWNTEMPPATH), zipFileName), multipartFile);
        // 删除压缩包
        FileUtil.deleteFileAll(new File(tempFilePath + zipFileName));

        //获取服务器下载路径
        DownloadVO vo = DownloadVO.builder()
                .name(zipFileName)
                .url(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + FileTypeConstant.FILEZIPDOWNTEMPPATH))
                .build();
        return ActionResult.success(vo);
    }

    /**
     * 递归获取文件夹下的文件
     *
     * @param fileIdList
     * @param mainPath
     */
    private void createdFiles(List<String> fileIdList, String mainPath) {
        for (String id : fileIdList) {
            DocumentEntity info = documentService.getInfo(id);
            if (info != null) {
                String fileId = StringUtil.isNotEmpty(info.getFilePath()) ? XSSEscape.escape(info.getFilePath()).trim() : "";
                String fileName = XSSEscape.escapePath(info.getFullName()).trim();
                if (Objects.equals(info.getType(), 0)) {
                    //文件夹
                    File file = new File(FileUploadUtils.getLocalBasePath() + mainPath + fileName);
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    List<DocumentEntity> allList = documentService.getChildList(id, true);
                    List<String> collect = allList.stream().map(DocumentEntity::getId).collect(Collectors.toList());
                    createdFiles(collect, mainPath + fileName + File.separator);
                } else {
                    try {
                        //文件
                        FileUploadUtils.downloadFileToLocal(new FileParameter(FilePathUtil.getFilePath(FileTypeConstant.DOCUMENT), fileId).setLocaFilelPath(mainPath).setLocalFileName(fileName));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * 批量删除
     */
    @Operation(summary = "批量删除")
    @PostMapping("/BatchDelete")
    @Parameter(name = "ids", description = "主键", required = true)
    public ActionResult<Object> batchDelete(@RequestBody DocumentShareForm obj) {
        for (String id : obj.getIds()) {
            DocumentEntity entity = documentService.getInfo(id);
            if (entity != null) {
                if (!Objects.equals(entity.getCreatorUserId(), UserProvider.getLoginUserId())) {
                    return ActionResult.fail(MsgCode.AD104.get());
                }
                List<DocumentEntity> allList = new ArrayList<>();
                documentService.getChildSrcList(entity.getId(), allList, 1);
                allList.add(entity);
                //添加删除记录
                DocumentLogEntity logent = new DocumentLogEntity();
                logent.setDocumentId(id);
                List<String> collect = allList.stream().map(DocumentEntity::getId).collect(Collectors.toList());
                logent.setChildDocument(collect.stream().collect(Collectors.joining(",")));
                documentLogService.save(logent);
                for (DocumentEntity item : allList) {
                    documentService.delete(item);
                }
            }
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 回收站（彻底删除）
     *
     * @return
     */
    @Operation(summary = "回收站（彻底删除）")
    @PostMapping("/Trash")
    @Parameter(name = "ids", description = "主键数组", required = true)
    public ActionResult<Object> trashdelete(@RequestBody DocumentShareForm obj) {
        documentService.trashdelete(obj.getIds());
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 回收站（还原文件）
     *
     * @return
     */
    @Operation(summary = "回收站（还原文件）")
    @PostMapping("/Trash/Actions/Recovery")
    @Parameter(name = "ids", description = "主键数组", required = true)
    @DSTransactional
    public ActionResult<Object> trashRecovery(@RequestBody DocumentShareForm obj) {
        documentService.trashRecoveryConstainSrc(obj.getIds());
        return ActionResult.success(MsgCode.SU010.get());
    }

    /**
     * 共享文件（创建）
     *
     * @param documentShareForm 分享模型
     * @return
     */
    @Operation(summary = "分享文件/文件夹")
    @PostMapping("/Actions/Share")
    @Parameter(name = "documentShareForm", description = "分享模型", required = true)
    public ActionResult<Object> shareCreate(@RequestBody DocumentShareForm documentShareForm) {
        documentService.sharecreate(documentShareForm);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 取消共享
     *
     * @param obj 主键值
     * @return
     */
    @Operation(summary = "取消分享文件/文件夹")
    @PostMapping("/Actions/CancelShare")
    @Parameter(name = "ids", description = "主键", required = true)
    public ActionResult<Object> shareCancel(@RequestBody DocumentShareForm obj) {
        documentService.shareCancel(obj.getIds());
        return ActionResult.success(MsgCode.SU005.get());
    }


    @Operation(summary = "共享用户调整")
    @PostMapping("/Actions/ShareAdjustment/{id}")
    @Parameter(name = "id", description = "文档主键", required = true)
    @Parameter(name = "userIds", description = "共享用户组", required = true)
    public ActionResult<Object> shareAdjustment(@PathVariable("id") String id, @RequestBody DocumentShareForm obj) {
        if (!obj.getUserIds().isEmpty()) {
            documentService.shareAdjustment(id, obj.getUserIds());
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    @Operation(summary = "移动文件/文件夹")
    @PutMapping("/Actions/MoveTo/{toId}")
    @Parameter(name = "ids", description = "主键", required = true)
    @Parameter(name = "toId", description = "将要移动到Id", required = true)
    @DSTransactional
    public ActionResult<Object> moveTo(@RequestBody DocumentShareForm obj, @PathVariable("toId") String toId) {
        List<String> allIds = new ArrayList<>(obj.getIds());
        List<DocumentEntity> childList = new ArrayList<>();
        for (String oneId : obj.getIds()) {
            documentService.getChildSrcList(oneId, childList, 1);
        }
        allIds.addAll(childList.stream().map(DocumentEntity::getId).collect(Collectors.toList()));
        if (allIds.contains(toId)) {
            return ActionResult.fail(MsgCode.ETD103.get());
        }

        for (String id : obj.getIds()) {
            boolean flag = documentService.moveTo(id, toId);
            if (!flag) {
                return ActionResult.fail(MsgCode.FA002.get());
            }
        }

        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 封装上传附件
     * 流程归档-文件上传
     *
     * @return
     */
    private void uploaderVO(DocumentUploader documentUploader) {
        MultipartFile file = documentUploader.getFile();
        String parentId = documentUploader.getParentId();
        String taskId = documentUploader.getTaskId();
        String s = StringUtil.isNotEmpty(documentUploader.getFileName()) ? documentUploader.getFileName() : file.getOriginalFilename();
        String fileName = StringUtil.isNotEmpty(documentUploader.getTaskId()) ? documentUploader.getTaskId() : s;
        String fileType = UpUtil.getFileType(file);
        String creatorUserId = "";
        List<String> userList = new ArrayList<>();
        if (StringUtil.isNotEmpty(taskId)) {
            try { //获取流程信息
                FileModel fileModel = workFlowApi.getFileModel(taskId);
                fileName = fileModel.getFilename();
                creatorUserId = fileModel.getUserId();
                userList.addAll(fileModel.getUserList());
                fileType = "pdf";

                List<DocumentEntity> allList = documentService.getAllList("0", creatorUserId);
                DocumentEntity documentEntity = allList.stream().filter(t -> Objects.equals(t.getType(), 0) && FileModel.FOLDER_NAME.equals(t.getFullName())).findFirst().orElse(null);
                if (Objects.isNull(documentEntity)) {
                    documentEntity = new DocumentEntity();
                    documentEntity.setFullName(FileModel.FOLDER_NAME);
                    documentEntity.setType(0);
                    documentEntity.setParentId("0");
                    documentEntity.setCreatorUserId(creatorUserId);
                    documentService.create(documentEntity);
                }
                parentId = documentEntity.getId();
            } catch (Exception e) {
                throw new DataException(MsgCode.FA001.get());
            }
        }

        List<DocumentEntity> data = documentService.getAllList(parentId);
        String finalFileName = fileName;
        data = data.stream().filter(t -> {
            assert finalFileName != null;
            return finalFileName.equals(t.getFullName());
        }).collect(Collectors.toList());
        if (!data.isEmpty()) {
            assert fileName != null;
            fileName = fileName.substring(0, fileName.lastIndexOf("."))
                    + "副本" + UUID.randomUUID().toString().substring(0, 5)
                    + fileName.substring(fileName.lastIndexOf("."));
        }
        //上传
        MergeChunkDto mergeChunkDto = new MergeChunkDto();
        mergeChunkDto.setType(FileTypeConstant.DOCUMENT);
        mergeChunkDto.setFileType(fileType);
        UploaderVO uploaderVO = fileService.uploadFile(mergeChunkDto, file);
        DocumentEntity entity = new DocumentEntity();
        entity.setType(1);
        entity.setFullName(fileName);
        entity.setParentId(parentId);
        entity.setFileExtension(fileType);
        entity.setFilePath(uploaderVO.getName());
        entity.setFileSize(String.valueOf(file.getSize()));
        entity.setCreatorUserId(creatorUserId);
        entity.setEnabledMark(1);
        String desc = null;
        TaskEntity taskEntity = workFlowApi.getInfoSubmit(taskId, TaskEntity::getId, TaskEntity::getTemplateId);
        if (null != taskEntity) {
            desc = taskId + "-" + taskEntity.getTemplateId();
        }
        entity.setDescription(desc);
        entity.setUploaderUrl(UploaderUtil.uploaderImg("/api/file/Image/document/", uploaderVO.getName()));
        documentService.create(entity);
        if (StringUtil.isNotEmpty(taskId)) {
            DocumentShareForm documentShareForm = new DocumentShareForm();
            documentShareForm.setUserIds(userList);
            ArrayList<String> strings = new ArrayList<>();
            strings.add(entity.getId());
            documentShareForm.setIds(strings);
            documentShareForm.setCreatorUserId(creatorUserId);
            documentService.sharecreate(documentShareForm);
        }
    }

    /**
     * 判断是否存在归档文件
     *
     * @param taskId 流程任务主键
     */
    @Override
    public Boolean checkFlowFile(String taskId) {
        QueryWrapper<DocumentEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().like(DocumentEntity::getDescription, taskId);
        return documentService.count(wrapper) < 1;
    }

    /**
     * 获取归档文件
     *
     * @param model 参数
     */
    @Override
    public List<Map<String, Object>> getFlowFile(FlowFileModel model) {
        return documentService.getFlowFile(model);
    }
}
