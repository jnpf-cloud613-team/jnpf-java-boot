package jnpf.permission.util;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.*;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.button.ButtonModel;
import jnpf.base.model.column.ColumnModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.print.PaginationPrint;
import jnpf.base.model.resource.ResourceModel;
import jnpf.base.service.*;
import jnpf.constant.AuthorizeConst;
import jnpf.constant.CodeConst;
import jnpf.constant.JnpfConst;
import jnpf.constant.PermissionConst;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.model.template.TemplateTreeListVo;
import jnpf.permission.entity.*;
import jnpf.permission.model.authorize.*;
import jnpf.permission.model.user.UserAuthForm;
import jnpf.permission.model.user.mod.UserAuthorizeModel;
import jnpf.permission.model.user.vo.UserAuthorizeVO;
import jnpf.permission.service.*;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限查询列表
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/19 14:59:51
 */
@Component
@RequiredArgsConstructor
public class AuthPermUtil {
    private final AuthorizeService authorizeService;
    private final UserService userService;
    private final OrganizeService organizeService;
    private final PositionService positionService;
    private final RoleRelationService roleRelationService;
    private final SystemService systemApi;
    private final ModuleService moduleApi;
    private final ModuleButtonService buttonApi;
    private final ModuleColumnService columnApi;
    private final ModuleDataAuthorizeSchemeService schemeApi;
    private final ModuleFormService formApi;
    private final TemplateApi templateApi;
    private final PrintDevService printDevApi;

    public AuthorizeDataReturnVO getAuthMenuList(AuthorizeParam authorizeParam) {
        UserInfo userInfo = UserProvider.getUser();
        Boolean isManageRole = userInfo.getIsManageRole();
        Boolean isDevRole = userInfo.getIsDevRole();
        //当前用户可配置权限
        AuthorizeVO authorizeModel = authorizeService.getAuthorize(false, null, 0, true);
        //继承上级权限
        List<AuthorizeEntity> authorizeList = authorizeService.list(new QueryWrapper<AuthorizeEntity>().lambda().eq(AuthorizeEntity::getObjectId, authorizeParam.getObjectId()));

        //获取资源系统和菜单全数据
        setAllSysAndModule(authorizeParam);

        // 处理当前用户不可选的
        List<ModuleModel> authModelList = authorizeModel.getModuleList();
        List<SystemBaeModel> authSysList = authorizeModel.getSystemList();
        Set<String> noContainsIds = CollUtil.newHashSet();

        //额外添加用户创建的应用-及菜单
        List<SystemEntity> listByCreUser = systemApi.getListByCreUser(userInfo.getUserId());
        List<String> creSysIds = listByCreUser.stream().map(SystemEntity::getId).collect(Collectors.toList());
        authSysList.addAll(JsonUtil.getJsonToList(listByCreUser, SystemBaeModel.class));
        noContainsIds.addAll(moduleApi.getModuleBySystemIds(creSysIds, null, null, 1).stream().map(ModuleEntity::getId).collect(Collectors.toList()));

        //添加当前用户有的权限
        for (SystemBaeModel systemBaeModel : authSysList) {
            noContainsIds.add(systemBaeModel.getId());
            if (Objects.equals(systemBaeModel.getIsMain(), 1)) {
                noContainsIds.add(CodeConst.XTCD);
            } else {
                noContainsIds.add(CodeConst.YYCD);
            }
        }
        for (ModuleModel moduleModel : authModelList) {
            noContainsIds.add(moduleModel.getId());
            if (JnpfConst.WEB.equals(moduleModel.getCategory())) {
                noContainsIds.add(moduleModel.getSystemId() + "2");
            }
            if (JnpfConst.APP.equals(moduleModel.getCategory())) {
                noContainsIds.add(moduleModel.getSystemId() + "1");
            }
        }

        String moduleIds = authorizeParam.getModuleIds();
        List<String> selectIds = new ArrayList<>();
        if (StringUtils.isNotBlank(moduleIds)) {
            String[] split = moduleIds.split(",");
            selectIds.addAll(Arrays.asList(split));
        }

        //出系统菜单外其他类型的数据
        List<ModuleModel> otherMList = new ArrayList<>();
        // 选中的菜单
        List<String> ids = new ArrayList<>();
        //实际业务
        List<ModuleEntity> menuEntityList = authorizeParam.getMenuEntityList();
        Set<String> hasModule = new HashSet<>();
        switch (authorizeParam.getItemType()) {
            case AuthorizeConst.MODULE:
                ids = authorizeList.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                break;
            case AuthorizeConst.BUTTON:
                List<ModuleButtonEntity> buttonList = authorizeParam.getButtonEntityList();
                for (ModuleButtonEntity item : buttonList) {
                    //菜单选中的数据过滤
                    if (selectIds.contains(item.getModuleId())) {
                        ModuleModel model = JsonUtil.getJsonToBean(item, ModuleModel.class);
                        model.setParentId(item.getModuleId());
                        otherMList.add(model);
                        hasModule.add(item.getModuleId());
                    }
                }
                //移除没有按钮的菜单
                menuEntityList = removeLeaf(menuEntityList, hasModule);
                authorizeParam.setMenuEntityList(menuEntityList);
                //处理选中的
                ids = authorizeList.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                //处理当前用户不可选的
                List<ButtonModel> authBtnList = authorizeModel.getButtonList();
                authBtnList.stream().forEach(t -> noContainsIds.add(t.getId()));
                break;
            case AuthorizeConst.COLUMN:
                List<ModuleColumnEntity> columnEntityList = authorizeParam.getColumnEntityList();
                for (ModuleColumnEntity item : columnEntityList) {
                    //菜单选中的数据过滤
                    if (selectIds.contains(item.getModuleId())) {
                        ModuleModel model = JsonUtil.getJsonToBean(item, ModuleModel.class);
                        model.setParentId(item.getModuleId());
                        otherMList.add(model);
                        hasModule.add(item.getModuleId());
                    }
                }
                //移除没有的菜单
                menuEntityList = removeLeaf(menuEntityList, hasModule);
                authorizeParam.setMenuEntityList(menuEntityList);
                //处理选中的
                ids = authorizeList.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                //处理当前用户不可选的
                List<ColumnModel> columnList = authorizeModel.getColumnList();
                columnList.stream().forEach(t -> noContainsIds.add(t.getId()));
                break;
            case AuthorizeConst.RESOURCE:
                List<ModuleDataAuthorizeSchemeEntity> resEntityList = authorizeParam.getResEntityList();
                for (ModuleDataAuthorizeSchemeEntity item : resEntityList) {
                    //菜单选中的数据过滤
                    if (selectIds.contains(item.getModuleId())) {
                        ModuleModel model = JsonUtil.getJsonToBean(item, ModuleModel.class);
                        model.setParentId(item.getModuleId());
                        otherMList.add(model);
                        hasModule.add(item.getModuleId());
                    }
                }
                //移除没有的菜单
                menuEntityList = removeLeaf(menuEntityList, hasModule);
                authorizeParam.setMenuEntityList(menuEntityList);
                //处理选中的
                ids = authorizeList.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                //处理当前用户不可选的
                List<ResourceModel> resourceList = authorizeModel.getResourceList();
                resourceList.stream().forEach(t -> noContainsIds.add(t.getId()));
                break;
            case AuthorizeConst.FROM:
                List<ModuleFormEntity> formEntityList = authorizeParam.getFormEntityList();
                for (ModuleFormEntity item : formEntityList) {
                    //菜单选中的数据过滤
                    if (selectIds.contains(item.getModuleId())) {
                        ModuleModel model = JsonUtil.getJsonToBean(item, ModuleModel.class);
                        model.setParentId(item.getModuleId());
                        otherMList.add(model);
                        hasModule.add(item.getModuleId());
                    }
                }
                //移除没有的菜单
                menuEntityList = removeLeaf(menuEntityList, hasModule);
                authorizeParam.setMenuEntityList(menuEntityList);
                //处理选中的
                ids = authorizeList.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                //处理当前用户不可选的
                List<ModuleFormModel> formsList = authorizeModel.getFormsList();
                formsList.stream().forEach(t -> noContainsIds.add(t.getId()));
                break;
            default:
                break;
        }

        //组装系统菜单数据
        List<ModuleModel> moduleList = new ArrayList<>(getModuleModels(authorizeParam));
        //添加其他类型数据
        moduleList.addAll(otherMList);
        //树形转换
        List<String> allIds = moduleList.stream().map(t -> t.getId()).collect(Collectors.toList());
        List<AuthorizeDataModel> treeList = JsonUtil.getJsonToList(moduleList, AuthorizeDataModel.class);
        treeList = treeList.stream().sorted(Comparator.comparing(AuthorizeDataModel::getSortCode, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AuthorizeDataModel::getCreatorTime, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        //递归禁用当前用户不可操作的数据
        Set<String> disList = new HashSet<>();
        treeList.forEach(t -> {
            if (!noContainsIds.contains(t.getId())) {
                t.setDisabled(true);
                disList.add(t.getId());
            }
        });
        setDisable(treeList, disList);

        List<SumTree<AuthorizeDataModel>> trees = TreeDotUtils.convertListToTreeDot(treeList, "-1");
        List<AuthorizeDataReturnModel> data = JsonUtil.getJsonToList(trees, AuthorizeDataReturnModel.class);

        if ((isManageRole || isDevRole)) {
            setDisableFalse(data, true);
        }
        setParentFalse(data, ids);

        AuthorizeDataReturnVO vo = new AuthorizeDataReturnVO();
        vo.setList(data);
        vo.setAll(allIds);
        vo.setIds(ids);
        return vo;
    }

    /**
     * 菜单添加上级
     *
     * @param authorizeParam
     * @return
     */
    private List<ModuleModel> getModuleModels(AuthorizeParam authorizeParam) {
        boolean isMain = JnpfConst.MAIN_SYSTEM_CODE.equals(authorizeParam.getAppCode());
        List<SystemEntity> systemEntityList = authorizeParam.getSystemEntityList();
        List<ModuleEntity> menuEntityList = authorizeParam.getMenuEntityList();
        List<String> menuHasSystemId = menuEntityList.stream().map(ModuleEntity::getSystemId).collect(Collectors.toList());

        List<String> xtIds = new ArrayList<>();//系统
        List<ModuleModel> yyList = new ArrayList<>();//应用
        for (SystemEntity systemEntity : systemEntityList) {
            if (!menuHasSystemId.contains(systemEntity.getId())) continue;
            if (Objects.equals(systemEntity.getIsMain(), 1)) {
                xtIds.add(systemEntity.getId());
            } else {
                ModuleModel yymodel = JsonUtil.getJsonToBean(systemEntity, ModuleModel.class);
                yymodel.setParentId(CodeConst.YYCD);
                yyList.add(yymodel);
            }
        }
        List<ModuleModel> xtCdList = new ArrayList<>();//系统菜单
        List<ModuleModel> yyCdList = new ArrayList<>();//应用菜单
        Map<String, String> appIds = new HashMap<>(16);
        Map<String, String> webIds = new HashMap<>(16);
        for (ModuleEntity moduleEntity : menuEntityList) {
            ModuleModel t = JsonUtil.getJsonToBean(moduleEntity, ModuleModel.class);
            if (xtIds.contains(t.getSystemId())) {
                if ("-1".equals(t.getParentId())) {
                    t.setParentId(CodeConst.XTCD);
                }
                xtCdList.add(t);
            } else {
                if (JnpfConst.APP.equals(t.getCategory()) && "-1".equals(t.getParentId())) {
                    if (!appIds.containsKey(t.getSystemId())) {
                        t.setParentId(t.getSystemId() + JnpfConst.APP);
                        ModuleModel appData = new ModuleModel();
                        appData.setId(t.getSystemId() + JnpfConst.APP);
                        appData.setSortCode(0L);
                        appData.setFullName("APP菜单");
                        appData.setIcon(PermissionConst.APP_ICON);
                        appData.setParentId(t.getSystemId());
                        appData.setSystemId(t.getSystemId());
                        yyCdList.add(appData);
                        appIds.put(t.getSystemId(), appData.getId());
                    } else {
                        t.setParentId(appIds.get(t.getSystemId()) + "");
                    }
                } else if (JnpfConst.WEB.equals(t.getCategory()) && "-1".equals(t.getParentId())) {
                    if (!webIds.containsKey(t.getSystemId())) {
                        t.setParentId(t.getSystemId() + JnpfConst.WEB);
                        ModuleModel webData = new ModuleModel();
                        webData.setId(t.getSystemId() + JnpfConst.WEB);
                        webData.setSortCode(-1L);
                        webData.setFullName("WEB菜单");
                        webData.setIcon(PermissionConst.PC_ICON);
                        webData.setParentId(t.getSystemId());
                        webData.setSystemId(t.getSystemId());
                        yyCdList.add(webData);
                        webIds.put(t.getSystemId(), webData.getId());
                    } else {
                        t.setParentId(webIds.get(t.getSystemId()) + "");
                    }
                }
                yyCdList.add(t);
            }
        }

        List<ModuleModel> modelList = new ArrayList<>();
        if (isMain) {
            ModuleModel moduleModel = new ModuleModel();
            moduleModel.setId(CodeConst.XTCD);
            moduleModel.setFullName("系统菜单");
            moduleModel.setParentId("-1");
            ModuleModel moduleModel2 = new ModuleModel();
            moduleModel2.setId(CodeConst.YYCD);
            moduleModel2.setFullName("应用菜单");
            moduleModel2.setParentId("-1");
            if (CollUtil.isNotEmpty(xtCdList)) {
                modelList.add(moduleModel);
            }
            if (CollUtil.isNotEmpty(yyCdList)) {
                modelList.add(moduleModel2);
            }
            modelList.addAll(yyList);
            modelList.addAll(xtCdList);
            modelList.addAll(yyCdList);
        } else {
            modelList.addAll(yyCdList);
        }
        return modelList;
    }

    /**
     * 获取当前 组织、岗位、角色拥有的全部系统和菜单权限
     */
    private void setAllSysAndModule(AuthorizeParam authorizeParam) {
        String objectId = authorizeParam.getObjectId();
        String objectType = authorizeParam.getObjectType();
        String itemType = authorizeParam.getItemType();

        boolean needFilter = true;//根据组织岗位角色判断是否需要过滤
        List<AuthorizeEntity> list = new ArrayList<>();
        if (PermissionConst.ORGANIZE.equals(objectType)) {
            OrganizeEntity info = organizeService.getInfo(objectId);
            if ("-1".equals(info.getParentId()) || StringUtil.isEmpty(info.getParentId())) {
                //全数据
                needFilter = false;
            } else {
                //获取父级组织和组织角色的全部授权菜单
                List<String> objectIds = new ArrayList<>();
                objectIds.add(info.getParentId());
                List<String> collect = roleRelationService.getListByObjectId(info.getParentId(), PermissionConst.ORGANIZE)
                        .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                objectIds.addAll(collect);
                list = authorizeService.list(new QueryWrapper<AuthorizeEntity>().lambda().in(AuthorizeEntity::getObjectId, objectIds));
            }
        }
        if (PermissionConst.POSITION.equals(objectType)) {
            PositionEntity info = positionService.getInfo(objectId);
            if ("-1".equals(info.getParentId()) || StringUtil.isEmpty(info.getParentId())) {
                List<String> objectIds = new ArrayList<>();
                objectIds.add(info.getOrganizeId());
                List<String> collect = roleRelationService.getListByObjectId(info.getOrganizeId(), PermissionConst.ORGANIZE)
                        .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                objectIds.addAll(collect);
                list = authorizeService.list(new QueryWrapper<AuthorizeEntity>().lambda().in(AuthorizeEntity::getObjectId, objectIds));
            } else {
                //获取父级岗位及岗位角色的全部授权信息
                List<String> objectIds = new ArrayList<>();
                objectIds.add(info.getParentId());
                List<String> collect = roleRelationService.getListByObjectId(info.getParentId(), PermissionConst.POSITION)
                        .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                objectIds.addAll(collect);
                list = authorizeService.list(new QueryWrapper<AuthorizeEntity>().lambda().in(AuthorizeEntity::getObjectId, objectIds));
            }
        }
        if (PermissionConst.ROLE.equals(objectType) || PermissionConst.PERMISSION_GROUP.equals(objectType)) {
            //全数据
            needFilter = false;
        }

        if (needFilter) {
            List<String> systemList = list.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            List<String> moduleList = list.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            authorizeParam.setSystemEntityList(systemApi.getListByIds(systemList, null)
                    .stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            authorizeParam.setMenuEntityList(moduleApi.getModuleByIds(moduleList)
                    .stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            if (AuthorizeConst.BUTTON.equals(itemType)) {
                List<String> itemIds = list.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                authorizeParam.setButtonEntityList(buttonApi.getListByIds(itemIds).stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
            if (AuthorizeConst.COLUMN.equals(itemType)) {
                List<String> itemIds = list.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                authorizeParam.setColumnEntityList(columnApi.getListByIds(itemIds).stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
            if (AuthorizeConst.RESOURCE.equals(itemType)) {
                List<String> itemIds = list.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                authorizeParam.setResEntityList(schemeApi.getListByIds(itemIds).stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
            if (AuthorizeConst.FROM.equals(itemType)) {
                List<String> itemIds = list.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                authorizeParam.setFormEntityList(formApi.getListByIds(itemIds).stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
        } else {
            authorizeParam.setSystemEntityList(systemApi.getList().stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            authorizeParam.setMenuEntityList(moduleApi.getList().stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            if (AuthorizeConst.BUTTON.equals(itemType)) {
                authorizeParam.setButtonEntityList(buttonApi.getList().stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
            if (AuthorizeConst.COLUMN.equals(itemType)) {
                authorizeParam.setColumnEntityList(columnApi.getList().stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
            if (AuthorizeConst.RESOURCE.equals(itemType)) {
                authorizeParam.setResEntityList(schemeApi.getList().stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
            if (AuthorizeConst.FROM.equals(itemType)) {
                authorizeParam.setFormEntityList(formApi.getList().stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList()));
            }
        }
    }

    /**
     * 递归获取包含的上级（根据下级移除没有下级的数据）
     *
     * @param list
     * @param hasIds
     * @return
     */
    public static List<ModuleEntity> removeLeaf(List<ModuleEntity> list, Set<String> hasIds) {
        List<ModuleEntity> res = new ArrayList<>();
        Set<String> newHasIds = new HashSet<>();
        for (ModuleEntity moduleEntity : list) {
            if (hasIds.contains(moduleEntity.getId())) {
                res.add(moduleEntity);
                newHasIds.add(moduleEntity.getParentId());
            }
        }
        if (CollUtil.isNotEmpty(newHasIds)) {
            res.addAll(removeLeaf(list, newHasIds));
        }
        return res;
    }

    private static void setDisable(List<AuthorizeDataModel> list, Set<String> disList) {
        Set<String> newIds = new HashSet<>();
        for (AuthorizeDataModel item : list) {
            if (disList.contains(item.getId())) {
                item.setDisabled(true);
                if (StringUtil.isNotEmpty(item.getParentId()) && !"-1".equals(item.getParentId())) {
                    newIds.add(item.getParentId());
                }
            }
        }
        if (CollUtil.isNotEmpty(newIds)) {
            setDisable(list, newIds);
        }
    }

    /**
     * 管理员和开发者角色：应用部分菜单可授权
     *
     * @param list
     * @param isFirst 是否一级
     */
    private static void setDisableFalse(List<AuthorizeDataReturnModel> list, boolean isFirst) {
        for (AuthorizeDataReturnModel item : list) {
            if (!isFirst || CodeConst.YYCD.equals(item.getId())) {
                item.setDisabled(false);
                if (CollUtil.isNotEmpty(item.getChildren())) {
                    setDisableFalse(item.getChildren(), false);
                }
            }
        }
    }

    private static void setParentFalse(List<AuthorizeDataReturnModel> list, List<String> ids) {
        for (AuthorizeDataReturnModel item : list) {
            if (CollUtil.isNotEmpty(item.getChildren())) {
                //先进底层设置
                setParentFalse(item.getChildren(), ids);
                //设置完后，设置当前层级问题
                List<AuthorizeDataReturnModel> children = item.getChildren();
                List<AuthorizeDataReturnModel> collect = children.stream().filter(t -> t.isDisabled()).collect(Collectors.toList());
                boolean disCheck = collect.stream().anyMatch(t -> ids.contains(t.getId()));
                //全部禁用不调整，禁用的没有被勾选--放开上级勾选
                if (children.size() != collect.size() && !disCheck) {
                    item.setDisabled(false);
                }
            }
        }
    }

    /**
     * 递归获取需要删除的权限idlist
     *
     * @param param
     * @return
     */
    public static List<String> getDelAllAuth(AuthorizeSaveParam param) {
        List<String> listRes = new ArrayList<>();
        Map<String, List<AuthorizeEntity>> allAuthMap = param.getAllAuthMap();
        List<RoleRelationEntity> roleRealationList = param.getRoleRealationList();
        List<OrganizeEntity> allOrgList = param.getAllOrgList();
        List<PositionEntity> allPosList = param.getAllPosList();
        String objectType = param.getObjectType();
        String objectId = param.getObjectId();

        List<String> systemSave = new ArrayList<>(param.getSystemSave());
        List<String> moduleSave = new ArrayList<>(param.getModuleSave());
        List<String> buttonSave = new ArrayList<>(param.getButtonSave());
        List<String> columnSave = new ArrayList<>(param.getColumnSave());
        List<String> resourceSave = new ArrayList<>(param.getResourceSave());
        List<String> formSave = new ArrayList<>(param.getFormSave());

        if (PermissionConst.ORGANIZE.equals(objectType)) {
            OrganizeEntity organizeEntity = allOrgList.stream().filter(t -> objectId.equals(t.getId())).findFirst().orElse(null);
            if (organizeEntity != null) {
                List<String> roleIds = roleRealationList.stream().filter(t -> PermissionConst.ORGANIZE.equals(t.getObjectType())
                        && objectId.equals(t.getObjectId())).map(RoleRelationEntity::getRoleId).collect(Collectors.toList());

                //本级角色拥有的下级不删除
                for (String roleId : roleIds) {
                    //移除角色相关权限时，需要跳过当前权限
                    if (StringUtil.isNotEmpty(param.getThisRole())) continue;
                    //角色权限为空跳过
                    if (allAuthMap.get(roleId) != null) {
                        for (AuthorizeEntity t : allAuthMap.get(roleId)) {
                            if (AuthorizeConst.SYSTEM.equals(t.getItemType()) && !systemSave.contains(t.getItemId()))
                                systemSave.add(t.getItemId());
                            if (AuthorizeConst.MODULE.equals(t.getItemType()) && !moduleSave.contains(t.getItemId()))
                                moduleSave.add(t.getItemId());
                            if (AuthorizeConst.BUTTON.equals(t.getItemType()) && !buttonSave.contains(t.getItemId()))
                                buttonSave.add(t.getItemId());
                            if (AuthorizeConst.COLUMN.equals(t.getItemType()) && !columnSave.contains(t.getItemId()))
                                columnSave.add(t.getItemId());
                            if (AuthorizeConst.RESOURCE.equals(t.getItemType()) && !resourceSave.contains(t.getItemId()))
                                resourceSave.add(t.getItemId());
                            if (AuthorizeConst.FROM.equals(t.getItemType()) && !formSave.contains(t.getItemId()))
                                formSave.add(t.getItemId());
                        }
                    }
                }
                //移除子组织权限
                List<OrganizeEntity> orgList = allOrgList.stream().filter(t -> objectId.equals(t.getParentId())).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(orgList)) {
                    for (OrganizeEntity entity : orgList) {
                        //获取需要删除的权限关联数据id
                        List<AuthorizeEntity> authorizeEntities = allAuthMap.get(entity.getId()) == null ? Collections.emptyList() : allAuthMap.get(entity.getId());
                        List<String> delIds = authorizeEntities.stream().filter(t ->
                                (AuthorizeConst.SYSTEM.equals(t.getItemType()) && !systemSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.MODULE.equals(t.getItemType()) && !moduleSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.BUTTON.equals(t.getItemType()) && !buttonSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.COLUMN.equals(t.getItemType()) && !columnSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.RESOURCE.equals(t.getItemType()) && !resourceSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.FROM.equals(t.getItemType()) && !formSave.contains(t.getItemId()))).map(AuthorizeEntity::getId).collect(Collectors.toList());
                        listRes.addAll(delIds);

                        //递归子数据删除
                        List<String> deleteAllAuth = AuthPermUtil.getDelAllAuth(AuthorizeSaveParam
                                .builder().objectId(entity.getId()).objectType(PermissionConst.ORGANIZE).allOrgList(allOrgList).allPosList(allPosList).allAuthMap(allAuthMap)
                                .roleRealationList(roleRealationList)
                                .systemSave(authorizeEntities.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType()) && systemSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .moduleSave(authorizeEntities.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType()) && moduleSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .buttonSave(authorizeEntities.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType()) && buttonSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .columnSave(authorizeEntities.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType()) && columnSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .resourceSave(authorizeEntities.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType()) && resourceSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .formSave(authorizeEntities.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType()) && formSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .build());
                        listRes.addAll(deleteAllAuth);
                    }
                }
                //移除改组织下岗位的权限
                List<PositionEntity> posList = allPosList.stream().filter(t -> objectId.equals(t.getOrganizeId()) && StringUtil.isEmpty(t.getParentId())).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(posList)) {
                    for (PositionEntity entity : posList) {
                        //获取需要删除的权限关联数据id
                        List<AuthorizeEntity> authorizeEntities = allAuthMap.get(entity.getId()) == null ? Collections.emptyList() : allAuthMap.get(entity.getId());
                        List<String> delIds = authorizeEntities.stream().filter(t ->
                                (AuthorizeConst.SYSTEM.equals(t.getItemType()) && !systemSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.MODULE.equals(t.getItemType()) && !moduleSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.BUTTON.equals(t.getItemType()) && !buttonSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.COLUMN.equals(t.getItemType()) && !columnSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.RESOURCE.equals(t.getItemType()) && !resourceSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.FROM.equals(t.getItemType()) && !formSave.contains(t.getItemId()))).map(AuthorizeEntity::getId).collect(Collectors.toList());
                        listRes.addAll(delIds);
                        List<String> deleteAllAuth = AuthPermUtil.getDelAllAuth(AuthorizeSaveParam
                                .builder().objectId(entity.getId()).objectType(PermissionConst.POSITION).allOrgList(allOrgList).allPosList(allPosList).allAuthMap(allAuthMap)
                                .roleRealationList(roleRealationList)
                                .systemSave(authorizeEntities.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType()) && systemSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .moduleSave(authorizeEntities.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType()) && moduleSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .buttonSave(authorizeEntities.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType()) && buttonSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .columnSave(authorizeEntities.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType()) && columnSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .resourceSave(authorizeEntities.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType()) && resourceSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .formSave(authorizeEntities.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType()) && formSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .build());
                        listRes.addAll(deleteAllAuth);
                    }
                }
            }
        }
        if (PermissionConst.POSITION.equals(objectType)) {
            PositionEntity positionEntity = allPosList.stream().filter(t -> objectId.equals(t.getId())).findFirst().orElse(null);
            if (positionEntity != null) {
                List<String> roleIds = roleRealationList.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType())
                        && objectId.equals(t.getObjectId())).map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                //本级角色拥有的下级不删除
                for (String roleId : roleIds) {
                    if (allAuthMap.get(roleId) == null) continue;
                    for (AuthorizeEntity t : allAuthMap.get(roleId)) {
                        if (AuthorizeConst.SYSTEM.equals(t.getItemType()) && !systemSave.contains(t.getItemId()))
                            systemSave.add(t.getItemId());
                        if (AuthorizeConst.MODULE.equals(t.getItemType()) && !moduleSave.contains(t.getItemId()))
                            moduleSave.add(t.getItemId());
                        if (AuthorizeConst.BUTTON.equals(t.getItemType()) && !buttonSave.contains(t.getItemId()))
                            buttonSave.add(t.getItemId());
                        if (AuthorizeConst.COLUMN.equals(t.getItemType()) && !columnSave.contains(t.getItemId()))
                            columnSave.add(t.getItemId());
                        if (AuthorizeConst.RESOURCE.equals(t.getItemType()) && !resourceSave.contains(t.getItemId()))
                            resourceSave.add(t.getItemId());
                        if (AuthorizeConst.FROM.equals(t.getItemType()) && !formSave.contains(t.getItemId()))
                            formSave.add(t.getItemId());
                    }
                }
                //移除下级岗位的权限
                List<PositionEntity> posList = allPosList.stream().filter(t -> objectId.equals(t.getParentId())).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(posList)) {
                    for (PositionEntity entity : posList) {
                        //获取需要删除的权限关联数据id
                        List<AuthorizeEntity> authorizeEntities = allAuthMap.get(entity.getId()) == null ? Collections.emptyList() : allAuthMap.get(entity.getId());
                        List<String> delIds = authorizeEntities.stream().filter(t ->
                                (AuthorizeConst.SYSTEM.equals(t.getItemType()) && !systemSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.MODULE.equals(t.getItemType()) && !moduleSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.BUTTON.equals(t.getItemType()) && !buttonSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.COLUMN.equals(t.getItemType()) && !columnSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.RESOURCE.equals(t.getItemType()) && !resourceSave.contains(t.getItemId())) ||
                                        (AuthorizeConst.FROM.equals(t.getItemType()) && !formSave.contains(t.getItemId()))).map(AuthorizeEntity::getId).collect(Collectors.toList());
                        listRes.addAll(delIds);
                        List<String> deleteAllAuth = AuthPermUtil.getDelAllAuth(AuthorizeSaveParam
                                .builder().objectId(entity.getId()).objectType(PermissionConst.POSITION).allOrgList(allOrgList).allPosList(allPosList).allAuthMap(allAuthMap)
                                .roleRealationList(roleRealationList)
                                .systemSave(authorizeEntities.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType()) && systemSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .moduleSave(authorizeEntities.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType()) && moduleSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .buttonSave(authorizeEntities.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType()) && buttonSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .columnSave(authorizeEntities.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType()) && columnSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .resourceSave(authorizeEntities.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType()) && resourceSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .formSave(authorizeEntities.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType()) && formSave.contains(t.getItemId()))
                                        .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                .build());
                        listRes.addAll(deleteAllAuth);
                    }
                }
            }
        }
        if (PermissionConst.ROLE.equals(objectType)) {
            List<RoleRelationEntity> rList = roleRealationList.stream().filter(t -> t.getRoleId().equals(objectId)).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(rList)) {
                for (RoleRelationEntity item : rList) {
                    if (PermissionConst.ORGANIZE.equals(item.getObjectType())) {
                        OrganizeEntity organizeEntity = allOrgList.stream().filter(t -> item.getObjectId().equals(t.getId())).findFirst().orElse(null);
                        if (organizeEntity != null) {
                            List<AuthorizeEntity> authorizeEntities = allAuthMap.get(organizeEntity.getId()) == null ? Collections.emptyList() : allAuthMap.get(organizeEntity.getId());
                            //当前不移除，移除子组织的相关权限
                            List<String> deleteAllAuth = AuthPermUtil.getDelAllAuth(AuthorizeSaveParam
                                    .builder().objectId(organizeEntity.getId()).objectType(PermissionConst.ORGANIZE).allOrgList(allOrgList).allPosList(allPosList).allAuthMap(allAuthMap)
                                    .roleRealationList(roleRealationList)
                                    .systemSave(authorizeEntities.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType()) && systemSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .moduleSave(authorizeEntities.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType()) && moduleSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .buttonSave(authorizeEntities.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType()) && buttonSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .columnSave(authorizeEntities.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType()) && columnSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .resourceSave(authorizeEntities.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType()) && resourceSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .formSave(authorizeEntities.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType()) && formSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .thisRole(objectId)
                                    .build());
                            listRes.addAll(deleteAllAuth);
                        }
                    } else {
                        PositionEntity positionEntity = allPosList.stream().filter(t -> item.getObjectId().equals(t.getId())).findFirst().orElse(null);
                        if (positionEntity != null) {
                            List<AuthorizeEntity> authorizeEntities = allAuthMap.get(positionEntity.getId()) == null ? Collections.emptyList() : allAuthMap.get(positionEntity.getId());
                            //当前不移除，移除子岗位的相关权限
                            List<String> deleteAllAuth = AuthPermUtil.getDelAllAuth(AuthorizeSaveParam
                                    .builder().objectId(positionEntity.getId()).objectType(PermissionConst.ORGANIZE).allOrgList(allOrgList).allPosList(allPosList).allAuthMap(allAuthMap)
                                    .roleRealationList(roleRealationList)
                                    .systemSave(authorizeEntities.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType()) && systemSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .moduleSave(authorizeEntities.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType()) && moduleSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .buttonSave(authorizeEntities.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType()) && buttonSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .columnSave(authorizeEntities.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType()) && columnSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .resourceSave(authorizeEntities.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType()) && resourceSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .formSave(authorizeEntities.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType()) && formSave.contains(t.getItemId()))
                                            .map(AuthorizeEntity::getItemId).collect(Collectors.toList()))
                                    .thisRole(objectId)
                                    .build());
                            listRes.addAll(deleteAllAuth);
                        }
                    }
                }
            }
        }
        return listRes;
    }

    /**
     * 个人权限
     *
     * @return
     */
    public UserAuthorizeVO getUserAuth(UserAuthForm param) {
        String userId = param.getUserId();
        if (StringUtil.isEmpty(param.getUserId())) {
            userId = UserProvider.getUser().getUserId();
        }
        UserEntity info = userService.getInfo(userId);
        if (info == null) return new UserAuthorizeVO();
        boolean isAdmin = Objects.equals(info.getIsAdministrator(), 1);
        boolean filter = isAdmin;
        List<AuthorizeEntity> list;
        if (PermissionConst.POSITION.equals(param.getObjectType()) || PermissionConst.ROLE.equals(param.getObjectType())) {
            list = authorizeService.getListByPosOrRoleId(param.getObjectId(), param.getObjectType());
            filter = false;
        } else {
            list = authorizeService.getListByUserId(false, userId, false);
        }
        List<String> systemIds = list.stream().filter(t -> AuthorizeConst.SYSTEM.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        List<String> moduleIds = list.stream().filter(t -> AuthorizeConst.MODULE.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());


        List<SystemEntity> systemList = filter ? systemApi.getList() : systemApi.getListByIds(systemIds, new ArrayList<>());
        List<AuthorizeModel> sysModelList = new ArrayList<>();
        Set<String> mainSys = new HashSet<>();
        for (SystemEntity s : systemList) {
            setSystem(s, mainSys, sysModelList);
        }
        List<ModuleEntity> moduleList = filter ? moduleApi.getList() : moduleApi.getModuleByIds(moduleIds);
        for (ModuleEntity me : moduleList) {
            if (StringUtil.isEmpty(me.getParentId()) || "-1".equals(me.getParentId())) {
                if (mainSys.contains(me.getSystemId())) {
                    me.setParentId(CodeConst.XTCD);
                } else {
                    if (JnpfConst.WEB.equals(me.getCategory())) {
                        me.setParentId(me.getSystemId() + JnpfConst.WEB);
                    } else {
                        me.setParentId(me.getSystemId() + JnpfConst.APP);
                    }
                }
            }
        }
        List<AuthorizeModel> moduleAuth = JsonUtil.getJsonToList(moduleList, AuthorizeModel.class);
        //获取菜单
        if (isAdmin) {
            moduleIds = adminGetAuth(moduleList, null, mainSys);
        }
        List<UserAuthorizeModel> module = getEachAuth(sysModelList, moduleAuth, moduleIds);
        List<AuthorizeModel> sysAndModule = new ArrayList<>();
        sysAndModule.addAll(sysModelList);
        sysAndModule.addAll(moduleAuth);

        //获取按钮
        List<UserAuthorizeModel> button = getButton(filter, list, isAdmin, moduleList, mainSys, sysAndModule);

        //获取列表
        List<UserAuthorizeModel> column = getColumn(filter, list, isAdmin, moduleList, mainSys, sysAndModule);

        //获取表单
        List<UserAuthorizeModel> form = getForm(filter, list, isAdmin, moduleList, mainSys, sysAndModule);

        //获取数据权限
        List<UserAuthorizeModel> dataScheme = getDataAuth(filter, list, isAdmin, moduleList, mainSys, sysAndModule);

        //全部应用
        List<SystemEntity> systemAll = systemApi.getList();
        List<AuthorizeModel> systemAuthAll = JsonUtil.getJsonToList(systemAll, AuthorizeModel.class);

        //获取流程权限
        List<UserAuthorizeModel> flow = getFlow(isAdmin, list, systemAuthAll);

        //获取打印权限
        List<UserAuthorizeModel> print = getPrint(filter, list, isAdmin, systemAuthAll);

        return UserAuthorizeVO.builder()
                .module(module)
                .button(button)
                .column(column)
                .form(form)
                .resource(dataScheme)
                .flow(flow)
                .print(print)
                .build();
    }

    private List<UserAuthorizeModel> getDataAuth(boolean filter, List<AuthorizeEntity> list, boolean isAdmin, List<ModuleEntity> moduleList, Set<String> mainSys, List<AuthorizeModel> sysAndModule) {
        List<String> dataIds = list.stream().filter(t -> AuthorizeConst.RESOURCE.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        List<ModuleDataAuthorizeSchemeEntity> dataEntityList = filter ? schemeApi.getList() : schemeApi.getListByIds(dataIds);
        List<AuthorizeModel> dataAuth = new ArrayList<>();
        for (ModuleDataAuthorizeSchemeEntity item : dataEntityList) {
            AuthorizeModel auth = JsonUtil.getJsonToBean(item, AuthorizeModel.class);
            auth.setParentId(item.getModuleId());
            dataAuth.add(auth);
        }
        if (isAdmin) {
            List<String> collect = dataEntityList.stream().map(ModuleDataAuthorizeSchemeEntity::getModuleId).collect(Collectors.toList());
            dataIds = adminGetAuth(moduleList, collect, mainSys);
        }
        return getEachAuth(sysAndModule, dataAuth, dataIds);
    }

    private List<UserAuthorizeModel> getButton(boolean filter, List<AuthorizeEntity> list, boolean isAdmin, List<ModuleEntity> moduleList, Set<String> mainSys, List<AuthorizeModel> sysAndModule) {
        List<String> buttonIds = list.stream().filter(t -> AuthorizeConst.BUTTON.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());

        List<ModuleButtonEntity> buttonEntityList = filter ? buttonApi.getList() : buttonApi.getListByIds(buttonIds);
        List<AuthorizeModel> buttonAuth = new ArrayList<>();
        for (ModuleButtonEntity item : buttonEntityList) {
            AuthorizeModel auth = JsonUtil.getJsonToBean(item, AuthorizeModel.class);
            auth.setParentId(item.getModuleId());
            buttonAuth.add(auth);
        }
        if (isAdmin) {
            List<String> collect = buttonEntityList.stream().map(ModuleButtonEntity::getModuleId).collect(Collectors.toList());
            buttonIds = adminGetAuth(moduleList, collect, mainSys);
        }
        return getEachAuth(sysAndModule, buttonAuth, buttonIds);
    }

    private List<UserAuthorizeModel> getColumn(boolean filter, List<AuthorizeEntity> list, boolean isAdmin, List<ModuleEntity> moduleList, Set<String> mainSys, List<AuthorizeModel> sysAndModule) {
        List<String> columnIds = list.stream().filter(t -> AuthorizeConst.COLUMN.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        List<ModuleColumnEntity> columnEntityList = filter ? columnApi.getList() : columnApi.getListByIds(columnIds);
        List<AuthorizeModel> columnAuth = new ArrayList<>();
        for (ModuleColumnEntity item : columnEntityList) {
            AuthorizeModel auth = JsonUtil.getJsonToBean(item, AuthorizeModel.class);
            auth.setParentId(item.getModuleId());
            columnAuth.add(auth);
        }
        if (isAdmin) {
            List<String> collect = columnEntityList.stream().map(ModuleColumnEntity::getModuleId).collect(Collectors.toList());
            columnIds = adminGetAuth(moduleList, collect, mainSys);
        }
        return getEachAuth(sysAndModule, columnAuth, columnIds);
    }

    private List<UserAuthorizeModel> getForm(boolean filter, List<AuthorizeEntity> list, boolean isAdmin, List<ModuleEntity> moduleList, Set<String> mainSys, List<AuthorizeModel> sysAndModule) {
        List<String> formIds = list.stream().filter(t -> AuthorizeConst.FROM.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        List<ModuleFormEntity> formEntityList = filter ? formApi.getList() : formApi.getListByIds(formIds);
        List<AuthorizeModel> formAuth = new ArrayList<>();
        for (ModuleFormEntity item : formEntityList) {
            AuthorizeModel auth = JsonUtil.getJsonToBean(item, AuthorizeModel.class);
            auth.setParentId(item.getModuleId());
            formAuth.add(auth);
        }
        if (isAdmin) {
            List<String> collect = formEntityList.stream().map(ModuleFormEntity::getModuleId).collect(Collectors.toList());
            formIds = adminGetAuth(moduleList, collect, mainSys);
        }
        return getEachAuth(sysAndModule, formAuth, formIds);
    }

    private static void setSystem(SystemEntity s, Set<String> mainSys, List<AuthorizeModel> sysModelList) {
        AuthorizeModel authorizeModel;
        if (Objects.equals(s.getIsMain(), 1)) {
            authorizeModel = new AuthorizeModel();
            authorizeModel.setId(CodeConst.XTCD);
            authorizeModel.setFullName("系统菜单");
            authorizeModel.setParentId("-1");
            authorizeModel.setSortCode(0l);
            mainSys.add(s.getId());
        } else {
            authorizeModel = JsonUtil.getJsonToBean(s, AuthorizeModel.class);
            AuthorizeModel web = new AuthorizeModel();
            web.setId(authorizeModel.getId() + JnpfConst.WEB);
            web.setFullName("WEB菜单");
            web.setParentId(authorizeModel.getId());
            web.setSortCode(0l);
            AuthorizeModel app = new AuthorizeModel();
            app.setId(authorizeModel.getId() + JnpfConst.APP);
            app.setFullName("APP菜单");
            app.setParentId(authorizeModel.getId());
            app.setSortCode(0l);
            sysModelList.add(web);
            sysModelList.add(app);
        }
        sysModelList.add(authorizeModel);
    }

    private List<UserAuthorizeModel> getFlow(boolean isAdmin, List<AuthorizeEntity> list, List<AuthorizeModel> systemAuthAll) {
        List<String> flowIds = list.stream().filter(t -> AuthorizeConst.FLOW.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        if (isAdmin) {
            List<TemplateTreeListVo> treeList = templateApi.treeListWithPower();
            List<String> idAll = new ArrayList<>();
            idList(treeList, idAll);
            flowIds = idAll;
        }
        List<TemplateEntity> flowEntityList = templateApi.getListByFlowIds(flowIds);
        List<AuthorizeModel> flowAuth = new ArrayList<>();
        List<String> flowIdsAdmin = new ArrayList<>();
        for (TemplateEntity item : flowEntityList) {
            AuthorizeModel auth = JsonUtil.getJsonToBean(item, AuthorizeModel.class);
            auth.setParentId(item.getSystemId());
            flowAuth.add(auth);
            flowIdsAdmin.add(item.getId());
            flowIdsAdmin.add(item.getSystemId());
        }
        if (isAdmin) {
            flowIds = flowIdsAdmin;
        }
        return getEachAuth(systemAuthAll, flowAuth, flowIds);
    }

    private List<UserAuthorizeModel> getPrint(boolean filter, List<AuthorizeEntity> list, boolean isAdmin, List<AuthorizeModel> systemAuthAll) {
        List<String> printIds = list.stream().filter(t -> AuthorizeConst.PRINT.equals(t.getItemType())).collect(Collectors.toList())
                .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
        PaginationPrint paginationPrint = new PaginationPrint();
        paginationPrint.setDataType(1);
        paginationPrint.setVisibleType(2);
        List<PrintDevEntity> printEntityList = filter ? printDevApi.getWorkSelector(paginationPrint) : printDevApi.getListByIds(printIds);
        List<AuthorizeModel> printAuth = new ArrayList<>();
        List<String> printIdsAdmin = new ArrayList<>();
        for (PrintDevEntity item : printEntityList) {
            AuthorizeModel auth = JsonUtil.getJsonToBean(item, AuthorizeModel.class);
            auth.setParentId(item.getSystemId());
            printAuth.add(auth);
            printIdsAdmin.add(item.getId());
            printIdsAdmin.add(item.getSystemId());
        }
        if (isAdmin) {
            printIds = printIdsAdmin;
        }
        return getEachAuth(systemAuthAll, printAuth, printIds);
    }

    private static List<UserAuthorizeModel> getEachAuth(List<AuthorizeModel> sysModelList, List<AuthorizeModel> moduleAuth, List<String> moduleIds) {
        List<AuthorizeModel> allAuth = new ArrayList<>();
        for (AuthorizeModel authorizeModel : sysModelList) {
            if (moduleIds.contains(authorizeModel.getId())) {
                allAuth.add(authorizeModel);
            }
        }
        allAuth.addAll(moduleAuth);

        List<SumTree<AuthorizeModel>> trees = TreeDotUtils.convertListToTreeDot(allAuth);
        List<UserAuthorizeModel> module = JsonUtil.getJsonToList(trees, UserAuthorizeModel.class);
        module = module.stream().filter(t -> CollUtil.isNotEmpty(t.getChildren())).collect(Collectors.toList());
        return module;
    }

    private void idList(List<TemplateTreeListVo> list, List<String> idAll) {
        for (TemplateTreeListVo vo : list) {
            idAll.add(vo.getId());
            vo.setDisabled(false);
            if (vo.getChildren() != null) {
                idList(vo.getChildren(), idAll);
            }
        }
    }

    private static List<String> adminGetAuth(List<ModuleEntity> moduleList, List<String> moduleIds, Set<String> mainSys) {
        Set<String> auth = new HashSet<>();
        List<ModuleEntity> moduleListResult;
        if (moduleIds != null) {
            List<String> finalModuleList = new ArrayList<>();
            //递归获取父级菜单
            getParent(moduleList, moduleIds, finalModuleList);
            //有权限的菜单
            finalModuleList.addAll(moduleIds);
            moduleListResult = moduleList.stream().filter(t -> finalModuleList.contains(t.getId())).collect(Collectors.toList());
        } else {
            moduleListResult = moduleList;
        }
        for (ModuleEntity item : moduleListResult) {
            auth.add(item.getId());
            if (mainSys.contains(item.getSystemId())) {
                auth.add(CodeConst.XTCD);
            } else {
                auth.add(item.getSystemId());
                if (JnpfConst.WEB.equals(item.getCategory())) {
                    auth.add(item.getSystemId() + "2");
                } else {
                    auth.add(item.getSystemId() + "1");
                }
            }
        }
        return new ArrayList<>(auth);
    }

    /**
     * 递归获取父级菜单
     *
     * @param list
     * @param child
     * @param parent
     */
    private static void getParent(List<ModuleEntity> list, List<String> child, List<String> parent) {
        List<ModuleEntity> childList = list.stream().filter(t -> child.contains(t.getId())).collect(Collectors.toList());
        List<String> parenIds = childList.stream().map(ModuleEntity::getParentId).collect(Collectors.toList());
        List<ModuleEntity> parentList = list.stream().filter(t -> parenIds.contains(t.getId())).collect(Collectors.toList());
        List<String> newParent = parentList.stream().map(ModuleEntity::getId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(parentList)) {
            parent.addAll(newParent);
            getParent(list, newParent, parent);
        }
    }
}
