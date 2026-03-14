package jnpf.visualdata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import com.google.common.base.Joiner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.model.export.VisualScreenExportVo;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.model.FileListVO;
import jnpf.util.*;
import jnpf.visual.service.VisualScreenApi;
import jnpf.visualdata.entity.VisualCategoryEntity;
import jnpf.visualdata.entity.VisualConfigEntity;
import jnpf.visualdata.entity.VisualEntity;
import jnpf.visualdata.enums.VisualImgEnum;
import jnpf.visualdata.model.VisualPageVO;
import jnpf.visualdata.model.visual.*;
import jnpf.visualdata.model.visualcategory.VisualCategoryListVO;
import jnpf.visualdata.model.visualconfig.VisualConfigInfoModel;
import jnpf.visualdata.model.visualfile.ImageVO;
import jnpf.visualdata.service.VisualCategoryService;
import jnpf.visualdata.service.VisualConfigService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 大屏基本信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@RestController
@Tag(name = "大屏基本信息", description = "visual")
@RequestMapping("/api/blade-visual/visual")
@Slf4j
@RequiredArgsConstructor
public class VisualController extends SuperController<VisualService, VisualEntity> implements VisualScreenApi {

    
    private final FileExport fileExport;
    
    private final VisualCategoryService visualCategoryService;
    
    private final VisualService visualService;
    
    private final VisualConfigService configService;
    
    private final ConfigValueUtil configValueUtil;
    
    private final VisualCategoryService categoryService;


    /**
     * 列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "分页")
    @GetMapping("/list")
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> list(VisualPaginationModel pagination) {
        List<VisualEntity> data = visualService.getList(pagination);
        List<VisualListVO> list = JsonUtil.getJsonToList(data, VisualListVO.class);
        VisualPageVO<VisualListVO> paginationVO = JsonUtil.getJsonToBean(pagination, VisualPageVO.class);
        paginationVO.setRecords(list);
        return ActionResult.success(paginationVO);
    }

    /**
     * 详情
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "详情")
    @GetMapping("/detail")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<VisualInfoVO> info(@RequestParam("id") String id) {
        VisualEntity visual = visualService.getInfo(id);
        VisualConfigEntity config = configService.getInfo(id);
        VisualInfoVO vo = new VisualInfoVO();
        vo.setVisual(JsonUtil.getJsonToBean(visual, VisualInfoModel.class));
        vo.setConfig(JsonUtil.getJsonToBean(config, VisualConfigInfoModel.class));
        return ActionResult.success(vo);
    }

    /**
     * 新增
     *
     * @param visualCrform 大屏模型
     * @return
     */
    @Operation(summary = "新增")
    @PostMapping("/save")
    @Parameter(name = "visualCrform", description = "大屏模型", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> create(@RequestBody @Valid VisualCrform visualCrform) {

        VisualEntity visual = JsonUtil.getJsonToBean(visualCrform.getVisual(), VisualEntity.class);
        visual.setBackgroundUrl(VisusalImgUrl.URL + configValueUtil.getBiVisualPath() + "bg/bg1.png");
        VisualConfigEntity config = JsonUtil.getJsonToBean(visualCrform.getConfig(), VisualConfigEntity.class);
        visualService.create(visual, config);
        Map<String, String> data = new HashMap<>(16);
        data.put("id", visual.getId());
        return ActionResult.success(data);
    }

    /**
     * 修改
     *
     * @param categoryUpForm 大屏模型
     * @return
     */
    @Operation(summary = "修改")
    @PostMapping("/update")
    @Parameter(name = "categoryUpForm", description = "大屏模型", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> update(@RequestBody VisualUpform categoryUpForm) {
        VisualEntity visual = JsonUtil.getJsonToBean(categoryUpForm.getVisual(), VisualEntity.class);
        VisualConfigEntity config = JsonUtil.getJsonToBean(categoryUpForm.getConfig(), VisualConfigEntity.class);
        visualService.update(visual.getId(), visual, config);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param ids 主键
     * @return
     */
    @Operation(summary = "删除")
    @PostMapping("/remove")
    @Parameter(name = "ids", description = "主键", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> delete(@RequestParam("ids") String ids) {
        VisualEntity entity = visualService.getInfo(ids);
        if (entity != null) {
            visualService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 复制
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "复制")
    @PostMapping("/copy")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> copy(@RequestParam("id") String id) {
        VisualEntity entity = visualService.getInfo(id);
        VisualConfigEntity config = configService.getInfo(id);
        if (entity != null) {
            entity.setTitle(entity.getTitle() + "_复制");
            visualService.create(entity, config);
            return ActionResult.success(MsgCode.SU005.get(), entity.getId());
        }
        return ActionResult.fail(MsgCode.FA001.get());
    }

    /**
     * 获取类型
     *
     * @return
     */
    @Operation(summary = "获取类型")
    @GetMapping("/category")
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<List<VisualCategoryListVO>> list() {
        List<VisualCategoryEntity> data = categoryService.getList();
        List<VisualCategoryListVO> list = JsonUtil.getJsonToList(data, VisualCategoryListVO.class);
        return ActionResult.success(list);
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @param type 类型
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "上传文件")
    @Parameter(name = "type", description = "类型", required = true)
    @PostMapping(value = "/put-file/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<ImageVO> file(MultipartFile file, @PathVariable("type") String type) {
        ImageVO vo = new ImageVO();
        VisualImgEnum imgEnum = VisualImgEnum.getByMessage(type);
        if (imgEnum != null) {
            String path = imgEnum.getMessage();
            String filePath = configValueUtil.getBiVisualPath() + path + File.separator;
            String name = RandomUtil.uuId() + "." + UpUtil.getFileType(file);
            //上传文件
            FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(filePath, name), file);
            vo.setOriginalName(fileInfo.getOriginalFilename());
            vo.setLink(VisusalImgUrl.URL + fileInfo.getPath() + fileInfo.getFilename());
            vo.setName(VisusalImgUrl.URL + fileInfo.getPath() + fileInfo.getFilename());
        }
        return ActionResult.success(vo);
    }

    /**
     * 获取图片列表
     *
     * @param type 文件夹
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "获取图片列表")
    @GetMapping("/{type}")
    @Parameter(name = "type", description = "文件夹", required = true)
    public ActionResult<List<ImageVO>> getFile(@PathVariable("type") String type) {
        List<ImageVO> vo = new ArrayList<>();
        VisualImgEnum imgEnum = VisualImgEnum.getByMessage(type);
        if (imgEnum != null) {
            String path = configValueUtil.getBiVisualPath() + imgEnum.getMessage() + File.separator;
            List<FileListVO> fileList = FileUploadUtils.getFileList(new FileParameter().setRemotePath(path));
            fileList.forEach(fileListVO -> {
                ImageVO imageVO = new ImageVO();
                imageVO.setName(fileListVO.getFileName());
                imageVO.setLink(VisusalImgUrl.URL + fileListVO.getFileName());
                imageVO.setOriginalName(fileListVO.getFileName());
                vo.add(imageVO);
            });
        }
        return ActionResult.success(vo);
    }

    /**
     * 大屏下拉框
     */
    @Operation(summary = "大屏下拉框")
    @GetMapping("/Selector")
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<ListVO<VisualSelectorVO>> selector() {
        List<VisualEntity> visualList = visualService.getList();
        List<VisualCategoryEntity> categoryList = categoryService.getList();
        List<VisualSelectorVO> listVos = new ArrayList<>();
        for (VisualCategoryEntity category : categoryList) {
            VisualSelectorVO categoryModel = new VisualSelectorVO();
            categoryModel.setId(category.getCategoryvalue());
            categoryModel.setFullName(category.getCategorykey());
            List<VisualEntity> visualAll = visualList.stream().filter(t -> t.getCategory().equals(Integer.parseInt(category.getCategoryvalue()))).collect(Collectors.toList());
            if (!visualAll.isEmpty()) {
                List<VisualSelectorVO> childList = new ArrayList<>();
                for (VisualEntity visual : visualAll) {
                    VisualSelectorVO visualModel = new VisualSelectorVO();
                    visualModel.setId(visual.getId());
                    visualModel.setFullName(visual.getTitle());
                    visualModel.setChildren(null);
                    visualModel.setHasChildren(false);
                    childList.add(visualModel);
                }
                categoryModel.setHasChildren(true);
                categoryModel.setChildren(childList);
                listVos.add(categoryModel);
            }
        }
        ListVO<VisualSelectorVO> vo = new ListVO<>();
        vo.setList(listVos);
        return ActionResult.success(vo);
    }

    /**
     * 大屏导出
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "大屏导出")
    @PostMapping("/{id}/Actions/ExportData")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<DownloadVO> exportData(@PathVariable("id") String id) {
        VisualEntity entity = visualService.getInfo(id);
        VisualConfigEntity configEntity = configService.getInfo(id);
        VisualModel model = new VisualModel();
        model.setEntity(entity);
        model.setConfigEntity(configEntity);
        DownloadVO downloadVO = fileExport.exportFile(model, FileTypeConstant.TEMPORARY, entity.getTitle(), ModuleTypeEnum.VISUAL_DATA.getTableName());
        return ActionResult.success(downloadVO);
    }

    /**
     * 大屏导入
     *
     * @param multipartFile 文件
     * @return
     */
    @Operation(summary = "大屏导入")
    @SaCheckPermission("onlineDev.dataScreen")
    @PostMapping(value = "/Model/Actions/ImportData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> importData(MultipartFile multipartFile) throws DataException {
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.VISUAL_DATA.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        VisualModel vo = JsonUtil.getJsonToBean(fileContent, VisualModel.class);
        visualService.createImport(vo.getEntity(), vo.getConfigEntity());
        return ActionResult.success(MsgCode.SU000.get());
    }

    /**
     * 获取API动态数据
     *
     * @param apiRequest 大屏模型
     * @return
     */
    @Operation(summary = "获取API动态数据")
    @PostMapping(value = "/GetApiData")
    @Parameter(name = "apiRequest", description = "大屏模型", required = true)
    public String getApiData(@RequestBody @Valid VisualApiRequest apiRequest) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(apiRequest.getTimeout())).build();
        Headers headers;
        Request request;
        if (!apiRequest.getHeaders().isEmpty()) {
            Headers.Builder builder = new Headers.Builder();
            apiRequest.getHeaders().forEach(builder::add);
            headers = builder.build();
        } else {
            headers = new Headers.Builder().build();
        }
        if (apiRequest.getMethod().equalsIgnoreCase("post")) {
            request = new Request.Builder().url(apiRequest.getUrl())
                    .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json;charset=utf-8"), apiRequest.getParams().isEmpty() ? "" : JsonUtil.getObjectToString(apiRequest.getParams())))
                    .headers(headers)
                    .build();
        } else {
            String params = Joiner.on("&")
                    .useForNull("")
                    .withKeyValueSeparator("=")
                    .join(apiRequest.getParams());
            request = new Request.Builder().url(apiRequest.getUrl() + (apiRequest.getUrl().contains("?") ? "&" : "?") + params)
                    .get()
                    .headers(headers)
                    .build();
        }
        return Objects.requireNonNull(client.newCall(request).execute().body()).string();
    }


    /**
     * 获取API动态数据
     *
     * @param proxyModel 代理模型
     * @return
     */
    @Operation(summary = "获取API动态数据")
    @PostMapping(value = "/proxy")
    @Parameter(name = "proxyModel", description = "代理模型", required = true)
    public String getApiData(@RequestBody @Valid VisualProxyModel proxyModel){
        Map<String, String> headers;
        boolean isForm = false;
        if (!proxyModel.getHeaders().isEmpty()) {
            headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll(proxyModel.getHeaders());
            if (headers.containsKey("form")) {
                //头部指定当前为form表单
                isForm = true;
            }
        } else {
            headers = new HashMap<>(1, 1);
        }
        //Header无自定义TOKEN 取当前TOKEN
        if (!headers.containsKey("Authorization")) {
            String token = UserProvider.getToken();
            if (StringUtil.isNotEmpty(token)) {
                headers.put("Authorization", token);
            }
        }
        HttpRequest httpRequest = HttpRequest.of(proxyModel.getUrl()).method(Method.valueOf(proxyModel.getMethod().toUpperCase())).addHeaders(headers);
        if (isForm) {
            httpRequest.form(proxyModel.getData());
        } else if (proxyModel.getData() != null && !proxyModel.getData().isEmpty()) {
            httpRequest.body(JsonUtil.getObjectToString(proxyModel.getData()));
        } else {
            httpRequest.form(proxyModel.getParams());
        }
        try {
            return httpRequest.timeout(10000).execute().body();
        } catch (Exception e) {
            log.info("接口请求失败 {} {}", proxyModel.getUrl(), e.getMessage());
            throw new DataException(MsgCode.FA037.get());
        }
    }

    @Override
    public VisualScreenExportVo getExportList(String systemId) {
        VisualScreenExportVo vo = new VisualScreenExportVo();
        List<VisualCategoryEntity> category = categoryService.getListBySystemId(systemId);
        vo.setCategory(new ArrayList<>(category));
        List<VisualEntity> visList = visualService.getListBySystemId(systemId);
        List<Object> list = new ArrayList<>(visList.size());
        for (VisualEntity item : visList) {
            VisualConfigEntity configEntity = configService.getInfo(item.getId());
            VisualModel model = new VisualModel();
            model.setEntity(item);
            model.setConfigEntity(configEntity);
            list.add(model);
        }
        vo.setList(list);
        return vo;
    }

    @Override
    public boolean importCopy(VisualScreenExportVo vo, String systemId) {
        try {
            if (CollectionUtils.isNotEmpty(vo.getList())) {
                VisualCateModel model = JsonUtil.getJsonToBean(vo, VisualCateModel.class);
                for (VisualCategoryEntity entity : model.getCategory()) {
                    entity.setId(RandomUtil.uuId());
                    entity.setSystemId(systemId);
                    visualCategoryService.save(entity);
                }
                for (VisualModel item : model.getList()) {
                    //获取文件内容
                    VisualEntity entity = item.getEntity();
                    entity.setId(RandomUtil.uuId());
                    entity.setSystemId(systemId);
                    entity.setCreateUser(UserProvider.getUser().getUserId());
                    entity.setCreateTime(new Date());
                    visualService.save(entity);
                    VisualConfigEntity configEntity = item.getConfigEntity();
                    configEntity.setVisualId(entity.getId());
                    configService.create(configEntity);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        visualCategoryService.deleteBySystemId(systemId);
        List<VisualEntity> listBySystemId = visualService.getListBySystemId(systemId);
        for (VisualEntity item : listBySystemId) {
            visualService.delete(item);
        }
    }
}
