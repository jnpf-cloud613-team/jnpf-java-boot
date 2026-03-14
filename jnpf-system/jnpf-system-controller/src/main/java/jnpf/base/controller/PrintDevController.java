package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.PrintDevEntity;
import jnpf.base.entity.PrintVersionEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.export.PrintExportVo;
import jnpf.base.model.print.*;
import jnpf.base.model.query.PrintDevDataQuery;
import jnpf.base.model.query.PrintDevParam;
import jnpf.base.model.vo.PrintDevListVO;
import jnpf.base.model.vo.PrintDevVO;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.PrintDevService;
import jnpf.base.service.PrintVersionService;
import jnpf.base.service.SystemService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.CodeConst;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 打印模板 -控制器
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Tag(name = "打印模板", description = "print")
@RestController
@RequestMapping("/api/system/printDev")
@RequiredArgsConstructor
public class PrintDevController {

    public static final String SYSTEM_SLICE_PAGE_NO ="systemSlicePageNo";
    public static final String PRINT_DATA ="printData";

    
    private final PrintDevService printDevService;
    
    private final PrintVersionService versionService;
    
    private final FileExport fileExport;
    
    private final DictionaryDataService dictionaryDataService;
    
    private final UserService userService;
    
    private final CodeNumService codeNumService;
    
    private final SystemService systemService;

    @Operation(summary = "列表")
    @SaCheckPermission("onlineDev.printDev")
    @GetMapping
    public ActionResult<PageListVO<PrintDevListVO>>list(PaginationPrint paginationPrint) {
        SystemEntity systemEntity = systemService.getInfoByEnCode(RequestContext.getAppCode());
        paginationPrint.setSystemId(systemEntity.getId());
        List<PrintDevEntity> list = printDevService.getList(paginationPrint);
        List<String> userId = list.stream().map(t -> t.getCreatorUserId()).collect(Collectors.toList());
        List<String> lastUserId = list.stream().map(t -> t.getLastModifyUserId()).collect(Collectors.toList());
        lastUserId.removeAll(Collections.singleton(null));
        List<UserEntity> userEntities = userService.getUserName(userId);
        List<UserEntity> lastUserIdEntities = userService.getUserName(lastUserId);
        List<DictionaryDataEntity> typeList = dictionaryDataService.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        List<PrintDevListVO> listVOS = new ArrayList<>();
        for (PrintDevEntity entity : list) {
            PrintDevListVO vo = JsonUtil.getJsonToBean(entity, PrintDevListVO.class);
            DictionaryDataEntity dataEntity = typeList.stream().filter(t -> t.getId().equals(entity.getCategory())).findFirst().orElse(null);
            if (dataEntity != null) {
                vo.setCategory(dataEntity.getFullName());
            } else {
                vo.setCategory("");
            }
            //创建者
            UserEntity creatorUser = userEntities.stream().filter(t -> t.getId().equals(entity.getCreatorUserId())).findFirst().orElse(null);
            vo.setCreatorUser(creatorUser != null ? creatorUser.getRealName() + "/" + creatorUser.getAccount() : entity.getCreatorUserId());
            //修改人
            UserEntity lastModifyUser = lastUserIdEntities.stream().filter(t -> t.getId().equals(entity.getLastModifyUserId())).findFirst().orElse(null);
            vo.setLastModifyUser(lastModifyUser != null ? lastModifyUser.getRealName() + "/" + lastModifyUser.getAccount() : entity.getLastModifyUserId());
            listVOS.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationPrint, PaginationVO.class);
        return ActionResult.page(listVOS, paginationVO);
    }

    /*============模板增删改==============*/

    @Operation(summary = "新增")
    @Parameter(name = "printDevForm", description = "打印模板数据传输对象")
    @SaCheckPermission("onlineDev.printDev")
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid PrintDevFormDTO printDevForm) {
        SystemEntity systemEntity = systemService.getInfoByEnCode(RequestContext.getAppCode());
        printDevForm.setSystemId(systemEntity.getId());
        printDevService.create(printDevForm);
        return ActionResult.success(MsgCode.SU001.get(), printDevForm.getId());
    }

    @Operation(summary = "详情")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("onlineDev.printDev")
    @GetMapping("/{id}")
    public ActionResult<PrintDevInfoVO> info(@PathVariable("id") String id) {
        PrintDevEntity byId = printDevService.getById(id);
        PrintDevInfoVO vo = JsonUtil.getJsonToBean(byId, PrintDevInfoVO.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "form", description = "流程模型", required = true)
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid PrintDevFormDTO printDevForm){
        PrintDevEntity printDevEntity = JsonUtil.getJsonToBean(printDevForm, PrintDevEntity.class);
        PrintDevEntity originEntity = printDevService.getById(id);
        printDevService.creUpdateCheck(printDevEntity,
                !originEntity.getFullName().equals(printDevForm.getFullName()),
                !originEntity.getEnCode().equals(printDevForm.getEnCode()));
        printDevEntity.setId(id);
        if (StringUtil.isEmpty(printDevEntity.getEnCode())) {
            printDevEntity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.DY), code -> printDevService.isExistByEnCode(code, id)));
        }
        printDevEntity.setLastModifyTime(DateUtil.getNowDate());
        printDevEntity.setLastModifyUserId(UserProvider.getUser().getUserId());
        printDevService.updateById(printDevEntity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("onlineDev.printDev")
    @DeleteMapping("/{id}")
    public ActionResult<PrintDevFormDTO> delete(@PathVariable String id) {
        //对象存在判断
        if (printDevService.getById(id) != null) {
            printDevService.removeById(id);
            versionService.removeByTemplateId(id);
            return ActionResult.success(MsgCode.SU003.get());
        } else {
            return ActionResult.fail(MsgCode.FA003.get());
        }
    }

    /*============版本增删改==============*/
    @Operation(summary = "版本详情")
    @Parameter(name = "versionId", description = "打印版本id")
    @SaCheckPermission("onlineDev.printDev")
    @GetMapping("/Info/{versionId}")
    public ActionResult<PrintDevInfoVO> versionInfo(@PathVariable String versionId) {
        PrintDevInfoVO info = printDevService.getVersionInfo(versionId);
        return ActionResult.success(info);
    }

    @Operation(summary = "版本新增")
    @Parameter(name = "versionId", description = "打印版本id")
    @SaCheckPermission("onlineDev.printDev")
    @PostMapping("/Info/{versionId}")
    public ActionResult<Object>copyVersion(@PathVariable String versionId) {
        String newVersionId = versionService.copyVersion(versionId);
        return ActionResult.success(MsgCode.SU005.get(), newVersionId);
    }

    @Operation(summary = "版本删除")
    @Parameter(name = "versionId", description = "打印版本id")
    @SaCheckPermission("onlineDev.printDev")
    @DeleteMapping("/Info/{versionId}")
    public ActionResult<Object>deleteVersion(@PathVariable String versionId) {
        PrintVersionEntity byId = versionService.getById(versionId);
        if (byId != null) {
            List<PrintVersionEntity> list = versionService.getList(byId.getTemplateId());
            if (list.size() == 1) {
                return ActionResult.fail(MsgCode.SYS043.get());
            }
            if (Objects.equals(byId.getState(), 1)) {
                return ActionResult.fail(MsgCode.SYS044.get());
            }
            if (Objects.equals(byId.getState(), 2)) {
                return ActionResult.fail(MsgCode.SYS045.get());
            }
            versionService.removeById(versionId);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    @Operation(summary = "版本列表")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("onlineDev.printDev")
    @GetMapping("/Version/{id}")
    public ActionResult<List<PrintVersionListVO>> versionList(@PathVariable String id) {
        List<PrintVersionEntity> list = versionService.getList(id);
        List<PrintVersionListVO> listVO = new ArrayList<>();
        for (PrintVersionEntity jsonEntity : list) {
            PrintVersionListVO vo = JsonUtil.getJsonToBean(jsonEntity, PrintVersionListVO.class);
            vo.setFullName("打印版本V" + vo.getVersion());
            listVO.add(vo);
        }
        return ActionResult.success(listVO);
    }

    //***********************************动作start
    @Operation(summary = "保存或者发布")
    @Parameter(name = "printDevForm", description = "打印模板数据传输对象")
    @SaCheckPermission("onlineDev.printDev")
    @PostMapping("/Save")
    public ActionResult<Object>saveOrRelease(@RequestBody @Valid PrintDevUpForm form) {
        printDevService.saveOrRelease(form);
        if (Objects.equals(form.getType(), 1)) {
            return ActionResult.success(MsgCode.SU011.get());
        }
        return ActionResult.success(MsgCode.SU002.get());
    }

    @Operation(summary = "复制")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("onlineDev.printDev")
    @PostMapping("/{id}/Actions/Copy")
    public ActionResult<PageListVO<PrintDevEntity>> copy(@PathVariable String id) {
        printDevService.copyPrintdev(id);
        return ActionResult.success(MsgCode.SU007.get());
    }

    @Operation(summary = "导出")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("onlineDev.printDev")
    @GetMapping("/{id}/Actions/Export")
    public ActionResult<DownloadVO> export(@PathVariable String id) {
        PrintDevEntity entity = printDevService.getById(id);
        List<PrintVersionEntity> list = versionService.getList(id);
        if (CollectionUtils.isEmpty(list)) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        PrintDevInfoVO info = printDevService.getVersionInfo(list.get(0).getId());
        info.setSystemId(entity.getSystemId());
        PrintExportVo vo = JsonUtil.getJsonToBean(info, PrintExportVo.class);
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(vo, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.SYSTEM_PRINT.getTableName());
        return ActionResult.success(downloadVO);
    }

    @Operation(summary = "导入")
    @SaCheckPermission("onlineDev.printDev")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<PageListVO<PrintDevEntity>> importData(@RequestPart("file") MultipartFile multipartFile,
                                                               @RequestParam("type") Integer type) throws DataException {
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        if (sysInfo == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.SYSTEM_PRINT.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //读取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        PrintExportVo infVo = JsonUtil.getJsonToBean(fileContent, PrintExportVo.class);

        if (!sysInfo.getId().equals(infVo.getSystemId())) {
            infVo.setId(RandomUtil.uuId());
            infVo.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.DY), code -> printDevService.isExistByEnCode(code, null)));
            infVo.setSystemId(sysInfo.getId());
        }
        String str = printDevService.importData(infVo, type);
        if (StringUtil.isNotEmpty(str)) {
            return ActionResult.fail(str);
        }
        return ActionResult.success(MsgCode.IMP001.get());
    }
    //***********************************动作end

    /**
     * 下拉列表
     *
     * @return 返回列表数据
     */
    @Operation(summary = "下拉列表")
    @GetMapping("/Selector")
    public ActionResult<ListVO<PrintDevVO>> selectorList(String category) {
        ListVO<PrintDevVO> vo = new ListVO<>();
        vo.setList(printDevService.getTreeModel(category));
        return ActionResult.success(vo);
    }

    @Operation(summary = "Sql数据获取")
    @PostMapping("/BatchData")
    public ActionResult<Object>getBatchData(@RequestBody PrintDevDataQuery query) {
        String id = XSSEscape.escape(query.getId());
        PrintDevEntity entity = printDevService.getById(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.PRI001.get());
        }
        ArrayList<Object> list = new ArrayList<>();
        if (query.getFormInfo() != null && !query.getFormInfo().isEmpty()) {
            List<PrintDevParam> flowTaskInfo = query.getFormInfo();
            for (PrintDevParam rid : flowTaskInfo) {
                String oneId = "";
                String flowTaskId = "";
                if (Objects.nonNull(rid.getFormId())) {
                    oneId = XSSEscape.escape(rid.getFormId());
                }
                if (Objects.nonNull(rid.getFlowTaskId())) {
                    flowTaskId = XSSEscape.escape(rid.getFlowTaskId());
                }
                Map<String, Object> dataMap = printDevService.getDataMap(id, oneId, flowTaskId, query.getMap());
                dataMap.put("fullName", entity.getFullName());

                //打印全局配置
                if (Objects.nonNull(dataMap.get("globalConfig"))) {
                    duoLianDaYin(dataMap, list);
                } else {
                    list.add(dataMap);
                }
            }
            return ActionResult.success(list);
        }
        return ActionResult.success(list);
    }

    /**
     * 多联打印配置
     *
     * @param dataMap
     * @param list
     */
    private static void duoLianDaYin(Map<String, Object> dataMap, ArrayList<Object> list) {
        Map<String, Object> thisData = new HashMap<>(dataMap);
        Map<String, Object> printData = (Map<String, Object>) thisData.get(PRINT_DATA);

        GlobalConfig globalConfig = JsonUtil.getJsonToBean(dataMap.get("globalConfig").toString(), GlobalConfig.class);
        List<SliceConfig> sliceConfigList = globalConfig.getSliceConfig();
        List<SliceConfig> newSliceConfigList = sliceConfigList.stream().filter(t -> StringUtil.isNotEmpty(t.getDataSet()) && t.getLimit() != null
                && t.getLimit() > 0).collect(Collectors.toList());
        //无效配置不处理
        if (CollectionUtils.isEmpty(newSliceConfigList)) {
            list.add(dataMap);
            return;
        }
        if (newSliceConfigList.size() == 1) {
            SliceConfig sliceConfig = newSliceConfigList.get(0);
            List<Object> listOne = (List<Object>) printData.get(sliceConfig.getDataSet());
            if (CollectionUtils.isNotEmpty(listOne)) {
                oneList(list, listOne, sliceConfig, thisData, printData);
            } else {
                printData.put(SYSTEM_SLICE_PAGE_NO, "1-1");
                list.add(dataMap);
            }
        }
        if (newSliceConfigList.size() == 2) {
            SliceConfig sliceConfig1 = newSliceConfigList.get(0);
            SliceConfig sliceConfig2 = newSliceConfigList.get(1);
            List<Object> listOne = (List<Object>) printData.get(sliceConfig1.getDataSet());
            List<Object> listTwo = (List<Object>) printData.get(sliceConfig2.getDataSet());
            if (CollectionUtils.isNotEmpty(listOne) && CollectionUtils.isNotEmpty(listTwo)) {
                twoList(list, listOne, sliceConfig1, listTwo, sliceConfig2, thisData, printData);
            } else if (CollectionUtils.isNotEmpty(listOne)) {
                oneList(list, listOne, sliceConfig1, thisData, printData);
            } else if (CollectionUtils.isNotEmpty(listTwo)) {
                oneList(list, listTwo, sliceConfig2, thisData, printData);
            } else {
                printData.put(SYSTEM_SLICE_PAGE_NO, "1-1");
                list.add(dataMap);
            }
        }
    }

    private static void oneList(ArrayList<Object> list, List<Object> listOne, SliceConfig sliceConfig, Map<String, Object> thisData, Map<String, Object> printData) {
        List<List<Object>> lists = Lists.partition(listOne, sliceConfig.getLimit());
        int num = 1;
        for (List<Object> m : lists) {
            Map<String, Object> thisMap = new HashMap<>(thisData);
            Map<String, Object> thisPrintData = new HashMap<>(printData);
            thisPrintData.put(sliceConfig.getDataSet(), m);
            thisPrintData.put(SYSTEM_SLICE_PAGE_NO, num + "-1");
            thisMap.put(PRINT_DATA, thisPrintData);
            list.add(thisMap);
            num++;
        }
    }

    private static void twoList(ArrayList<Object> list, List<Object> listOne, SliceConfig sliceConfig1, List<Object> listTwo, SliceConfig sliceConfig2, Map<String, Object> thisData, Map<String, Object> printData) {
        List<List<Object>> lists1 = Lists.partition(listOne, sliceConfig1.getLimit());
        List<List<Object>> lists2 = Lists.partition(listTwo, sliceConfig2.getLimit());
        int l = 1;
        for (List<Object> m : lists1) {
            if (!lists2.isEmpty()) {
                int num = 1;
                for (int n = 0; n < lists2.size(); n++) {
                    List<Object> nObject = lists2.get(n);
                    Map<String, Object> thisMap = new HashMap<>(thisData);
                    Map<String, Object> thisPrintData = new HashMap<>(printData);
                    thisPrintData.put(sliceConfig1.getDataSet(), m);
                    thisPrintData.put(sliceConfig2.getDataSet(), nObject);
                    thisPrintData.put(SYSTEM_SLICE_PAGE_NO, num + "-" + lists2.size());
                    thisMap.put(PRINT_DATA, thisPrintData);
                    list.add(thisMap);
                    num++;
                }
            } else {
                Map<String, Object> thisMap = new HashMap<>(thisData);
                Map<String, Object> thisPrintData = new HashMap<>(printData);
                thisPrintData.put(sliceConfig1.getDataSet(), m);
                thisPrintData.put(SYSTEM_SLICE_PAGE_NO, l + "-1");
                thisMap.put(PRINT_DATA, thisPrintData);
                list.add(thisMap);
                l++;
            }
        }
    }

    /**
     * 查询打印列表
     *
     * @param ids
     * @return
     */
    @Operation(summary = "查询打印列表")
    @Parameter(name = "ids", description = "主键集合")
    @PostMapping("getListById")
    public List<PrintOption> getListById(@RequestBody List<String> ids) {
        return printDevService.getPrintTemplateOptions(ids);
    }

    @Operation(summary = "查询打印列表")
    @Parameter(name = "data", description = "打印模板-数查询对象")
    @PostMapping("getListOptions")
    public ActionResult<Object>getListOptions(@RequestBody PrintDevDataQuery data) {
        List<String> ids = data.getIds();
        QueryWrapper<PrintDevEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(PrintDevEntity::getId, ids);
        List<PrintDevEntity> list = printDevService.getBaseMapper().selectList(wrapper);
        List<PrintOption> options = JsonUtil.getJsonToList(list, PrintOption.class);
        return ActionResult.success(options);
    }

    //新增

    @Operation(summary = "列表预览")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("onlineDev.printDev")
    @GetMapping("/{id}/Actions/Preview")
    public ActionResult<PrintDevInfoVO> previewInfo(@PathVariable("id") String id) {
        List<PrintVersionEntity> list = versionService.getList(id);
        if (CollectionUtils.isEmpty(list)) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        PrintDevInfoVO info = printDevService.getVersionInfo(list.get(0).getId());
        PrintDevInfoVO vo = JsonUtil.getJsonToBean(info, PrintDevInfoVO.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "打印模板业务列表")
    @GetMapping("/WorkSelector")
    public ActionResult<PageListVO<PrintDevListVO>> getWorkSelector(PaginationPrint pagination) {
        List<PrintDevEntity> list = printDevService.getWorkSelector(pagination);
        List<String> collect = systemService.getList().stream().map(SystemEntity::getId).collect(Collectors.toList());
        list = list.stream().filter(t -> collect.contains(t.getSystemId())).collect(Collectors.toList());
        List<PrintDevListVO> listVO = JsonUtil.getJsonToList(list, PrintDevListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }


}
