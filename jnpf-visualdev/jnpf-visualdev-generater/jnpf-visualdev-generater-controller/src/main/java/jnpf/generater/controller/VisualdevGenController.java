package jnpf.generater.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.VisualAliasForm;
import jnpf.base.model.read.ReadListVO;
import jnpf.base.model.read.ReadModel;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.VisualAliasService;
import jnpf.base.service.VisualdevReleaseService;
import jnpf.base.service.VisualdevService;
import jnpf.base.util.ReadFile;
import jnpf.base.util.VisualUtil;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.generater.service.VisualdevGenService;
import jnpf.model.visualjson.TableModel;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * 可视化开发功能表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Tag(name = "代码生成器", description = "Generater")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/Generater")
public class VisualdevGenController {

    private final ConfigValueUtil configValueUtil;
    private final RedisUtil redisUtil;
    private final VisualdevService visualdevService;
    private final VisualdevReleaseService visualdevReleaseService;
    private final VisualdevGenService visualdevGenService;
    private final DictionaryDataService dictionaryDataApi;
    private final VisualAliasService aliasService;


    /**
     * 下载文件
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "下载文件")
    @GetMapping("/DownloadVisCode")
    public void downloadCode() throws DataException {
        HttpServletRequest request = ServletUtil.getRequest();
        String reqJson = request.getParameter("encryption");
        String name = request.getParameter("name");
        String fileNameAll = DesUtil.aesDecode(reqJson);
        if (!StringUtil.isEmpty(fileNameAll)) {
            String token = fileNameAll.split("#")[0];
            if (TicketUtil.parseTicket(token) != null) {
                TicketUtil.deleteTicket(token);
                String fileName = fileNameAll.split("#")[1];
                String path = FilePathUtil.getFilePath(FileTypeConstant.CODETEMP);
                //下载到本地
                FileUploadUtils.downloadFile(new FileParameter(path, fileName), inputStream -> FileDownloadUtil.outFile(inputStream, name));
            } else {
                throw new DataException(MsgCode.VS014.get());
            }
        } else {
            throw new DataException(MsgCode.VS014.get());
        }
    }


    @Operation(summary = "下载代码")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Actions/DownloadCode")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    @DSTransactional
    public ActionResult<Object> downloadCode(@PathVariable("id") String id, @RequestBody DownloadCodeForm downloadCodeForm) {
        if (downloadCodeForm.getModule() != null) {
            DictionaryDataEntity info = dictionaryDataApi.getInfo(downloadCodeForm.getModule());
            if (info != null) {
                downloadCodeForm.setModule(info.getEnCode());
            }
        }
        VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(visualdevReleaseService.getById(id), VisualdevEntity.class);
        String s = VisualUtil.checkPublishVisualModel(visualdevEntity, MsgCode.VS006.get());
        if (s != null) {
            return ActionResult.fail(s);
        }
        DownloadVO vo;
        String fileName = visualdevGenService.codeGengerateV3(visualdevEntity, downloadCodeForm);

        //服务器生成路径
        String filePath = FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + fileName + ".zip";
        FileUtil.toZip(filePath, true, FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + fileName);
        // 删除源文件
        FileUtil.deleteFileAll(new File(FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + fileName));

        //上传压缩包到服务器
        MultipartFile multipartFile = FileUtil.createFileItem(new File(XSSEscape.escapePath(filePath)));
        FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(configValueUtil.getServiceDirectoryPath(), fileName + ".zip"), multipartFile);
        // 删除压缩包
        FileUtil.deleteFileAll(new File(filePath));
        //下载文件服务器上的压缩包
        vo = DownloadVO.builder().name(fileInfo.getFilename()).url(UploaderUtil.uploaderVisualFile(fileInfo.getFilename()) + "&name=" + fileName + ".zip").build();
        if (vo == null) {
            return ActionResult.fail(MsgCode.FA006.get());
        }
        return ActionResult.success(vo);
    }


    /**
     * 输出移动开发模板
     *
     * @return
     */
    @Operation(summary = "预览代码")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Actions/CodePreview")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> codePreview(@PathVariable("id") String id, @RequestBody DownloadCodeForm downloadCodeForm) {
        if (downloadCodeForm.getModule() != null) {
            DictionaryDataEntity info = dictionaryDataApi.getInfo(downloadCodeForm.getModule());
            if (info != null) {
                downloadCodeForm.setModule(info.getEnCode());
            }
        }
        VisualdevReleaseEntity releaseEntity = visualdevReleaseService.getById(id);
        VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(releaseEntity, VisualdevEntity.class);
        String s = VisualUtil.checkPublishVisualModel(visualdevEntity, "预览");
        if (s != null) {
            return ActionResult.fail(s);
        }
        String fileName = visualdevGenService.codeGengerateV3(visualdevEntity, downloadCodeForm);

        List<ReadListVO> dataList = ReadFile.priviewCode(FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + fileName);
        // 删除源文件
        FileUtil.deleteFileAll(new File(FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + fileName));
        if (dataList.isEmpty()) {
            return ActionResult.fail(MsgCode.FA015.get());
        }

        //代码对比
        if (downloadCodeForm.isContrast()) {
            VisualdevEntity oldEntity = visualdevService.getInfo(id);
            //修改状态 - 旧的是已发布，新的是草稿版本；发布状态 - 旧的是 旧的发布版本，新的是当前发布版本；
            boolean statusUpdate = true;
            if (Objects.equals(oldEntity.getState(), 1) && StringUtils.isNotBlank(releaseEntity.getOldContent())) {
                statusUpdate = false;
                VisualdevEntity jsonToBean = JsonUtil.getJsonToBean(releaseEntity.getOldContent(), VisualdevEntity.class);
                oldEntity.setVisualTables(jsonToBean.getVisualTables());
                oldEntity.setFormData(jsonToBean.getFormData());
                oldEntity.setColumnData(jsonToBean.getColumnData());
                oldEntity.setAppColumnData(jsonToBean.getAppColumnData());
                oldEntity.setWebType(jsonToBean.getWebType());
                oldEntity.setDbLinkId(jsonToBean.getDbLinkId());
            }

            String oldFileName = visualdevGenService.codeGengerateV3(oldEntity, downloadCodeForm);
            List<ReadListVO> oldDataList = ReadFile.priviewCode(FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + oldFileName);
            // 删除源文件
            FileUtil.deleteFileAll(new File(FileUploadUtils.getLocalBasePath() + configValueUtil.getServiceDirectoryPath() + oldFileName));

            for (ReadListVO m : dataList) {
                ReadListVO n = oldDataList.stream().filter(t -> m.getFileName().equals(t.getFileName())).findFirst().orElse(null);
                if (Objects.nonNull(n)) {
                    List<ReadModel> mChildren = m.getChildren();
                    List<ReadModel> nChildren = n.getChildren();
                    for (ReadModel mChild : mChildren) {
                        ReadModel nChild = nChildren.stream().filter(t -> mChild.getFileName().equals(t.getFileName())).findFirst().orElse(null);
                        String nfileContent = Objects.nonNull(nChild) ? nChild.getFileContent() : "";
                        if (statusUpdate) {
                            mChild.setOldFileContent(mChild.getFileContent());
                            mChild.setFileContent(nfileContent);
                        } else {
                            mChild.setOldFileContent(nfileContent);
                        }
                    }
                }
            }
        }

        ListVO<ReadListVO> datas = new ListVO<>();
        datas.setList(dataList);
        return ActionResult.success(datas);
    }

    /**
     * App预览(后台APP表单设计)
     *
     * @return
     */
    @Operation(summary = "App预览(后台APP表单设计)")
    @Parameter(name = "data", description = "数据")
    @PostMapping("/App/Preview")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> appPreview(String data) {
        String id = RandomUtil.uuId();
        redisUtil.insert(id, data, 300);
        return ActionResult.success((Object) id);
    }

    /**
     * App预览(后台APP表单设计)
     *
     * @return
     */
    @Operation(summary = "App预览查看")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/App/{id}/Preview")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> preview(@PathVariable("id") String id) {
        if (redisUtil.exists(id)) {
            Object object = redisUtil.getString(id);
            return ActionResult.success(object);
        } else {
            return ActionResult.fail(MsgCode.FA019.get());
        }
    }

    @Operation(summary = "获取命名规范")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}/Alias/Info")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> getAliasInfo(@PathVariable("id") String id) {
        List<TableModel> aliasInfo = aliasService.getAliasInfo(id);
        return ActionResult.success(aliasInfo);
    }

    @Operation(summary = "命名规范保存")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Alias/Save")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> aliasSave(@PathVariable("id") String id, @RequestBody VisualAliasForm form) {
        aliasService.aliasSave(id, form);
        return ActionResult.success(MsgCode.SU002.get());
    }
}
