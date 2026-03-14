package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.DictionaryTypeEntity;
import jnpf.base.model.dictionarydata.*;
import jnpf.base.model.dictionarytype.DictionaryExportModel;
import jnpf.base.model.dictionarytype.DictionaryTypeSelectModel;
import jnpf.base.model.dictionarytype.DictionaryTypeSelectVO;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.DictionaryTypeService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.util.FileUtil;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.StringUtil;
import jnpf.util.treeutil.ListToTreeUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "数据字典", description = "DictionaryData")
@RestController
@RequestMapping("/api/system/DictionaryData")
@RequiredArgsConstructor
public class DictionaryDataController extends SuperController<DictionaryDataService, DictionaryDataEntity> {

    public static final String EN_CODE = "enCode";


    private final DictionaryDataService dictionaryDataService;

    private final DictionaryTypeService dictionaryTypeService;

    private final String PERMITTION_DICTIONARY = "sysData.dictionary";

    /**
     * 获取数据字典列表
     *
     * @param dictionaryTypeId   数据字典id
     * @param pageDictionaryData 分页参数
     * @return ignore
     */
    @Operation(summary = "获取数据字典列表")
    @Parameter(name = "dictionaryTypeId", description = "数据分类id", required = true)
    @GetMapping("/{dictionaryTypeId}")
    public ActionResult<Object> bindDictionary(@PathVariable("dictionaryTypeId") String dictionaryTypeId, PageDictionaryData pageDictionaryData) {
        List<DictionaryDataEntity> data = dictionaryDataService.getList(dictionaryTypeId);
        List<DictionaryDataEntity> dataAll = data;
        if (StringUtil.isNotEmpty(pageDictionaryData.getKeyword())) {
            data = data.stream().filter(t -> t.getFullName().contains(pageDictionaryData.getKeyword()) || t.getEnCode().contains(pageDictionaryData.getKeyword())).collect(Collectors.toList());
        }
        if (pageDictionaryData.getIsTree() != null && "1".equals(pageDictionaryData.getIsTree())) {
            List<DictionaryDataEntity> treeData = JsonUtil.getJsonToList(ListToTreeUtil.treeWhere(data, dataAll), DictionaryDataEntity.class);
            List<DictionaryDataModel> voListVO = JsonUtil.getJsonToList(treeData, DictionaryDataModel.class);
            List<SumTree<DictionaryDataModel>> sumTrees = TreeDotUtils.convertListToTreeDot(voListVO);
            List<DictionaryDataListVO> list = JsonUtil.getJsonToList(sumTrees, DictionaryDataListVO.class);
            ListVO<DictionaryDataListVO> treeVo = new ListVO<>();
            treeVo.setList(list);
            return ActionResult.success(treeVo);
        }
        List<DictionaryDataModel> voListVO = JsonUtil.getJsonToList(data, DictionaryDataModel.class);
        ListVO<DictionaryDataModel> treeVo = new ListVO<>();
        treeVo.setList(voListVO);
        return ActionResult.success(treeVo);
    }


    /**
     * 获取数据字典列表
     *
     * @return ignore
     */
    @Operation(summary = "获取数据字典列表(分类+内容)")
    @GetMapping("/All")
    public ActionResult<ListVO<Map<String, Object>>> allBindDictionary() {
        List<DictionaryTypeEntity> dictionaryTypeList = dictionaryTypeService.getList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DictionaryTypeEntity dictionaryTypeEntity : dictionaryTypeList) {
            List<DictionaryDataEntity> childNodeList = dictionaryDataService.getList(dictionaryTypeEntity.getId(), true);
            if (dictionaryTypeEntity.getIsTree().compareTo(1) == 0) {
                List<Map<String, Object>> selectList = new ArrayList<>();
                for (DictionaryDataEntity item : childNodeList) {
                    Map<String, Object> ht = new HashMap<>(16);
                    ht.put("fullName", item.getFullName());
                    ht.put(EN_CODE, item.getEnCode());
                    ht.put("id", item.getId());
                    ht.put("parentId", item.getParentId());
                    selectList.add(ht);
                }
                List<DictionaryDataAllModel> jsonToList = JsonUtil.getJsonToList(selectList, DictionaryDataAllModel.class);
                //==============转换树
                List<SumTree<DictionaryDataAllModel>> list1 = TreeDotUtils.convertListToTreeDot(jsonToList);
                List<DictionaryDataAllVO> list2 = JsonUtil.getJsonToList(list1, DictionaryDataAllVO.class);
                //==============
                Map<String, Object> htItem = new HashMap<>(16);
                htItem.put("id", dictionaryTypeEntity.getId());
                htItem.put(EN_CODE, dictionaryTypeEntity.getEnCode());
                htItem.put("dictionaryList", list2);
                htItem.put("isTree", 1);
                list.add(htItem);
            } else {
                List<Map<String, Object>> selectList = new ArrayList<>();
                for (DictionaryDataEntity item : childNodeList) {
                    Map<String, Object> ht = new HashMap<>(16);
                    ht.put(EN_CODE, item.getEnCode());
                    ht.put("id", item.getId());
                    ht.put("fullName", item.getFullName());
                    selectList.add(ht);
                }
                Map<String, Object> htItem = new HashMap<>(16);
                htItem.put("id", dictionaryTypeEntity.getId());
                htItem.put(EN_CODE, dictionaryTypeEntity.getEnCode());
                htItem.put("dictionaryList", selectList);
                htItem.put("isTree", 0);
                list.add(htItem);
            }
        }
        ListVO<Map<String, Object>> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }


    /**
     * 获取数据字典下拉框数据
     *
     * @param dictionaryTypeId 类别主键
     * @param isTree           是否为树
     * @param id               主键
     * @return ignore
     */
    @Operation(summary = "获取数据字典分类下拉框数据")
    @Parameter(name = "dictionaryTypeId", description = "数据分类id", required = true)
    @Parameter(name = "isTree", description = "是否树形")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("{dictionaryTypeId}/Selector/{id}")
    public ActionResult<ListVO<DictionaryDataSelectVO>> treeView(@PathVariable("dictionaryTypeId") String dictionaryTypeId, String isTree, @PathVariable("id") String id) {
        DictionaryTypeEntity typeEntity = dictionaryTypeService.getInfo(dictionaryTypeId);
        List<DictionaryDataModel> treeList = new ArrayList<>();
        DictionaryDataModel treeViewModel = new DictionaryDataModel();
        treeViewModel.setId("0");
        treeViewModel.setFullName(typeEntity.getFullName());
        treeViewModel.setParentId("-1");
        treeViewModel.setIcon("fa fa-tags");
        treeList.add(treeViewModel);
        if ("1".equals(isTree)) {
            List<DictionaryDataEntity> data = dictionaryDataService.getList(dictionaryTypeId).stream().filter(t -> "1".equals(String.valueOf(t.getEnabledMark()))).collect(Collectors.toList());
            //过滤子集
            if (!"0".equals(id)) {
                data.remove(dictionaryDataService.getInfo(id));
            }
            for (DictionaryDataEntity entity : data) {
                DictionaryDataModel treeModel = new DictionaryDataModel();
                treeModel.setId(entity.getId());
                treeModel.setFullName(entity.getFullName());
                treeModel.setParentId("-1".equals(entity.getParentId()) ? entity.getDictionaryTypeId() : entity.getParentId());
                treeList.add(treeModel);
            }
        }
        List<SumTree<DictionaryDataModel>> sumTrees = TreeDotUtils.convertListToTreeDotFilter(treeList);
        List<DictionaryDataSelectVO> list = JsonUtil.getJsonToList(sumTrees, DictionaryDataSelectVO.class);
        ListVO<DictionaryDataSelectVO> treeVo = new ListVO<>();
        treeVo.setList(list);
        return ActionResult.success(treeVo);
    }

    /**
     * 获取字典分类
     *
     * @param dictionaryTypeId 分类id、分类编码
     * @return ignore
     */
    @Operation(summary = "获取某个字典数据下拉框列表")
    @Parameter(name = "dictionaryTypeId", description = "数据分类id", required = true)
    @GetMapping("/{dictionaryTypeId}/Data/Selector")
    public ActionResult<ListVO<DictionaryTypeSelectVO>> selectorOneTreeView(@PathVariable("dictionaryTypeId") String dictionaryTypeId) {
        List<DictionaryDataEntity> data = dictionaryDataService.getList(dictionaryTypeId, true);
        if(data.isEmpty()){
            DictionaryTypeEntity typeEntity = dictionaryTypeService.getInfoByEnCode(dictionaryTypeId);
            if(typeEntity != null){
                data = dictionaryDataService.getList(typeEntity.getId(), true);
            }

        }
        List<DictionaryTypeSelectModel> voListVO = JsonUtil.getJsonToList(data, DictionaryTypeSelectModel.class);
        List<SumTree<DictionaryTypeSelectModel>> sumTrees = TreeDotUtils.convertListToTreeDot(voListVO);
        List<DictionaryTypeSelectVO> list = JsonUtil.getJsonToList(sumTrees, DictionaryTypeSelectVO.class);
        ListVO<DictionaryTypeSelectVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 获取数据字典信息
     *
     * @param id 主键
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "获取数据字典信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @GetMapping("/{id}/Info")
    public ActionResult<DictionaryDataInfoVO> info(@PathVariable("id") String id) throws DataException {
        DictionaryDataEntity entity = dictionaryDataService.getInfo(id);
        DictionaryDataInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, DictionaryDataInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 重复验证（名称）
     *
     * @param dictionaryTypeId 类别主键
     * @param fullName         名称
     * @param id               主键值
     * @return ignore
     */
    @Operation(summary = "（待定）重复验证（名称）")
    @GetMapping("/IsExistByFullName")
    public ActionResult<Object> isExistByFullName(String dictionaryTypeId, String fullName, String id) {
        boolean data = dictionaryDataService.isExistByFullName(dictionaryTypeId, fullName, id);
        return ActionResult.success(data);
    }

    /**
     * 重复验证（编码）
     *
     * @param dictionaryTypeId 类别主键
     * @param enCode           编码
     * @param id               主键值
     * @return ignore
     */
    @Operation(summary = "（待定）重复验证（编码）")
    @GetMapping("/IsExistByEnCode")
    public ActionResult<Object> isExistByEnCode(String dictionaryTypeId, String enCode, String id) {
        boolean data = dictionaryDataService.isExistByEnCode(dictionaryTypeId, enCode, id);
        return ActionResult.success(data);
    }


    /**
     * 添加数据字典
     *
     * @param dictionaryDataCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "添加数据字典")
    @Parameter(name = "dictionaryDataCrForm", description = "实体对象", required = true)
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid DictionaryDataCrForm dictionaryDataCrForm) {
        StpUtil.checkPermissionOr(PERMITTION_DICTIONARY, dictionaryDataCrForm.getDictionaryTypeId());
        DictionaryDataEntity entity = JsonUtil.getJsonToBean(dictionaryDataCrForm, DictionaryDataEntity.class);
        if (dictionaryDataService.isExistByFullName(entity.getDictionaryTypeId(), entity.getFullName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (dictionaryDataService.isExistByEnCode(entity.getDictionaryTypeId(), entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        dictionaryDataService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改数据字典
     *
     * @param id                   主键值
     * @param dictionaryDataUpForm 实体对象
     * @return ignore
     */
    @Operation(summary = "修改数据字典")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "dictionaryDataUpForm", description = "实体对象", required = true)
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid DictionaryDataUpForm dictionaryDataUpForm) {
        StpUtil.checkPermissionOr(PERMITTION_DICTIONARY, dictionaryDataUpForm.getDictionaryTypeId());
        DictionaryDataEntity entity = JsonUtil.getJsonToBean(dictionaryDataUpForm, DictionaryDataEntity.class);
        if (dictionaryDataService.isExistByFullName(entity.getDictionaryTypeId(), entity.getFullName(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (dictionaryDataService.isExistByEnCode(entity.getDictionaryTypeId(), entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = dictionaryDataService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());

    }

    /**
     * 删除数据字典
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除数据字典")
    @Parameter(name = "id", description = "主键值", required = true)
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        DictionaryDataEntity entity = dictionaryDataService.getInfo(id);
        if (entity != null) {
            StpUtil.checkPermissionOr(PERMITTION_DICTIONARY, entity.getDictionaryTypeId());
            if (Boolean.TRUE.equals(dictionaryDataService.isExistSubset(entity.getId()))) {
                return ActionResult.fail(MsgCode.SYS014.get());
            }
            dictionaryDataService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 更新字典状态
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "更新字典状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("sysData.dictionary")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object> update(@PathVariable("id") String id) {
        DictionaryDataEntity entity = dictionaryDataService.getInfo(id);
        if (entity != null) {
            if ("1".equals(String.valueOf(entity.getEnabledMark()))) {
                entity.setEnabledMark(0);
            } else {
                entity.setEnabledMark(1);
            }
            boolean flag = dictionaryDataService.update(entity.getId(), entity);
            if (!flag) {
                return ActionResult.success(MsgCode.FA002.get());
            }
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 数据字典导出功能
     *
     * @param id 接口id
     * @return ignore
     */
    @Operation(summary = "导出数据字典数据")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("sysData.dictionary")
    @GetMapping("/{id}/Actions/Export")
    public ActionResult<Object> exportFile(@PathVariable("id") String id) {
        DownloadVO downloadVO = dictionaryDataService.exportData(id);
        return ActionResult.success(downloadVO);
    }

    /**
     * 数据字典导入功能
     *
     * @param multipartFile 文件
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "数据字典导入功能")
    @SaCheckPermission("sysData.dictionary")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> importFile(@RequestPart("file") MultipartFile multipartFile,
                                   @RequestParam("type") Integer type) throws DataException {
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.SYSTEM_DICTIONARYDATA.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        try {
            //获取文件内容
            String fileContent = FileUtil.getFileContent(multipartFile);
            DictionaryExportModel exportModel = JsonUtil.getJsonToBean(fileContent, DictionaryExportModel.class);
            List<DictionaryTypeEntity> list = exportModel.getList();
            //父级分类id不存在的话，直接抛出异常
            //如果分类只有一个
            if (list.size() == 1 && !"-1".equals(list.get(0).getParentId()) && dictionaryTypeService.getInfo(list.get(0).getParentId()) == null) {
                return ActionResult.fail(MsgCode.IMP010.get());
            }
            //如果有多个需要验证分类是否存在
            if (list.stream().filter(t -> "-1".equals(t.getParentId())).count() < 1) {
                boolean exist = false;
                for (DictionaryTypeEntity dictionaryTypeEntity : list) {
                    //判断父级是否存在
                    if (dictionaryTypeService.getInfo(dictionaryTypeEntity.getParentId()) != null) {
                        exist = true;
                    }
                }
                if (!exist) {
                    return ActionResult.fail(MsgCode.IMP010.get());
                }
            }
            //判断数据是否存在
            return dictionaryDataService.importData(exportModel, type);
        } catch (Exception e) {
            throw new DataException(MsgCode.IMP004.get());
        }
    }

}
