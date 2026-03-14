package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.entity.AiEntity;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.model.flow.FlowStateModel;
import jnpf.base.model.schedule.ScheduleNewCrForm;
import jnpf.base.model.systemconfig.SysConfigModel;
import jnpf.base.service.*;
import jnpf.base.vo.DownloadVO;
import jnpf.constant.*;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.emnus.SysParamEnum;
import jnpf.exception.WorkFlowException;
import jnpf.extend.service.DocumentApi;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.model.template.TemplateExportModel;
import jnpf.flowable.model.util.FlowContextHolder;
import jnpf.flowable.model.util.FlowNature;
import jnpf.message.model.SentMessageForm;
import jnpf.message.service.SendMsgService;
import jnpf.model.SystemParamModel;
import jnpf.model.document.FlowFileModel;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.VisualParamModel;
import jnpf.permission.entity.*;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.*;
import jnpf.util.DataFileExport;
import jnpf.util.StringUtil;
import jnpf.util.enums.DictionaryDataEnum;
import jnpf.visual.service.VisualdevApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static jnpf.util.Constants.ADMIN_KEY;

@Component
@RequiredArgsConstructor
public class ServiceUtil {
    
    private final DictionaryDataService dictionaryDataApi;
    
    private final UserRelationService userRelationApi;
    
    private final  UserService userApi;
    
    private final  RoleService roleApi;
    
    private final  RoleRelationService roleRelationApi;
    
    private final  OrganizeService organizeApi;
    
    private final  PositionService positionApi;
    
    private final  CodeNumService codeNumApi;
    
    private final  DataInterfaceService dataInterfaceApi;
    
    private final  SendMsgService sendMessageApi;
    
    private final  DataFileExport fileExport;
    
    private final  CommonWordsService commonWordsApi;
    
    private final  SignService signApi;
    
    private final  AuthorizeService authorizeApi;
    
    private final  AiService aiApi;

    
    private final  VisualdevApi visualdevApi;
    
    private final  DocumentApi documentApi;
    
    private final  BillRuleService billRuleApi;
    
    private final  SystemConfigApi systemConfigApi;
    
    private final  SystemService systemApi;
    
    private final  ScheduleNewApi scheduleNewApi;

    // 创建日程
    public void createSchedule(ScheduleNewCrForm fo) throws WorkFlowException {
        ActionResult<Object> result = scheduleNewApi.create(fo);
        if (result.getCode() != 200) {
            throw new WorkFlowException(result.getMsg());
        }
    }

    // 获取系统配置
    public SysConfigModel getSysConfig() {
        ActionResult<SysConfigModel> actionResult = systemConfigApi.list();
        return actionResult.getData();
    }

    // 获取系统配置，流程签收
    public Boolean getFlowSign() {
        ActionResult<SysConfigModel> actionResult = systemConfigApi.list();
        SysConfigModel data = actionResult.getData();
        return data.getFlowSign() == 0;
    }

    // 获取系统配置，流程办理
    public Boolean getFlowTodo() {
        ActionResult<SysConfigModel> actionResult = systemConfigApi.list();
        SysConfigModel data = actionResult.getData();
        return data.getFlowTodo() == 0;
    }

    // 流水号
    public String getBillNumber() {
        return billRuleApi.getBillNumber(FlowNature.REVOKE_BILL_CODE, false);
    }

    // -------------------------------签名-----------------------------
    public void updateSign(String signId, String signImg) {
        SignEntity signEntity = signApi.getById(signId);
        if (null != signEntity) {
            signApi.updateDefault(signId);
        } else {
            signEntity = new SignEntity();
            signEntity.setIsDefault(1);
            signEntity.setSignImg(signImg);
            signApi.create(signEntity);
        }
    }

    // -------------------------------流程编码-----------------------------
    public String getCode() {
        return codeNumApi.getCodeOnce(CodeConst.LC);
    }

    // -------------------------------常用语-----------------------------
    public void addCommonWordsNum(String handleOpinion) {
        commonWordsApi.addCommonWordsNum(handleOpinion);
    }

    // -------------------------------文件-----------------------------
    // 判断是否存在归档文件，不存在为true
    public Boolean checkFlowFile(String taskId) {
        return documentApi.checkFlowFile(taskId);
    }

    // 获取归档文件
    public List<Map<String, Object>> getFlowFile(FlowFileModel model) {
        return documentApi.getFlowFile(model);
    }

    // -------------------------------流程全部权限-----------------------------
    public List<String> getPermission(String userId) {
        return getPermission(userId, AuthorizeConst.FLOW);
    }

    public List<String> getPermission(String userId, String itmeType) {
        List<String> userIdList = ImmutableList.of(userId);
        List<String> objectIdList = new ArrayList<>();
        List<String> posId = getListByUserIdAll(userIdList).stream().filter(e -> Objects.equals(PermissionConst.POSITION, e.getObjectType())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        objectIdList.addAll(posId);
        List<String> roleId = getRoleObjectId(userIdList).stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        objectIdList.addAll(roleId);
        List<String> resList = new ArrayList<>();
        if (!objectIdList.isEmpty()) {
            resList = getAuthorizeObjectId(objectIdList).stream().filter(e -> e.getItemType().equals(itmeType)).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        }
        return resList;
    }

    // -------------------------------发起权限-----------------------------
    public List<String> getLaunchPermission() {
        AuthorizeVO authorizeByUser = getAuthorizeByUser();
        return authorizeByUser.getFlowIdList();
    }

    // true是普通用户
    public boolean isCommonUser(String userId) {
        UserEntity userInfo = getUserInfo(userId);
        return userInfo == null || !Objects.equals(userInfo.getAccount(), ADMIN_KEY);
    }

    // 获取流程有发起权限的人员
    public List<UserEntity> getLaunchUserByTemplateId(TemplateEntity template, Pagination pagination) {
        if (ObjectUtil.equals(template.getVisibleType(), FlowNature.ALL)) {
            // 公开直接返回用户分页
            return userApi.getUserPage(pagination);
        }
        // 根据流程主键、权限为流程类型 获取权限关联
        List<String> userIdList = new ArrayList<>();
        List<AuthorizeEntity> authorizeList = getListByObjectAndItemIdAndType(template.getId()).stream().filter(e -> !Objects.equals(PermissionConst.ORGANIZE, e.getObjectType())).collect(Collectors.toList());
        for (AuthorizeEntity entity : authorizeList) {
            String objectType = PermissionConst.POSITION.equals(entity.getObjectType()) ? SysParamEnum.POS.getCode() : SysParamEnum.ROLE.getCode();
            userIdList.add(entity.getObjectId() + "--" + objectType);
        }
        List<String> userListAll = this.getUserListAll(userIdList);
        return this.getUserName(userListAll, pagination);
    }

    //--------------------------------表单------------------------------
    public VisualdevEntity getFormInfo(String id) {
        return visualdevApi.getFormConfig(id);
    }

    public List<VisualdevEntity> getFormList(List<String> formIds) {
        if (formIds.isEmpty()) {
            return new ArrayList<>();
        }
        return visualdevApi.getFormConfigList(formIds);
    }

    public void saveOrUpdateFormData(FlowFormDataModel model) throws WorkFlowException {
        ActionResult<Object> actionResult = visualdevApi.saveOrUpdate(model);
        if (actionResult.getCode() != 200) {
            throw new WorkFlowException(actionResult.getMsg());
        }
    }

    public void deleteFormData(String formId, String id) {
        if (StringUtils.isBlank(formId) || StringUtils.isBlank(id)) {
            return;
        }
        visualdevApi.delete(formId, id);
    }

    public Map<String, Object> infoData(String formId, String id)  {
        // 流程调用表单接口
        Map<String, Object> dataAll = new HashMap<>();
        if (StringUtil.isNotEmpty(formId) && StringUtil.isNotEmpty(id)) {
            ActionResult<Object> result = visualdevApi.info(formId, id);
            if (null != result && null != result.getData()) {
                dataAll = (Map<String, Object>) result.getData();
            }
        }
        return dataAll;
    }

    public void handleFormData(String flowId, Boolean isTransfer) throws WorkFlowException {
        try {
            Map<String, Map<String, Object>> allData = FlowContextHolder.getAllData();
            Map<String, List<Map<String, Object>>> formOperates = FlowContextHolder.getFormOperates();
            List<String> writeIdList = FlowContextHolder.getWriteIdList();
            for (String key : writeIdList) {
                String[] idList = key.split(JnpfConst.SIDE_MARK);
                List<Map<String, Object>> operates = formOperates.get(key);
                Map<String, Object> formData = allData.get(key);
                formData = formData == null ? new HashMap<>() : formData;
                FlowFormDataModel formDataModel = FlowFormDataModel.builder().formId(idList[1]).id(idList[0]).map(formData).formOperates(operates)
                        .flowId(flowId).isTransfer(isTransfer).build();
                this.saveOrUpdateFormData(formDataModel);
            }
        } finally {
            FlowContextHolder.clearAll();
        }
    }

    public List<Map<String, Object>> getListWithTableList(VisualDevJsonModel visualDevJsonModel, PaginationModel pagination, UserInfo userInfo) {
        return visualdevApi.getListWithTableList(visualDevJsonModel, pagination, userInfo);
    }

    public VisualdevEntity getReleaseInfo(String formId) {
        return visualdevApi.getReleaseInfo(formId);
    }

    public DataModel visualCreate(VisualdevEntity visualdevEntity, Map<String, Object> data) throws WorkFlowException {
        VisualParamModel visualParamModel = VisualParamModel.builder().visualdevEntity(visualdevEntity).data(data).build();
        return visualdevApi.visualCreate(visualParamModel);
    }

    public DataModel visualUpdate(VisualdevEntity visualdevEntity, Map<String, Object> data, String id) throws WorkFlowException  {
        VisualParamModel visualParamModel = VisualParamModel.builder().visualdevEntity(visualdevEntity).data(data).id(id).onlyUpdate(true).build();
        return visualdevApi.visualUpdate(visualParamModel);
    }

    public void visualDelete(VisualdevEntity visualdevEntity, List<Map<String, Object>> dataList) throws WorkFlowException {
        VisualParamModel visualParamModel = VisualParamModel.builder().visualdevEntity(visualdevEntity).dataList(dataList).build();
        visualdevApi.visualDelete(visualParamModel);
    }

    public void deleteSubTable(FlowFormDataModel model) throws SQLException, WorkFlowException {
        visualdevApi.deleteByTableName(model);
    }

    public void saveState(FlowStateModel stateModel) {
        visualdevApi.saveState(stateModel);
    }

    //--------------------------------数据字典------------------------------
    public List<DictionaryDataEntity> getDiList() {
        return dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
    }

    public List<DictionaryDataEntity> getDictionName(List<String> id) {
        return dictionaryDataApi.getDictionName(id);
    }

    //--------------------------------用户关系表------------------------------
    public List<UserRelationEntity> getListByUserIdAll(List<String> id) {
        return userRelationApi.getListByUserIdAll(id).stream().filter(t -> StringUtil.isNotEmpty(t.getObjectId())).collect(Collectors.toList());
    }

    public List<UserRelationEntity> getListByObjectIdAll(List<String> id) {
        return userRelationApi.getListByObjectIdAll(id);
    }

    //--------------------------------用户权限------------------------------

    public List<AuthorizeEntity> getAuthorizeObjectId(List<String> objectIdList) {
        return authorizeApi.getListByObjectId(objectIdList);
    }

    public List<AuthorizeEntity> getListByObjectAndItemIdAndType(String templateId) {
        return authorizeApi.getListByObjectAndItemIdAndType(templateId, AuthorizeConst.FLOW);
    }

    public AuthorizeVO getAuthorizeByUser() {
        return authorizeApi.getAuthorizeByUser(false);
    }

    //--------------------------------用户------------------------------

    public String getAdmin() {
        UserEntity admin = userApi.getUserByAccount(ADMIN_KEY);
        return admin.getId();
    }

    public List<UserEntity> getUserByAccount(List<String> accountList) {
        List<UserEntity> list = new ArrayList<>();
        if (CollUtil.isEmpty(accountList)) {
            return list;
        }
        for (String account : accountList) {
            UserEntity user = userApi.getUserByAccount(account);
            if (null != user) {
                list.add(user);
            }
        }
        return list;
    }

    public List<UserEntity> getUserName(List<String> id) {
        return getUserName(id, false);
    }

    public List<UserEntity> getUserName(List<String> id, boolean enableMark) {
        List<UserEntity> list = userApi.getUserName(id);
        if (enableMark) list = list.stream().filter(t -> t.getEnabledMark() != 0).collect(Collectors.toList());
        return list;
    }

    public List<UserEntity> getUserName(List<String> id, Pagination pagination) {
        return userApi.getUserName(id, pagination);
    }

    public UserEntity getUserInfo(String id) {
        UserEntity entity = null;
        if (StringUtil.isNotEmpty(id)) {
            entity = id.equalsIgnoreCase(ADMIN_KEY) ? userApi.getUserByAccount(id) : userApi.getInfo(id);
        }
        return entity;
    }

    public List<String> getUserListAll(List<String> idList) {
        return userApi.getUserIdList(idList);
    }

    //--------------------------------角色关系表------------------------------

    public List<RoleRelationEntity> getListByRoleId(List<String> roleIdList) {
        return roleRelationApi.getListByRoleId(roleIdList, PermissionConst.USER);
    }

    public List<RoleRelationEntity> getRoleObjectId(List<String> userId) {
        return roleRelationApi.getListByObjectId(userId, PermissionConst.USER);
    }

    //--------------------------------角色------------------------------
    public List<RoleEntity> getRoleList(List<String> id) {
        return roleApi.getListByIds(id);
    }


    //--------------------------------组织------------------------------

    public OrganizeEntity getOrganizeInfo(String id) {
        return StringUtil.isNotEmpty(id) ? organizeApi.getInfo(id) : null;
    }

    public List<OrganizeEntity> getDepartmentAll(String organizeId) {
        return organizeApi.getDepartmentAll(organizeId);
    }

    public List<OrganizeEntity> getOrganizeList(List<String> idList) {
        return organizeApi.getListByIds(idList);
    }

    //--------------------------------岗位------------------------------

    public PositionEntity getPositionInfo(String id) {
        return StringUtil.isNotEmpty(id) ? positionApi.getInfo(id) : null;
    }

    public List<PositionEntity> getChildPosition(String id) {
        return StringUtil.isNotEmpty(id) ? positionApi.getAllChild(id) : new ArrayList<>();
    }

    public List<PositionEntity> getListByOrgIds(List<String> orgIds) {
        return positionApi.getListByOrgIds(orgIds);
    }

    public List<PositionEntity> getPosList(List<String> idList) {
        return positionApi.getListByIds(idList);
    }


    //--------------------------------远端------------------------------
    public ActionResult<Object> infoToId(String interId, Map<String, String> parameterMap) {
        return dataInterfaceApi.infoToId(interId, null, parameterMap);
    }

    public Map<String, String> getSystemFieldValue() {
        return userApi.getSystemFieldValue(new SystemParamModel());
    }

    //--------------------------------应用------------------------------

    public String getSystemCodeById(String systemCode) {
        List<String> systemIdList = ImmutableList.of(JnpfConst.MAIN_SYSTEM_CODE, JnpfConst.WORK_FLOW_CODE);
        String systemId = JnpfConst.MAIN_SYSTEM_CODE;
        if (StringUtil.isEmpty(systemCode)) {
            return systemId;
        }
        if (systemIdList.contains(systemCode)) {
            return "";
        }
        SystemEntity entity = getInfoByEnCode(systemCode);
        if (entity != null) {
            systemId = entity.getId();
        }
        return systemId;
    }

    public SystemEntity getInfoByEnCode(String systemCode) {
        return systemApi.getInfoByEnCode(systemCode);
    }

    public List<SystemEntity> getSystemList(List<String> idList) {
        return systemApi.getListByIds(idList, new ArrayList<>());
    }

    //--------------------------------发送消息------------------------------
    public void sendMessage(List<SentMessageForm> messageListAll) {
        for (SentMessageForm messageForm : messageListAll) {
            if (messageForm.isSysMessage()) {
                sendMessageApi.sendMessage(messageForm);
            }
        }
    }

    public List<String> sendDelegateMsg(List<SentMessageForm> messageListAll) {
        List<String> list = new ArrayList<>();
        for (SentMessageForm messageForm : messageListAll) {
            List<String> errList = sendMessageApi.sendScheduleMessage(messageForm);
            list.addAll(errList);
        }
        return list;
    }

    //--------------------------------Ai------------------------------
    public AiEntity getDefault() {
        return aiApi.getDefault();
    }

    //------------------------------导出-------------------------------
    public DownloadVO exportData(TemplateExportModel model) {
        return fileExport.exportFile(model, FileTypeConstant.TEMPORARY, model.getTemplate().getFullName(), ModuleTypeEnum.FLOW_FLOWENGINE.getTableName());
    }
}
