package jnpf.permission.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.page.PageMethod;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.source.DbBase;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.emnus.SysParamEnum;
import jnpf.exception.DataException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.SystemParamModel;
import jnpf.model.tenant.TenantVO;
import jnpf.permission.entity.*;
import jnpf.permission.mapper.*;
import jnpf.permission.model.organize.OrganizeSelectorVO;
import jnpf.permission.model.rolerelaiton.RoleRelationPage;
import jnpf.permission.model.user.UserIdListVo;
import jnpf.permission.model.user.UserRelationIds;
import jnpf.permission.model.user.UserSystemCountModel;
import jnpf.permission.model.user.mod.UserConditionModel;
import jnpf.permission.model.user.page.PageUser;
import jnpf.permission.model.user.page.PaginationUser;
import jnpf.permission.model.user.page.UserPagination;
import jnpf.permission.model.user.vo.BaseInfoVo;
import jnpf.permission.service.UserService;
import jnpf.permission.util.UserUtil;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static jnpf.util.Constants.ADMIN_KEY;

/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends SuperServiceImpl<UserMapper, UserEntity> implements UserService {
    private final RedisUtil redisUtil;
    private final CacheKeyUtil cacheKeyUtil;
    private final DataSourceUtil dataSourceUtil;
    private final ConfigValueUtil configValueUtil;
    private final SysconfigService sysconfigApi;
    private final UserUtil userUtil;
    private final DictionaryDataService dictionaryDataService;
    private final GroupMapper groupMapper;
    private final RoleMapper roleMapper;
    private final OrganizeMapper organizeMapper;
    private final PositionMapper positionMapper;
    private final SocialsUserMapper socialsUserMapper;
    private final UserRelationMapper userRelationMapper;
    private final RoleRelationMapper roleRelationMapper;
    private final UserOldPasswordMapper userOldPasswordMapper;
    private final PermissionGroupMapper permissionGroupMapper;

    @Override
    public List<UserEntity> getList(UserPagination pagination) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().ne(UserEntity::getAccount, ADMIN_KEY);
        boolean filterLastTime = false;
        //关键字（账户、姓名、手机）
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            filterLastTime = true;
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        if (pagination.getEnabledMark() != null) {
            if (Objects.equals(pagination.getEnabledMark(), 12)) {
                queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
            } else {
                queryWrapper.lambda().eq(UserEntity::getEnabledMark, pagination.getEnabledMark());
            }
        }
        if (StringUtil.isNotEmpty(pagination.getGender())) {
            queryWrapper.lambda().eq(UserEntity::getGender, pagination.getGender());
        }
        //有分组id过滤
        if (StringUtil.isNotEmpty(pagination.getGroupId()) && hasGroup(pagination, queryWrapper)) {
            return pagination.setData(Collections.emptyList(), 0);
        }

        //有岗位id
        if ((StringUtil.isNotEmpty(pagination.getPositionId()) && hasPosition(pagination, queryWrapper))
                ||(StringUtil.isNotEmpty(pagination.getOrganizeId()) && hasOrg(pagination, queryWrapper))) {
            return pagination.setData(Collections.emptyList(), 0);
        }

        //有角色id
        if (StringUtil.isNotEmpty(pagination.getRoleId()) && hasRole(pagination, queryWrapper)) {
            return pagination.setData(Collections.emptyList(), 0);
        }

        long count = this.count(queryWrapper);
        queryWrapper.lambda().select(UserEntity::getId);
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        if (filterLastTime) {
            queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
        }
        if (Objects.equals(pagination.getDataType(), 1)) {
            return this.list(queryWrapper);
        }
        Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize(), count, false);
        page.setOptimizeCountSql(false);
        IPage<UserEntity> iPage = this.page(page, queryWrapper);

        if (!iPage.getRecords().isEmpty()) {
            List<String> ids = iPage.getRecords().stream().map(m -> m.getId()).collect(Collectors.toList());
            queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserEntity::getId, ids);
            queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
            if (filterLastTime) {
                queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
            }
            iPage.setRecords(this.list(queryWrapper));
        }
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    private boolean hasGroup(UserPagination pagination, QueryWrapper<UserEntity> queryWrapper) {
        List<UserRelationEntity> listUser = userRelationMapper.getListByObjectId(pagination.getGroupId(), PermissionConst.GROUP);
        List<String> users = listUser.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) {
            return true;
        }
        List<List<String>> lists = Lists.partition(users, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> userItem : lists) {
                t.in(UserEntity::getId, userItem).or();
            }
        });
        return false;
    }

    private boolean hasPosition(UserPagination pagination, QueryWrapper<UserEntity> queryWrapper) {
        List<UserRelationEntity> listUser = userRelationMapper.getListByObjectId(pagination.getPositionId(), PermissionConst.POSITION);
        List<String> users = listUser.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) {
            return true;
        }
        List<List<String>> lists = Lists.partition(users, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> userItem : lists) {
                t.in(UserEntity::getId, userItem).or();
            }
        });
        return false;
    }

    private boolean hasOrg(UserPagination pagination, QueryWrapper<UserEntity> queryWrapper) {
        List<String> orgIds = new ArrayList<>();
        orgIds.add(pagination.getOrganizeId());
        if (Objects.equals(pagination.getShowSubOrganize(), 1)) {
            List<OrganizeEntity> allChild = organizeMapper.getAllChild(pagination.getOrganizeId());
            orgIds.addAll(allChild.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
        }
        List<PositionEntity> listPost = positionMapper.getListByOrgIds(orgIds);
        List<String> listPostId = listPost.stream().map(PositionEntity::getId).collect(Collectors.toList());
        List<UserRelationEntity> listUser = userRelationMapper.getListByObjectIdAll(listPostId);
        List<String> users = listUser.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) {
            return true;
        }
        List<List<String>> lists = Lists.partition(users, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> userItem : lists) {
                t.in(UserEntity::getId, userItem).or();
            }
        });
        return false;
    }

    private boolean hasRole(UserPagination pagination, QueryWrapper<UserEntity> queryWrapper) {
        List<RoleRelationEntity> listUser = roleRelationMapper.getListByRoleId(pagination.getRoleId(), PermissionConst.USER);
        List<String> users = listUser.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList());
        if (CollUtil.isEmpty(users)) {
            return true;
        }
        List<List<String>> lists = Lists.partition(users, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> userItem : lists) {
                t.in(UserEntity::getId, userItem).or();
            }
        });
        return false;
    }

    @Override
    public List<UserEntity> getList(boolean filterEnabledMark) {
        return this.baseMapper.getList(filterEnabledMark);
    }

    @Override
    public List<UserEntity> getUserNameList(List<String> idList) {
        return this.baseMapper.getUserNameList(idList);
    }

    @Override
    public List<UserEntity> getUserNameList(Set<String> idList) {
        return this.baseMapper.getUserNameList(idList);
    }

    @Override
    public Map<String, Object> getUserMap() {
        return this.baseMapper.getUserMap();
    }

    @Override
    public Map<String, Object> getUserNameAndIdMap() {
        return this.baseMapper.getUserNameAndIdMap();
    }

    @Override
    public Map<String, Object> getUserNameAndIdMap(boolean enabledMark) {
        return this.baseMapper.getUserNameAndIdMap(enabledMark);
    }

    @Override
    public UserEntity getByRealName(String realName) {
        return this.baseMapper.getByRealName(realName);
    }

    @Override
    public UserEntity getByRealName(String realName, String account) {
        return this.baseMapper.getByRealName(realName, account);
    }

    @Override
    public List<UserEntity> getAdminList() {
        return this.baseMapper.getAdminList();
    }

    @Override
    public List<UserEntity> getList(PaginationUser pagination, String organizeId, boolean flag, boolean filter, Integer enabledMark, String gender) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean filterLastTime = false;
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        if (flag) {
            queryWrapper.lambda().ne(UserEntity::getId, userId);
        }
        if (filter) {
            queryWrapper.lambda().ne(UserEntity::getAccount, ADMIN_KEY);
        }
        //组织机构
        if (!StringUtil.isEmpty(organizeId)) {
            return getOrgUser(pagination, organizeId, enabledMark, gender);
        }
        //关键字（账户、姓名、手机）
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            filterLastTime = true;
            queryWrapper.lambda().and(
                    t -> t.like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        if (enabledMark != null) {
            if (Objects.equals(pagination.getEnabledMark(), 1)) {
                queryWrapper.lambda().ne(UserEntity::getEnabledMark, 0);
            } else {
                queryWrapper.lambda().eq(UserEntity::getEnabledMark, pagination.getEnabledMark());
            }
        }
        if (StringUtil.isNotEmpty(gender)) {
            queryWrapper.lambda().eq(UserEntity::getGender, gender);
        }
        //不分页
        if (Objects.equals(pagination.getDataType(), 1)) {
            queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
            if (filterLastTime) {
                queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
            }
            return this.list(queryWrapper);
        }
        //分页
        //排序
        long count = this.count(queryWrapper);
        queryWrapper.lambda().select(UserEntity::getId);
        queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
        if (filterLastTime) {
            queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
        }
        Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize(), count, false);
        page.setOptimizeCountSql(false);
        IPage<UserEntity> iPage = this.page(page, queryWrapper);
        if (!iPage.getRecords().isEmpty()) {
            List<String> ids = iPage.getRecords().stream().map(m -> m.getId()).collect(Collectors.toList());
            queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(UserEntity::getId, ids);
            queryWrapper.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
            if (filterLastTime) {
                queryWrapper.lambda().orderByDesc(UserEntity::getLastModifyTime);
            }
            iPage.setRecords(this.list(queryWrapper));
        }
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    private List<UserEntity> getOrgUser(PaginationUser pagination, String organizeId, Integer enabledMark, String gender) {
        List<String> orgIdList = organizeMapper.getUnderOrganizationss(organizeId);
        orgIdList.add(organizeId);
        PageMethod.startPage((int) pagination.getCurrentPage(), (int) pagination.getPageSize(), false);
        //组织数量很多时解析SQL很慢, COUNT不解析SQL不去除ORDERBY
        PageMethod.getLocalPage().keepOrderBy(true);
        // 用户id
        List<String> query;
        String dbSchema = null;
        // 判断是否为多租户
        if (configValueUtil.isMultiTenancy() && DbBase.DM.equalsIgnoreCase(dataSourceUtil.getDbType())) {
            dbSchema = dataSourceUtil.getDbSchema();
        }
        String keyword = null;
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            keyword = "%" + pagination.getKeyword() + "%";
        }
        query = this.baseMapper.query(orgIdList, keyword, dbSchema, enabledMark, gender);
        Long count = this.baseMapper.count(orgIdList, keyword, dbSchema, enabledMark, gender);
        pagination.setTotal(count);
        if (!query.isEmpty()) {
            // 存放返回结果
            QueryWrapper<UserEntity> queryWrapper1 = new QueryWrapper<>();
            List<List<String>> lists = Lists.partition(query, 1000);
            queryWrapper1.lambda().and(t -> {
                for (List<String> id : lists) {
                    t.or().in(UserEntity::getId, id);
                }
            });
            queryWrapper1.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
            return getBaseMapper().selectList(queryWrapper1);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<UserEntity> getList(PageUser pagination, Boolean filterCurrentUser) {
        return this.baseMapper.getList(pagination, filterCurrentUser);
    }

    @Override
    public List<UserEntity> getUserPage(Pagination pagination) {
        return this.baseMapper.getUserPage(pagination);
    }

    @Override
    public List<UserEntity> getListByOrganizeId(String organizeId, String keyword) {
        List<String> userIds = userRelationMapper.getListByObjectId(organizeId, PermissionConst.ORGANIZE).stream()
                .map(UserRelationEntity::getUserId).collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            QueryWrapper<UserEntity> query = new QueryWrapper<>();
            query.lambda().in(UserEntity::getId, userIds);
            // 通过关键字查询
            if (StringUtil.isNotEmpty(keyword)) {
                query.lambda().and(
                        t -> t.like(UserEntity::getAccount, keyword)
                                .or().like(UserEntity::getRealName, keyword)
                );
            }
            // 只查询正常的用户
            query.lambda().ne(UserEntity::getEnabledMark, 0);
            query.lambda().orderByAsc(UserEntity::getSortCode).orderByDesc(UserEntity::getCreatorTime);
            return this.list(query);
        }
        return new ArrayList<>(0);
    }

    @Override
    public List<UserEntity> getListByManagerId(String managerId, String keyword) {
        return this.baseMapper.getListByManagerId(managerId, keyword);
    }

    @Override
    public UserEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public UserEntity getUserByAccount(String account) {
        return this.baseMapper.getUserByAccount(account);
    }

    @Override
    public UserEntity getUserByMobile(String mobile) {
        return this.baseMapper.getUserByMobile(mobile);
    }

    @Override
    public Boolean setAdminListByIds(List<String> adminIds) {
        return this.setAdminListByIds(adminIds);
    }

    @Override
    public boolean isExistByAccount(String account) {
        return this.baseMapper.isExistByAccount(account);
    }

    @Override
    @DSTransactional
    public Boolean create(UserEntity entity){
        beforeCheck();
        if (StringUtil.isNotEmpty(entity.getGroupId()) && entity.getGroupId().contains(",")) {
            entity.setGroupId(null);
        }
        //添加用户 初始化
        String userId = RandomUtil.uuId();
        if (StringUtil.isNotEmpty(entity.getId())) {
            userId = entity.getId();
        }
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        entity.setPassword(Md5Util.getStringMd5(sysInfo.getNewUserDefaultPassword()));
        entity.setId(userId);
        if (StringUtil.isEmpty(entity.getAccount())) {
            throw new DataException(MsgCode.PS007.get());
        }
        if (StringUtil.isEmpty(entity.getRealName())) {
            throw new DataException(MsgCode.PS008.get());
        }
        //获取头像
        String oldHeadIcon = entity.getHeadIcon();
        if (StringUtil.isEmpty(oldHeadIcon)) {
            entity.setHeadIcon("001.png");
        } else {
            //获取头像
            String[] headIcon = oldHeadIcon.split("/");
            if (headIcon.length > 0) {
                entity.setHeadIcon(headIcon[headIcon.length - 1]);
            }
        }
        entity.setSecretkey(RandomUtil.uuId());
        entity.setPassword(Md5Util.getStringMd5(entity.getPassword().toLowerCase() + entity.getSecretkey().toLowerCase()));
        entity.setIsAdministrator(0);
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        String groupId = entity.getGroupId();
        if (StringUtil.isNotEmpty(groupId)) {
            UserRelationEntity groupRelation = new UserRelationEntity();
            groupRelation.setId(RandomUtil.uuId());
            groupRelation.setObjectType(PermissionConst.GROUP);
            groupRelation.setObjectId(groupId);
            groupRelation.setUserId(entity.getId());
            groupRelation.setCreatorTime(entity.getCreatorTime());
            groupRelation.setCreatorUserId(entity.getCreatorUserId());
            userRelationMapper.insert(groupRelation);
        }
        //添加岗位关系
        if (StringUtil.isNotEmpty(entity.getPositionId())) {
            String[] split = entity.getPositionId().split(",");
            for (String s : split) {
                UserRelationEntity posRelation = new UserRelationEntity();
                posRelation.setId(RandomUtil.uuId());
                posRelation.setObjectType(PermissionConst.POSITION);
                posRelation.setObjectId(s);
                posRelation.setUserId(entity.getId());
                posRelation.setCreatorTime(entity.getCreatorTime());
                posRelation.setCreatorUserId(entity.getCreatorUserId());
                userRelationMapper.insert(posRelation);
            }
        }
        //添加组织关系
        if (StringUtil.isNotEmpty(entity.getOrganizeId())) {
            String[] split = entity.getOrganizeId().split(",");
            for (String s : split) {
                UserRelationEntity posRelation = new UserRelationEntity();
                posRelation.setId(RandomUtil.uuId());
                posRelation.setObjectType(PermissionConst.ORGANIZE);
                posRelation.setObjectId(s);
                posRelation.setUserId(entity.getId());
                posRelation.setCreatorTime(entity.getCreatorTime());
                posRelation.setCreatorUserId(entity.getCreatorUserId());
                userRelationMapper.insert(posRelation);
            }
        }
        //添加角色关系
        if (StringUtil.isNotEmpty(entity.getRoleId())) {
            String[] split = entity.getRoleId().split(",");
            for (String s : split) {
                RoleRelationEntity posRelation = new RoleRelationEntity();
                posRelation.setId(RandomUtil.uuId());
                posRelation.setObjectType(PermissionConst.USER);
                posRelation.setObjectId(entity.getId());
                posRelation.setRoleId(s);
                posRelation.setCreatorTime(entity.getCreatorTime());
                posRelation.setCreatorUserId(entity.getCreatorUserId());
                roleRelationMapper.insert(posRelation);
            }
        } else {
            //创建没有角色的时候，默认使用者
            RoleEntity byEnCode = roleMapper.getByEnCode(PermissionConst.USER_CODE);
            RoleRelationEntity posRelation = new RoleRelationEntity();
            posRelation.setId(RandomUtil.uuId());
            posRelation.setObjectType(PermissionConst.USER);
            posRelation.setObjectId(entity.getId());
            posRelation.setRoleId(byEnCode.getId());
            posRelation.setCreatorTime(entity.getCreatorTime());
            posRelation.setCreatorUserId(entity.getCreatorUserId());
            roleRelationMapper.insert(posRelation);
        }
        //写入时清空
        entity.setGroupId("");
        entity.setPositionId("");
        entity.setOrganizeId("");
        entity.setRoleId("");
        entity.setQuickQuery(PinYinUtil.getFirstSpell(entity.getRealName()));
        //清理获取所有用户的redis缓存
        redisUtil.remove(cacheKeyUtil.getAllUser());
        this.save(entity);
        return true;
    }

    /**
     * 验证是否还有额度
     */
    @Override
    public void beforeCheck() {
        String tenantId = UserProvider.getUser().getTenantId();
        // 开启多租住的
        if (StringUtil.isNotEmpty(tenantId)) {
            TenantVO cacheTenantInfo = TenantDataSourceUtil.getCacheTenantInfo(tenantId);
            long count = this.count();
            if (cacheTenantInfo.getAccountNum() != 0 && cacheTenantInfo.getAccountNum() < count) {
                throw new DataException(MsgCode.PS009.get());
            }
        }
    }

    @Override
    @DSTransactional
    public Boolean update(String userId, UserEntity entity){
        //更新用户
        entity.setId(userId);
        if (StringUtil.isEmpty(entity.getAccount())) {
            throw new DataException(MsgCode.PS007.get());
        }
        if (StringUtil.isEmpty(entity.getRealName())) {
            throw new DataException(MsgCode.PS008.get());
        }
        //获取头像
        String oldHeadIcon = entity.getHeadIcon();
        if (StringUtil.isEmpty(oldHeadIcon)) {
            entity.setHeadIcon("001.png");
        }
        entity.setLastModifyTime(DateUtil.getNowDate());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        //获取头像
        String[] headIcon = entity.getHeadIcon().split("/");
        if (headIcon.length > 0) {
            entity.setHeadIcon(headIcon[headIcon.length - 1]);
        }
        entity.setQuickQuery(PinYinUtil.getFirstSpell(entity.getRealName()));
        //清理获取所有用户的redis缓存
        redisUtil.remove(cacheKeyUtil.getAllUser());
        if (StringUtil.isNotEmpty(entity.getGroupId()) && entity.getGroupId().contains(",")) {
            entity.setGroupId(null);
        }
        this.updateById(entity);
        return true;
    }

    @Override
    @DSTransactional
    public void delete(UserEntity entity) {
        this.removeById(entity.getId());
        //删除用户关联
        userRelationMapper.deleteAllByUserId(Arrays.asList(entity.getId()));
        //删除用户关联角色
        roleRelationMapper.deleteAllByObjId(Arrays.asList(entity.getId()));
        //删除用户绑定关系
        socialsUserMapper.deleteAllByUserId(Arrays.asList(entity.getId()));
    }

    @Override
    @DSTransactional
    public void batchDelete(List<String> userIdList) {
        if (userIdList == null || userIdList.isEmpty()) {
            return;
        }
        this.baseMapper.deleteByIds(userIdList);
        //删除用户关联
        userRelationMapper.deleteAllByUserId(userIdList);
        //删除用户关联角色
        roleRelationMapper.deleteAllByObjId(userIdList);
        //删除用户绑定关系
        socialsUserMapper.deleteAllByUserId(userIdList);
    }

    @Override
    public void updatePassword(UserEntity entity) {
        entity.setSecretkey(RandomUtil.uuId());
        entity.setPassword(Md5Util.getStringMd5(entity.getPassword().toLowerCase() + entity.getSecretkey().toLowerCase()));
        entity.setChangePasswordDate(DateUtil.getNowDate());
        this.updateById(entity);

        //加入到旧密码记录表
        UserOldPasswordEntity userOldPasswordEntity = new UserOldPasswordEntity();
        userOldPasswordEntity.setOldPassword(entity.getPassword());
        userOldPasswordEntity.setSecretkey(entity.getSecretkey());
        userOldPasswordEntity.setUserId(entity.getId());
        userOldPasswordEntity.setAccount(entity.getAccount());
        userOldPasswordMapper.create(userOldPasswordEntity);
    }

    @Override
    public List<UserEntity> getUserName(List<String> id) {
        return this.baseMapper.getUserName(id);
    }

    /**
     * 查询用户名称
     *
     * @param id 主键值
     * @return
     */
    @Override
    public List<UserEntity> getUserName(List<String> id, boolean filterEnabledMark) {
        return this.baseMapper.getUserName(id, filterEnabledMark);
    }

    @Override
    public List<UserEntity> getListByUserIds(List<String> id) {
        return this.baseMapper.getListByUserIds(id);
    }

    @Override
    public List<UserEntity> getUserList(List<String> id) {
        return this.baseMapper.getUserList(id);
    }

    @Override
    public UserEntity getUserEntity(String account) {
        return this.baseMapper.getUserEntity(account);
    }

    @Override
    public List<String> getListId() {
        return this.baseMapper.getListId();
    }

    @Override
    public void update(UserEntity entity, String type) {
        this.baseMapper.update(entity, type);
    }

    @Override
    public void getOrganizeIdTree(String organizeId, StringBuilder organizeParentIdList) {
        OrganizeEntity entity = organizeMapper.getInfo(organizeId);
        if (Objects.nonNull(entity) && StringUtil.isNotEmpty(entity.getParentId())) {
            // 记录id
            organizeParentIdList.append(organizeId).append(",");
            getOrganizeIdTree(entity.getParentId(), organizeParentIdList);
        }
    }

    @Override
    public List<UserEntity> getUserName(List<String> id, Pagination pagination) {
        return this.baseMapper.getUserName(id, pagination);
    }

    @Override
    public List<UserEntity> getUserNames(List<String> id, PaginationUser pagination, Boolean flag, Boolean enabledMark) {
        return this.baseMapper.getUserNames(id, pagination, flag, enabledMark);
    }

    @Override
    public List<String> getFullNameByIds(List<String> ids) {
        List<String> list = new ArrayList<>();
        if (ids != null) {
            ids.forEach(selectedId -> {
                if (StringUtil.isNotEmpty(selectedId)) {
                    String[] split = selectedId.split("--");
                    // 截取type后获取详情
                    if (split.length > 1) {
                        String type = split[1];
                        if (PermissionConst.COMPANY.equalsIgnoreCase(type) || PermissionConst.DEPARTMENT.equalsIgnoreCase(type)) {
                            OrganizeEntity organizeEntity = organizeMapper.getInfo(split[0]);
                            if (organizeEntity != null) {
                                list.add(organizeEntity.getFullName());
                            }
                        } else if (PermissionConst.ROLE.equalsIgnoreCase(type)) {
                            RoleEntity roleEntity = roleMapper.getInfo(split[0]);
                            if (roleEntity != null) {
                                list.add(roleEntity.getFullName());
                            }
                        } else if (PermissionConst.POSITION.equalsIgnoreCase(type)) {
                            PositionEntity positionEntity = positionMapper.getInfo(split[0]);
                            if (positionEntity != null) {
                                list.add(positionEntity.getFullName());
                            }
                        } else if (PermissionConst.GROUP.equalsIgnoreCase(type)) {
                            GroupEntity groupEntity = groupMapper.getInfo(split[0]);
                            if (groupEntity != null) {
                                list.add(groupEntity.getFullName());
                            }
                        } else if ("user".equalsIgnoreCase(type)) {
                            UserEntity userEntity = this.getInfo(split[0]);
                            if (userEntity != null) {
                                list.add(userEntity.getRealName());
                            }
                        }
                    } else {
                        UserEntity userEntity = this.getInfo(split[0]);
                        if (userEntity != null) {
                            list.add(userEntity.getRealName());
                        }
                    }
                }
            });
        }
        return list;
    }

    @Override
    public List<BaseInfoVo> selectedByIds(List<String> ids) {
        List<BaseInfoVo> list = new ArrayList<>();
        List<OrganizeEntity> organizeEntityList = organizeMapper.getList(true);
        Map<String, Object> allOrgsTreeName = organizeMapper.getAllOrgsTreeName();
        List<OrganizeSelectorVO> organizeSelectorList = JsonUtil.getJsonToList(organizeEntityList, OrganizeSelectorVO.class);
        for (OrganizeSelectorVO item : organizeSelectorList) {
            if (PermissionConst.DEPARTMENT.equals(item.getCategory())) {
                item.setIcon(PermissionConst.DEPARTMENT_ICON);
            } else {
                item.setIcon(PermissionConst.COMPANY_ICON);
            }

            item.setFullName(item.getOrgNameTree());

            if (StringUtil.isNotEmpty(item.getParentId()) && Objects.nonNull(allOrgsTreeName.get(item.getParentId()))) {
                item.setParentName(allOrgsTreeName.get(item.getParentId()).toString());
            }
            String[] orgs = item.getOrganizeIdTree().split(",");
            item.setOrganizeIds(Arrays.asList(orgs));

        }
        if (ids != null) {
            Map<String, String> orgIdNameMaps = organizeMapper.getInfoList();
            ids.forEach(selectedId -> {
                if (StringUtil.isNotEmpty(selectedId)) {
                    // 判断是否为系统参数
                    if (JnpfConst.SYSTEM_PARAM.containsKey(selectedId)) {
                        UserIdListVo vo = new UserIdListVo();
                        vo.setId(selectedId);
                        vo.setFullName(JnpfConst.SYSTEM_PARAM.get(selectedId));
                    }
                    //组织
                    String[] split = selectedId.split("--");

                    // 截取type后获取详情
                    if (split.length > 1) {
                        String type = split[1];
                        if (PermissionConst.COMPANY.equalsIgnoreCase(type) || PermissionConst.DEPARTMENT.equalsIgnoreCase(type)) {
                            OrganizeEntity organizeEntity = organizeMapper.getInfo(split[0]);
                            if (organizeEntity != null) {
                                BaseInfoVo vo = JsonUtil.getJsonToBean(organizeEntity, BaseInfoVo.class);
                                if ("department".equals(organizeEntity.getCategory())) {
                                    vo.setIcon(PermissionConst.DEPARTMENT_ICON);
                                } else if ("company".equals(organizeEntity.getCategory())) {
                                    vo.setIcon(PermissionConst.COMPANY_ICON);
                                }
                                vo.setOrganize(organizeMapper.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/"));
                                vo.setOrganizeIds(organizeMapper.getOrgIdTree(organizeEntity));
                                vo.setType(organizeEntity.getCategory());
                                list.add(vo);
                            }
                        } else if (PermissionConst.ROLE.equalsIgnoreCase(type)) {
                            RoleEntity roleEntity = roleMapper.getInfo(split[0]);
                            if (roleEntity != null) {
                                BaseInfoVo vo = JsonUtil.getJsonToBean(roleEntity, BaseInfoVo.class);
                                List<RoleRelationEntity> relationListByRoleId = roleRelationMapper.getListByRoleId(vo.getId(), null);
                                StringJoiner orgName = new StringJoiner(",");
                                relationListByRoleId.forEach(item -> orgName.add(item.getObjectId()));
                                vo.setId(selectedId);
                                vo.setOrganize(orgName.toString());
                                vo.setType(SysParamEnum.ROLE.getCode());
                                vo.setIcon(PermissionConst.ROLE_ICON);
                                vo.setOrgNameTree(vo.getFullName());
                                list.add(vo);
                            }
                        } else if (SysParamEnum.POS.getCode().equalsIgnoreCase(type) ||
                                SysParamEnum.SUBPOS.getCode().equalsIgnoreCase(type) ||
                                SysParamEnum.PROGENYPOS.getCode().equalsIgnoreCase(type)) {
                            PositionEntity positionEntity = positionMapper.getInfo(split[0]);
                            if (positionEntity != null) {
                                BaseInfoVo vo = JsonUtil.getJsonToBean(positionEntity, BaseInfoVo.class);
                                vo.setId(selectedId);
                                OrganizeEntity info = organizeMapper.getInfo(positionEntity.getOrganizeId());
                                String orgName = "";
                                if (info != null) {
                                    vo.setOrganize(organizeMapper.getFullNameByOrgIdTree(orgIdNameMaps, info.getOrganizeIdTree(), "/"));
                                    orgName = vo.getOrganize();
                                }
                                vo.setType(SysParamEnum.get(type).getCode());
                                vo.setIcon(PermissionConst.POSITION_ICON);
                                vo.setOrgNameTree(orgName + "/" + positionEntity.getFullName() + SysParamEnum.get(type).getSuffix());
                                list.add(vo);
                            }
                        } else if (SysParamEnum.GROUP.getCode().equalsIgnoreCase(type)) {
                            GroupEntity groupEntity = groupMapper.getInfo(split[0]);
                            if (groupEntity != null) {

                                BaseInfoVo vo = JsonUtil.getJsonToBean(groupEntity, BaseInfoVo.class);
                                vo.setIcon(PermissionConst.GROUP_ICON);
                                vo.setId(selectedId);
                                vo.setType(SysParamEnum.GROUP.getCode());
                                vo.setOrgNameTree(groupEntity.getFullName() + SysParamEnum.get(type).getSuffix());
                                list.add(vo);
                            }
                        } else if (SysParamEnum.USER.getCode().equalsIgnoreCase(type)) {
                            UserEntity userEntity = this.getInfo(split[0]);
                            if (userEntity != null) {
                                BaseInfoVo vo = JsonUtil.getJsonToBean(userEntity, BaseInfoVo.class);
                                List<UserRelationEntity> listByObjectType = userRelationMapper.getListByObjectType(userEntity.getId(), PermissionConst.ORGANIZE);
                                StringJoiner orgName = new StringJoiner(",");
                                listByObjectType.forEach(userRelationEntity -> {
                                    OrganizeEntity info = organizeMapper.getInfo(userRelationEntity.getObjectId());
                                    if (info != null) {
                                        String fullNameByOrgIdTree = organizeMapper.getFullNameByOrgIdTree(orgIdNameMaps, info.getOrganizeIdTree(), "/");
                                        orgName.add(fullNameByOrgIdTree);
                                    }
                                });
                                vo.setId(selectedId);
                                vo.setOrganize(orgName.toString());
                                vo.setType(SysParamEnum.USER.getCode());
                                vo.setHeadIcon(UploaderUtil.uploaderImg(vo.getHeadIcon()));
                                vo.setFullName(vo.getRealName() + "/" + vo.getAccount());
                                vo.setOrgNameTree(vo.getFullName());
                                list.add(vo);
                            }
                        } else {
                            BaseInfoVo vo = new BaseInfoVo();
                            List<OrganizeSelectorVO> collect = organizeSelectorList.stream()
                                    .filter(it -> split[0].equals(it.getId()))
                                    .collect(Collectors.toList());
                            if (!collect.isEmpty()) {

                                vo = BeanUtil.copyProperties(collect.get(0), BaseInfoVo.class);
                                String id = split[0];
                                SysParamEnum sysParamEnum = SysParamEnum.get(type);
                                String suffix = sysParamEnum != null ? sysParamEnum.getSuffix() : "";
                                vo.setId(selectedId);
                                vo.setOrgNameTree(allOrgsTreeName.get(id) + suffix);

                            }


                            vo.setId(selectedId);
                            vo.setFullName(SysParamEnum.get(split[1]).getName());
                            vo.setRealName(orgIdNameMaps.get(split[0]) + "\\" + vo.getFullName());
                            vo.setType(split[1]);
                            list.add(vo);
                        }
                    } else {
                        UserEntity userEntity = this.getInfo(split[0]);
                        if (userEntity != null) {
                            extracted(userEntity, orgIdNameMaps, list, userEntity.getRealName() + "/" + userEntity.getAccount());
                        }
                        if (selectedId.equals(DataInterfaceVarConst.USER)) {
                            UserInfo user = UserProvider.getUser();
                            userEntity = this.getInfo(user.getUserId());
                            userEntity.setId(DataInterfaceVarConst.USER);
                            String userName = "当前用户";
                            extracted(userEntity, orgIdNameMaps, list, userName);
                        }

                    }
                }
            });
        }
        return list;
    }


    private void extracted(UserEntity userEntity, Map<String, String> orgIdNameMaps, List<BaseInfoVo> list, String userName) {
        BaseInfoVo vo = JsonUtil.getJsonToBean(userEntity, BaseInfoVo.class);
        List<UserRelationEntity> listByObjectType = userRelationMapper.getListByObjectType(userEntity.getId(), PermissionConst.ORGANIZE);
        StringJoiner orgName = new StringJoiner(",");
        listByObjectType.forEach(userRelationEntity -> {
            OrganizeEntity info = organizeMapper.getInfo(userRelationEntity.getObjectId());
            if (info != null) {
                String fullNameByOrgIdTree = organizeMapper.getFullNameByOrgIdTree(orgIdNameMaps, info.getOrganizeIdTree(), "/");
                orgName.add(fullNameByOrgIdTree);
            }
        });


        vo.setOrganize(orgName.toString());
        vo.setType("user");
        vo.setHeadIcon(UploaderUtil.uploaderImg(vo.getHeadIcon()));
        vo.setFullName(vo.getRealName() + "/" + vo.getAccount());
        vo.setOrgNameTree(userName);
        list.add(vo);
    }


    @Override
    public Boolean delCurRoleUser(String message, List<String> objectIdAll) {
        List<PermissionGroupEntity> list = permissionGroupMapper.list(objectIdAll).stream().filter(t -> Objects.equals(t.getEnabledMark(), 1)).collect(Collectors.toList());
        List<String> member = list.stream().filter(t -> StringUtil.isNotEmpty(t.getPermissionMember())).map(PermissionGroupEntity::getPermissionMember).collect(Collectors.toList());
        List<String> userIdList = new ArrayList<>();
        if (list.stream().filter(t -> Objects.equals(t.getType(), 0)).count() > 0) {
            userIdList.addAll(getUserMap().keySet());
        } else {
            // 判断角色下面的人
            userIdList.addAll(this.getUserIdList(member));
        }
        delCurUser(message, userIdList);
        return true;
    }

    @Override
    public List<UserEntity> getList(List<String> orgIdList, String keyword) {
        // 得到用户关系表
        List<UserRelationEntity> listByObjectId = userRelationMapper.getListByOrgId(orgIdList);
        if (listByObjectId.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(UserEntity::getId, listByObjectId.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList())).and(
                t -> t.like(UserEntity::getRealName, keyword)
                        .or().like(UserEntity::getAccount, keyword)
        );

        return this.list(queryWrapper);
    }

    @Override
    public List<UserEntity> getListBySyn(List<String> orgIdList, String keyword) {
        // 得到用户关系表
        List<UserRelationEntity> listByObjectId = userRelationMapper.getListByOrgId(orgIdList);
        //根据userId分类
        Map<String, List<UserRelationEntity>> collect = listByObjectId.stream().collect(Collectors.groupingBy(UserRelationEntity::getUserId));
        if (listByObjectId.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(UserEntity::getId, listByObjectId.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList())).and(
                t -> t.like(UserEntity::getRealName, keyword)
                        .or().like(UserEntity::getAccount, keyword)
        );
        List<UserEntity> list = this.list(queryWrapper);
        ArrayList<UserEntity> userEntities = new ArrayList<>();
        for (UserEntity userEntity : list) {
            List<UserRelationEntity> userRelationEntities = collect.get(userEntity.getId());
            for (UserRelationEntity userRelationEntity : userRelationEntities) {
                UserEntity entity = BeanUtil.copyProperties(userEntity, UserEntity.class);
                entity.setOrganizeId(userRelationEntity.getObjectId());
                userEntities.add(entity);
            }
        }
        return userEntities;
    }

    @Override
    public List<String> getUserIdList(List<String> idList) {
        Set<String> userIds = new LinkedHashSet<>();
        idList.forEach(selectedId -> {
            if (StringUtil.isNotEmpty(selectedId)) {
                if (selectedId.equals(DataInterfaceVarConst.USER)) {
                    userIds.add(UserProvider.getUser().getUserId());
                    return;
                }

                String[] split = selectedId.split("--");

                if (Arrays.stream(split).count() == 1) {
                    UserEntity info = this.baseMapper.getInfo(selectedId);
                    if (BeanUtil.isNotEmpty(info)) {
                        userIds.add(info.getId());
                    }
                    return;
                }

                if (selectedId.substring(selectedId.length() - 3).equalsIgnoreCase(SysParamEnum.ORG.getCode())) {
                    ArrayList<String> strings = new ArrayList<>();
                    if (selectedId.contains(SysParamEnum.SUBORG.getCode())) {
                        List<OrganizeEntity> listByParentId = organizeMapper.getListByParentId(split[0]);
                        if (listByParentId != null) {
                            strings.addAll(listByParentId.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                        }
                    }
                    if (selectedId.contains(SysParamEnum.PROGENYORG.getCode())) {
                        List<OrganizeEntity> grandSonList = organizeMapper.getAllChild(split[0]);
                        if (grandSonList != null) {
                            strings.addAll(grandSonList.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                        }
                    }
                    strings.add(split[0]);
                    List<PositionEntity> listByOrgIds = positionMapper.getListByOrgIds(strings.stream()
                            .filter(Objects::nonNull).collect(Collectors.toList()));
                    List<String> positionIds = listByOrgIds.stream().map(PositionEntity::getId).collect(Collectors.toList());
                    List<UserRelationEntity> listByObjectIdAll = userRelationMapper.getListByObjectIdAll(positionIds);
                    userIds.addAll(listByObjectIdAll.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                } else if (selectedId.substring(selectedId.length() - 3).equalsIgnoreCase(SysParamEnum.POS.getCode())) {
                    ArrayList<String> strings = new ArrayList<>();
                    strings.add(split[0]);
                    PositionEntity info = positionMapper.getInfo(split[0]);
                    if (info != null && selectedId.contains(SysParamEnum.SUBPOS.getCode())) {
                        List<PositionEntity> byParentId = positionMapper.getByParentId(info.getId());
                        if (null != byParentId && !byParentId.isEmpty()) {
                            strings.addAll(byParentId.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                        }
                    }
                    if (info != null && selectedId.contains(SysParamEnum.PROGENYPOS.getCode())) {
                        List<PositionEntity> grandSonList = positionMapper.getAllChild(info.getId());
                        if (null != grandSonList) {
                            strings.addAll(grandSonList.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                        }
                    }
                    List<String> collect = userRelationMapper.getListByObjectIdAll(strings)
                            .stream()
                            .map(UserRelationEntity::getUserId)
                            .collect(Collectors.toList());
                    userIds.addAll(collect);

                } else if (selectedId.contains(SysParamEnum.GROUP.getCode())) {
                    List<String> collect = userRelationMapper.getListByObjectId(split[0])
                            .stream()
                            .map(UserRelationEntity::getUserId)
                            .collect(Collectors.toList());
                    userIds.addAll(collect);
                } else if (selectedId.contains(SysParamEnum.ROLE.getCode())) {
                    List<String> collect = roleRelationMapper.getListByRoleId(split[0], PermissionConst.USER)
                            .stream()
                            .map(RoleRelationEntity::getObjectId)
                            .collect(Collectors.toList());
                    userIds.addAll(collect);
                } else if (selectedId.contains(SysParamEnum.USER.getCode())) {
                    UserEntity info = this.baseMapper.getInfo(split[0]);
                    if (BeanUtil.isNotEmpty(info)) {
                        userIds.add(info.getId());
                    }
                }
            }
        });
        return new ArrayList<>(userIds);
    }

    @Override
    public List<String> getRelUserEnable(List<String> idList) {
        List<String> userIdList = this.getUserIdList(idList);
        List<UserEntity> listByUserIds = this.getListByUserIds(userIdList);
        return listByUserIds.stream().filter(t -> !Objects.equals(t.getEnabledMark(), 0)).map(UserEntity::getId).collect(Collectors.toList());
    }

    @Override
    public List<BaseInfoVo> getObjList(List<String> userIds, PaginationUser pagination) {
        // 得到所有的用户id关系
        Map<String, String> orgIdNameMaps = organizeMapper.getInfoList();
        List<UserEntity> userEntityList = getUserIdListByOrganize(userIds, pagination);
        if (userEntityList.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserIdListVo> jsonToList = JsonUtil.getJsonToList(userEntityList, UserIdListVo.class);
        jsonToList.forEach(userIdListVo -> {
            List<UserRelationEntity> listByObjectType = userRelationMapper.getListByObjectType(userIdListVo.getId(), PermissionConst.ORGANIZE);
            StringJoiner orgName = new StringJoiner(",");
            listByObjectType.forEach(userRelationEntity -> {
                OrganizeEntity info = organizeMapper.getInfo(userRelationEntity.getObjectId());
                if (info != null) {
                    String fullNameByOrgIdTree = organizeMapper.getFullNameByOrgIdTree(orgIdNameMaps, info.getOrganizeIdTree(), "/");
                    orgName.add(fullNameByOrgIdTree);
                }
            });
            userIdListVo.setOrganize(orgName.toString());
            userIdListVo.setType(SysParamEnum.USER.getCode());

            userIdListVo.setFullName(userIdListVo.getRealName() + "/" + userIdListVo.getAccount());
            userIdListVo.setHeadIcon(UploaderUtil.uploaderImg(userIdListVo.getHeadIcon()));
        });

        return JsonUtil.getJsonToList(jsonToList, BaseInfoVo.class);
    }

    private List<UserEntity> getUserIdListByOrganize(List<String> userIds, PaginationUser pagination) {
        List<String> userObjectIds = getUserIdList(userIds);
        if (userObjectIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(UserEntity::getAccount, ADMIN_KEY);
        wrapper.ne(UserEntity::getEnabledMark, 0);
        //idList范围过滤
        if (userObjectIds.size() > 1000) {
            List<List<String>> lists = Lists.partition(userObjectIds, 1000);
            wrapper.and(t -> {
                for (List<String> item : lists) {
                    t.in(UserEntity::getId, item).or();
                }
            });
        } else {
            wrapper.in(UserEntity::getId, userObjectIds);
        }
        //关键字
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            wrapper.and(
                    t -> t.like(UserEntity::getRealName, pagination.getKeyword())
                            .or().like(UserEntity::getAccount, pagination.getKeyword())
                            .or().like(UserEntity::getMobilePhone, pagination.getKeyword())
            );
        }
        wrapper.orderByAsc(UserEntity::getId);
        Page<UserEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<UserEntity> iPage = this.baseMapper.selectPage(page, wrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    /**
     * 查询给定的条件是否有默认当前登录者的默认用户值
     *
     * @param userConditionModel
     * @return
     */
    @Override
    public String getDefaultCurrentValueUserId(UserConditionModel userConditionModel) {
        UserInfo userInfo = UserProvider.getUser();
        int currentFinded = 0;
        if (userConditionModel.getUserIds() != null && !userConditionModel.getUserIds().isEmpty() && userConditionModel.getUserIds().contains(userInfo.getUserId())) {
            currentFinded = 1;
        }
        if (currentFinded == 0 && userConditionModel.getDepartIds() != null && !userConditionModel.getDepartIds().isEmpty()) {
            List<OrganizeEntity> orgList = organizeMapper.getOrgEntityList(userConditionModel.getDepartIds(), true);
            List<String> orgLIdList = orgList.stream().map(OrganizeEntity::getId).collect(Collectors.toList());
            if (orgLIdList != null && !orgLIdList.isEmpty()) {
                List<String> userIds = userRelationMapper.getListByObjectIdAll(orgLIdList).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
                if (userIds != null && !userIds.isEmpty() && userIds.contains(userInfo.getUserId())) {
                    currentFinded = 1;
                }
            }
        }
        if (currentFinded == 0 && userConditionModel.getRoleIds() != null && !userConditionModel.getRoleIds().isEmpty()) {
            List<RoleEntity> roleList = roleMapper.getListByIds(userConditionModel.getRoleIds(), null, false);
            List<String> roleIdList = roleList.stream().filter(t -> t.getEnabledMark() == 1).map(RoleEntity::getId).collect(Collectors.toList());
            if (roleIdList != null && !roleIdList.isEmpty()) {
                List<String> userIds = userRelationMapper.getListByObjectIdAll(roleIdList).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
                if (userIds != null && !userIds.isEmpty() && userIds.contains(userInfo.getUserId())) {
                    currentFinded = 1;
                }
            }
        }
        if (currentFinded == 0 && userConditionModel.getPositionIds() != null && !userConditionModel.getPositionIds().isEmpty()) {
            List<PositionEntity> positionList = positionMapper.getPosList(userConditionModel.getPositionIds());
            List<String> positionIdList = positionList.stream().filter(t -> t.getEnabledMark() == 1).map(PositionEntity::getId).collect(Collectors.toList());
            if (positionIdList != null && !positionIdList.isEmpty()) {
                List<String> userIds = userRelationMapper.getListByObjectIdAll(positionIdList).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
                if (userIds != null && !userIds.isEmpty() && userIds.contains(userInfo.getUserId())) {
                    currentFinded = 1;
                }
            }
        }
        if (currentFinded == 0 && userConditionModel.getGroupIds() != null && !userConditionModel.getGroupIds().isEmpty()) {
            List<GroupEntity> groupList = groupMapper.getListByIds(userConditionModel.getGroupIds());
            List<String> groupIdList = groupList.stream().map(GroupEntity::getId).collect(Collectors.toList());
            if (groupIdList != null && !groupIdList.isEmpty()) {
                List<String> userIds = userRelationMapper.getListByObjectIdAll(groupIdList).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
                if (userIds != null && !userIds.isEmpty() && userIds.contains(userInfo.getUserId())) {
                    currentFinded = 1;
                }
            }
        }
        return (currentFinded == 1) ? userInfo.getUserId() : "";
    }

    @Override
    public List<UserEntity> getUserAccount(List<String> ids) {
        return this.baseMapper.getUserAccount(ids);
    }

    @Override
    public void updateStand(List<String> ids, int standing) {
        this.baseMapper.updateStand(ids, standing);
    }

    @Override
    public void delCurUser(String message, List<String> userIds) {
        userUtil.delCurUser(message, userIds);
    }

    @Override
    public void majorStandFreshUser() {
        userUtil.majorStandFreshUser();
    }

    @Override
    public Boolean logoutUser(String message, List<String> userIds) {
        return userUtil.logoutUser(message, userIds);
    }

    @Override
    public List<UserEntity> getPageByIds(RoleRelationPage pagination) {
        return this.baseMapper.getPageByIds(pagination);
    }

    @Override
    public UserRelationIds getUserObjectIdList(String userId) {
        List<UserRelationEntity> listUser = userRelationMapper.getListByObjectType(userId, null);
        List<String> group = listUser.stream().filter(t -> PermissionConst.GROUP.equals(t.getObjectType())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        List<String> organize = listUser.stream().filter(t -> PermissionConst.ORGANIZE.equals(t.getObjectType())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        List<String> position = listUser.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        List<String> rogAndPos = listUser.stream().filter(t -> PermissionConst.ORGANIZE.equals(t.getObjectType()) || PermissionConst.POSITION.equals(t.getObjectType()))
                .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        rogAndPos.add(userId);
        List<String> role = roleRelationMapper.getListByObjectId(rogAndPos, null).stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        return UserRelationIds.builder()
                .group(group)
                .organize(organize)
                .position(position)
                .role(role)
                .build();
    }

    @Override
    public Map<String, String> getSystemFieldValue(SystemParamModel model) {
        Map<String, String> mapList = new HashMap<>();
        List<String> needList = model.getList();
        UserInfo userInfo = UserProvider.getUser();
        String userId = userInfo.getUserId();
        List<String> organizeIds = userInfo.getOrganizeIds() != null ? userInfo.getOrganizeIds() : new ArrayList<>();
        List<String> positionIds = userInfo.getPositionIds() != null ? userInfo.getPositionIds() : new ArrayList<>();

        for (String key : needList) {
            String values = getValues(key, userId, organizeIds, positionIds);
            mapList.put(key, values);
        }
        return mapList;
    }

    private String getValues(String key, String userId, List<String> organizeIds, List<String> positionIds) {
        List<String> dataValue = new ArrayList<>();
        String values = "";
        switch (key) {
            case DataInterfaceVarConst.CURRENTTIME:
                values = String.valueOf(DateUtil.stringToDate(DateUtil.getNow()).getTime());
                break;
            case DataInterfaceVarConst.USER:
                values = userId;
                break;
            case DataInterfaceVarConst.ORG:
                values = JsonUtil.getObjectToString(organizeIds);
                break;
            case DataInterfaceVarConst.POSITIONID:
                values = JsonUtil.getObjectToString(positionIds);
                break;
            case DataInterfaceVarConst.USERANDSUB:
                dataValue.add(userId);
                if (!positionIds.isEmpty()) {
                    dataValue.addAll(userUtil.getUserAndSub(positionIds).stream()
                            .map(UserEntity::getId).collect(Collectors.toList()));
                }
                values = JsonUtil.getObjectToString(dataValue);
                break;
            case DataInterfaceVarConst.USERANDPROGENY:
                dataValue.add(userId);
                if (!positionIds.isEmpty()) {
                    dataValue.addAll(userUtil.getUserProgeny(positionIds, null).stream()
                            .map(UserEntity::getId).collect(Collectors.toList()));
                }
                values = JsonUtil.getObjectToString(dataValue);
                break;
            case DataInterfaceVarConst.ORGANDSUB:
                if (!organizeIds.isEmpty()) {
                    List<OrganizeEntity> listByParentIds = organizeMapper.getListByParentIds(organizeIds);
                    dataValue.addAll(organizeIds);
                    dataValue.addAll(listByParentIds.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                }
                values = JsonUtil.getObjectToString(dataValue);
                break;
            case DataInterfaceVarConst.ORGANIZEANDPROGENY:
                if (!organizeIds.isEmpty()) {
                    List<OrganizeEntity> allChild = organizeMapper.getProgeny(organizeIds, null);
                    dataValue.addAll(allChild.stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
                }
                values = JsonUtil.getObjectToString(dataValue);
                break;
            case DataInterfaceVarConst.POSITIONANDSUB:
                if (!positionIds.isEmpty()) {
                    List<PositionEntity> listByParentIds = positionMapper.getListByParentIds(positionIds);
                    dataValue.addAll(positionIds);
                    dataValue.addAll(listByParentIds.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                }
                values = JsonUtil.getObjectToString(dataValue);
                break;
            case DataInterfaceVarConst.POSITIONANDPROGENY:
                if (!positionIds.isEmpty()) {
                    List<PositionEntity> allChild = positionMapper.getProgeny(positionIds, null);
                    dataValue.addAll(allChild.stream().map(PositionEntity::getId).collect(Collectors.toList()));
                }
                values = JsonUtil.getObjectToString(dataValue);
                break;
            default:
                break;
        }
        return values;
    }

    @Override
    public List<UserEntity> pageUser(UserSystemCountModel userSystemCountModel) {
        return this.baseMapper.pageUser(userSystemCountModel);
    }
}
