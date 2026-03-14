package jnpf.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import jnpf.base.UserInfo;
import jnpf.base.model.module.ModuleModel;
import jnpf.constant.EventConst;
import jnpf.event.ProjectEventListener;
import jnpf.module.ProjectEventInstance;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.RoleService;
import jnpf.permissions.PermissionInterfaceImpl;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static jnpf.util.Constants.ADMIN_KEY;

/**
 * 接口权限初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterfacePermissionListener {


    private final RoleService roleApi;

    private final LockTemplate lockTemplate;

    private final WorkFlowApi workFlowApi;

    @ProjectEventListener(channel = EventConst.EVENT_INIT_LOGIN_PERMISSION)
    public void initPermission(ProjectEventInstance eventInstance) {
        UserInfo userInfo = UserProvider.getUser();
        //本地事件
        AuthorizeVO authorizeVO = (AuthorizeVO) eventInstance.getSource();
        initSecurityAuthorities(authorizeVO, userInfo);
    }


    /**
     * 初始化接口鉴权用的账号权限
     * 本接口插入权限缓存， SaInterfaceImpl中框架鉴权时动态调用获取权限列表
     *
     * @param authorizeModel
     * @param userInfo
     */
    private void initSecurityAuthorities(AuthorizeVO authorizeModel, UserInfo userInfo) {
        if (authorizeModel.getCurrentSystem() == null) return;
        String sysId = authorizeModel.getCurrentSystem().getId();
        if (Objects.equals(1, userInfo.getIsBackend())) {
            sysId += "_backend";
        }
        //接口权限
        Set<String> authorityList = new HashSet<>();
        Set<String> roleAuthorityList = new HashSet<>();
        Map<String, ModuleModel> moduleModelMap = new LinkedHashMap<>();
        Map<String, String> flowFormMap = new HashMap<>();
        Map<String, String> allFlowFormMap = workFlowApi.getFlowFormMap();

        Map<String, List<Map<String, Object>>> columnsMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> formMap = new HashMap<>();
        for (ModuleModel moduleModel : authorizeModel.getModuleList()) {
            // 添加菜单权限
            // 添加菜单ID, 代码生成用
            authorityList.add(moduleModel.getId());
            // 添加菜单编码
            authorityList.add(moduleModel.getEnCode());
            moduleModelMap.put(moduleModel.getId(), moduleModel);
            //功能菜单 3：功能菜单, 9: 流程菜单, 4: 数据字典, 10: 报表
            if (moduleModel.getType() == 3 || moduleModel.getType() == 9
                    || moduleModel.getType() == 10 || moduleModel.getType() == 4) {
                JSONObject propertyJSON = JSON.parseObject(Optional.of(moduleModel.getPropertyJson()).orElse("{}"));
                String moduleId = propertyJSON.getString("moduleId");
                if (!StringUtil.isEmpty(moduleId)) {
                    authorityList.add(moduleId);
                    // 流程菜单 拥有流程菜单权限, 直接赋予流程下第一个表单的权限
                    if (moduleModel.getType() == 9) {
                        // 表单编码
                        String formId = flowFormMap.get(moduleId);
                        if (formId == null) {
                            formId = allFlowFormMap.get(moduleId);
                        }
                        if (StringUtil.isNotEmpty(formId)) {
                            flowFormMap.put(moduleId, formId);
                            authorityList.add(formId);
                        } else {
                            log.error("初始化流程菜单权限失败, 流程表单不存在：{}", moduleModel.getFullName());
                        }
                    }
                }
            }
        }
        //按钮权限 菜单编码::按钮编码
        authorizeModel.getButtonList().forEach(t -> {
            ModuleModel m = moduleModelMap.get(t.getModuleId());
            if (m != null) {
                authorityList.add(m.getEnCode() + "::" + t.getEnCode());
                //功能菜单的按钮权限 3：功能菜单, 9: 流程菜单
                if (m.getType() == 3 || m.getType() == 9) {
                    JSONObject propertyJSON = JSON.parseObject(Optional.of(m.getPropertyJson()).orElse("{}"));

                    String moduleId = propertyJSON.getString("moduleId");
                    if (!StringUtil.isEmpty(moduleId)) {
                        authorityList.add(moduleId + "::" + t.getEnCode());
                        // 流程菜单, 直接赋予流程下第一个表单的对应流程按钮权限
                        if (m.getType() == 9) {
                            // 表单编码
                            String formId = flowFormMap.get(moduleId);
                            if (StringUtil.isNotEmpty(formId)) {
                                authorityList.add(formId + "::" + t.getEnCode());
                            }
                        }
                    }
                }
            }
        });
        //列表权限 菜单编码::列表编码
        authorizeModel.getColumnList().forEach(t -> {
            ModuleModel m = moduleModelMap.get(t.getModuleId());
            if (m != null) {
                authorityList.add(m.getEnCode() + "::" + t.getEnCode());
            }
            List<Map<String, Object>> list;
            if (columnsMap.get(t.getModuleId()) == null) {
                list = new ArrayList<>();
                list.add(JsonUtil.entityToMap(t));
                columnsMap.put(t.getModuleId(), list);
            } else {
                list = columnsMap.get(t.getModuleId());
                list.add(JsonUtil.entityToMap(t));
                columnsMap.put(t.getModuleId(), list);
            }
        });
        //表单权限 菜单编码::表单编码
        authorizeModel.getFormsList().forEach(t -> {
            ModuleModel m = moduleModelMap.get(t.getModuleId());
            if (m != null) {
                authorityList.add(m.getEnCode() + "::" + t.getEnCode());
            }
            List<Map<String, Object>> list;
            if (formMap.get(t.getModuleId()) == null) {
                list = new ArrayList<>();
                list.add(JsonUtil.entityToMap(t));
                formMap.put(t.getModuleId(), list);
            } else {
                list = formMap.get(t.getModuleId());
                list.add(JsonUtil.entityToMap(t));
                formMap.put(t.getModuleId(), list);
            }
        });

        if (userInfo.getRoleIds() != null && !userInfo.getRoleIds().isEmpty() || Boolean.TRUE.equals(userInfo.getIsAdministrator())) {
            List<RoleEntity> roles;
            if (Boolean.TRUE.equals(userInfo.getIsAdministrator())) {
                roles = roleApi.getList(false, null, null);
            } else {
                roles = roleApi.getListByIds(userInfo.getRoleIds(), null, false);
            }
            roleAuthorityList = roles.stream().filter(r -> r.getEnabledMark().equals(1)).map(r -> "ROLE_" + r.getEnCode()).collect(Collectors.toSet());

        }

        //管理员都是用同一个缓存, 普通账号使用账号名,
        //权限列表：authorize_:租户_authorize_authorize_(admin|账号)
        //角色列表：authorize_:租户_authorize_role_(admin|账号)
        String account = Boolean.TRUE.equals(userInfo.getIsAdministrator()) ? ADMIN_KEY : userInfo.getUserId();
        try {
            if (!authorityList.isEmpty() || !roleAuthorityList.isEmpty()) {
                // 等待其他服务初始化权限完毕
                PermissionInterfaceImpl.setUserAuth(account, sysId, authorityList, roleAuthorityList, null);
                PermissionInterfaceImpl.setMap(account, sysId, columnsMap, formMap);

            }
        } catch (Exception e) {
            log.error("初始化接口权限失败", e);
        }

    }
}
