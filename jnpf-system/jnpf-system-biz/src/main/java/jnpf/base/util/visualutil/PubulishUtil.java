package jnpf.base.util.visualutil;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ImmutableMap;
import jnpf.base.UserInfo;
import jnpf.base.entity.*;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.model.online.AuthFlieds;
import jnpf.base.model.online.PerColModels;
import jnpf.base.model.online.VisualMenuModel;
import jnpf.base.service.*;
import jnpf.constant.JnpfConst;
import jnpf.exception.WorkFlowException;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 功能发布
 *
 * @author JNPF开发平台组
 * @version V3.4
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/4/7
 */
@Component
@RequiredArgsConstructor
public class PubulishUtil {

    
    private final ModuleService moduleService;
    
    private final  ModuleButtonService moduleButtonService;
    
    private final  ModuleColumnService moduleColumnService;
    
    private final  ModuleFormService moduleFormService;
    
    private final  ModuleDataAuthorizeService moduleDataAuthorizeService;
    
    private final  ModuleDataAuthorizeSchemeService moduleDataAuthorizeSchemeService;
    
    private final  SystemService systemService;



    /**
     * pc父级菜单 默认
     */
    private static final String PC_CATE = "功能示例";


    /**
     * app父级菜单 默认
     */
    private static final String APP_CATE = "移动应用";

    /**
     * pc端分类
     */
    private static final String PC_CATEGORY = JnpfConst.WEB;

    /**
     * app端分类
     */
    private static final String APP_CATEGORY = JnpfConst.APP;



    /**
     * 图标
     */
    private static final String ICON = "icon-ym icon-ym-webForm";


    private static final Map<Integer, String> pcAddress = ImmutableMap.of(3, "model", 8, "portal", 10, "report", 11, "form");

    public void publishMenu(VisualMenuModel visualMenuModel) throws WorkFlowException {
        UserInfo userInfo = UserProvider.getUser();

        List<ModuleEntity> moduleList = moduleService.getModuleList(visualMenuModel.getId());

        ModuleEntity moduleEntity = new ModuleEntity();
        PerColModels pcPerCols = visualMenuModel.getPcPerCols() != null ? visualMenuModel.getPcPerCols() : new PerColModels();
        PerColModels appPerCols = visualMenuModel.getAppPerCols() != null ? visualMenuModel.getAppPerCols() : new PerColModels();

        moduleEntity.setCategory(PC_CATEGORY);

        moduleEntity.setFullName(visualMenuModel.getFullName());
        moduleEntity.setEnCode(visualMenuModel.getEnCode());
        moduleEntity.setIcon(ICON);
        moduleEntity.setType(visualMenuModel.getType());
        moduleEntity.setModuleId(visualMenuModel.getId());
        PropertyJsonModel jsonModel = new PropertyJsonModel();
        jsonModel.setModuleId(visualMenuModel.getId());
        jsonModel.setIconBackgroundColor("");
        jsonModel.setIsTree(0);
        jsonModel.setWebType(visualMenuModel.getWebType());
        moduleEntity.setPropertyJson(JsonUtil.getObjectToString(jsonModel));
        moduleEntity.setSortCode((999L));
        moduleEntity.setEnabledMark(1);
        moduleEntity.setCreatorTime(DateUtil.getNowDate());
        moduleEntity.setCreatorUserId(userInfo.getUserId());
        String address = pcAddress.get(visualMenuModel.getType());
        moduleEntity.setUrlAddress(address + "/" + visualMenuModel.getEnCode());
        if (Objects.equals(visualMenuModel.getType(), 11)) {
            moduleEntity.setPageAddress(visualMenuModel.getWebAddress());
        }

        if (1 == visualMenuModel.getPc()) {
            List<Integer> pcAuth = visualMenuModel.getPcAuth();
            if (pcAuth == null || pcAuth.isEmpty() || pcAuth.size() != 4) {
                pcAuth = Arrays.asList(0, 0, 0, 0);
            }
            moduleEntity.setIsButtonAuthorize(pcAuth.get(0));
            moduleEntity.setIsColumnAuthorize(pcAuth.get(1));
            moduleEntity.setIsDataAuthorize(pcAuth.get(2));
            moduleEntity.setIsFormAuthorize(pcAuth.get(3));
            List<ModuleEntity> pcModuleList = moduleList.stream().filter(module -> PC_CATEGORY.equals(module.getCategory())).collect(Collectors.toList());
            //是否生成过菜单
            if (!pcModuleList.isEmpty()) {
                for (ModuleEntity entity : pcModuleList) {
                    //变更权限
                    if (pcPerCols != null) alterPer(entity, pcPerCols);
                    //更新菜单
                    entity.setPropertyJson(JsonUtil.getObjectToString(jsonModel));
                    entity.setIsButtonAuthorize(pcAuth.get(0));
                    entity.setIsColumnAuthorize(pcAuth.get(1));
                    entity.setIsDataAuthorize(pcAuth.get(2));
                    entity.setIsFormAuthorize(pcAuth.get(3));
                    entity.setUrlAddress(moduleEntity.getUrlAddress());
                    entity.setPageAddress(moduleEntity.getPageAddress());
                    moduleService.update(entity.getId(), entity);
                }
            }

            this.createMenu(moduleEntity, visualMenuModel, pcPerCols, true);
        }
        Map<Integer, String> appAddress = ImmutableMap.of(3, "dynamicModel", 8, "visualPortal", 11, "form");
        moduleEntity.setCategory(APP_CATEGORY);
        String portalAddress = appAddress.get(visualMenuModel.getType());
        String urlAddress = StringUtil.isNotEmpty(portalAddress) ? "/pages/apply/" + portalAddress + "/index?id=" + visualMenuModel.getId() : visualMenuModel.getEnCode();
        moduleEntity.setUrlAddress(urlAddress);
        if (Objects.equals(visualMenuModel.getType(), 11)) {
            moduleEntity.setUrlAddress(visualMenuModel.getAppAddress());
            moduleEntity.setPageAddress(visualMenuModel.getAppAddress());
        }
        if (1 == visualMenuModel.getApp()) {
            List<Integer> appAuth = visualMenuModel.getAppAuth();
            if (appAuth == null || appAuth.isEmpty() || appAuth.size() != 4) {
                appAuth = Arrays.asList(0, 0, 0, 0);
            }
            moduleEntity.setIsButtonAuthorize(appAuth.get(0));
            moduleEntity.setIsColumnAuthorize(appAuth.get(1));
            moduleEntity.setIsDataAuthorize(appAuth.get(2));
            moduleEntity.setIsFormAuthorize(appAuth.get(3));
            List<ModuleEntity> appModuleList = moduleList.stream().filter(module -> APP_CATEGORY.equals(module.getCategory())).collect(Collectors.toList());
            if (!appModuleList.isEmpty()) {
                for (ModuleEntity entity : appModuleList) {
                    //变更权限
                    if (appPerCols != null) alterPer(entity, appPerCols);
                    //更新菜单
                    entity.setPropertyJson(JsonUtil.getObjectToString(jsonModel));
                    entity.setIsButtonAuthorize(appAuth.get(0));
                    entity.setIsColumnAuthorize(appAuth.get(1));
                    entity.setIsDataAuthorize(appAuth.get(2));
                    entity.setIsFormAuthorize(appAuth.get(3));
                    entity.setUrlAddress(moduleEntity.getUrlAddress());
                    entity.setPageAddress(moduleEntity.getPageAddress());
                    moduleService.update(entity.getId(), entity);
                }
            }

            this.createMenu(moduleEntity, visualMenuModel, appPerCols, false);
        }
    }

    /**
     * 创建菜单验证
     *
     * @param moduleEntity
     * @return
     */
    private void createMenu(ModuleEntity moduleEntity, VisualMenuModel visualMenuModel, PerColModels perCols, Boolean isPc) throws WorkFlowException {
        List<ModuleEntity> list = new ArrayList<>();
        List<String> parentIds;
        if (Boolean.TRUE.equals(isPc)) {
            parentIds = visualMenuModel.getPcModuleParentId();
        } else {
            parentIds = visualMenuModel.getAppModuleParentId();
        }
        //创建菜单
        String fullName = moduleEntity.getFullName();
        String enCode = moduleEntity.getEnCode();
        List<ModuleEntity> moduleList = moduleService.getModuleList(visualMenuModel.getId());
        if (CollUtil.isEmpty(parentIds) && CollUtil.isEmpty(moduleList)) {
            throw new WorkFlowException("上级不能为空");
        }
        for (String pid : parentIds) {
            ModuleEntity saveEnt = BeanUtil.copyProperties(moduleEntity, ModuleEntity.class);
            saveEnt.setId(RandomUtil.uuId());
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            ModuleEntity pInfo = moduleService.getInfo(pid);
            //查询不到菜单说明上级是系统
            if (pInfo != null) {
                saveEnt.setParentId(pid);
                saveEnt.setSystemId(pInfo.getSystemId());
            } else {
                SystemEntity info = systemService.getInfo(pid);
                if (info == null) {
                    throw new WorkFlowException("找不到该上级");
                }
                saveEnt.setParentId("-1");
                saveEnt.setSystemId(pid);
            }
            saveEnt.setFullName(fullName);
            saveEnt.setEnCode(enCode + copyNum);
            if (Boolean.TRUE.equals(isPc)) {
                String address = pcAddress.get(visualMenuModel.getType());
                saveEnt.setUrlAddress(address + "/" + saveEnt.getEnCode());
            }
            if (moduleService.isExistByFullName(saveEnt, saveEnt.getCategory(), saveEnt.getSystemId())) {
                throw new WorkFlowException("名称重复");
            }
            if (moduleService.isExistByEnCode(saveEnt, saveEnt.getCategory(), saveEnt.getSystemId())) {
                throw new WorkFlowException("编码重复");
            }
            if (perCols != null) batchCreatePermissions(perCols, saveEnt.getId());
            list.add(saveEnt);
        }
        moduleService.saveBatch(list);
    }

    private void batchCreatePermissions(PerColModels perColModels, String moduleId) {

        List<AuthFlieds> buttonPermission = Objects.nonNull(perColModels.getButtonPermission()) ? perColModels.getButtonPermission() : new ArrayList<>();
        List<AuthFlieds> formPermission = Objects.nonNull(perColModels.getFormPermission()) ? perColModels.getFormPermission() : new ArrayList<>();
        List<AuthFlieds> listPermission = Objects.nonNull(perColModels.getListPermission()) ? perColModels.getListPermission() : new ArrayList<>();
        List<ModuleDataAuthorizeSchemeEntity> dataPermissionScheme = Objects.nonNull(perColModels.getDataPermissionScheme()) ? perColModels.getDataPermissionScheme() : new ArrayList<>();

        //按钮
        List<ModuleButtonEntity> buttonEntities = buttonPermission.stream().map(button -> {
            ModuleButtonEntity buttonEntity = new ModuleButtonEntity();
            buttonEntity.setEnabledMark(Boolean.TRUE.equals(button.getStatus()) ? 1 : 0);
            buttonEntity.setEnCode(button.getEnCode());
            buttonEntity.setFullName(button.getFullName());
            buttonEntity.setParentId("-1");
            buttonEntity.setModuleId(moduleId);
            buttonEntity.setSortCode(0L);
            return buttonEntity;
        }).collect(Collectors.toList());

        //表单权限
        List<ModuleFormEntity> moduleFormEntities = formPermission.stream().map(form -> {
            ModuleFormEntity formEntity = new ModuleFormEntity();
            formEntity.setBindTable(form.getBindTableName());
            formEntity.setEnabledMark(Boolean.TRUE.equals(form.getStatus()) ? 1 : 0);
            formEntity.setEnCode(form.getEnCode());
            formEntity.setFullName(form.getFullName());
            formEntity.setParentId("-1");
            formEntity.setModuleId(moduleId);
            formEntity.setFieldRule(form.getRule());
            formEntity.setChildTableKey(form.getChildTableKey());
            formEntity.setSortCode(0L);
            return formEntity;
        }).collect(Collectors.toList());

        //列表
        List<ModuleColumnEntity> moduleColumnEntities = listPermission.stream().map(list -> {
            ModuleColumnEntity moduleColumnEntity = new ModuleColumnEntity();
            moduleColumnEntity.setBindTable(list.getBindTableName());
            moduleColumnEntity.setEnabledMark(Boolean.TRUE.equals(list.getStatus()) ? 1 : 0);
            moduleColumnEntity.setEnCode(list.getEnCode());
            moduleColumnEntity.setFullName(list.getFullName());
            moduleColumnEntity.setParentId("-1");
            moduleColumnEntity.setModuleId(moduleId);
            moduleColumnEntity.setSortCode(0L);
            moduleColumnEntity.setChildTableKey(list.getChildTableKey());
            moduleColumnEntity.setFieldRule(list.getRule());
            return moduleColumnEntity;
        }).collect(Collectors.toList());

        for (ModuleButtonEntity btn : buttonEntities) {
            moduleButtonService.create(btn);
        }
        for (ModuleFormEntity formEntity : moduleFormEntities) {
            moduleFormService.create(formEntity);
        }
        for (ModuleColumnEntity moduleColumnEntity : moduleColumnEntities) {
            moduleColumnService.create(moduleColumnEntity);
        }

        //方案
        for (ModuleDataAuthorizeSchemeEntity moduleDataAuthorizeEntity : dataPermissionScheme) {
            moduleDataAuthorizeEntity.setId(RandomUtil.uuId());
            moduleDataAuthorizeEntity.setModuleId(moduleId);
            moduleDataAuthorizeSchemeService.save(moduleDataAuthorizeEntity);
        }

        //创建全部数据方案
        if (Objects.nonNull(perColModels.getDataPermissionScheme())) {
            Boolean exist = moduleDataAuthorizeSchemeService.isExistAllData(moduleId);
            if (Boolean.FALSE.equals(exist)) {
                ModuleDataAuthorizeSchemeEntity moduleDataAuthorizeSchemeEntity = new ModuleDataAuthorizeSchemeEntity();
                moduleDataAuthorizeSchemeEntity.setFullName("全部数据");
                moduleDataAuthorizeSchemeEntity.setEnCode("jnpf_alldata");
                moduleDataAuthorizeSchemeEntity.setAllData(1);
                moduleDataAuthorizeSchemeEntity.setModuleId(moduleId);
                moduleDataAuthorizeSchemeService.create(moduleDataAuthorizeSchemeEntity);
            }
        }
    }



    /**
     * 自动变更权限
     *
     * @param entity
     * @param perColModelSour
     * @return
     */
    private void alterPer(ModuleEntity entity, PerColModels perColModelSour) {
        String moduleMainId = entity.getId();
        PerColModels perColModel = BeanUtil.copyProperties(perColModelSour, PerColModels.class);
        PerColModels perColModels = new PerColModels();
        //列表
        if (perColModel.getListPermission() != null) {
            Map<String, String> colMap = new HashMap<>();
            List<ModuleColumnEntity> columnEntities = moduleColumnService.getList(moduleMainId);
            columnEntities.stream().forEach(col -> colMap.put(col.getEnCode(), col.getId()));
            List<AuthFlieds> listPermission = perColModel.getListPermission() != null ? perColModel.getListPermission() : new ArrayList<>();

            //只变更状态
            List<AuthFlieds> authColList = intersectList1(listPermission, colMap);
            Map<String, Boolean> stateMap = new HashMap<>();
            authColList.forEach(auth -> stateMap.put(auth.getEnCode(), auth.getStatus()));
            for (ModuleColumnEntity columnEntity : columnEntities) {
                if (Objects.nonNull(stateMap.get(columnEntity.getEnCode()))) {
                    columnEntity.setEnabledMark(Boolean.TRUE.equals(stateMap.get(columnEntity.getEnCode())) ? 1 : 0);
                    moduleColumnService.update(columnEntity.getId(), columnEntity);
                }
            }
            //新增
            List<AuthFlieds> authColCreList = intersectList2(listPermission, authColList);
            perColModels.setListPermission(authColCreList);
        }

        //表单
        if (perColModel.getFormPermission() != null) {
            Map<String, String> formMap = new HashMap<>();
            List<ModuleFormEntity> formEntities = moduleFormService.getList(moduleMainId);
            formEntities.stream().forEach(form -> formMap.put(form.getEnCode(), form.getId()));
            List<AuthFlieds> formPermission = perColModel.getFormPermission() != null ? perColModel.getFormPermission() : new ArrayList<>();
            List<AuthFlieds> authFormList = intersectList1(formPermission, formMap);

            Map<String, Boolean> stateFMap = new HashMap<>();
            authFormList.forEach(auth -> stateFMap.put(auth.getEnCode(), auth.getStatus()));
            for (ModuleFormEntity formEntity : formEntities) {
                if (Objects.nonNull(stateFMap.get(formEntity.getEnCode()))) {
                    formEntity.setEnabledMark(Boolean.TRUE.equals(stateFMap.get(formEntity.getEnCode())) ? 1 : 0);
                    moduleFormService.update(formEntity.getId(), formEntity);
                }
            }

            List<AuthFlieds> authFormCreList = intersectList2(formPermission, authFormList);
            perColModels.setFormPermission(authFormCreList);
        }
        //按钮权限
        if (perColModel.getButtonPermission() != null) {
            Map<String, String> btnMap = new HashMap<>();
            List<ModuleButtonEntity> buttonEntities = moduleButtonService.getListByModuleIds(moduleMainId);
            buttonEntities.stream().forEach(btn -> btnMap.put(btn.getEnCode(), btn.getId()));
            List<AuthFlieds> buttonPermission = perColModel.getButtonPermission() != null ? perColModel.getButtonPermission() : new ArrayList<>();
            List<AuthFlieds> authBtnList = intersectList1(buttonPermission, btnMap);
            Map<String, Boolean> stateBMap = new HashMap<>();

            authBtnList.forEach(auth -> stateBMap.put(auth.getEnCode(), auth.getStatus()));
            for (ModuleButtonEntity btnEntity : buttonEntities) {
                if (Objects.nonNull(stateBMap.get(btnEntity.getEnCode()))) {
                    btnEntity.setEnabledMark(Boolean.TRUE.equals(stateBMap.get(btnEntity.getEnCode())) ? 1 : 0);
                    moduleButtonService.update(btnEntity.getId(), btnEntity);
                }
            }

            List<AuthFlieds> authBtnCreList = intersectList2(buttonPermission, authBtnList);
            perColModels.setButtonPermission(authBtnCreList);
        }

        //表单权限方案
        if (perColModel.getDataPermissionScheme() != null) {
            //交集
            List<ModuleDataAuthorizeSchemeEntity> togetherList = new ArrayList<>();
            List<ModuleDataAuthorizeSchemeEntity> dataAuthorizeSchemeList = moduleDataAuthorizeSchemeService.getList(moduleMainId);
            List<ModuleDataAuthorizeSchemeEntity> dataPermissionScheme = perColModel.getDataPermissionScheme();
            List<ModuleDataAuthorizeSchemeEntity> dataPermissionSchemeClone = new ArrayList<>(dataPermissionScheme);

            for (ModuleDataAuthorizeSchemeEntity authFlieds : dataPermissionScheme) {
                for (ModuleDataAuthorizeSchemeEntity schemeEntity : dataAuthorizeSchemeList) {
                    if (schemeEntity.getConditionJson() == null) {
                        continue;
                    }
                    if (Objects.equals(-9527l, schemeEntity.getSortCode()) && schemeEntity.getConditionText().contains(authFlieds.getConditionText())) {
                        togetherList.add(authFlieds);
                    }
                }
            }

            //需要新增 dataPermissionScheme
            dataPermissionScheme.removeAll(togetherList);

            //需要删除
            List<String> collect = dataPermissionSchemeClone.stream().map(ModuleDataAuthorizeSchemeEntity::getDescription).collect(Collectors.toList());
            List<ModuleDataAuthorizeSchemeEntity> deleteSchemeList = new ArrayList<>();
            if (!dataAuthorizeSchemeList.isEmpty()) {
                for (ModuleDataAuthorizeSchemeEntity item : dataAuthorizeSchemeList) {
                    if (!collect.contains(item.getDescription()) && Objects.equals(-9527l, item.getSortCode())) {
                        //需要移除的权限方案
                        deleteSchemeList.add(item);
                    }
                }
            }

            for (ModuleDataAuthorizeSchemeEntity scheme : deleteSchemeList) {
                moduleDataAuthorizeSchemeService.delete(scheme);
            }
            perColModels.setDataPermissionScheme(new ArrayList<>(dataPermissionScheme));
        }

        //新增的权限
        batchCreatePermissions(perColModels, moduleMainId);

    }


    /**
     * 取交集 （不需要变动的数据）
     *
     * @param authFlieds  新提交过来的
     * @param databaseMap 数据库存在数据
     * @return
     */
    private List<AuthFlieds> intersectList1(List<AuthFlieds> authFlieds, Map<String, String> databaseMap) {
        List<AuthFlieds> lastList = new LinkedList<>();
        for (AuthFlieds authFlied : authFlieds) {
            if (databaseMap.containsKey(authFlied.getEnCode())) {
                lastList.add(authFlied);
            }
        }
        return lastList;
    }

    /**
     * 求差集 （需要新增的数据） create
     *
     * @param auth1 新提交过来的
     * @param auth2 与数据库的交集
     * @return
     */
    private List<AuthFlieds> intersectList2(List<AuthFlieds> auth1, List<AuthFlieds> auth2) {
        auth1.removeAll(auth2);
        return auth1;
    }








}

