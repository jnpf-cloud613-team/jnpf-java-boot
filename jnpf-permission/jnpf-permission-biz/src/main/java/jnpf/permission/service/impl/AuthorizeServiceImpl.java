package jnpf.permission.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.button.ButtonModel;
import jnpf.base.model.column.ColumnModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.portalmanage.SavePortalAuthModel;
import jnpf.base.model.resource.ResourceModel;
import jnpf.base.service.ModuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.base.service.SystemService;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.*;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.database.sql.util.SqlFrameFastUtil;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.DbTypeUtil;
import jnpf.database.util.JdbcUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.exception.NoPermiLoginException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.login.UserSystemVO;
import jnpf.model.tenant.TenantAuthorizeModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.permission.entity.*;
import jnpf.permission.mapper.*;
import jnpf.permission.model.authorize.*;
import jnpf.permission.model.condition.AuthConditionModel;
import jnpf.permission.model.condition.AuthGroup;
import jnpf.permission.model.condition.AuthItem;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.service.AuthorizeService;
import jnpf.permission.service.PermissionGroupService;
import jnpf.permission.util.AuthPermUtil;
import jnpf.permission.util.UserUtil;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizeServiceImpl extends SuperServiceImpl<AuthorizeMapper, AuthorizeEntity> implements AuthorizeService {

    private final DataSourceUtil dataSourceUtils;
    private final CacheKeyUtil cacheKeyUtil;
    private final ConfigValueUtil configValueUtil;
    private final RedisUtil redisUtil;
    private final SysconfigService sysconfigApi;
    private final ModuleService moduleApi;
    private final SystemService systemApi;
    private final UserUtil userUtil;
    private final UserMapper userMapper;
    private final OrganizeMapper organizeMapper;
    private final PositionMapper positionMapper;
    private final RoleMapper roleMapper;
    private final StandingMapper standingMapper;
    private final RoleRelationMapper roleRelationMapper;
    private final UserRelationMapper userRelationMapper;
    private final PermissionGroupService permissionGroupService;

    //当前系统权限
    @Override
    public AuthorizeVO getAuthorize(boolean singletonOrg, String currentSystemCode, Integer isBackend) {
        return this.getAuthorize(singletonOrg, currentSystemCode, isBackend, false);
    }

    //全部系统权限
    @Override
    public AuthorizeVO getAuthorizeByUser(boolean singletonOrg) {
        //获取全部应用权限
        return this.getAuthorize(singletonOrg, null, 0, true);
    }

    @Override
    public AuthorizeVO getAuthorize(boolean singletonOrg, String currentSystemCode, Integer isBackend, boolean allSystem) {
        boolean isPc = RequestContext.isOrignPc();
        String pcCode = isPc ? JnpfConst.WEB : JnpfConst.APP;
        BaseSystemInfo baseSystemInfo = sysconfigApi.getSysInfo();
        UserInfo userInfo = UserProvider.getUser();
        OtherModel otherModel = new OtherModel();
        List<ModuleModel> moduleList = new ArrayList<>();
        List<ButtonModel> buttonList = new ArrayList<>();
        List<ColumnModel> columnList = new ArrayList<>();
        List<ResourceModel> resourceList = new ArrayList<>();
        List<ModuleFormModel> formsList = new ArrayList<>();
        List<SystemBaeModel> systemList = new ArrayList<>();
        List<String> flowList = new ArrayList<>();
        List<UserSystemVO> standingListVo = new ArrayList<>();
        boolean isAdmin = userInfo.getIsAdministrator();
        String currentSystemId = "";
        SystemEntity info = null;
        if (StringUtil.isNotEmpty(currentSystemCode)) {
            try {
                currentSystemCode = URLDecoder.decode(currentSystemCode, "UTF-8");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            info = systemApi.getInfoByEnCode(currentSystemCode);
            if (info == null) {
                throw new NoPermiLoginException(MsgCode.PS032.get());
            }
            currentSystemId = info.getId();
        }

        List<String> moduleAuthorize = new ArrayList<>();
        List<String> moduleUrlAddressAuthorize = new ArrayList<>();
        if (configValueUtil.isMultiTenancy()) {
            TenantAuthorizeModel tenantAuthorizeModel = TenantDataSourceUtil.getCacheModuleAuthorize(userInfo.getTenantId());
            moduleAuthorize = tenantAuthorizeModel.getModuleIdList();
            moduleUrlAddressAuthorize = tenantAuthorizeModel.getUrlAddressList();
        }

        //后台菜单编码列表
        List<String> appComModule = new ArrayList<>();
        appComModule.add(JnpfConst.APP_BACKEND);
        appComModule.addAll(JnpfConst.APP_CONFIG_MODULE);
        appComModule.addAll(JnpfConst.ONLINE_DEV_MODULE);
        if (!isAdmin) {
            //获取用户所有权限
            List<String> objectIds = new ArrayList<>();
            List<String> positionIds = userInfo.getPositionIds();
            List<String> roleIds = roleMapper.getListByIds(userInfo.getRoleIds()).stream()
                    .filter(t -> !PermissionConst.ORGANIZE.equals(t.getType())).map(RoleEntity::getId).collect(Collectors.toList());
            objectIds.addAll(positionIds);
            objectIds.addAll(roleIds);
            List<AuthorizeEntity> authorizeList = this.getListByObjectId(objectIds);
            List<AuthorizeEntity> standingList = authorizeList.stream().filter(t -> PermissionConst.STAND.equals(t.getItemType())).collect(Collectors.toList());

            List<String> posAndRoles = new ArrayList<>();
            //非管理员，走身份（standingSwitch）
            if (CollUtil.isNotEmpty(standingList) && Objects.equals(baseSystemInfo.getStandingSwitch(), 1)) {
                //获取身份id列表
                List<String> standingIds = standingList.stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                //设置当前身份
                String standingId = setCurrentStanding(standingIds, standingListVo);
                if (StringUtil.isNotEmpty(standingId)) {
                    //根据当前身份获取角色和岗位
                    List<String> stdPos = standingList.stream().filter(t -> t.getItemId().equals(standingId)
                            && PermissionConst.POSITION.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
                    //获取当前岗位角色
                    List<String> stdPosRole = roleRelationMapper.getListByObjectId(stdPos, null)
                            .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                    List<String> stdRole = standingList.stream().filter(t -> t.getItemId().equals(standingId)
                            && PermissionConst.ROLE.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
                    posAndRoles.addAll(stdPos);
                    posAndRoles.addAll(stdPosRole);
                    posAndRoles.addAll(stdRole);
                    //重新设置当前用户角色
                    List<RoleEntity> roleList = roleMapper.getListByIds(stdRole);
                    for (RoleEntity roleEntity : roleList) {
                        if (PermissionConst.MANAGER_CODE.equals(roleEntity.getEnCode())) {
                            otherModel.setIsManageRole(true);
                        } else if (PermissionConst.DEVELOPER_CODE.equals(roleEntity.getEnCode())) {
                            otherModel.setIsDevRole(true);
                        } else if (PermissionConst.USER_CODE.equals(roleEntity.getEnCode())) {
                            otherModel.setIsUserRole(true);
                        } else {
                            otherModel.setIsOtherRole(true);
                        }
                    }
                    //通过身份过滤掉多于的权限
                    authorizeList = authorizeList.stream().filter(t -> posAndRoles.contains(t.getObjectId())).collect(Collectors.toList());
                }
            } else {
                //重新设置当前用户角色
                List<RoleEntity> roleList = roleMapper.getListByIds(roleIds);
                for (RoleEntity roleEntity : roleList) {
                    if (PermissionConst.MANAGER_CODE.equals(roleEntity.getEnCode())) {
                        otherModel.setIsManageRole(true);
                    } else if (PermissionConst.DEVELOPER_CODE.equals(roleEntity.getEnCode())) {
                        otherModel.setIsDevRole(true);
                    } else if (PermissionConst.USER_CODE.equals(roleEntity.getEnCode())) {
                        otherModel.setIsUserRole(true);
                    } else {
                        otherModel.setIsOtherRole(true);
                    }
                }
            }

            //应用
            List<String> sysMainList = systemApi.getMainList().stream().map(SystemEntity::getId).collect(Collectors.toList());
            List<String> authSysList = authorizeList.stream().filter(t -> t.getItemId().contains(pcCode))
                    .map(t -> t.getItemId().substring(0, t.getItemId().indexOf(pcCode))).collect(Collectors.toList());
            List<String> systemId = authorizeList.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType()) &&
                            (Objects.equals(isBackend, 1) || authSysList.contains(t.getItemId()) || sysMainList.contains(t.getItemId())))
                    .map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (!systemId.isEmpty()) {
                List<SystemEntity> systemAdmin = systemApi.getListByIds(systemId, moduleAuthorize);
                //配置了流程和协作的菜单但是没有主系统菜单时，直接添加主系统权限
                List<SystemEntity> mainList = systemAdmin.stream().filter(t -> JnpfConst.MAIN_SYSTEM_CODE.equals(t.getEnCode())).collect(Collectors.toList());
                if (CollUtil.isEmpty(mainList)) {
                    List<SystemEntity> collect = systemAdmin.stream().filter(t -> JnpfConst.WORK_FLOW_CODE.equals(t.getEnCode()) || JnpfConst.TEAMWORK_CODE.equals(t.getEnCode())).collect(Collectors.toList());
                    if (CollUtil.isNotEmpty(collect)) {
                        systemAdmin.add(systemApi.getInfoByEnCode(JnpfConst.MAIN_SYSTEM_CODE));
                    }
                }
                //app无主应用时获取当前第一个应用
                String finalCurrentSystemId = currentSystemId;
                if (!isPc && (StringUtil.isEmpty(currentSystemCode) || !systemAdmin.stream().anyMatch(t->t.getId().equals(finalCurrentSystemId)))) {
                    info = systemAdmin.stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).findFirst().orElse(null);
                    if (info != null) {
                        currentSystemId = info.getId();
                        currentSystemCode = info.getEnCode();
                    }
                }
                systemList = JsonUtil.getJsonToList(systemAdmin, SystemBaeModel.class);
            }
            List<String> moduleId = authorizeList.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (!moduleId.isEmpty()) {
                List<ModuleModel> allSysMenu = this.baseMapper.findModule(moduleId, null, moduleAuthorize, moduleUrlAddressAuthorize, singletonOrg ? 0 : 1);
                List<String> currSys = new ArrayList<>();
                if (StringUtil.isNotEmpty(currentSystemId) && !allSystem) {
                    currSys.add(currentSystemId);
                }
                moduleList = CollUtil.isNotEmpty(currSys) ? allSysMenu.stream().filter(t -> currSys.contains(t.getSystemId())).collect(Collectors.toList()) : allSysMenu;
                //是后台管理-非admin
                if (Objects.equals(isBackend, 1)) {
                    moduleList = allSysMenu.stream().filter(t -> appComModule.contains(t.getEnCode())).collect(Collectors.toList());
                }
                //当前系统是否有流程菜单
                if (StringUtil.isNotEmpty(currentSystemCode)) {
                    if (JnpfConst.MAIN_SYSTEM_CODE.equals(currentSystemCode)) {
                        otherModel.setWorkflowEnabled(allSysMenu.stream().anyMatch(t -> JnpfConst.MODULE_CODE.contains(t.getEnCode()) && pcCode.equals(t.getCategory())) ? 1 : 0);
                    } else {
                        otherModel.setWorkflowEnabled(moduleList.stream().anyMatch(t -> JnpfConst.MODULE_CODE.contains(t.getEnCode()) && pcCode.equals(t.getCategory())) ? 1 : 0);
                    }
                }
            }
            // 按钮
            List<String> buttonId = authorizeList.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (!buttonId.isEmpty()) {
                buttonList = this.baseMapper.findButton(buttonId);
            }
            // 列表
            List<String> columnId = authorizeList.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (!columnId.isEmpty()) {
                columnList = this.baseMapper.findColumn(columnId);
            }
            // 数据
            List<String> resourceId = authorizeList.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (!resourceId.isEmpty()) {
                resourceList = this.baseMapper.findResource(resourceId);
                if (CollUtil.isNotEmpty(posAndRoles)) {
                    resourceList = resourceList.stream().filter(t -> posAndRoles.contains(t.getObjectId())).collect(Collectors.toList());
                }
            }
            // 表单
            List<String> formId = authorizeList.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (!formId.isEmpty()) {
                formsList = this.baseMapper.findForms(formId);
            }
            // 流程
            flowList = authorizeList.stream().filter(t -> AuthorizeConst.FLOW.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        } else {
            buttonList = this.baseMapper.findButtonAdmin(1);
            columnList = this.baseMapper.findColumnAdmin(1);
            resourceList = this.baseMapper.findResourceAdmin(1);
            formsList = this.baseMapper.findFormsAdmin(1);
            List<SystemEntity> systemAdmin = systemApi.findSystemAdmin(moduleAuthorize);
            if (!isPc && StringUtil.isEmpty(currentSystemCode)) {
                info = systemAdmin.stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).findFirst().orElse(null);
                if (info != null) {
                    currentSystemId = info.getId();
                    currentSystemCode = info.getEnCode();
                }
            }
            systemList = JsonUtil.getJsonToList(systemAdmin, SystemBaeModel.class);
            List<ModuleEntity> moduleAdmin = moduleApi.findModuleAdmin(singletonOrg ? 0 : 1, null, moduleAuthorize, moduleUrlAddressAuthorize);
            String thisid = currentSystemId;
            List<ModuleModel> allSysMenu = JsonUtil.getJsonToList(moduleAdmin, ModuleModel.class);

            if (StringUtil.isNotEmpty(currentSystemId) && !allSystem) {
                moduleList = allSysMenu.stream().filter(t -> Objects.equals(t.getSystemId(), thisid)).collect(Collectors.toList());
            } else {
                moduleList = allSysMenu;
            }
            //当前系统是否有流程菜单
            if (StringUtil.isNotEmpty(currentSystemCode)) {
                if (JnpfConst.MAIN_SYSTEM_CODE.equals(currentSystemCode)) {
                    otherModel.setWorkflowEnabled(allSysMenu.stream().anyMatch(t -> JnpfConst.MODULE_CODE.contains(t.getEnCode()) && pcCode.equals(t.getCategory())) ? 1 : 0);
                } else {
                    otherModel.setWorkflowEnabled(moduleList.stream().anyMatch(t -> JnpfConst.MODULE_CODE.contains(t.getEnCode()) && pcCode.equals(t.getCategory())) ? 1 : 0);
                }
            }

            //超管添加身份
            if (Objects.equals(baseSystemInfo.getStandingSwitch(), 1)) {
                UserSystemVO admin = new UserSystemVO();
                admin.setId(1 + "");
                admin.setName(MsgCode.OA025.get());
                admin.setCurrentStanding(true);
                admin.setIcon(PermissionConst.SD_ADMIN_ICON);
                standingListVo.add(admin);
            }
            //是后台管理-admin
            if (Objects.equals(isBackend, 1)) {
                List<ModuleEntity> listByEnCode = moduleApi.getListByEnCode(appComModule);
                if (CollUtil.isNotEmpty(moduleAuthorize)) {
                    List<String> finalModuleAuthorize = moduleAuthorize;
                    listByEnCode = listByEnCode.stream().filter(t -> !finalModuleAuthorize.contains(t.getEnCode())
                            && !finalModuleAuthorize.contains(t.getUrlAddress())).collect(Collectors.toList());
                }
                moduleList = JsonUtil.getJsonToList(listByEnCode, ModuleModel.class);
            }
        }

        //系统配置-流程开关
        moduleList = moduleList.stream().filter(t ->
                !((!Objects.equals(baseSystemInfo.getFlowSign(), 1) && JnpfConst.WORK_FLOWSIGN.equals(t.getEnCode())) ||
                        (!Objects.equals(baseSystemInfo.getFlowTodo(), 1) && JnpfConst.WORK_FLOWTODO.equals(t.getEnCode())) ||
                        (!Objects.equals(baseSystemInfo.getStandingSwitch(), 1) && JnpfConst.PERMISSION_IDENTITY.equals(t.getEnCode())))
        ).collect(Collectors.toList());
        //应用前台不需要添加菜单数据
        return AuthorizeVO.builder()
                .moduleList(moduleList)
                .buttonList(buttonList)
                .columnList(columnList)
                .resourceList(resourceList)
                .formsList(formsList)
                .systemList(systemList)
                .standingList(standingListVo)
                .currentSystem(info)
                .flowIdList(flowList)
                .otherModel(otherModel)
                .build();
    }

    /**
     * 设置当前身份
     *
     * @param standingIds
     * @param standingListVo
     */
    private String setCurrentStanding(List<String> standingIds, List<UserSystemVO> standingListVo) {
        List<StandingEntity> listByIds = standingMapper.getListByIds(standingIds);
        UserEntity info = userMapper.getInfo(UserProvider.getUser().getUserId());
        String currentStanding = "";
        if (RequestContext.isOrignPc()) {
            currentStanding = info.getStanding();
        } else {
            currentStanding = info.getAppStanding();
        }
        for (StandingEntity standing : listByIds) {
            UserSystemVO standingVo = JsonUtil.getJsonToBean(standing, UserSystemVO.class);
            standingVo.setName(standing.getFullName());
            if (StringUtil.isNotEmpty(currentStanding) && currentStanding.equals(standing.getId())) {
                standingVo.setCurrentStanding(true);
            }
            String icon = "";
            switch (standing.getEnCode()) {
                case PermissionConst.MANAGER_CODE:
                    icon = PermissionConst.SD_MANAGER_ICON;
                    break;
                case PermissionConst.DEVELOPER_CODE:
                    icon = PermissionConst.SD_DEVELOPER_ICON;
                    break;
                case PermissionConst.USER_CODE:
                    icon = PermissionConst.SD_USER_ICON;
                    break;
                default:
                    icon = PermissionConst.SD_EXPERIENCER_ICON;
                    break;
            }
            standingVo.setIcon(icon);
            standingListVo.add(standingVo);
        }
        if (CollUtil.isEmpty(standingListVo)) {
            return null;
        }
        UserSystemVO currStand = standingListVo.stream().filter(t -> t.isCurrentStanding()).findFirst().orElse(null);
        if (currStand == null) {
            UserSystemVO userSystemVO = standingListVo.stream().filter(t -> PermissionConst.USER_CODE.equals(t.getEnCode()))
                    .findFirst().orElse(standingListVo.get(0));
            userSystemVO.setCurrentStanding(true);
            if (RequestContext.isOrignPc()) {
                info.setStanding(userSystemVO.getId());
            } else {
                info.setAppStanding(userSystemVO.getId());
            }
            userMapper.updateById(info);
            currentStanding = userSystemVO.getId();
        } else {
            currentStanding = currStand.getId();
        }
        return currentStanding;
    }

    @Override
    @DSTransactional
    public void saveItemAuth(SavePortalAuthModel portalAuthModel) {
        List<String> ids = portalAuthModel.getIds();
        String id = portalAuthModel.getId();
        String type = portalAuthModel.getType();
        String userId = UserProvider.getLoginUserId();
        // 原始授权角色
        List<AuthorizeEntity> list = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            AuthorizeEntity authorizeEntity = new AuthorizeEntity();
            authorizeEntity.setId(RandomUtil.uuId());
            authorizeEntity.setItemType(type);
            authorizeEntity.setItemId(ids.get(i));
            authorizeEntity.setObjectType(portalAuthModel.getObjectType());
            authorizeEntity.setObjectId(id);
            authorizeEntity.setSortCode((long) i);
            authorizeEntity.setCreatorTime(new Date());
            authorizeEntity.setCreatorUserId(userId);
            list.add(authorizeEntity);
        }
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getItemType, type);
        queryWrapper.lambda().eq(AuthorizeEntity::getObjectId, id);
        this.remove(queryWrapper);
        list.forEach(this::save);
        List<String> userIds = new ArrayList<>();
        String objectType = portalAuthModel.getObjectType();
        if (PermissionConst.ORGANIZE.equals(objectType) || PermissionConst.POSITION.equals(objectType)) {
            userIds.addAll(userRelationMapper.getListByObjectId(id).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
        }
        if (PermissionConst.ROLE.equals(objectType)) {
            userIds.addAll(roleRelationMapper.getListByRoleId(id, PermissionConst.USER).stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList()));
        }
        userUtil.delCurUser(MsgCode.PS010.get(), userIds);
    }

    @Override
    public String save(AuthorizeDataUpForm form) {
        String errStr = "";
        try {
            UserInfo userInfo = UserProvider.getUser();
            String objectType = form.getObjectType();
            String objectId = form.getObjectId();

            List<AuthorizeEntity> objectList = new ArrayList<>();
            List<AuthorizeEntity> authorizeList = new ArrayList<>();
            PosConModel posConModel = null;
            // 设置权限归属对象
            if (PermissionConst.ORGANIZE.equals(objectType)) {
                setEntity(new String[]{objectId}, PermissionConst.ORGANIZE, objectList, true);
            }
            if (PermissionConst.POSITION.equals(objectType)) {
                setEntity(new String[]{objectId}, PermissionConst.POSITION, objectList, true);
                PositionEntity info = positionMapper.getInfo(objectId);
                if (Objects.equals(info.getIsCondition(), 1)) {
                    posConModel = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
                    posConModel.init();
                }
            }
            if (PermissionConst.ROLE.equals(objectType)) {
                setEntity(new String[]{objectId}, PermissionConst.ROLE, objectList, true);
                RoleEntity info = roleMapper.getInfo(objectId);
                if (Objects.equals(info.getIsCondition(), 1)) {
                    posConModel = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
                    posConModel.init();
                }
            }

            if (PermissionConst.PERMISSION_GROUP.equals(objectType)) {
                setEntity(new String[]{objectId}, PermissionConst.PERMISSION_GROUP, objectList, true);
            }

            List<SystemEntity> sysList = systemApi.getList();
            List<String> mainSysIds = sysList.stream().filter(t -> Objects.equals(t.getIsMain(), 1)).map(SystemEntity::getId).collect(Collectors.toList());

            if (form.getModule() != null) {
                List<String> menuList = Arrays.asList(form.getModule());
                List<ModuleEntity> menuEntityList = moduleApi.getModuleByIds(menuList);
                Set<String> array = new HashSet<>(menuList);
                //超出权限基数的截取
                if (posConModel != null && posConModel.getNumFlag()) {
                    //移除非菜单数据，用于基数计算
                    List<String> menuIds = menuEntityList.stream().map(ModuleEntity::getId).collect(Collectors.toList());
                    array = menuList.stream().filter(menuIds::contains).collect(Collectors.toSet());
                    List<String> listByObjectId = this.getListByObjectId(objectId, PermissionConst.MODULE).stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                    listByObjectId.removeAll(mainSysIds);
                    if (!listByObjectId.isEmpty() && menuList.size() == listByObjectId.size() && menuList.containsAll(listByObjectId)) {
                        return "";
                    }

                    //数据库存储权限实际菜单数量
                    List<String> dbMenuIds = moduleApi.getModuleByIds(listByObjectId)
                            .stream().filter(t -> !Objects.equals(t.getType(), 1)).map(ModuleEntity::getId).collect(Collectors.toList());
                    //修改的时候权限包含原来的全部权限-并且原权限已达基数。
                    if (!listByObjectId.isEmpty() && dbMenuIds.size() >= posConModel.getPermissionNum() && menuList.containsAll(dbMenuIds)) {
                        throw new DataException(MsgCode.SYS144.get());
                    }

                    //权限基数的时候只算实际菜单
                    List<String> collect = menuEntityList.stream().filter(t -> !Objects.equals(t.getType(), 1)).map(ModuleEntity::getId).collect(Collectors.toList());
                    List<String> collect1 = menuList.stream().filter(collect::contains).collect(Collectors.toList());
                    int num = collect1.size() - posConModel.getPermissionNum();
                    if (num > 0) {
                        errStr = MsgCode.SYS145.get();
                        List<String> newIds = collect1.subList(0, posConModel.getPermissionNum());
                        newIds.addAll(getParentMenu(menuEntityList, new HashSet<>(newIds)));
                        array = new HashSet<>(newIds);
                    }
                }
                Set<String> systemIds = new HashSet<>();
                Set<String> moduleIds = new HashSet<>(array);
                for (ModuleEntity item : menuEntityList) {
                    if (array.contains(item.getId())) {
                        moduleIds.add(item.getSystemId());
                        systemIds.add(item.getSystemId());
                        if (mainSysIds.contains(item.getSystemId())) {
                            moduleIds.add(CodeConst.XTCD);
                        } else {
                            moduleIds.add(CodeConst.YYCD);
                            if (JnpfConst.WEB.equals(item.getCategory())) {
                                moduleIds.add(item.getSystemId() + JnpfConst.WEB);
                            }
                            if (JnpfConst.APP.equals(item.getCategory())) {
                                moduleIds.add(item.getSystemId() + JnpfConst.APP);
                            }
                        }
                    }
                }
                form.setModule(moduleIds.toArray(new String[0]));
                form.setSystemIds(systemIds.toArray(new String[0]));
                setEntity(form.getSystemIds(), AuthorizeConst.SYSTEM, authorizeList, false);
            }

            // 设置权限模块
            setEntity(form.getButton(), AuthorizeConst.BUTTON, authorizeList, false);
            setEntity(form.getModule(), AuthorizeConst.MODULE, authorizeList, false);
            setEntity(form.getColumn(), AuthorizeConst.COLUMN, authorizeList, false);
            setEntity(form.getResource(), AuthorizeConst.RESOURCE, authorizeList, false);
            setEntity(form.getForm(), AuthorizeConst.FROM, authorizeList, false);

            //删除角色相关信息 移除下级权限
            List<OrganizeEntity> allOrgList = organizeMapper.getList(true);
            List<PositionEntity> allPosList = positionMapper.getList(true);
            List<String> objectIdAll = objectList.stream().map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
            //移除关联子数据
            deleteAllAuth(form, allOrgList, allPosList);
            //移除权限缓存
            this.removeAuthByUserOrMenu(null, Arrays.asList(form.getModule()));
            //移除权限
            String ids = String.join(",", objectIdAll);
            JdbcUtil.creUpDe(new PrepSqlDTO(XSSEscape.escapeEmpty(SqlFrameFastUtil.AUTHOR_DEL.replace("{authorizeIds}", ids))).withConn(dataSourceUtils, null));

            //权限变更提示
            List<String> userIds = new ArrayList<>();
            List<String> listIds = new ArrayList<>();
            if (PermissionConst.ORGANIZE.equals(objectType)) {
                List<String> orgIds = allOrgList.stream().filter(t -> t.getOrganizeIdTree().contains(objectId))
                        .map(OrganizeEntity::getId).collect(Collectors.toList());
                List<String> posIds = allPosList.stream().filter(t -> listIds.contains(t.getOrganizeId())).map(PositionEntity::getId).collect(Collectors.toList());
                listIds.addAll(orgIds);
                listIds.addAll(posIds);
            }
            if (PermissionConst.POSITION.equals(objectType)) {
                List<String> positionIds = allPosList.stream().filter(t -> StringUtil.isNotEmpty(t.getPositionIdTree()) && t.getPositionIdTree().contains(objectId))
                        .map(PositionEntity::getId).collect(Collectors.toList());
                listIds.addAll(positionIds);
            }
            if (PermissionConst.ROLE.equals(objectType)) {
                List<RoleRelationEntity> listByRoleId = roleRelationMapper.getListByRoleId(objectId, null);
                for (RoleRelationEntity rre : listByRoleId) {
                    if (PermissionConst.ORGANIZE.equals(rre.getObjectType())) {
                        List<String> orgIds = allOrgList.stream().filter(t -> t.getOrganizeIdTree().contains(rre.getObjectId()))
                                .map(OrganizeEntity::getId).collect(Collectors.toList());
                        List<String> posIds = allPosList.stream().filter(t -> listIds.contains(t.getOrganizeId())).map(PositionEntity::getId).collect(Collectors.toList());
                        listIds.addAll(orgIds);
                        listIds.addAll(posIds);
                    } else if (PermissionConst.POSITION.equals(rre.getObjectType())) {
                        List<String> positionIds = allPosList.stream().filter(t -> t.getPositionIdTree().contains(rre.getObjectId()))
                                .map(PositionEntity::getId).collect(Collectors.toList());
                        listIds.addAll(positionIds);
                    } else {
                        userIds.addAll(listByRoleId.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList()));
                    }
                }
            }
            if (!listIds.isEmpty()) {
                List<UserRelationEntity> listByObjectIdAll = userRelationMapper.getListByObjectIdAll(listIds);
                userIds.addAll(listByObjectIdAll.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
            }

            // 插入数据
            String sql = DbTypeUtil.checkOracle(dataSourceUtils) || DbTypeUtil.checkPostgre(dataSourceUtils) ?
                    SqlFrameFastUtil.INSERT_AUTHORIZE2 : SqlFrameFastUtil.INSERT_AUTHORIZE;

            String columnKey = StringUtils.EMPTY;
            String columnPlceholder = StringUtils.EMPTY;
            String columnValue = TenantDataSourceUtil.getTenantColumn();
            if (StringUtil.isNotEmpty(columnValue)) {
                columnKey = StrPool.COMMA + configValueUtil.getMultiTenantColumn();
                columnPlceholder = ",?";
            }
            sql = sql.replace("%COLUMN_KEY%", columnKey).replace("%COLUMN_PLACEHOLDER%", columnPlceholder);
            PrepSqlDTO dto = new PrepSqlDTO(sql).withConn(dataSourceUtils, null);
            for (int i = 0; i < objectList.size(); i++) {
                for (AuthorizeEntity entityItem : authorizeList) {
                    List<Object> data = new LinkedList<>();
                    data.add(RandomUtil.uuId());
                    data.add(entityItem.getItemType());
                    data.add(entityItem.getItemId());
                    data.add(objectList.get(i).getObjectType());
                    data.add(objectList.get(i).getObjectId());
                    data.add(i);
                    data.add(DateUtil.getNow());
                    data.add(userInfo.getUserId());
                    if (StringUtil.isNotEmpty(columnValue)) {
                        data.add(columnValue);
                    }
                    dto.addMultiData(data);
                }
            }
            JdbcUtil.creUpDeBatchOneSql(dto);

            userUtil.delCurUser(MsgCode.PS010.get(), userIds);
        } catch (DataException e1) {
            e1.printStackTrace();
            log.error("权限报错:" + e1.getMessage());
            throw new DataException(e1.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("权限报错:" + e.getMessage());
        }
        return errStr;
    }

    private Set<String> getParentMenu(List<ModuleEntity> allMenu, Set<String> childIds) {
        Set<String> newIds = new HashSet<>();
        if (CollUtil.isNotEmpty(childIds)) {
            for (ModuleEntity menu : allMenu) {
                if (childIds.contains(menu.getId()) && !"-1".equals(menu.getParentId())) {
                    newIds.add(menu.getParentId());
                }
            }
            if (CollUtil.isNotEmpty(newIds)) {
                newIds.addAll(getParentMenu(allMenu, newIds));
            }
            newIds.addAll(childIds);
        }
        return newIds;
    }

    /**
     * 删除子权限
     *
     * @param form
     * @param allOrgList
     * @param allPosList
     */
    private void deleteAllAuth(AuthorizeDataUpForm form, List<OrganizeEntity> allOrgList, List<PositionEntity> allPosList) {
        List<RoleRelationEntity> roleRealationList = roleRelationMapper.selectList(new QueryWrapper<>());
        List<AuthorizeEntity> allAuthList = this.list(new QueryWrapper<>());
        Map<String, List<AuthorizeEntity>> allAuthMap = allAuthList.stream().collect(Collectors.groupingBy(AuthorizeEntity::getObjectId));
        //表单提交的列表
        List<String> systemSave = form.getSystemIds() == null ? Collections.emptyList() : Arrays.asList(form.getModule());
        List<String> moduleSave = form.getModule() == null ? Collections.emptyList() : Arrays.asList(form.getModule());
        List<String> buttonSave = form.getButton() == null ? Collections.emptyList() : Arrays.asList(form.getButton());
        List<String> columnSave = form.getColumn() == null ? Collections.emptyList() : Arrays.asList(form.getColumn());
        List<String> resourceSave = form.getResource() == null ? Collections.emptyList() : Arrays.asList(form.getResource());
        List<String> formSave = form.getForm() == null ? Collections.emptyList() : Arrays.asList(form.getForm());

        //递归获取
        List<String> deleteAllAuth = AuthPermUtil.getDelAllAuth(AuthorizeSaveParam
                .builder().objectId(form.getObjectId()).objectType(form.getObjectType()).allOrgList(allOrgList).allPosList(allPosList).allAuthMap(allAuthMap)
                .roleRealationList(roleRealationList)
                .systemSave(systemSave).moduleSave(moduleSave).buttonSave(buttonSave).columnSave(columnSave).resourceSave(resourceSave).formSave(formSave)
                .build());
        if (CollUtil.isNotEmpty(deleteAllAuth)) {
            QueryWrapper<AuthorizeEntity> qw = new QueryWrapper<>();
            if (deleteAllAuth.size() > 1000) {
                List<List<String>> lists = Lists.partition(deleteAllAuth, 1000);
                for (List<String> list : lists) {
                    qw.lambda().in(AuthorizeEntity::getId, list).or();
                }
            } else {
                qw.lambda().in(AuthorizeEntity::getId, deleteAllAuth);
            }
            this.remove(qw);
        }
    }

    /**
     * 权限
     */
    private void setEntity(String[] ids, String type, List<AuthorizeEntity> entityList, boolean objectFlag) {
        if (ids != null) {
            for (String id : ids) {
                AuthorizeEntity entity = new AuthorizeEntity();
                if (objectFlag) {
                    entity.setObjectType(type);
                    entity.setObjectId(id);
                } else {
                    entity.setItemType(type);
                    entity.setItemId(id);
                }
                entityList.add(entity);
            }
        }
    }

    @Override
    public List<AuthorizeEntity> getListByUserId(boolean isAdmin, String userId, boolean standingfilter) {
        if (!isAdmin) {
            QueryWrapper<UserRelationEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(UserRelationEntity::getUserId, userId);
            queryWrapper.lambda().eq(UserRelationEntity::getObjectType, PermissionConst.POSITION);
            List<UserRelationEntity> list = userRelationMapper.selectList(queryWrapper);
            List<String> userRelationList = list.stream().map(u -> u.getObjectId()).collect(Collectors.toList());
            userRelationList.add(userId);
            List<String> roleList = roleRelationMapper.getListByObjectId(userRelationList, null)
                    .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
            userRelationList.addAll(roleList);

            //如果开启身份，根据身份过滤部分权限
            List<AuthorizeEntity> listByObjectId = this.getListByObjectId(userRelationList);
            List<AuthorizeEntity> standingList = listByObjectId.stream().filter(t -> PermissionConst.STAND.equals(t.getItemId())).collect(Collectors.toList());
            BaseSystemInfo baseSystemInfo = sysconfigApi.getSysInfo();
            if (standingfilter && Objects.equals(baseSystemInfo.getStandingSwitch(), 1) && !standingList.isEmpty()) {
                UserEntity info = userMapper.getInfo(userId);
                List<AuthorizeEntity> authorizeByItem = this.getAuthorizeByItem(PermissionConst.STAND, info.getStanding());
                List<String> collect = authorizeByItem.stream().map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
                userRelationList = userRelationList.stream().filter(collect::contains).collect(Collectors.toList());
            }

            if (CollUtil.isEmpty(userRelationList)) {
                return Collections.emptyList();
            }
            QueryWrapper<AuthorizeEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().in(AuthorizeEntity::getObjectId, userRelationList);
            return this.list(wrapper);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<AuthorizeEntity> getListByPosOrRoleId(String objectId, String objectType) {
        QueryWrapper<AuthorizeEntity> wrapper = new QueryWrapper<>();
        if (PermissionConst.POSITION.equals(objectType)) {
            List<String> posId = userRelationMapper.getListByObjectId(objectId, objectType).stream().map(u -> u.getObjectId()).collect(Collectors.toList());
            List<String> posRoleList = roleRelationMapper.getListByObjectId(posId, null)
                    .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
            posId.addAll(posRoleList);
            wrapper.lambda().in(AuthorizeEntity::getObjectId, posId);
        } else {
            wrapper.lambda().eq(AuthorizeEntity::getObjectId, objectId);
            wrapper.lambda().eq(AuthorizeEntity::getObjectType, PermissionConst.ROLE);
        }
        return this.list(wrapper);
    }

    @Override
    @DS("")
    public List<SuperJsonModel> getConditionSql(String moduleId, String systemCode) {
        List<SuperJsonModel> list = new ArrayList<>();
        UserInfo userInfo = UserProvider.getUser();
        String reidsKey = cacheKeyUtil.getUserAuthorize() + moduleId + "_" + userInfo.getUserId();
        long time = (long) 60 * 5;
        AuthorizeVO model;
        if (redisUtil.exists(reidsKey)) {
            model = JsonUtil.getJsonToBean(redisUtil.getString(reidsKey).toString(), AuthorizeVO.class);
        } else {
            model = this.getAuthorize(false, systemCode, 0);
            redisUtil.insert(reidsKey, JsonUtil.getObjectToString(model), time);
        }
        if (model == null) {
            return new ArrayList<>();
        }
        List<ResourceModel> resourceListAll = model.getResourceList().stream().filter(m -> m.getModuleId().equals(moduleId)).collect(Collectors.toList());
        //先遍历一次 查找其中有没有全部方案
        boolean isAll = resourceListAll.stream().filter(item -> "jnpf_alldata".equals(item.getEnCode()) || item.getEnCode().startsWith("jnpf_alldata")).count() > 0;
        //未分配权限方案
        if (isAll || Boolean.TRUE.equals(userInfo.getIsAdministrator())) {
            SuperJsonModel superJsonModel = new SuperJsonModel();
            list.add(superJsonModel);
            return list;
        }
        Map<String, List<ResourceModel>> authorizeMap = resourceListAll.stream().filter(t -> StringUtil.isNotEmpty(t.getObjectId())).collect(Collectors.groupingBy(ma -> ma.getObjectId()));
        int num = 0;
        //方案
        for (Map.Entry<String, List<ResourceModel>> entry : authorizeMap.entrySet()) {
            String key = entry.getKey();
            List<ResourceModel> resourceList = authorizeMap.get(key);
            boolean authorizeLogic = num == 0;
            for (ResourceModel item : resourceList) {
                AuthConditionModel authConditionModel = JsonUtil.getJsonToBean(item.getConditionJson(), AuthConditionModel.class);
                String matchLogic = authConditionModel.getMatchLogic();
                List<SuperQueryJsonModel> conditionList = new ArrayList<>();
                //分组
                for (AuthGroup group : authConditionModel.getConditionList()) {
                    String logic = group.getLogic();
                    List<FieLdsModel> groupList = new ArrayList<>();
                    //条件
                    for (AuthItem fieldItem : group.getGroups()) {

                        FieLdsModel fieLdsModel = JsonUtil.getJsonToBean(fieldItem, FieLdsModel.class);
                        String itemField = fieldItem.getField();
                        String table = fieldItem.getTableName();
                        String vModel = "";
                        if (itemField.contains("_jnpf_")) {
                            vModel = itemField.split("_jnpf_")[1];
                        } else if (itemField.toLowerCase().startsWith("tablefield")) {
                            vModel = itemField.split("-")[1];
                        } else {
                            vModel = itemField;
                        }
                        ConfigModel config = fieLdsModel.getConfig();
                        String jnpfKey = fieldItem.getJnpfKey();
                        if (AuthorizeConditionEnum.CURRENTTIME.getCondition().equals(jnpfKey)) {
                            jnpfKey = AuthorizeConst.DATE_PICKER;
                        }
                        config.setJnpfKey(jnpfKey);
                        config.setTableName(table);
                        fieLdsModel.setConfig(config);
                        fieLdsModel.setSymbol(fieldItem.getSymbol());
                        fieLdsModel.setVModel(vModel);
                        fieLdsModel.setId(itemField);
                        fieLdsModel.setFieldValue(fieldItem.getFieldValue());
                        groupList.add(fieLdsModel);
                    }
                    //搜索条件
                    SuperQueryJsonModel queryJsonModel = new SuperQueryJsonModel();
                    queryJsonModel.setGroups(groupList);
                    queryJsonModel.setLogic(logic);
                    conditionList.add(queryJsonModel);
                }
                if (!conditionList.isEmpty()) {
                    SuperJsonModel superJsonModel = new SuperJsonModel();
                    superJsonModel.setMatchLogic(matchLogic);
                    superJsonModel.setConditionList(conditionList);
                    superJsonModel.setAuthorizeLogic(authorizeLogic);
                    list.add(superJsonModel);
                }
            }
            num += list.isEmpty() ? 0 : 1;
        }
        return list;
    }

    @Override
    public void removeAuthByUserOrMenu(List<String> userIds, List<String> menuIds) {
        userIds = userIds == null ? new ArrayList<>() : userIds;
        menuIds = menuIds == null ? new ArrayList<>() : menuIds;
        if(!userIds.isEmpty() && !menuIds.isEmpty()) {
            Set<String> deleteKeys = new HashSet<>();
            Set<String> userAuthorizeKeys = redisUtil.findKeysContaining(cacheKeyUtil.getUserAuthorize() + "*");
            for (String cacheKey : userAuthorizeKeys) {
                boolean isMatch = false;
                for (String user : userIds) {
                    if (cacheKey.contains(user)) {
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch) {
                    for (String menuId : menuIds) {
                        if (cacheKey.contains(menuId)) {
                            isMatch = true;
                            break;
                        }
                    }
                }
                if (isMatch) {
                    deleteKeys.add(cacheKey);
                }
            }
            if (!deleteKeys.isEmpty()) {
                redisUtil.remove(deleteKeys);
            }
        }
    }

    @Override
    public List<AuthorizeEntity> getListByObjectId(List<String> objectId) {
        return this.baseMapper.getListByObjectId(objectId);
    }

    @Override
    public List<AuthorizeEntity> getListByRoleId(String roleId) {
        return this.baseMapper.getListByRoleId(roleId);
    }

    @Override
    public List<AuthorizeEntity> getListByObjectId(String objectId, String itemType) {
        return this.baseMapper.getListByObjectId(objectId, itemType);
    }

    @Override
    public List<AuthorizeEntity> getListByObjectAndItem(String itemId, String objectType) {
        return this.baseMapper.getListByObjectAndItem(itemId, objectType);
    }

    @Override
    public List<AuthorizeEntity> getListByObjectAndItemIdAndType(String itemId, String itemType) {
        return this.baseMapper.getListByObjectAndItemIdAndType(itemId, itemType);
    }

    @Override
    @DSTransactional
    public void saveObjectAuth(SavePortalAuthModel portalAuthModel) {
        this.baseMapper.saveObjectAuth(portalAuthModel);
    }

    @Override
    public List<AuthorizeEntity> getAuthorizeByItem(String itemType, String itemId) {
        return this.baseMapper.getAuthorizeByItem(itemType, itemId);
    }

    @Override
    public List<AuthorizeEntity> getListByRoleIdsAndItemType(List<String> roleIds, String itemType) {
        return this.baseMapper.getListByRoleIdsAndItemType(roleIds, itemType);
    }

    @Override
    public void setPermissionGroup(String objectId, String objectType) {
        if (PermissionConst.PERMISSION_GROUP.equals(objectType)) {
            PermissionGroupEntity info = permissionGroupService.info(objectId);
            if (info != null && StringUtil.isNotEmpty(info.getPermissionMember())) {
                String[] split = info.getPermissionMember().split(",");
                //根据ids获取上级，赋予新的权限
                List<String> userIdList = permissionGroupService.setAuthByIds(objectId, Arrays.asList(split));
                //岗位组织获取用户刷新权限
                userUtil.delCurUser(null, userIdList);
                //移除权限缓存
                this.removeAuthByUserOrMenu(userIdList, null);
            }
        }
    }
}
