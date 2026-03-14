package jnpf.flowable.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.export.TemplateExportVo;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.enums.TemplateJsonStatueEnum;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.template.*;
import jnpf.flowable.model.templatejson.FlowListModel;
import jnpf.flowable.model.templatejson.TemplateJsonExportModel;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatejson.TemplateJsonSelectVO;
import jnpf.flowable.model.templatenode.TemplateNodeCrFrom;
import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.*;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.OperatorUtil;
import jnpf.flowable.util.ServiceUtil;
import jnpf.model.FlowWorkListVO;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.model.user.WorkHandoverModel;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "流程模板", description = "flowTemplate")
@RestController
@RequestMapping("/api/workflow/template")
@RequiredArgsConstructor
public class TemplateController extends SuperController<TemplateService, TemplateEntity> implements TemplateApi {


    private final  ServiceUtil serviceUtil;

    private final  OperatorUtil operatorUtil;


    private final  TemplateService templateService;

    private final  TemplateJsonService templateJsonService;

    private final  CommonService commonService;

    private final  OperatorService operatorService;

    private final  TemplateUseNumService templateUseNumService;

    private final  FlowUtil flowUtil;

    /**
     * 流程列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "流程列表")
    @GetMapping
    public ActionResult<PageListVO<TemplatePageLisVO>> list(TemplatePagination pagination) {
        pagination.setSystemId(serviceUtil.getSystemCodeById(RequestContext.getAppCode()));
        List<TemplateEntity> list = templateService.getList(pagination);
        List<DictionaryDataEntity> dictionList = serviceUtil.getDictionName(list.stream().map(TemplateEntity::getCategory).collect(Collectors.toList()));
        List<UserEntity> userList = serviceUtil.getUserName(list.stream().map(TemplateEntity::getCreatorUserId).collect(Collectors.toList()));
        List<TemplatePageLisVO> listVO = new ArrayList<>();
        for (TemplateEntity entity : list) {
            TemplatePageLisVO vo = JsonUtil.getJsonToBean(entity, TemplatePageLisVO.class);
            DictionaryDataEntity dataEntity = dictionList.stream().filter(t -> t.getId().equals(entity.getCategory())).findFirst().orElse(null);
            vo.setCategory(dataEntity != null ? dataEntity.getFullName() : "");
            UserEntity userEntity = userList.stream().filter(t -> t.getId().equals(entity.getCreatorUserId())).findFirst().orElse(null);
            vo.setCreatorUser(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
            listVO.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }

    /**
     * 流程列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "流程列表")
    @GetMapping("/Selector")
    public ActionResult<PageListVO<TemplatePageVo>> selector(TemplatePagination pagination) {
        List<TemplatePageVo> list = templateService.getSelector(pagination);
        //添加应用信息
        List<String> sysIds = list.stream().map(TemplatePageVo::getSystemId).collect(Collectors.toList());
        Map<String, SystemEntity> sysMap = serviceUtil.getSystemList(sysIds).stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        list.stream().forEach(t -> t.setSystemName(sysMap.get(t.getSystemId()) != null ? sysMap.get(t.getSystemId()).getFullName() : ""));
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    @Operation(summary = "常用流程前六")
    @GetMapping("/useNumSelect")
    public ActionResult<List<TemplateUseNumVo>> useNumSelector() {
        AuthorizeVO authorize = serviceUtil.getAuthorizeByUser();
        List<TemplateUseNumVo> menuUseNum = templateUseNumService.getMenuUseNum(0, authorize.getFlowIdList());
        return ActionResult.success(menuUseNum);
    }

    @Operation(summary = "用户访问流程次数记录")
    @PostMapping("/useTemplateNum/{templateId}")
    public ActionResult<Object> useTemplateNum(@PathVariable String templateId) {
        templateUseNumService.insertOrUpdateUseNum(templateId);
        return ActionResult.success();
    }

    @Operation(summary = "用户访问流程次数清空")
    @DeleteMapping("/useTemplateNum/{templateId}")
    public ActionResult<Object> deleteUseTemplateNum(@PathVariable String templateId) {
        String userId = UserProvider.getUser().getUserId();
        templateUseNumService.deleteUseNum(templateId, userId);
        return ActionResult.success();
    }

    /**
     * 常用流程树
     */
    @Operation(summary = "常用流程树")
    @GetMapping("/CommonFlowTree")
    public ActionResult<Object> getTreeCommon() {
        ListVO<TemplateTreeListVo> vo = new ListVO<>();
        vo.setList(templateService.getTreeCommon());
        return ActionResult.success(vo);
    }

    /**
     * 树形列表
     */
    @Operation(summary = "树形列表")
    @GetMapping("/TreeList")
    public ActionResult<ListVO<TemplateTreeListVo>> treeList(@RequestParam(value = "formType", required = false) Integer formType) {
        ListVO<TemplateTreeListVo> vo = new ListVO<>();
        vo.setList(templateService.treeList(formType));
        return ActionResult.success(vo);
    }

    /**
     * 流程基础信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "流程基础信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<TemplateInfoVO> info(@PathVariable("id") String id) throws WorkFlowException {
        TemplateEntity entity = templateService.getInfo(id);
        TemplateInfoVO vo = JsonUtil.getJsonToBean(entity, TemplateInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 流程版本列表
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "流程版本列表")
    @GetMapping("/Version/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<List<TemplateJsonSelectVO>> version(@PathVariable("id") String id)  {
        List<TemplateJsonEntity> list = templateJsonService.getList(id).stream().filter(e -> ObjectUtil.isNotEmpty(e.getVersion())).collect(Collectors.toList());
        List<TemplateJsonSelectVO> listVO = new ArrayList<>();
        for (TemplateJsonEntity jsonEntity : list) {
            TemplateJsonSelectVO vo = JsonUtil.getJsonToBean(jsonEntity, TemplateJsonSelectVO.class);
            vo.setFlowVersion(jsonEntity.getVersion());
            vo.setFullName("流程版本V" + jsonEntity.getVersion());
            listVO.add(vo);
        }
        return ActionResult.success(listVO);
    }

    /**
     * 流程模板信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "流程模板信息")
    @GetMapping("/Info/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<TemplateJsonInfoVO> templateJsonInfo(@PathVariable("id") String id) throws WorkFlowException {
        return ActionResult.success(templateJsonService.getInfoVo(id));
    }

    /**
     * 新建流程
     *
     * @param form 流程模型
     * @return
     */
    @Operation(summary = "新建流程")
    @PostMapping
    @Parameter(name = "form", description = "流程模型", required = true)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> create(@RequestBody @Valid TemplateNodeCrFrom form) throws WorkFlowException {
        TemplateEntity entity = JsonUtil.getJsonToBean(form, TemplateEntity.class);
        String flowConfig = form.getFlowConfig();
        FlowConfigModel config = JsonUtil.getJsonToBean(flowConfig, FlowConfigModel.class);
        config = config == null ? new FlowConfigModel() : config;
        entity.setVisibleType(config.getVisibleType());
        templateService.create(entity, form.getFlowXml(), form.getFlowNodes());
        return ActionResult.success(MsgCode.SU001.get(), entity.getId());
    }

    /**
     * 更新流程
     *
     * @param id   主键
     * @param form 流程模型
     * @return
     */
    @Operation(summary = "更新流程")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "form", description = "流程模型", required = true)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid TemplateNodeUpFrom form) throws WorkFlowException {
        TemplateEntity entity = JsonUtil.getJsonToBean(form, TemplateEntity.class);
        String flowConfig = form.getFlowConfig();
        FlowConfigModel config = JsonUtil.getJsonToBean(flowConfig, FlowConfigModel.class);
        config = config == null ? new FlowConfigModel() : config;
        entity.setVisibleType(config.getVisibleType());
        TemplateEntity info = templateService.getInfo(id);
        entity.setSystemId(info.getSystemId());
        templateService.update(id, entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 更新流程类型
     *
     * @param id 主键
     */
    @Operation(summary = "更新流程类型")
    @PutMapping("/{id}/UpdateType")
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> updateType(@PathVariable("id") String id) throws WorkFlowException {
        TemplateEntity entity = templateService.getInfo(id);
        entity.setType(FlowNature.STANDARD);
        templateService.updateById(entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除流程引擎
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除流程引擎")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> delete(@PathVariable("id") String id) throws WorkFlowException {
        TemplateEntity entity = templateService.getInfo(id);
        templateService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 删除流程版本
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除流程版本")
    @DeleteMapping("/Info/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> deleteInfo(@PathVariable("id") String id) throws WorkFlowException {
        TemplateJsonEntity entity = templateJsonService.getInfo(id);
        List<TemplateJsonEntity> list = templateJsonService.getList(entity.getTemplateId());
        if (list.size() == 1) {
            return ActionResult.fail(MsgCode.WF071.get());
        }
        if (Objects.equals(entity.getState(), TemplateJsonStatueEnum.START.getCode())) {
            return ActionResult.fail(MsgCode.WF072.get());
        }
        if (Objects.equals(entity.getState(), TemplateJsonStatueEnum.HISTORY.getCode())) {
            return ActionResult.fail(MsgCode.WF073.get());
        }
        templateJsonService.delete(ImmutableList.of(id));
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 上架下架
     *
     * @param id 主键
     * @param fo 参数
     */
    @Operation(summary = "上架下架")
    @PutMapping("/{id}/UpDownShelf")
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> updateStatus(@PathVariable("id") String id, @RequestBody UpDownModel fo) throws WorkFlowException {
        TemplateEntity entity = templateService.getInfo(id);
        if (ObjectUtil.equals(fo.getIsUp(), 0)) {
            entity.setStatus(TemplateStatueEnum.UP.getCode());
        } else {
            entity.setStatus(ObjectUtil.equals(fo.getIsHidden(), 0) ? TemplateStatueEnum.DOWN_CONTINUE.getCode() : TemplateStatueEnum.DOWN_HIDDEN.getCode());
        }
        templateService.updateById(entity);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 复制流程引擎
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "复制流程引擎")
    @PostMapping("/{id}/Actions/Copy")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> copy(@PathVariable("id") String id) throws WorkFlowException {
        TemplateEntity entity = templateService.getInfo(id);
        templateService.copy(entity);
        return ActionResult.success(MsgCode.SU007.get());
    }

    /**
     * 复制流程版本
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "复制流程版本")
    @PostMapping("/Info/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> copyVersion(@PathVariable("id") String id) throws WorkFlowException {
        String templateJsonId = RandomUtil.uuId();
        TemplateJsonEntity entity = templateJsonService.getInfo(id);
        templateJsonService.copy(entity, templateJsonId);
        return ActionResult.success(MsgCode.SU007.get(), templateJsonId);
    }

    /**
     * 流程保存或发布
     *
     * @return
     */
    @Operation(summary = "流程保存或发布")
    @PostMapping("/Save")
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<Object> save(@RequestBody @Valid TemplateNodeUpFrom form) throws WorkFlowException {
        templateJsonService.save(form);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 导出
     *
     * @param id 主键
     */
    @Operation(summary = "导出")
    @GetMapping("/{id}/Actions/Export")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<DownloadVO> export(@PathVariable("id") String id) throws WorkFlowException {
        TemplateExportModel model = templateService.export(id);
        DownloadVO downloadVO = serviceUtil.exportData(model);
        return ActionResult.success(downloadVO);
    }

    /**
     * 导入
     *
     * @param file 文件
     * @param type 类型
     */
    @Operation(summary = "导入")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckPermission(value = {"onlineDev.flowEngine"})
    public ActionResult<String> importData(@RequestPart("file") MultipartFile file, @RequestParam("type") String type) throws WorkFlowException {
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(file, ModuleTypeEnum.FLOW_FLOWENGINE.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(file);
        TemplateExportModel model = JsonUtil.getJsonToBean(fileContent, TemplateExportModel.class);
        if (ObjectUtil.isEmpty(model.getTemplate())) {
            return ActionResult.fail(MsgCode.IMP004.get());
        }
        templateService.importData(model, type);
        return ActionResult.success(MsgCode.IMP001.get());
    }

    /**
     * 委托流程选择展示
     *
     * @param ids 版本主键集合
     */
    @Operation(summary = "委托流程选择展示")
    @PostMapping("/GetFlowList")
    public ActionResult<Object> getFlowList(@RequestBody List<String> ids) {
        List<TemplateEntity> list = templateService.getList(ids);
        List<FlowListModel> voList = new ArrayList<>();
        for (TemplateEntity templateEntity : list) {
            FlowListModel model = new FlowListModel();
            model.setId(templateEntity.getId());
            model.setFullName(templateEntity.getFullName());
            model.setEnCode(templateEntity.getEnCode());
            voList.add(model);
        }
        return ActionResult.success(voList);
    }

    /**
     * 子流程表单信息
     *
     * @param id 版本主键
     */
    @Operation(summary = "子流程表单信息")
    @GetMapping("/{id}/FormInfo")
    public ActionResult<Object> formInfo(@PathVariable("id") String id) throws WorkFlowException {
        VisualdevEntity formInfo = templateJsonService.getFormInfo(id);
        return ActionResult.success(formInfo);
    }

    /**
     * 根据表单主键获取流程
     *
     * @param formId 表单主键
     */
    @Operation(summary = "根据表单主键获取流程")
    @GetMapping("/{formId}/FlowList")
    public ActionResult<Object> getByFormId(@PathVariable("formId") String formId, Boolean start) {
        FlowByFormModel model = templateService.getFlowByFormId(formId, start);
        return ActionResult.success(model);
    }

    /**
     * 子流程可发起人员
     *
     * @param id         版本主键
     * @param pagination 分页参数
     */
    @Operation(summary = "子流程可发起人员")
    @GetMapping("/{id}/SubFlowUserList")
    public ActionResult<PageListVO<CandidateUserVo>> getSubFlowUserList(@PathVariable("id") String id, TemplatePagination pagination) throws WorkFlowException {
        List<UserEntity> list = templateService.getSubFlowUserList(id, pagination);
        List<String> userIdList = list.stream().map(UserEntity::getId).collect(Collectors.toList());
        List<CandidateUserVo> voList = operatorUtil.getUserModel(userIdList, pagination);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 常用流程
     *
     * @param id 版本主键
     */
    @Operation(summary = "常用流程")
    @PostMapping("/SetCommonFlow/{id}")
    public ActionResult<Object> setCommonFlow(@PathVariable("id") String id) {
        int flag = commonService.setCommonFLow(id);
        if (flag == 2) {
            return ActionResult.success(MsgCode.SU021.get());
        }
        return ActionResult.success(MsgCode.SU016.get());
    }


    /**
     * 常用流程列表
     */
    @Operation(summary = "常用流程")
    @GetMapping("/getCommonFlowList")
    public ActionResult<Object> getCommonFlowList(TemplatePagination pagination) {
        pagination.setSystemId(serviceUtil.getSystemCodeById(RequestContext.getAppCode()));
        List<TemplatePageVo> commonList = templateService.getCommonList(pagination);
        return ActionResult.success(new ListVO<>(commonList));
    }


    /**
     * 根据模板主键获取表单
     *
     * @param templateId 流程模板主键
     */
    @Operation(summary = "根据模板主键获取表单")
    @GetMapping("/StartForm/{templateId}")
    public ActionResult<Object> getFormByTemplateId(@PathVariable("templateId") String templateId) throws WorkFlowException {
        return ActionResult.success(templateService.getFormByTemplateId(templateId));
    }

    /**
     * 根据模板主键获取表单主键和流程版本主键
     *
     * @param templateId 流程模板主键
     */
    @Operation(summary = "根据模板主键获取表单主键和流程版本主键")
    @GetMapping("/StartFormId/{templateId}")
    public ActionResult<Object> getFormIdAndFlowIdByTemplateId(@PathVariable("templateId") String templateId) throws WorkFlowException {
        List<String> userIdAll = ImmutableList.of(UserProvider.getLoginUserId());
        return ActionResult.success(templateService.getFormIdAndFlowId(userIdAll, templateId));
    }

    @Override
    public FlowByFormModel getFlowByFormId(String formId, Boolean start) {
        return templateService.getFlowByFormId(formId, start);
    }

    @Override
    public String getFormByFlowId(String templateId) {
        String formId = "";
        try {
            VisualdevEntity entity = templateService.getFormByTemplateId(templateId);
            if (null == entity) {
                throw new WorkFlowException(MsgCode.VS412.get());
            }
            formId = entity.getId();
        } catch (WorkFlowException e) {
            e.getStackTrace();
        }
        return formId;
    }

    @Override
    public List<TemplateEntity> getListByFlowIds(List<String> flowId) {
        return templateService.getListByIds(flowId);
    }

    @Override
    public List<TemplateTreeListVo> treeListWithPower() {
        return templateService.treeListWithPower();
    }

    @Override
    public FlowWorkListVO flowWork(String fromId) {
        return operatorService.flowWork(fromId);
    }

    @Override
    public boolean flowWork(WorkHandoverModel workHandoverModel) {
        return operatorService.flowWork(workHandoverModel);
    }

    @Override
    public List<TemplatePageVo> getCommonList(TemplatePagination pagination) {
        return templateService.getCommonList(pagination);
    }


    @Override
    public List<TemplateUseNumVo> getMenuUseNum(int i, List<String> authFlowList) {
        return templateUseNumService.getMenuUseNum(i, authFlowList);
    }

    @Override
    public List<TemplateEntity> getListByCreUser(String creUser) {
        return templateService.getListByCreUser(creUser);
    }

    @Override
    public List<TemplateExportVo> getExportList(String systemId) {
        return templateService.getExportList(systemId);
    }

    @Override
    public boolean importCopy(List<TemplateExportVo> list, String systemId) {
        try {
            if (CollectionUtils.isNotEmpty(list)) {
                for (TemplateExportVo item : list) {
                    TemplateExportModel model = JsonUtil.getJsonToBean(item, TemplateExportModel.class);
                    if (ObjectUtil.isNotEmpty(model.getTemplate())) {
                        TemplateEntity entity = model.getTemplate();
                        TemplateJsonExportModel versionModel = model.getFlowVersion();
                        Map<String, Map<String, Object>> flowNodes = new HashMap<>();
                        for (TemplateNodeEntity nodeEntity : model.getNodeList()) {
                            flowNodes.put(nodeEntity.getNodeCode(), JsonUtil.stringToMap(nodeEntity.getNodeJson()));
                        }
                        UserInfo userInfo = UserProvider.getUser();
                        entity.setId(RandomUtil.uuId());
                        entity.setEnCode(templateService.getEnCode(entity));
                        entity.setCreatorUserId(userInfo.getUserId());
                        entity.setCreatorTime(new Date());
                        entity.setFlowId(null);
                        entity.setVersion(null);
                        entity.setEnabledMark(0);
                        entity.setStatus(TemplateStatueEnum.NONE.getCode());
                        entity.setLastModifyUserId(null);
                        entity.setLastModifyTime(null);
                        entity.setSystemId(systemId);
                        templateService.save(entity);
                        TemplateNodeUpFrom from = new TemplateNodeUpFrom();
                        from.setId(entity.getId());
                        from.setFlowXml(versionModel.getFlowXml());
                        from.setFlowNodes(flowNodes);
                        flowUtil.create(from);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        templateService.deleteBySystemId(systemId);
    }
}
