package jnpf.permission.mapper;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.button.ButtonModel;
import jnpf.base.model.column.ColumnModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.portalmanage.SavePortalAuthModel;
import jnpf.base.model.resource.ResourceModel;
import jnpf.constant.AuthorizeConst;
import jnpf.constant.PermissionConst;
import jnpf.permission.entity.AuthorizeEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Param;

import java.util.*;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:27
 */
public interface AuthorizeMapper extends SuperMapper<AuthorizeEntity> {


    List<ModuleModel> findModule(@Param("objectId") List<String> objectId, @Param("systemId") List<String> systemId, @Param("moduleAuthorize") List<String> moduleAuthorize, @Param("moduleUrlAddressAuthorize") List<String> moduleUrlAddressAuthorize, @Param("mark") Integer mark);

    List<ButtonModel> findButton(@Param("objectId") List<String> objectId);

    List<ColumnModel> findColumn(@Param("objectId") List<String> objectId);

    List<ResourceModel> findResource(@Param("objectId") List<String> objectId);

    List<ModuleFormModel> findForms(@Param("objectId") List<String> objectId);

    List<SystemBaeModel> findSystem(@Param("objectId") List<String> objectId, @Param("enCode") String enCode, @Param("moduleAuthorize") List<String> moduleAuthorize, @Param("mark") Integer mark);

    List<ButtonModel> findButtonAdmin(@Param("mark") Integer mark);

    List<ColumnModel> findColumnAdmin(@Param("mark") Integer mark);

    List<ResourceModel> findResourceAdmin(@Param("mark") Integer mark);

    List<ModuleFormModel> findFormsAdmin(@Param("mark") Integer mark);

    default void deleteByItemIds(List<String> itemIds) {
        if (CollUtil.isEmpty(itemIds)) return;
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(AuthorizeEntity::getItemId, itemIds);
        this.deleteByIds(selectList(queryWrapper));
    }

    default void deleteByObjIds(List<String> objIds) {
        if (CollUtil.isEmpty(objIds)) return;
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(AuthorizeEntity::getObjectId, objIds);
        this.deleteByIds(selectList(queryWrapper));
    }

    default void saveObjectAuth(SavePortalAuthModel portalAuthModel) {
        List<String> ids = portalAuthModel.getIds();
        String id = portalAuthModel.getId();
        String type = portalAuthModel.getType();
        String userId = UserProvider.getLoginUserId();
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getItemType, type);
        queryWrapper.lambda().eq(AuthorizeEntity::getItemId, id);
        List<AuthorizeEntity> authorizeEntities = this.selectList(queryWrapper);
        this.deleteByIds(authorizeEntities);

        List<AuthorizeEntity> portalSystem = new ArrayList<>();
        boolean isPortal = AuthorizeConst.AUTHORIZE_PORTAL_MANAGE.equals(type);
        if (isPortal && !ids.isEmpty() && StringUtil.isNotEmpty(portalAuthModel.getSystemId())) {
            QueryWrapper<AuthorizeEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(AuthorizeEntity::getItemType, AuthorizeConst.SYSTEM);
            wrapper.lambda().eq(AuthorizeEntity::getItemId, portalAuthModel.getSystemId());
            wrapper.lambda().in(AuthorizeEntity::getObjectId, ids);
            portalSystem.addAll(this.selectList(wrapper));
        }
        // 原始授权角色
        List<AuthorizeEntity> list = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String objectId = ids.get(i);
            AuthorizeEntity authorizeEntity = new AuthorizeEntity();
            authorizeEntity.setId(RandomUtil.uuId());
            authorizeEntity.setItemType(type);
            authorizeEntity.setObjectId(objectId);
            authorizeEntity.setObjectType(PermissionConst.ROLE);
            authorizeEntity.setItemId(id);
            authorizeEntity.setSortCode((long) i);
            authorizeEntity.setCreatorTime(new Date());
            authorizeEntity.setCreatorUserId(userId);
            list.add(authorizeEntity);
            if (isPortal && StringUtil.isNotEmpty(portalAuthModel.getSystemId())) {
                boolean portalCount = portalSystem.stream().filter(t -> Objects.equals(t.getObjectId(), objectId)).count() == 0;
                if (portalCount) {
                    AuthorizeEntity systemAuthorize = new AuthorizeEntity();
                    systemAuthorize.setId(RandomUtil.uuId());
                    systemAuthorize.setItemType(AuthorizeConst.SYSTEM);
                    systemAuthorize.setObjectId(ids.get(i));
                    systemAuthorize.setObjectType(PermissionConst.ROLE);
                    systemAuthorize.setItemId(portalAuthModel.getSystemId());
                    systemAuthorize.setSortCode(0l);
                    systemAuthorize.setCreatorTime(new Date());
                    systemAuthorize.setCreatorUserId(userId);
                    list.add(systemAuthorize);
                }
            }
        }
        list.forEach(this::insert);
    }

    default List<AuthorizeEntity> getAuthorizeByItem(String itemType, String itemId) {
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getItemType, itemType);
        queryWrapper.lambda().eq(AuthorizeEntity::getItemId, itemId);
        return this.selectList(queryWrapper);
    }

    default List<AuthorizeEntity> getListByRoleIdsAndItemType(List<String> roleIds, String itemType) {
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getItemType, itemType);
        queryWrapper.lambda().in(AuthorizeEntity::getObjectId, roleIds);
        return this.selectList(queryWrapper);
    }

    default List<AuthorizeEntity> getListByObjectId(List<String> objectId) {
        if (objectId.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(AuthorizeEntity::getObjectId, objectId);
        return this.selectList(queryWrapper);
    }

    default Boolean existAuthorize(String roleId, String systemId) {
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getObjectId, roleId);
        if (StringUtil.isNotEmpty(systemId)) {
            queryWrapper.lambda().eq(AuthorizeEntity::getItemId, systemId);
            queryWrapper.lambda().eq(AuthorizeEntity::getItemType, AuthorizeConst.SYSTEM);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<AuthorizeEntity> getListByRoleId(String roleId) {
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getObjectId, roleId);
        return this.selectList(queryWrapper);
    }

    default List<AuthorizeEntity> getListByObjectId(String objectId, String itemType) {
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(objectId)) {
            queryWrapper.lambda().eq(AuthorizeEntity::getObjectId, objectId);
        }
        if (StringUtil.isNotEmpty(itemType)) {
            queryWrapper.lambda().eq(AuthorizeEntity::getItemType, itemType);
        }
        return this.selectList(queryWrapper);
    }

    default List<AuthorizeEntity> getListByObjectAndItem(String itemId, String objectType) {
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getObjectType, objectType).eq(AuthorizeEntity::getItemId, itemId);
        return this.selectList(queryWrapper);
    }

    default List<AuthorizeEntity> getListByObjectAndItemIdAndType(String itemId, String itemType) {
        QueryWrapper<AuthorizeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthorizeEntity::getItemType, itemType).eq(AuthorizeEntity::getItemId, itemId);
        return this.selectList(queryWrapper);
    }
}
