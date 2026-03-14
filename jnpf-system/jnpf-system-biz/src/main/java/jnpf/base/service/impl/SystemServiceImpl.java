package jnpf.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.mapper.ModuleMapper;
import jnpf.base.mapper.SystemMapper;
import jnpf.base.model.AppAuthorizationModel;
import jnpf.base.model.base.SystemPageVO;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SystemService;
import jnpf.constant.CodeConst;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.exception.DataException;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.user.WorkHandoverModel;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.RoleService;
import jnpf.permission.service.UserService;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 系统
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class SystemServiceImpl extends SuperServiceImpl<SystemMapper, SystemEntity> implements SystemService {

    private final CodeNumService codeNumService;

    private final RoleService roleApi;

    private final UserService userApi;


    private final ModuleMapper moduleMapper;

    @Override
    public List<SystemEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<SystemEntity> getList(String keyword, boolean verifyAuth, boolean filterMain) {
        SystemPageVO pageVO = new SystemPageVO();
        pageVO.setKeyword(keyword);
        if (!verifyAuth) {
            pageVO.setType(null);
        } else {
            pageVO.setType(1);
        }
        pageVO.setFilterMain(filterMain);
        return this.baseMapper.getList(pageVO);
    }

    @Override
    public List<SystemEntity> getList(SystemPageVO pageVO) {
        return this.baseMapper.getList(pageVO);
    }

    @Override
    public List<SystemEntity> getListByIdsKey(List<String> ids, String keyword) {
        return this.baseMapper.getListByIdsKey(ids, keyword);
    }

    @Override
    public SystemEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public Boolean isExistFullName(String id, String fullName) {
        return this.baseMapper.isExistFullName(id, fullName);
    }

    @Override
    public Boolean isExistEnCode(String id, String enCode) {
        return this.baseMapper.isExistEnCode(id, enCode);
    }

    @Override
    @DSTransactional
    public Boolean create(SystemEntity entity) {
        setAutoEnCode(entity);
        this.baseMapper.create(entity);
        //创建审批中心菜单
        this.createWorkMenu(entity.getId());
        return true;
    }

    private void createWorkMenu(String systemId) {
        SystemEntity workFlowSys = this.baseMapper.getInfoByEnCode(JnpfConst.WORK_FLOW_CODE);
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(ModuleEntity::getEnCode, JnpfConst.MODULE_CODE);
        queryWrapper.lambda().eq(ModuleEntity::getSystemId, workFlowSys.getId());
        List<ModuleEntity> list = moduleMapper.selectList(queryWrapper);
        String parentId;
        //审批中心 目录id
        String workId = RandomUtil.uuId();
        for (ModuleEntity item : list) {
            String id = RandomUtil.uuId();
            ModuleEntity moduleEntity = BeanUtil.copyProperties(item, ModuleEntity.class);
            if (JnpfConst.WORK_FLOW_CODE.equals(moduleEntity.getEnCode())) {
                parentId = "-1";
                id = workId;
                moduleEntity.setSortCode(0l);
            } else {
                parentId = workId;
            }
            moduleEntity.setId(id);
            moduleEntity.setSystemId(systemId);
            moduleEntity.setParentId(parentId);
            moduleMapper.insert(moduleEntity);
        }
        String appParentId;
        //APP审批中心 目录id
        String appWorkId = RandomUtil.uuId();
        for (ModuleEntity item : list) {
            if (JnpfConst.WORK_FLOWQUICKLAUNCH.equalsIgnoreCase(item.getEnCode())) continue;
            String appId = RandomUtil.uuId();
            ModuleEntity moduleEntity = BeanUtil.copyProperties(item, ModuleEntity.class);
            if (JnpfConst.WORK_FLOW_CODE.equals(moduleEntity.getEnCode())) {
                appParentId = "-1";
                appId = appWorkId;
                moduleEntity.setSortCode(0l);
            } else {
                appParentId = appWorkId;
            }
            moduleEntity.setId(appId);
            moduleEntity.setCategory(JnpfConst.APP);
            moduleEntity.setSystemId(systemId);
            moduleEntity.setParentId(appParentId);
            moduleMapper.insert(moduleEntity);
        }
    }

    @Override
    @DSTransactional
    public Boolean update(String id, SystemEntity entity) {
        entity.setId(id);
        setAutoEnCode(entity);
        return this.baseMapper.update(id, entity);
    }

    @Override
    @DSTransactional
    public Boolean delete(String id) {
        moduleMapper.deleteBySystemId(id);
        return this.removeById(id);
    }

    @Override
    public List<SystemEntity> getListByIds(List<String> list, List<String> moduleAuthorize) {
        return this.baseMapper.getListByIds(list, moduleAuthorize);
    }

    @Override
    public SystemEntity getInfoByEnCode(String enCode) {
        return this.baseMapper.getInfoByEnCode(enCode);
    }

    @Override
    public List<SystemEntity> findSystemAdmin(List<String> moduleAuthorize) {
        return this.baseMapper.findSystemAdmin(moduleAuthorize);
    }

    @Override
    public boolean saveSystemAuthorizion(AppAuthorizationModel model) {
        return this.baseMapper.saveSystemAuthorizion(model);
    }

    @Override
    public List<SystemEntity> getAuthListByUser(String userId, Boolean isStand) {
        UserEntity user = userApi.getInfo(userId);
        List<RoleEntity> userRoles = roleApi.getUserRoles(userId);
        boolean isDevRole = userRoles.stream().anyMatch(t -> PermissionConst.DEVELOPER_CODE.equals(t.getEnCode()));
        if (Boolean.TRUE.equals(isStand)) {
            isDevRole = UserProvider.getUser().getIsDevRole();
        }
        boolean isAdmin = Objects.equals(user.getIsAdministrator(), 1);

        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        //开发人员才有编辑权限
        if (isDevRole || isAdmin) {
            //判断权限列表
            if (!isAdmin) {
                queryWrapper.lambda().eq(SystemEntity::getUserId, userId).or();
                queryWrapper.lambda().like(SystemEntity::getAuthorizeId, userId).or();
                queryWrapper.lambda().eq(SystemEntity::getAuthorizeId, PermissionConst.ALL_DEV_USER);
            }
        } else {
            queryWrapper.lambda().eq(SystemEntity::getUserId, userId);
        }
        return list(queryWrapper);
    }

    @Override
    public void workHandover(WorkHandoverModel workHandoverModel) {
        this.baseMapper.workHandover(workHandoverModel);
    }

    @Override
    public void changeSystemAuthorizion(AppAuthorizationModel model) {
        this.baseMapper.changeSystemAuthorizion(model);
    }

    @Override
    public List<SystemEntity> getListByIds(List<String> list, List<String> moduleAuthorize, int type) {
        return this.baseMapper.getListByIds(list, moduleAuthorize, type);
    }

    @Override
    public List<SystemEntity> getListByCreUser(String userId) {
        return this.baseMapper.getListByCreUser(userId);
    }

    @Override
    public void setAutoEnCode(SystemEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.YY), code -> this.isExistEnCode(entity.getId(), code)));
        }
    }

    @Override
    public void importCopy(SystemEntity entity, boolean isImport) {
        if (Boolean.TRUE.equals(isExistFullName(entity.getId(), entity.getFullName()))) {
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            String fullName = entity.getFullName() + ".副本" + copyNum;
            if (fullName.length() > 50) {
                throw new DataException(MsgCode.PRI006.get());
            }
            entity.setFullName(fullName);
        }
        entity.setEnCode(null);
        entity.setAppPortalId(null);
        entity.setPortalId(null);
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        if (isImport && !PermissionConst.ALL_DEV_USER.equals(entity.getAuthorizeId())) {
            entity.setAuthorizeId(null);
        }
        this.create(entity);
    }

    @Override
    public List<SystemEntity> getMainList(){
        return this.baseMapper.getMainList();
    }
}
