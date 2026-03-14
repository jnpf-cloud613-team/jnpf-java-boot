package jnpf.base.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.UserInfo;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.AppAuthorizationModel;
import jnpf.base.model.base.SystemPageVO;
import jnpf.constant.PermissionConst;
import jnpf.permission.model.user.WorkHandoverModel;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.*;


/**
 * 系统
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface SystemMapper extends SuperMapper<SystemEntity> {

    default List<SystemEntity> getList() {
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(SystemEntity::getSortCode)
                .orderByDesc(SystemEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<SystemEntity> getList(SystemPageVO pageVO) {
        UserInfo user = UserProvider.getUser();
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        String keyword = pageVO.getKeyword();
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.lambda().and(t ->
                    t.like(SystemEntity::getFullName, keyword).or().like(SystemEntity::getEnCode, keyword)
                            .or().like(SystemEntity::getDescription, keyword)
            );
        }
        // 过滤掉系统应用
        if (pageVO.isFilterMain()) {
            queryWrapper.lambda().ne(SystemEntity::getIsMain, 1);
        }
        //判断权限列表
        if (pageVO.getType() != null) {
            if (Boolean.TRUE.equals(user.getIsDevRole())) {
                if (Objects.equals(pageVO.getType(), 1)) {
                    queryWrapper.lambda().and(t -> t
                            .eq(SystemEntity::getUserId, user.getUserId()).or()
                            .like(SystemEntity::getAuthorizeId, user.getUserId()).or()
                            .eq(SystemEntity::getAuthorizeId, PermissionConst.ALL_DEV_USER));
                } else if (Objects.equals(pageVO.getType(), 2)) {
                    queryWrapper.lambda().eq(SystemEntity::getUserId, user.getUserId());
                } else if (Objects.equals(pageVO.getType(), 3)) {
                    queryWrapper.lambda().ne(SystemEntity::getUserId, user.getUserId());
                    queryWrapper.lambda().and(t -> t
                            .like(SystemEntity::getAuthorizeId, user.getUserId()).or()
                            .eq(SystemEntity::getAuthorizeId, PermissionConst.ALL_DEV_USER));
                }
            } else {
                if (Objects.equals(pageVO.getType(), 3)) {
                    return Collections.emptyList();
                }
                if (Boolean.FALSE.equals(user.getIsAdministrator()) || Objects.equals(pageVO.getType(), 2)) {
                    queryWrapper.lambda().eq(SystemEntity::getUserId, user.getUserId());
                }
            }
        }
        if ("DESC".equalsIgnoreCase(pageVO.getSort())) {
            //降序
            queryWrapper.lambda().orderByDesc(SystemEntity::getCreatorTime);
        } else {
            //升序
            queryWrapper.lambda().orderByAsc(SystemEntity::getCreatorTime);
        }
        return this.selectList(queryWrapper);
    }

    default List<SystemEntity> getListByIdsKey(List<String> ids, String keyword) {
        if (CollUtil.isEmpty(ids)) return Collections.emptyList();
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(SystemEntity::getId, ids);
        boolean flag = false;
        if (StringUtil.isNotEmpty(keyword)) {
            flag = true;
            queryWrapper.lambda().and(t ->
                    t.like(SystemEntity::getFullName, keyword).or().like(SystemEntity::getEnCode, keyword)
                            .or().like(SystemEntity::getDescription, keyword)
            );
        }
        if (flag) {
            queryWrapper.lambda().orderByDesc(SystemEntity::getLastModifyTime);
        } else {
            queryWrapper.lambda().orderByAsc(SystemEntity::getSortCode).orderByDesc(SystemEntity::getCreatorTime);
        }
        return this.selectList(queryWrapper);
    }

    default SystemEntity getInfo(String id) {
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default Boolean isExistFullName(String id, String fullName) {
        if (StringUtil.isEmpty(fullName)) return false;
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getFullName, fullName);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(SystemEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean isExistEnCode(String id, String enCode) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getEnCode, enCode);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().ne(SystemEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean create(SystemEntity entity) {
        String userId = UserProvider.getUser().getUserId();
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setIsMain(0);
        entity.setCreatorUserId(userId);
        entity.setCreatorTime(new Date());
        entity.setUserId(userId);
        return SqlHelper.retBool(this.insert(entity));
    }

    default Boolean update(String id, SystemEntity entity) {
        entity.setId(id);
        if (entity.getIsMain() == null) {
            entity.setIsMain(0);
        }
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(new Date());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default List<SystemEntity> getListByIds(List<String> list, List<String> moduleAuthorize) {
        List<SystemEntity> systemList = new ArrayList<>(16);
        if (!list.isEmpty()) {
            QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
            if (moduleAuthorize != null && !moduleAuthorize.isEmpty()) {
                queryWrapper.lambda().notIn(SystemEntity::getId, moduleAuthorize);
            }
            queryWrapper.lambda().in(SystemEntity::getId, list);
            queryWrapper.lambda().eq(SystemEntity::getEnabledMark, 1);
            queryWrapper.lambda().orderByAsc(SystemEntity::getSortCode).orderByDesc(SystemEntity::getCreatorTime);
            return this.selectList(queryWrapper);
        }
        return systemList;
    }

    default SystemEntity getInfoByEnCode(String enCode) {
        if (StringUtil.isEmpty(enCode)) {
            return null;
        }
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getEnCode, enCode);
        return this.selectOne(queryWrapper);
    }

    default List<SystemEntity> findSystemAdmin(List<String> moduleAuthorize) {
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        if (moduleAuthorize != null && !moduleAuthorize.isEmpty()) {
            queryWrapper.lambda().notIn(SystemEntity::getId, moduleAuthorize);
        }
        queryWrapper.lambda().orderByAsc(SystemEntity::getSortCode).orderByDesc(SystemEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default boolean saveSystemAuthorizion(AppAuthorizationModel model) {
        SystemEntity info = this.getInfo(model.getSystemId());
        List<String> devUsers = model.getDevUsers();
        if (devUsers != null && !devUsers.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            devUsers.forEach(item -> stringBuilder.append(item).append(","));
            info.setAuthorizeId(stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1));
        } else {
            info.setAuthorizeId("");
        }

        if (model.getIsAllDevUser() != null && model.getIsAllDevUser() == 1) {
            info.setAuthorizeId(PermissionConst.ALL_DEV_USER);
        }
        return SqlHelper.retBool(this.updateById(info));
    }

    default void workHandover(WorkHandoverModel workHandoverModel) {
        String userId = workHandoverModel.getFromId();
        List<String> appList = workHandoverModel.getAppList();
        String appHandoverUser = workHandoverModel.getAppHandoverUser();
        List<SystemEntity> listByIds = this.getListByIds(appList, null);
        for (SystemEntity entity : listByIds) {
            if (Objects.equals(entity.getUserId(), userId)) {
                entity.setUserId(appHandoverUser);
            }
            if (StringUtil.isNotEmpty(entity.getAuthorizeId())) {
                String[] userIds = entity.getAuthorizeId().split(",");
                String author = String.join(",", Arrays.stream(userIds).map(s -> s.equals(userId) ? appHandoverUser : s).toArray(String[]::new));
                entity.setAuthorizeId(author);
            }
            this.updateById(entity);
        }
    }

    default void changeSystemAuthorizion(AppAuthorizationModel model) {
        String systemId = model.getSystemId();
        String createUserId = model.getCreateUserId();
        SystemEntity info = this.getInfo(systemId);

        if (StringUtil.isNotEmpty(createUserId)) {
            info.setUserId(createUserId);
        }
        this.updateById(info);
    }

    default List<SystemEntity> getListByIds(List<String> list, List<String> moduleAuthorize, int type) {
        List<SystemEntity> systemList = new ArrayList<>(16);
        if (!list.isEmpty()) {
            QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
            if (moduleAuthorize != null && !moduleAuthorize.isEmpty()) {
                queryWrapper.lambda().notIn(SystemEntity::getId, moduleAuthorize);
            }
            queryWrapper.lambda().in(SystemEntity::getId, list);
            if (type == 1) {
                queryWrapper.lambda().eq(SystemEntity::getEnabledMark, 1);
            }
            queryWrapper.lambda().orderByAsc(SystemEntity::getSortCode).orderByDesc(SystemEntity::getCreatorTime);
            return this.selectList(queryWrapper);
        }
        return systemList;
    }

    default List<SystemEntity> getListByCreUser(String userId) {
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getUserId, userId);
        queryWrapper.lambda().eq(SystemEntity::getEnabledMark, 1);
        return selectList(queryWrapper);
    }

    default List<SystemEntity> getMainList() {
        QueryWrapper<SystemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemEntity::getEnabledMark, 1);
        queryWrapper.lambda().eq(SystemEntity::getIsMain, 1);
        queryWrapper.lambda().orderByAsc(SystemEntity::getSortCode)
                .orderByDesc(SystemEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }
}
