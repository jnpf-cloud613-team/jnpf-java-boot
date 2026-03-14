package jnpf.base.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.*;
import jnpf.base.model.*;
import jnpf.base.model.form.VisualFieldModel;
import jnpf.base.model.module.ModuleNameVO;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.model.online.VisualMenuModel;
import jnpf.base.service.*;
import jnpf.base.util.VisualUtil;
import jnpf.base.util.visualutil.PubulishUtil;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.GenerateConstant;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.model.template.FlowByFormModel;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormCloumnUtil;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.RecursionForm;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.service.VisualdevModelDataService;
import jnpf.onlinedev.util.OnlinePublicUtils;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.context.RequestContext;
import jnpf.util.enums.DictionaryDataEnum;
import jnpf.util.visiual.JnpfKeyConsts;
import jnpf.workflow.service.TemplateApi;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化基础模块
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "可视化基础模块", description = "Base")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/Base")
public class VisualdevController extends SuperController<VisualdevService, VisualdevEntity> {

    private final VisualdevService visualdevService;
    private final VisualdevReleaseService visualdevReleaseService;
    private final UserService userService;
    private final DictionaryDataService dictionaryDataApi;
    private final VisualdevModelDataService visualdevModelDataService;
    private final PubulishUtil pubulishUtil;
    private final ModuleService moduleService;
    private final DataInterfaceService dataInterFaceApi;
    private final VisualAliasService aliasService;
    private final WorkFlowApi workFlowApi;
    private final TemplateApi templateApi;
    private final SystemService systemService;

    @Operation(summary = "获取功能列表")
    @GetMapping
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<PageListVO<VisualFunctionModel>> list(PaginationVisualdev paginationVisualdev) {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        paginationVisualdev.setSystemId(infoByEnCode.getId());
        // 全部功能表单模板
        List<VisualdevEntity> data = visualdevService.getList(paginationVisualdev);
        List<String> userId = data.stream().map(t -> t.getCreatorUserId()).collect(Collectors.toList());
        List<String> lastUserId = data.stream().map(t -> t.getLastModifyUserId()).collect(Collectors.toList());
        List<UserEntity> userEntities = userService.getUserName(userId);
        List<UserEntity> lastUserIdEntities = userService.getUserName(lastUserId);
        // 表单类型
        List<DictionaryDataEntity> dictionList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        List<VisualFunctionModel> modelAll = new LinkedList<>();

        // 遍历功能表单模板
        for (VisualdevEntity entity : data) {
            VisualFunctionModel model = JsonUtil.getJsonToBean(entity, VisualFunctionModel.class);
            // 是否在表单类型中存在，若存在进行装配
            DictionaryDataEntity dataEntity = dictionList.stream().filter(t -> t.getId().equals(entity.getCategory())).findFirst().orElse(null);
            //避免导入的功能丢失
            model.setCategory(dataEntity != null ? dataEntity.getFullName() : null);
            UserEntity creatorUser = userEntities.stream().filter(t -> t.getId().equals(model.getCreatorUserId())).findFirst().orElse(null);
            model.setCreatorUser(creatorUser != null ? creatorUser.getRealName() + "/" + creatorUser.getAccount() : "");
            UserEntity lastmodifyuser = lastUserIdEntities.stream().filter(t -> t.getId().equals(model.getLastModifyUserId())).findFirst().orElse(null);
            model.setLastModifyUser(lastmodifyuser != null ? lastmodifyuser.getRealName() + "/" + lastmodifyuser.getAccount() : "");
            model.setIsRelease(entity.getState());
            model.setHasPackage(true);
            modelAll.add(model);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationVisualdev, PaginationVO.class);
        return ActionResult.page(modelAll, paginationVO);
    }

    @Operation(summary = "获取功能列表")
    @GetMapping("/list")
    public ActionResult<PageListVO<VisualDevListVO>> getList(PaginationVisualdev paginationVisualdev) {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        paginationVisualdev.setSystemId(infoByEnCode.getId());
        paginationVisualdev.setEnableFlow(1);
        List<VisualdevEntity> data = visualdevService.getPageList(paginationVisualdev);
        List<VisualDevListVO> modelAll = JsonUtil.getJsonToList(data, VisualDevListVO.class);

        //流程发起节点-调用弹窗添加系统表单是否引用
        if (Boolean.TRUE.equals(paginationVisualdev.getFlowStart())) {
            List<String> flowFormStart = workFlowApi.getFormList();//流程发起节点的表单列表
            for (VisualDevListVO item : modelAll) {
                if (Objects.equals(item.getType(), 2)) {//系统表单被引用不能再选
                    boolean contains = flowFormStart.contains(item.getId());
                    item.setIsQuote(contains ? 1 : 0);
                }
            }
        }

        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationVisualdev, PaginationVO.class);
        return ActionResult.page(modelAll, paginationVO);
    }


    @Operation(summary = "获取功能列表下拉框")
    @Parameter(name = "type", description = "类型(1-表单设计，2-系统表单)")
    @Parameter(name = "isRelease", description = "是否发布")
    @Parameter(name = "webType", description = "页面类型（1、纯表单，2、表单加列表，3、表单列表工作流、4、数据视图）")
    @Parameter(name = "enableFlow", description = "是否启用流程")
    @GetMapping("/Selector")
    public ActionResult<ListVO<VisualdevTreeVO>> selectorList(Integer type, Integer isRelease, String webType, Integer enableFlow) {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        List<VisualdevEntity> allList;
        List<VisualdevEntity> list = new ArrayList<>();
        List<VisualdevTreeVO> voList = new ArrayList<>();
        String systemId = infoByEnCode.getId();
        if (Objects.equals(infoByEnCode.getIsMain(), 1)) {
            systemId = "";
        }
        if (isRelease != null) {
            List<VisualdevReleaseEntity> releaseEntities = visualdevReleaseService.selectorList(systemId);
            allList = JsonUtil.getJsonToList(releaseEntities, VisualdevEntity.class);
        } else {
            allList = visualdevService.selectorList(systemId);
        }
        if (webType != null) {
            String[] webTypes = webType.split(",");
            for (String wbType : webTypes) {
                List<VisualdevEntity> collect = allList.stream().filter(l -> l.getWebType().equals(Integer.valueOf(wbType))).collect(Collectors.toList());
                list.addAll(collect);
            }
        } else {
            list = allList;
        }
        //主系统，套件关联表单用应用分类
        if (Objects.equals(infoByEnCode.getIsMain(), 1)) {
            List<String> sysIds = list.stream().map(VisualdevEntity::getSystemId).collect(Collectors.toList());
            List<SystemEntity> listByIds = systemService.getListByIds(sysIds, null);
            for (SystemEntity se : listByIds) {
                VisualdevTreeVO vo = JsonUtil.getJsonToBean(se, VisualdevTreeVO.class);
                List<VisualdevTreeVO> childList = new ArrayList<>();
                for (VisualdevEntity entity : list) {
                    if (vo.getId().equals(entity.getSystemId())) {
                        VisualdevTreeVO model = JsonUtil.getJsonToBean(entity, VisualdevTreeVO.class);
                        childList.add(model);
                    }
                }
                if (CollUtil.isNotEmpty(childList)) {
                    vo.setChildren(childList);
                    voList.add(vo);
                }
            }
            ListVO<VisualdevTreeVO> listVO = new ListVO<>();
            listVO.setList(voList);
            return ActionResult.success(listVO);
        }

        //非主系统，关联表单用category分类
        getNotMainCategory(type, list, voList);

        ListVO<VisualdevTreeVO> listVO = new ListVO<>();
        listVO.setList(voList);
        return ActionResult.success(listVO);
    }

    private void getNotMainCategory(Integer type, List<VisualdevEntity> list, List<VisualdevTreeVO> voList) {
        List<DictionaryDataEntity> dataEntityList;
        HashSet<String> cate = new HashSet<>(16);
        if (type != null) {
            list = list.stream().filter(t -> type.equals(t.getType())).collect(Collectors.toList());
            dataEntityList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.getTypeId(type));
            // 遍历数据字典得到外部分类
            for (DictionaryDataEntity dataEntity : dataEntityList) {
                List<VisualdevEntity> num = list.stream().filter(t -> dataEntity.getId().equals(t.getCategory())).collect(Collectors.toList());
                if (num.isEmpty()) {
                    continue;
                }
                int i = cate.size();
                cate.add(dataEntity.getId());
                if (cate.size() == i + 1) {
                    VisualdevTreeVO visualdevTreeVO = new VisualdevTreeVO();
                    visualdevTreeVO.setId(dataEntity.getId());
                    visualdevTreeVO.setFullName(dataEntity.getFullName());
                    visualdevTreeVO.setHasChildren(true);
                    voList.add(visualdevTreeVO);
                }
            }
        } else {
            // type为空时
            for (VisualdevEntity entity : list) {
                DictionaryDataEntity dataEntity = dictionaryDataApi.getInfo(entity.getCategory());
                if (dataEntity != null) {
                    int i = cate.size();
                    cate.add(dataEntity.getId());
                    if (cate.size() == i + 1) {
                        VisualdevTreeVO visualdevTreeVO = new VisualdevTreeVO();
                        visualdevTreeVO.setId(entity.getCategory());
                        visualdevTreeVO.setFullName(dataEntity.getFullName());
                        visualdevTreeVO.setHasChildren(true);
                        voList.add(visualdevTreeVO);
                    }
                }

            }
        }
        for (VisualdevTreeVO vo : voList) {
            List<VisualdevTreeVO> childList = new ArrayList<>();
            for (VisualdevEntity entity : list) {
                if (vo.getId().equals(entity.getCategory())) {
                    VisualdevTreeVO model = JsonUtil.getJsonToBean(entity, VisualdevTreeVO.class);
                    childList.add(model);
                }
            }
            vo.setChildren(childList);
        }
    }

    @Operation(summary = "获取功能信息")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> info(@PathVariable("id") String id) throws DataException {
        VisualdevEntity entity = visualdevService.getInfo(id);
        VisualDevInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, VisualDevInfoVO.class);
        if (StringUtil.isNotEmpty(entity.getInterfaceId())) {
            DataInterfaceEntity info = dataInterFaceApi.getInfo(entity.getInterfaceId());
            if (info != null) {
                vo.setInterfaceName(info.getFullName());
            }
        }
        return ActionResult.success(vo);
    }

    /**
     * 获取表单主表属性下拉框
     *
     * @param id
     * @return
     */
    @Operation(summary = "获取表单主表属性下拉框")
    @Parameter(name = "id", description = "主键")
    @Parameter(name = "filterType", description = "过滤类型：1-按键事件选择字段列表过滤")
    @GetMapping("/{id}/FormDataFields")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<ListVO<FormDataField>> getFormData(@PathVariable("id") String id, @RequestParam(value = "filterType", required = false) Integer filterType) {
        List<FormDataField> fieldList = visualdevModelDataService.fieldList(id, filterType);
        ListVO<FormDataField> listVO = new ListVO<>();
        listVO.setList(fieldList);
        return ActionResult.success(listVO);
    }

    /**
     * 关联数据分页数据
     *
     * @param id
     * @param paginationModel
     * @return
     */
    @Operation(summary = "关联数据分页数据")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}/FieldDataSelect")
    public ActionResult<PageListVO<Map<String, Object>>> getFormData(@PathVariable("id") String id, PaginationModel paginationModel) {
        VisualdevEntity entity = visualdevService.getReleaseInfo(id);
        List<Map<String, Object>> realList = visualdevModelDataService.getPageList(entity, paginationModel);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationModel, PaginationVO.class);
        return ActionResult.page(realList, paginationVO);
    }


    /**
     * 复制功能
     *
     * @param id
     * @return
     */
    @Operation(summary = "复制功能")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Actions/Copy")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> copyInfo(@PathVariable("id") String id) {
        VisualdevReleaseEntity releaseEntity = visualdevReleaseService.getById(id);
        boolean b = releaseEntity != null;
        VisualdevEntity entity;
        String uuid = RandomUtil.uuId();
        //已发布取发布版本
        if (b) {
            entity = JsonUtil.getJsonToBean(releaseEntity, VisualdevEntity.class);
            //已发布复制命名规范
            aliasService.copy(releaseEntity.getId(), uuid);
        } else {
            entity = visualdevService.getInfo(id);
        }
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        entity.setFullName(entity.getFullName() + ".副本" + copyNum);
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        entity.setCreatorTime(null);
        entity.setId(uuid);
        entity.setEnCode(entity.getEnCode() + copyNum);
        VisualdevEntity entity1 = JsonUtil.getJsonToBean(entity, VisualdevEntity.class);
        if (entity1.getEnCode().length() > 50 || entity1.getFullName().length() > 50) {
            return ActionResult.fail(MsgCode.PRI006.get());
        }
        visualdevService.create(entity1);
        return ActionResult.success(MsgCode.SU007.get());
    }


    /**
     * 更新功能状态
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "更新功能状态")
    @Parameter(name = "id", description = "主键")
    @PutMapping("/{id}/Actions/State")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> update(@PathVariable("id") String id) throws SQLException, WorkFlowException {
        VisualdevEntity entity = visualdevService.getInfo(id);
        if (entity != null) {
            boolean flag = visualdevService.update(entity.getId(), entity);
            if (!flag) {
                return ActionResult.fail(MsgCode.FA002.get());
            }
        }
        return ActionResult.success(MsgCode.SU004.get());
    }


    @Operation(summary = "新建功能")
    @PostMapping
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> create(@RequestBody VisualDevCrForm visualDevCrForm) {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        VisualdevEntity entity = JsonUtil.getJsonToBean(JsonUtilEx.getObjectToString(visualDevCrForm), VisualdevEntity.class);
        if (GenerateConstant.containKeyword(entity.getFullName())) {
            return ActionResult.fail("表单名称" + MsgCode.SYS128.get(entity.getFullName()));
        }
        if (Boolean.TRUE.equals(visualdevService.getObjByEncode(entity.getEnCode(), entity.getType()))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (Boolean.TRUE.equals(visualdevService.getCountByName(entity.getFullName(), entity.getType(), infoByEnCode.getId()))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())
                && !OnlineDevData.FORM_TYPE_SYS.equals(entity.getType())) {
            List<TableModel> tableModelList = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
            FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);

            //判断子表是否复用
            RecursionForm recursionForm = new RecursionForm();
            if (ObjectUtil.isNotNull(formData)) {
                //判断有表是否满足主键策略
                if (!tableModelList.isEmpty()) {
                    boolean isIncre = Objects.equals(formData.getPrimaryKeyPolicy(), 2);
                    String strategy = !isIncre ? "[雪花ID]" : "[自增长id]";
                    for (TableModel tableModel : tableModelList) {
                        boolean isAutoIncre = visualdevService.getPrimaryDbField(entity.getDbLinkId(), tableModel.getTable());
                        if (!isAutoIncre) {
                            return ActionResult.fail(MsgCode.FM011.get(tableModel.getTable()));
                        }
                        if (isIncre != isAutoIncre) {
                            return ActionResult.fail(MsgCode.FM012.get(strategy, tableModel.getTable()));
                        }
                    }
                }

                List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
                recursionForm.setList(list);
                recursionForm.setTableModelList(tableModelList);
                if (FormCloumnUtil.repetition(recursionForm, new ArrayList<>())) {
                    return ActionResult.fail(MsgCode.FM003.get());
                }

                //字段判断
                List<FieLdsModel> fields = new ArrayList<>();
                OnlinePublicUtils.getAllFields(fields, list);
                StringJoiner sj = new StringJoiner(",");
                for (FieLdsModel item : fields) {
                    if (StringUtil.isNotEmpty(item.getVModel()) && GenerateConstant.containKeyword(item.getVModel())) {
                        sj.add(item.getConfig().getLabel() + "-" + item.getVModel());
                    }
                }
                if (StringUtil.isNotEmpty(sj.toString())) {
                    return ActionResult.fail(MsgCode.SYS128.get(sj));
                }
            }
        }
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setSystemId(systemService.getInfoByEnCode(RequestContext.getAppCode()).getId());
        visualdevService.create(entity);

        return ActionResult.success(MsgCode.SU001.get(), new VisualDevInfoVO(entity.getId(), entity.getEnCode()));
    }

    @Operation(summary = "修改功能")
    @Parameter(name = "id", description = "主键")
    @PutMapping("/{id}")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody VisualDevUpForm visualDevUpForm) throws SQLException, WorkFlowException {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        VisualdevEntity visualdevEntity = visualdevService.getInfo(id);
        VisualdevEntity entity = JsonUtil.getJsonToBean(JsonUtilEx.getObjectToString(visualDevUpForm), VisualdevEntity.class);
        entity.setState(visualdevEntity.getState());
        if (GenerateConstant.containKeyword(entity.getFullName())) {
            return ActionResult.fail("表单名称" + MsgCode.SYS128.get(entity.getFullName()));
        }
        if (!Objects.equals(entity.getEnCode(), visualdevEntity.getEnCode())
                && Boolean.TRUE.equals(visualdevService.getObjByEncode(entity.getEnCode(), entity.getType()))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (!Objects.equals(entity.getFullName(), visualdevEntity.getFullName())
                && Boolean.TRUE.equals(visualdevService.getCountByName(entity.getFullName(), entity.getType(), infoByEnCode.getId()))) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }

        VisualdevReleaseEntity releaseEntity = visualdevReleaseService.getById(id);

        // 如果不是在线的,默认更新所有配置
        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())
                && !OnlineDevData.FORM_TYPE_SYS.equals(entity.getType())) {

            //已发布修改的时候，把表移除掉的时候需要提示选表
            List<TableModel> tableModelList = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
            if (releaseEntity != null && !VisualWebTypeEnum.DATA_VIEW.getType().equals(releaseEntity.getWebType())
                    && tableModelList.isEmpty()) {
                return ActionResult.fail(MsgCode.VS408.get());
            }
            //判断子表是否复用
            if (ObjectUtil.isNotNull(entity.getFormData())) {
                FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
                List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
                if (!tableModelList.isEmpty()) {
                    RecursionForm recursionForm = new RecursionForm();
                    recursionForm.setList(list);
                    recursionForm.setTableModelList(tableModelList);
                    if (FormCloumnUtil.repetition(recursionForm, new ArrayList<>())) {
                        return ActionResult.fail(MsgCode.FM003.get());
                    }
                }
                //字段判断
                List<FieLdsModel> fields = new ArrayList<>();
                OnlinePublicUtils.getAllFields(fields, list);
                StringJoiner sj = new StringJoiner(",");
                for (FieLdsModel item : fields) {
                    if (StringUtil.isNotEmpty(item.getVModel()) && GenerateConstant.containKeyword(item.getVModel())) {
                        sj.add(item.getConfig().getLabel() + "-" + item.getVModel());
                    }
                }
                if (StringUtil.isNotEmpty(sj.toString())) {
                    return ActionResult.fail(MsgCode.SYS128.get(sj));
                }
            }
        }

        //修改状态
        boolean released = Objects.equals(visualdevEntity.getState(), 1);
        if (visualdevEntity != null && released) {
            entity.setState(2);
        }
        boolean flag = visualdevService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        } else {
            visualdevService.initFlowState(entity);
        }
        return ActionResult.success(MsgCode.SU004.get(), new VisualDevInfoVO(entity.getId(), entity.getEnCode()));
    }


    @Operation(summary = "删除功能")
    @Parameter(name = "id", description = "主键")
    @DeleteMapping("/{id}")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        VisualdevEntity entity = visualdevService.getInfo(id);
        if (entity != null) {
            FlowByFormModel flowByFormId = templateApi.getFlowByFormId(id, false);
            if (flowByFormId != null && flowByFormId.getIsConfig()) {
                return ActionResult.fail(MsgCode.FM005.get());
            }
            if (Objects.equals(entity.getType(), 2) && !moduleService.getModuleList(id).isEmpty()) {
                return ActionResult.fail(MsgCode.FM014.get());
            }
            visualdevService.removeById(id);
            visualdevReleaseService.removeById(id);
            aliasService.removeByVisualId(id);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }


    @Operation(summary = "发布模板")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Actions/Release")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    @DSTransactional(rollbackFor = Exception.class)
    public ActionResult<Object> publish(@PathVariable("id") String id) {
        VisualdevEntity visualdevEntity = visualdevService.getInfo(id);
        if (GenerateConstant.containKeyword(visualdevEntity.getFullName())) {
            return ActionResult.fail("表单名称" + MsgCode.SYS128.get(visualdevEntity.getFullName()));
        }
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);

        String s = VisualUtil.checkPublishVisualModel(visualdevEntity, MsgCode.VS004.get());
        if (s != null) {
            return ActionResult.fail(s);
        }
        //数据视图没有formdata  系统表单不需要创表
        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(visualdevEntity.getWebType())
                && !OnlineDevData.FORM_TYPE_SYS.equals(visualdevEntity.getType())) {

            if (ObjectUtil.isNotNull(visualdevEntity.getFormData())) {
                FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
                List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
                //字段判断
                List<FieLdsModel> fields = new ArrayList<>();
                OnlinePublicUtils.getAllFields(fields, list);
                StringJoiner sj = new StringJoiner(",");
                for (FieLdsModel item : fields) {
                    if (StringUtil.isNotEmpty(item.getVModel()) && GenerateConstant.containKeyword(item.getVModel())) {
                        sj.add(item.getConfig().getLabel() + "-" + item.getVModel());
                    }
                }
                if (StringUtil.isNotEmpty(sj.toString())) {
                    return ActionResult.fail(MsgCode.SYS128.get(sj));
                }
            }

            if (tableModels.isEmpty()) {
                try {
                    visualdevService.createTable(visualdevEntity);
                } catch (WorkFlowException e) {
                    throw new DataException(MsgCode.VS003.get());
                } catch (SQLException | DataException e) {
                    throw new DataException(e.getMessage());
                }
            }
        }
        //线上
        visualdevEntity.setState(1);
        visualdevEntity.setEnabledMark(1);
        visualdevService.updateById(visualdevEntity);
        //复制旧版本信息存储

        VisualdevReleaseEntity newRelease = new VisualdevReleaseEntity();
        newRelease.setVisualTables(visualdevEntity.getVisualTables());
        newRelease.setFormData(visualdevEntity.getFormData());
        newRelease.setColumnData(visualdevEntity.getColumnData());
        newRelease.setAppColumnData(visualdevEntity.getAppColumnData());
        newRelease.setWebType(visualdevEntity.getWebType());
        newRelease.setDbLinkId(visualdevEntity.getDbLinkId());
        String newContent = JsonUtil.getObjectToString(newRelease);

        VisualdevReleaseEntity byId = visualdevReleaseService.getById(visualdevEntity.getId());
        if (byId != null) {
            VisualdevReleaseEntity oldRelease = new VisualdevReleaseEntity();
            oldRelease.setVisualTables(byId.getVisualTables());
            oldRelease.setFormData(byId.getFormData());
            oldRelease.setColumnData(byId.getColumnData());
            oldRelease.setAppColumnData(byId.getAppColumnData());
            oldRelease.setWebType(byId.getWebType());
            oldRelease.setDbLinkId(byId.getDbLinkId());
            String oldContent = JsonUtil.getObjectToString(oldRelease);
            if (oldContent.equals(newContent)) {
                newContent = byId.getOldContent();
            } else {
                newContent = oldContent;
            }
        }
        VisualdevEntity clone = new VisualdevEntity();
        BeanUtil.copyProperties(visualdevEntity, clone);
        VisualdevReleaseEntity releaseEntity = JsonUtil.getJsonToBean(clone, VisualdevReleaseEntity.class);
        releaseEntity.setOldContent(newContent);
        visualdevReleaseService.setIgnoreLogicDelete().saveOrUpdate(releaseEntity);
        visualdevReleaseService.clearIgnoreLogicDelete();
        return ActionResult.success(MsgCode.SU011.get());
    }


    @Operation(summary = "生成菜单")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Actions/Module")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    @DSTransactional(rollbackFor = Exception.class)
    public ActionResult<Object> createModule(@PathVariable("id") String id, @RequestBody VisualDevPubModel visualDevPubModel) throws WorkFlowException {
        VisualdevEntity visualdevEntity = visualdevService.getInfo(id);
        if (visualdevEntity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        visualdevEntity.setPlatformRelease(visualDevPubModel.getPlatformRelease());
        visualdevService.updateById(visualdevEntity);

        //将线上版本发布
        VisualdevEntity clone = BeanUtil.copyProperties(visualdevEntity, VisualdevEntity.class);
        VisualMenuModel visual;
        if (Objects.equals(visualdevEntity.getType(), 1)) {
            visual = VisualUtil.getVisual(clone, visualDevPubModel);
            visual.setType(3);
            visual.setWebType(visualdevEntity.getWebType());
        } else {
            visual = VisualUtil.getVisualHC(clone);
            visual.setType(11);
            visual.setWebAddress(clone.getWebAddress());
            visual.setAppAddress(clone.getAppAddress());
        }
        visual.setApp(visualDevPubModel.getApp());
        visual.setPc(visualDevPubModel.getPc());
        visual.setPcModuleParentId(visualDevPubModel.getPcModuleParentId());
        visual.setAppModuleParentId(visualDevPubModel.getAppModuleParentId());
        pubulishUtil.publishMenu(visual);
        return ActionResult.success(MsgCode.VS007.get());
    }

    @Operation(summary = "回滚模板")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}/Actions/RollbackTemplate")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<Object> rollbackTemplate(@PathVariable("id") String id) {
        VisualdevReleaseEntity releaseEntity = visualdevReleaseService.getById(id);
        boolean b = releaseEntity == null;
        if (b) {
            return ActionResult.fail(MsgCode.VS008.get());
        } else {
            VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(releaseEntity, VisualdevEntity.class);
            visualdevService.updateById(visualdevEntity);
        }
        return ActionResult.success(MsgCode.SU020.get());
    }

    @Operation(summary = "获取已发布菜单")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}/getReleaseMenu")
    @SaCheckPermission(value = {"onlineDev.formDesign", "onlineDev.sysForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<VisualFunctionModel> getReleaseMenu(@PathVariable("id") String id) {
        VisualdevEntity entity = visualdevService.getById(id);
        if (entity != null) {
            VisualFunctionModel model = JsonUtil.getJsonToBean(entity, VisualFunctionModel.class);
            model.setAppIsRelease(0);
            model.setPcIsRelease(0);
            ModuleNameVO moduleNameVO = moduleService.getModuleNameList(entity.getId());
            if (moduleNameVO != null) {
                if (StringUtil.isNotEmpty(moduleNameVO.getPcNames())) {
                    model.setPcIsRelease(1);
                    model.setPcReleaseName(moduleNameVO.getPcNames());
                }
                if (StringUtil.isNotEmpty(moduleNameVO.getAppNames())) {
                    model.setAppIsRelease(1);
                    model.setAppReleaseName(moduleNameVO.getAppNames());
                }
            }
            return ActionResult.success(model);
        }
        return ActionResult.fail(MsgCode.FA012.get());
    }

    @Operation(summary = "获取列表字段")
    @Parameter(name = "menuId", description = "菜单id")
    @GetMapping("/getColumnList")
    public ActionResult<List<VisualFieldModel>> getColumnList(@RequestParam("menuId") String menuId) {
        ModuleEntity info = moduleService.getInfo(menuId);
        //3-功能表单，11-回传表单，9-流程表单
        if (info == null || !Arrays.asList(3, 9, 11).contains(info.getType())) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        String formId = info.getModuleId();
        PropertyJsonModel model = JsonUtil.getJsonToBean(info.getPropertyJson(), PropertyJsonModel.class);
        if (Objects.equals(info.getType(), 9)) {
            formId = templateApi.getFormByFlowId(model.getModuleId());
        }
        if (StringUtil.isEmpty(formId)) {
            formId = model.getModuleId();
        }
        VisualdevReleaseEntity byId = visualdevReleaseService.getById(formId);
        if (byId == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }

        List<VisualFieldModel> list = new ArrayList<>();
        if (Objects.equals(byId.getType(), 1)) {
            //在线表单 -- 列表
            if (Objects.equals(byId.getWebType(), 2)) {
                FormDataModel formData = JsonUtil.getJsonToBean(byId.getFormData(), FormDataModel.class);
                List<FieLdsModel> fieLdsModelList = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
                List<FieLdsModel> allFields = new ArrayList<>();
                OnlinePublicUtils.recursionFormChildFields(allFields, fieLdsModelList);
                for (FieLdsModel field : allFields) {
                    String tableName = StringUtil.isNotEmpty(field.getConfig().getRelationTable()) ? field.getConfig().getRelationTable() : field.getConfig().getTableName();
                    VisualFieldModel vf = new VisualFieldModel();
                    vf.setFieldId(field.getVModel());
                    vf.setFieldName(field.getConfig().getLabel());
                    vf.setJnpfKey(field.getConfig().getJnpfKey());
                    vf.setMultiple(field.getMultiple());
                    vf.setTableName(tableName);
                    list.add(vf);
                }
            }
        } else {
            //回传表单
            List<VisualFieldModel> fieldList = StringUtil.isNotEmpty(byId.getFormData()) ?
                    JsonUtil.getJsonToList(byId.getFormData(), VisualFieldModel.class).stream().filter(t ->
                            !(t.getFieldId().toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX) && !t.getFieldId().contains("-"))).collect(Collectors.toList())
                    : new ArrayList<>();
            list.addAll(fieldList);
        }
        return ActionResult.success(list);
    }

    @Operation(summary = "获取应用下的功能列表")
    @GetMapping("/getListBySystem")
    public ActionResult<PageListVO<VisualDevListVO>> getListBySystem(PaginationVisualdev paginationVisualdev) {
        SystemEntity info = systemService.getInfo(paginationVisualdev.getSystemId());
        List<VisualdevEntity> data = visualdevService.getListBySystem(paginationVisualdev);
        List<VisualDevListVO> modelAll = JsonUtil.getJsonToList(data, VisualDevListVO.class);
        if (info != null) {
            modelAll.stream().forEach(t -> t.setShowName(info.getFullName() + "/" + t.getFullName()));
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationVisualdev, PaginationVO.class);
        return ActionResult.page(modelAll, paginationVO);
    }
}
