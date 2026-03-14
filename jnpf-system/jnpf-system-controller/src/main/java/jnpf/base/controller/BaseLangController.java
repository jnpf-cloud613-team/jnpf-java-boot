package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.BaseLangEntity;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.model.language.*;
import jnpf.base.service.BaseLangService;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.util.ExcelTool;
import jnpf.base.vo.DownloadVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.ConfigConst;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.model.ExcelColumnAttr;
import jnpf.model.ExcelImportVO;
import jnpf.model.ExcelModel;
import jnpf.util.JsonUtil;
import jnpf.util.RegexUtils;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 多语言管理
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/28 11:26:57
 */
@Tag(name = "多语言配置", description = "BaseLang")
@RestController
@RequestMapping("/api/system/BaseLang")
@RequiredArgsConstructor
public class BaseLangController {

    public static final String EN_CODE ="enCode";


    private final BaseLangService baseLangService;


    private final ConfigValueUtil configValueUtil;


    private final DictionaryDataService dictionaryDataApi;

    @Operation(summary = "标记翻译列表")
    @GetMapping("/List")
    public ActionResult<Object> getList(Pagination pagination) {

        BaseLangListVO list = baseLangService.getList(pagination);

        return ActionResult.success(list);
    }

    @Operation(summary = "标记翻译列表")
    @SaCheckPermission("sysData.language")
    @GetMapping()
    public ActionResult<Object> list(BaseLangPage pagination) {
        BaseLangListVO list = baseLangService.list(pagination);
        return ActionResult.success(list);
    }

    @Operation(summary = "创建")
     @Parameter(name = "form", description = "多语言表单对象", required = true)
    @SaCheckPermission(value = {"sysData.language", "onlineDev.formDesign"}, mode = SaMode.OR)
    @PostMapping()
    public ActionResult<Object> create(@RequestBody @Valid BaseLangForm form) {
        if (RegexUtils.checkEnCode2(form.getEnCode())) {
            ActionResult.fail(MsgCode.SYS050.get());
        }
        baseLangService.create(form);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "修改")
    @Parameter(name = "form", description = "多语言表单对象", required = true)
    @SaCheckPermission("sysData.language")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid BaseLangForm form) {
        if (RegexUtils.checkEnCode2(form.getEnCode())) {
            ActionResult.fail(MsgCode.SYS050.get());
        }
        form.setId(id);
        baseLangService.update(form);
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "详情")
    @Parameter(name = "enCode", description = "翻译标记", required = true)
    @SaCheckPermission("sysData.language")
    @GetMapping("/{id}")
    public ActionResult<Object> info(@PathVariable("id") String id) {
        BaseLangForm info = baseLangService.getInfo(id);
        return ActionResult.success(info);
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "翻译标记", required = true)
    @SaCheckPermission("sysData.language")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        baseLangService.delete(id);
        return ActionResult.success();
    }

    @Operation(summary = "获取语种json")
    @Parameter(name = "language", description = "语种编码", required = true)
    @GetMapping("/LangJson")
    public ActionResult<Object> langJson(Locale locale) {
        if (locale == null || StringUtil.isEmpty(locale.toLanguageTag()) || "und".equals(locale.toLanguageTag())) {
            locale = Locale.SIMPLIFIED_CHINESE;
        }
        String languageJson = baseLangService.getLanguageJson(locale);
        return ActionResult.success(MsgCode.SU000.get(), languageJson);
    }

    @Operation(summary = "获取语种json")
    @Parameter(name = "language", description = "语种编码", required = true)
    @GetMapping("/ServerLang")
    public ActionResult<List<BaseLangVo>> getServerLang(Locale locale) {
        List<BaseLangEntity> serverLang = baseLangService.getServerLang(locale);
        List<BaseLangVo> list = JsonUtil.getJsonToList(serverLang, BaseLangVo.class);
        return ActionResult.success(list);
    }

    //++++++++++++++++++++++++++++++++++以下导入导出接口++++++++++++++++++++++++++++++++++++++++++

    @Operation(summary = "模板下载")
    @SaCheckPermission("sysData.language")
    @GetMapping("/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload() {
        BaseLangColumn columnMap = new BaseLangColumn(getDicLangMap());
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType();
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(false);
        List<Map<String, Object>> list = columnMap.getDefaultList();
        Map<String, String[]> optionMap = new HashMap<>();
        optionMap.put("type", new String[]{"客户端", "服务端"});

        ExcelModel excelModel = ExcelModel.builder().models(models).selectKey(new ArrayList<>(keyMap.keySet())).optionMap(optionMap).hasHeader(true).build();
        DownloadVO vo = ExcelTool.getImportTemplate(FileTypeConstant.TEMPORARY, excelName, keyMap, list, excelModel);
        return ActionResult.success(vo);
    }

    /**
     * 从字典获取语言map
     *
     * @return
     */
    private Map<String, String> getDicLangMap() {
        List<DictionaryDataEntity> langTypeList = dictionaryDataApi.getListByTypeDataCode(ConfigConst.BASE_LANGUAGE);
        Map<String, String> collect = new LinkedHashMap<>();
        langTypeList.forEach(t -> collect.put(t.getEnCode(), t.getFullName()));
        return collect;
    }

    @Operation(summary = "上传导入Excel")
    @SaCheckPermission("sysData.language")
    @PostMapping("/Uploader")
    public ActionResult<Object> uploader() {
        return ExcelTool.uploader();
    }

    @Operation(summary = "导入数据")
    @SaCheckPermission("sysData.language")
    @PostMapping("/ImportData")
    public ActionResult<ExcelImportVO> importData(@RequestBody BaseLangModel model) {
        // 导入字段
        BaseLangColumn columnMap = new BaseLangColumn(getDicLangMap());
        Map<String, String> keyMap = columnMap.getColumnByType();
        Map<String, Object> headAndDataMap = ExcelTool.importPreview(FileTypeConstant.TEMPORARY, model.getFileName(), keyMap,1,1);
        List<Map<String, Object>> listData = (List<Map<String, Object>>) headAndDataMap.get("dataRow");
        List<BaseLangEntity> addList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        // 对数据做校验 (处理)
        this.validateImportData(listData, addList, failList);
        //写入数据
        baseLangService.importSaveOrUpdate(addList);
        return ActionResult.success(MsgCode.IMP001.get());
    }

    public void validateImportData(List<Map<String, Object>> listData, List<BaseLangEntity> addList, List<Map<String, Object>> failList) {
        List<DictionaryDataEntity> langTypeList = dictionaryDataApi.getListByTypeDataCode(ConfigConst.BASE_LANGUAGE);
        for (Map<String, Object> map : listData) {
            if (MapUtils.isNotEmpty(map) && map.get(EN_CODE)!=null && StringUtils.isNotBlank(map.get(EN_CODE).toString()) && RegexUtils.checkEnCode2(map.get(EN_CODE).toString())) {
                BaseLangEntity baseLangEntity = new BaseLangEntity();
                baseLangEntity.setEnCode(map.get(EN_CODE).toString());
                int type = 0;
                if (map.get("type") != null && StringUtils.isNotBlank(map.get("type").toString())) {
                    type = "服务端".equals(map.get("type").toString()) ? 1 : 0;
                }
                baseLangEntity.setType(type);
                boolean allNull = true;
                List<BaseLangEntity> thisList =new ArrayList<>();
                for (DictionaryDataEntity item : langTypeList) {
                    String enCode = item.getEnCode();
                    String fullName = "";
                    if (map.get(enCode) != null) {
                        fullName = map.get(enCode).toString();
                    }
                    if(StringUtils.isNotBlank(fullName)){
                        allNull = false;
                    }
                    baseLangEntity.setLanguage(enCode);
                    baseLangEntity.setFullName(fullName);
                    BaseLangEntity entity = BeanUtil.copyProperties(baseLangEntity, BaseLangEntity.class);
                    thisList.add(entity);
                }
                if(!allNull){
                    addList.addAll(thisList);
                }
            } else {
                failList.add(map);
            }
        }
    }
}
