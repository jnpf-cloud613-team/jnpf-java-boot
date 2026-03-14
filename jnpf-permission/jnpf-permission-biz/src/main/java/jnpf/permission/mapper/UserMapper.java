package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.rolerelaiton.RoleRelationPage;
import jnpf.permission.model.user.UserSystemCountModel;
import jnpf.permission.model.user.page.PageUser;
import jnpf.permission.model.user.page.PaginationUser;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Param;

import java.util.*;

import static jnpf.util.Constants.ADMIN_KEY;


/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface UserMapper extends SuperMapper<UserEntity> {
    /**
     * 获取用户id
     *
     * @return
     */
    List<String> getListId();

    /**
     * 通过组织id获取用户信息
     *
     * @param orgIdList
     * @param gender
     * @return
     */
    List<String> query(@Param("orgIdList") List<String> orgIdList, @Param("account") String account, @Param("dbSchema") String dbSchema, @Param("enabledMark") Integer enabledMark, @Param("gender") String gender);

    /**
     * 通过组织id获取用户信息
     *
     * @param orgIdList
     * @param gender
     * @return
     */
    Long count(@Param("orgIdList") List<String> orgIdList, @Param("account") String account, @Param("dbSchema") String dbSchema, @Param("enabledMark") Integer enabledMark, @Param("gender") String gender);

    default List<UserEntity> getList(boolean filterEnabledMark) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        if (filterEnabledMark) {
            queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        }
        queryWrapper.lambda().ne(UserEntity::getAccount, ADMIN_KEY);
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<UserEntity> getUserNameList(List<String> idList) {
        if (!idList.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(UserEntity::getId, UserEntity::getRealName, UserEntity::getEnabledMark).in(UserEntity::getId, idList);
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }

    default List<UserEntity> getUserNameList(Set<String> idList) {
        if (!idList.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(UserEntity::getId, UserEntity::getRealName, UserEntity::getAccount).in(UserEntity::getId, idList);
            return this.selectList(queryWrapper);
        }
        return new ArrayList<>();
    }

    default Map<String, Object> getUserMap() {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(UserEntity::getId, UserEntity::getRealName, UserEntity::getAccount);
        Map<String, Object> userMap = new HashMap<>();
        this.selectList(queryWrapper).stream().forEach(user -> userMap.put(user.getId(), user.getRealName() + "/" + user.getAccount()));
        return userMap;
    }

    default Map<String, Object> getUserNameAndIdMap() {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(UserEntity::getId, UserEntity::getRealName, UserEntity::getAccount);
        Map<String, Object> userMap = new HashMap<>();
        this.selectList(queryWrapper).stream().forEach(user -> userMap.put(user.getRealName() + "/" + user.getAccount(), user.getId()));
        return userMap;
    }

    default Map<String, Object> getUserNameAndIdMap(boolean enabledMark) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        if (enabledMark) {
            queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        }
        queryWrapper.lambda().select(UserEntity::getId, UserEntity::getRealName, UserEntity::getAccount);
        Map<String, Object> userMap = new HashMap<>();
        this.selectList(queryWrapper).stream().forEach(user -> userMap.put(user.getRealName() + "/" + user.getAccount(), user.getId()));
        return userMap;
    }

    default UserEntity getByRealName(String realName) {
        UserEntity userEntity = new UserEntity();
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getRealName, realName);
        queryWrapper.lambda().select(UserEntity::getId);
        List<UserEntity> list = this.selectList(queryWrapper);
        if (!list.isEmpty()) {
            userEntity = list.get(0);
        }
        return userEntity;
    }

    default UserEntity getByRealName(String realName, String account) {
        UserEntity userEntity = new UserEntity();
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getRealName, realName);
        queryWrapper.lambda().eq(UserEntity::getAccount, account);
        queryWrapper.lambda().select(UserEntity::getId);
        List<UserEntity> list = this.selectList(queryWrapper);
        if (!list.isEmpty()) {
            userEntity = list.get(0);
        }
        return userEntity;
    }

    default List<UserEntity> getAdminList() {
        QueryWrapper<UserEntity> query = new QueryWrapper<>();
        query.lambda().eq(UserEntity::getIsAdministrator, 1);
        query.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        return selectList(query);
    }

    default List<UserEntity> getList(PageUser pagination, boolean filterCurrentUser) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean filterLastTime = false;
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        if (filterCurrentUser) {
            String userId = UserProvider.getUser().getUserId();
            queryWrapper.lambda().ne(UserEntity::getId, userId);
        }
        queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        //关键字（账户、姓名、手机）
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            filterLastTime = true;
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        if (CollUtil.isNotEmpty(pagination.getIdList())) {
            List<List<String>> partition = Lists.partition(pagination.getIdList(), 1000);
            queryWrapper.lambda().and(t -> {
                for (List<String> list : partition) {
                    t.in(UserEntity::getId, list).or();
                }
            });
        }
        //排序
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        if (filterLastTime) {
            queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
        }
        Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<UserEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<UserEntity> getUserPage(Pagination pagination) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            //通过关键字查询
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        queryWrapper.lambda().select(UserEntity::getId, UserEntity::getAccount, UserEntity::getRealName, UserEntity::getGender, UserEntity::getEnabledMark);
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<UserEntity> iPage = this.selectPage(page, queryWrapper);
        return iPage.getRecords();
    }

    default List<UserEntity> getListByManagerId(String managerId, String keyword) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getManagerId, managerId);
        // 通过关键字查询
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, keyword)
                            .or().like(UserEntity::getRealName, keyword)
            );
        }
        // 只查询正常的用户
        queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default UserEntity getInfo(String id) {
        return this.selectById(id);
    }

    default UserEntity getUserByAccount(String account) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getAccount, account);
        List<UserEntity> list = this.selectList(queryWrapper);
        return CollUtil.isNotEmpty(list) ? list.get(0) : null;
    }

    default UserEntity getUserByMobile(String mobile) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getMobilePhone, mobile);
        List<UserEntity> list = this.selectList(queryWrapper);
        return CollUtil.isNotEmpty(list) ? list.get(0) : null;
    }

    default boolean isExistByAccount(String account) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getAccount, account);
        List<UserEntity> list = this.selectList(queryWrapper);
        return CollUtil.isNotEmpty(list);
    }


    default List<UserEntity> getUserName(List<String> id) {
        return getUserName(id, false);
    }

    /**
     * 查询用户名称
     *
     * @param id 主键值
     * @return
     */
    default List<UserEntity> getUserName(List<String> id, boolean filterEnabledMark) {
        List<UserEntity> list = new ArrayList<>();
        // 达梦数据库无法null值入参
        id.removeAll(Collections.singleton(null));
        if (!id.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserEntity::getId, id);
            if (filterEnabledMark) {
                queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
            }
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default List<UserEntity> getListByUserIds(List<String> id) {
        List<UserEntity> list = new ArrayList<>();
        // 达梦数据库无法null值入参
        id.removeAll(Collections.singleton(null));
        if (!id.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserEntity::getId, id);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default List<UserEntity> getUserList(List<String> id) {
        List<UserEntity> list = new ArrayList<>();
        // 达梦数据库无法null值入参
        id.removeAll(Collections.singleton(null));
        if (!id.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserEntity::getId, id);
            queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default UserEntity getUserEntity(String account) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getAccount, account);
        return this.selectOne(queryWrapper);
    }

    default void update(UserEntity entity, String type) {
        UpdateWrapper<UserEntity> wrapper = new UpdateWrapper<>();
        if ("Position".equals(type)) {
            wrapper.lambda().set(UserEntity::getPositionId, entity.getPositionId());
        } else {
            wrapper.lambda().set(UserEntity::getRoleId, entity.getRoleId());
        }
        wrapper.lambda().eq(UserEntity::getId, entity.getId());
        this.update(wrapper);
    }

    default void updateLastTime(UserEntity entity, String type) {
        UpdateWrapper<UserEntity> wrapper = new UpdateWrapper<>();
        if ("Position".equals(type)) {
            wrapper.lambda().set(UserEntity::getPositionId, entity.getPositionId());
        } else {
            wrapper.lambda().set(UserEntity::getRoleId, entity.getRoleId());
        }
        wrapper.lambda().set(UserEntity::getLastModifyTime, new Date());
        wrapper.lambda().set(UserEntity::getLastModifyUserId, entity.getLastModifyUserId());
        wrapper.lambda().eq(UserEntity::getId, entity.getId());
        this.update(wrapper);
    }

    default List<UserEntity> getUserName(List<String> id, Pagination pagination) {
        List<UserEntity> list = new ArrayList<>();
        id.removeAll(Collections.singleton(null));
        if (!id.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            if (!StringUtil.isEmpty(pagination.getKeyword())) {
                queryWrapper.lambda().and(
                        t -> t.like(UserEntity::getRealName, pagination.getKeyword())
                                .or().like(UserEntity::getAccount, pagination.getKeyword())
                );
            }
            queryWrapper.lambda().in(UserEntity::getId, id);
            queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
            queryWrapper.lambda().select(UserEntity::getId, UserEntity::getRealName, UserEntity::getAccount,
                    UserEntity::getGender, UserEntity::getHeadIcon, UserEntity::getMobilePhone);
            queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
            Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
            IPage<UserEntity> iPage = this.selectPage(page, queryWrapper);
            return pagination.setData(iPage.getRecords(), iPage.getTotal());
        }
        return pagination.setData(list, list.size());
    }

    default List<UserEntity> getUserNames(List<String> id, PaginationUser pagination, boolean flag, boolean enabledMark) {
        List<UserEntity> list = new ArrayList<>();
        id.removeAll(Collections.singleton(null));
        if (!id.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            if (!StringUtil.isEmpty(pagination.getKeyword())) {
                queryWrapper.lambda().and(
                        t -> t.like(UserEntity::getRealName, pagination.getKeyword())
                                .or().like(UserEntity::getAccount, pagination.getKeyword())
                                .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
                );
            }
            List<List<String>> lists = Lists.partition(id, 1000);
            queryWrapper.lambda().and(t -> {
                for (List<String> userId : lists) {
                    t.or().in(UserEntity::getId, userId);
                }
            });
            if (flag) {
                queryWrapper.lambda().ne(UserEntity::getId, UserProvider.getUser().getUserId());
            }
            if (enabledMark) {
                queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
            }
            if (StringUtil.isNotEmpty(pagination.getGender())) {
                queryWrapper.lambda().eq(UserEntity::getGender, pagination.getGender());
            }
            queryWrapper.lambda().orderByDesc(UserEntity::getCreatorTime);
            if (ObjectUtil.isEmpty(pagination) || Objects.equals(pagination.getDataType(), 1)) {
                return this.selectList(queryWrapper);
            }
            Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
            IPage<UserEntity> iPage = this.selectPage(page, queryWrapper);
            return pagination.setData(iPage.getRecords(), iPage.getTotal());
        }
        return ObjectUtil.isEmpty(pagination) ? new ArrayList<>() : pagination.setData(list, list.size());
    }

    default List<UserEntity> getUserAccount(List<String> ids) {
        List<UserEntity> list = new ArrayList<>();
        if (!ids.isEmpty()) {
            QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserEntity::getAccount, ids);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default void updateStand(List<String> ids, int standing) {
        List<UserEntity> userName = getUserName(new ArrayList<>(ids), false);
        for (UserEntity user : userName) {
            String positionId = user.getPositionId();
            String organizeId = user.getOrganizeId();
            String id = user.getId();
            UpdateWrapper<UserEntity> wrapper = new UpdateWrapper<>();
            wrapper.lambda().eq(UserEntity::getId, id);
            wrapper.lambda().set(UserEntity::getOrganizeId, organizeId);
            wrapper.lambda().set(UserEntity::getPositionId, positionId);
            update(wrapper);
        }
        if (!ids.isEmpty()) {
            UpdateWrapper<UserEntity> pcWrapper = new UpdateWrapper<>();
            pcWrapper.lambda().in(UserEntity::getId, ids);
            pcWrapper.lambda().eq(UserEntity::getStanding, standing);
            pcWrapper.lambda().set(UserEntity::getStanding, 3);
            update(pcWrapper);
            UpdateWrapper<UserEntity> appWrapper = new UpdateWrapper<>();
            appWrapper.lambda().in(UserEntity::getId, ids);
            appWrapper.lambda().eq(UserEntity::getAppStanding, standing);
            appWrapper.lambda().set(UserEntity::getAppStanding, 3);
            update(appWrapper);
        }
    }

    default List<UserEntity> getPageByIds(RoleRelationPage pagination) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(UserEntity::getId, UserEntity::getAccount, UserEntity::getRealName, UserEntity::getGender,
                UserEntity::getMobilePhone, UserEntity::getEnabledMark);
        boolean flag = false;
        //关键字（账户、姓名、手机）
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }

        List<List<String>> lists = Lists.partition(pagination.getIdList(), 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> item : lists) {
                t.in(UserEntity::getId, item).or();
            }
        });
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
            return this.selectList(queryWrapper);
        }
        Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<UserEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<UserEntity> pageUser(UserSystemCountModel model) {
        if (CollUtil.isEmpty(model.getUserIds())) {
            return Collections.emptyList();
        }
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        if (model.isFilter()) {
            queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
        }
        if (model.getPagination() != null && !StringUtil.isEmpty(model.getPagination().getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, model.getPagination().getKeyword())
                            .or().like(UserEntity::getRealName, model.getPagination().getKeyword())
                            .or().like(UserEntity::getMobilePhone, model.getPagination().getKeyword())
            );
        }
        if (CollUtil.isNotEmpty(model.getUserIds())) {
            queryWrapper.lambda().in(UserEntity::getId, model.getUserIds());
        }

        long count = this.selectCount(queryWrapper);
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        Page<UserEntity> page = new Page<>(model.getPagination().getCurrentPage()
                , model.getPagination().getPageSize(), count, false);
        page.setOptimizeCountSql(true);
        Page<UserEntity> userEntityPage = this.selectPage(page, queryWrapper);
        return model.getPagination().setData(userEntityPage.getRecords(), page.getTotal());
    }
}
