package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SysconfigService;
import jnpf.base.service.SystemService;
import jnpf.base.util.ExcelTool;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.flowable.enums.ExtraRuleEnum;
import jnpf.message.service.SynThirdDingTalkService;
import jnpf.message.service.SynThirdQyService;
import jnpf.model.*;
import jnpf.model.tenant.AdminInfoVO;
import jnpf.model.tenant.TenantReSetPasswordForm;
import jnpf.model.tenant.TenantVO;
import jnpf.permission.constant.UserColumnMap;
import jnpf.permission.entity.*;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.position.PositionListVO;
import jnpf.permission.model.position.PositionVo;
import jnpf.permission.model.standing.StandingModel;
import jnpf.permission.model.user.UserAuthForm;
import jnpf.permission.model.user.UserIdListVo;
import jnpf.permission.model.user.WorkHandoverModel;
import jnpf.permission.model.user.form.UserBatchForm;
import jnpf.permission.model.user.form.UserCrForm;
import jnpf.permission.model.user.form.UserResetPasswordForm;
import jnpf.permission.model.user.form.UserUpForm;
import jnpf.permission.model.user.mod.UserConditionModel;
import jnpf.permission.model.user.mod.UserIdModel;
import jnpf.permission.model.user.mod.UserIdModelByPage;
import jnpf.permission.model.user.mod.UsersByPositionModel;
import jnpf.permission.model.user.page.PageUser;
import jnpf.permission.model.user.page.PaginationUser;
import jnpf.permission.model.user.page.UserPagination;
import jnpf.permission.model.user.vo.*;
import jnpf.permission.rest.PullUserUtil;
import jnpf.permission.service.*;
import jnpf.permission.util.AuthPermUtil;
import jnpf.permission.util.PermissionUtil;
import jnpf.service.AuthService;
import jnpf.util.*;
import jnpf.util.enums.DictionaryDataEnum;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static jnpf.util.Constants.ADMIN_KEY;

/**
 * 用户管理
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "用户管理", description = "Users")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Users")
public class UserController extends SuperController<UserService, UserEntity> {

    private final CacheKeyUtil cacheKeyUtil;
    private final SynThirdQyService synThirdQyService;
    private final SynThirdDingTalkService synThirdDingTalkService;
    private final UserService userService;
    private final OrganizeService organizeService;
    private final RedisUtil redisUtil;
    private final UserRelationService userRelationService;
    private final RoleRelationService roleRelationService;
    private final RoleService roleService;
    private final PositionService positionService;
    private final GroupService groupService;
    private final TemplateApi templateApi;
    private final AuthService authService;
    private final DictionaryDataService dictionaryDataApi;
    private final ConfigValueUtil configValueUtil;
    private final SysconfigService sysconfigApi;
    private final AuthPermUtil authPermUtil;
    private final SystemService systemService;
    private final StandingService standingService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    @SaCheckPermission(value = {"permission.organize", "permission.user", "permission.role"}, mode = SaMode.OR)
    public ActionResult<PageListVO<UserListVO>> getList(UserPagination pagination) {
        List<UserEntity> userList = userService.getList(pagination);
        List<UserListVO> list = new ArrayList<>();
        List<DictionaryDataEntity> dataServiceList4 = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.SEX_TYPE.getDictionaryTypeId());
        Map<String, String> genderMap = dataServiceList4.stream().collect(Collectors.toMap(DictionaryDataEntity::getEnCode, DictionaryDataEntity::getFullName));
        Map<String, String> positionMap = positionService.getPosFullNameMap();
        //责任人（有岗位信息时添加属性）
        String dutyUser = "";
        if (StringUtil.isNotEmpty(pagination.getPositionId())) {
            PositionEntity info = positionService.getInfo(pagination.getPositionId());
            if (info != null) dutyUser = info.getDutyUser();
        }
        for (UserEntity userEntity : userList) {
            UserListVO userVO = JsonUtil.getJsonToBean(userEntity, UserListVO.class);
            userVO.setFullName(userVO.getRealName() + "/" + userVO.getAccount());
            userVO.setHandoverMark(userEntity.getHandoverMark() == null ? 0 : userEntity.getHandoverMark());
            userVO.setHeadIcon(UploaderUtil.uploaderImg(userVO.getHeadIcon()));
            // 时间小于当前时间则判断已解锁
            if (userVO.getEnabledMark() != null && userVO.getEnabledMark() != 0) {
                if (Objects.nonNull(userEntity.getUnlockTime()) && userEntity.getUnlockTime().getTime() > System.currentTimeMillis()) {
                    userVO.setEnabledMark(2);
                } else if (Objects.nonNull(userEntity.getUnlockTime()) && userEntity.getUnlockTime().getTime() < System.currentTimeMillis()) {
                    userVO.setEnabledMark(1);
                }
            }
            userVO.setGender(genderMap.get(userEntity.getGender()));
            StringJoiner positionJoiner = new StringJoiner(",");
            StringJoiner organizeJoiner = new StringJoiner(",");
            List<UserRelationEntity> allPostion = userRelationService.getListByObjectType(userEntity.getId(), PermissionConst.POSITION);
            if (CollUtil.isNotEmpty(allPostion)) {
                for (UserRelationEntity item : allPostion) {
                    String posName = positionMap.get(item.getObjectId());
                    if (posName != null) {
                        positionJoiner.add(posName);
                        organizeJoiner.add(posName.substring(0, posName.lastIndexOf("/")));
                    }

                }

            }
            userVO.setPosition(positionJoiner.toString());
            userVO.setOrganize(organizeJoiner.toString());
            if (StringUtil.isNotEmpty(dutyUser) && dutyUser.equals(userEntity.getId())) {
                userVO.setIsDutyUser(1);
            }
            list.add(userVO);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    @Operation(summary = "详情")
    @Parameter(name = "id", description = "用户id", required = true)
    @SaCheckPermission("permission.user")
    @GetMapping("/{id}")
    public ActionResult<UserInfoVO> getInfo(@PathVariable("id") String id) throws DataException {
        UserEntity entity = userService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }

        QueryWrapper<UserRelationEntity> roleQuery = new QueryWrapper<>();
        roleQuery.lambda().eq(UserRelationEntity::getUserId, id);
        roleQuery.lambda().eq(UserRelationEntity::getObjectType, PermissionConst.ROLE);
        List<String> roleIdList = new ArrayList<>();
        for (UserRelationEntity ure : userRelationService.list(roleQuery)) {
            roleIdList.add(ure.getObjectId());
        }

        entity.setHeadIcon(UploaderUtil.uploaderImg(entity.getHeadIcon()));
        // 得到组织树
        UserInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, UserInfoVO.class);
        vo.setRoleId(String.join(",", roleIdList));


        // 获取组织id数组
        QueryWrapper<UserRelationEntity> query = new QueryWrapper<>();
        query.lambda().eq(UserRelationEntity::getUserId, id);
        query.lambda().eq(UserRelationEntity::getObjectType, PermissionConst.ORGANIZE);
        List<String> organizeIds = new ArrayList<>();
        userRelationService.list(query).forEach(u -> organizeIds.add(u.getObjectId()));

        // 岗位装配
        QueryWrapper<UserRelationEntity> positionQuery = new QueryWrapper<>();
        positionQuery.lambda().eq(UserRelationEntity::getUserId, id);
        positionQuery.lambda().eq(UserRelationEntity::getObjectType, PermissionConst.POSITION);
        StringJoiner positionIdsJoiner = new StringJoiner(",");
        for (UserRelationEntity ure : userRelationService.list(positionQuery)) {
            PositionEntity info = positionService.getInfo(ure.getObjectId());
            if (info != null) {
                positionIdsJoiner.add(ure.getObjectId());
            }
        }
        vo.setPositionId(positionIdsJoiner.length() > 0 ? positionIdsJoiner.toString() : null);
        // 设置分组id
        List<UserRelationEntity> listByObjectType = userRelationService.getListByObjectType(entity.getId(), PermissionConst.GROUP);
        StringBuilder groupId = new StringBuilder();
        listByObjectType.forEach(t -> groupId.append("," + t.getObjectId()));
        if (groupId.length() > 0) {
            vo.setGroupId(groupId.toString().replaceFirst(",", ""));
        }
        vo.setOrganizeIdTree(PermissionUtil.getOrgIdsTree(organizeIds, 1, organizeService));
        return ActionResult.success(vo);
    }

    @Operation(summary = "新建用户")
    @Parameter(name = "userCrForm", description = "表单参数", required = true)
    @SaCheckPermission("permission.user")
    @PostMapping
    public ActionResult<String> create(@RequestBody @Valid UserCrForm userCrForm) {
        UserEntity entity = JsonUtil.getJsonToBean(userCrForm, UserEntity.class);
        if (userService.isExistByAccount(userCrForm.getAccount())) {
            return ActionResult.fail(MsgCode.EXIST006.get());
        }
        if (StringUtil.isEmpty(entity.getGender())) {
            return ActionResult.fail(MsgCode.PS020.get());
        }
        userService.create(entity);
        String catchKey = cacheKeyUtil.getAllUser();
        if (redisUtil.exists(catchKey)) {
            redisUtil.remove(catchKey);
        }
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        entity.setPassword(sysInfo.getNewUserDefaultPassword());
        PullUserUtil.syncUser(entity, "create", UserProvider.getUser().getTenantId());
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "修改用户")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "userUpForm", description = "表单参数", required = true)
    @SaCheckPermission("permission.user")
    @PutMapping("/{id}")
    public ActionResult<String> update(@PathVariable("id") String id, @RequestBody @Valid UserUpForm userUpForm) {
        UserEntity entity = JsonUtil.getJsonToBean(userUpForm, UserEntity.class);
        if (StringUtil.isEmpty(entity.getGender())) {
            return ActionResult.fail(MsgCode.PS020.get());
        }
        //将禁用的id加进数据
        UserEntity originUser = userService.getInfo(id);
        // 如果是管理员的话
        if ("1".equals(String.valueOf(originUser.getIsAdministrator()))) {
            UserInfo operatorUser = UserProvider.getUser();
            // 管理员可以修改自己，但是无法修改其他管理员
            if (Boolean.TRUE.equals(operatorUser.getIsAdministrator())) {
                if (originUser.getEnabledMark() != 0 && entity.getEnabledMark() == 0) {
                    return ActionResult.fail(MsgCode.PS021.get());
                }
                if (!ADMIN_KEY.equals(userService.getInfo(operatorUser.getUserId()).getAccount()) && !operatorUser.getUserId().equals(id)) {
                    return ActionResult.fail(MsgCode.PS022.get());
                }
            } else {
                return ActionResult.fail(MsgCode.PS023.get());
            }
        }

        if (!originUser.getAccount().equals(entity.getAccount()) && userService.isExistByAccount(entity.getAccount())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        // 如果账号被锁定
        if ("2".equals(String.valueOf(entity.getEnabledMark()))) {
            entity.setUnlockTime(null);
            entity.setLogErrorCount(0);
        }
        // 如果原来是锁定，现在不锁定，则置空错误次数
        if (originUser.getEnabledMark() == 2 && entity.getEnabledMark() == 1) {
            entity.setUnlockTime(null);
            entity.setLogErrorCount(0);
        }
        boolean flag = userService.update(id, entity);
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            try {
                //修改用户之后判断是否需要同步到企业微信
                synThirdQyService.updateUserSysToQy(false, entity, "", 1);
                //修改用户之后判断是否需要同步到钉钉
                synThirdDingTalkService.updateUserSysToDing(false, entity, "", 1);
            } catch (Exception e) {
                log.error("修改用户之后同步失败到企业微信或钉钉失败,异常： {}", e.getMessage());
            }
        });
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        // 踢出在线的用户
        if (Objects.equals(entity.getEnabledMark(), 0)) {
            userService.logoutUser(MsgCode.LOG208.get(), ImmutableList.of(entity.getId()));
        } else if (Objects.equals(entity.getEnabledMark(), 2)) {
            userService.logoutUser(MsgCode.LOG209.get(), ImmutableList.of(entity.getId()));
        }
        PullUserUtil.syncUser(entity, "update", UserProvider.getUser().getTenantId());
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.user")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        UserEntity entity = userService.getInfo(id);
        if (entity != null) {
            if ("1".equals(String.valueOf(entity.getIsAdministrator()))) {
                return ActionResult.fail(MsgCode.PS026.get());
            }
            //判断是否是部门主管
            if (organizeService.getList(false).stream().anyMatch(t -> id.equals(t.getManagerId()))) {
                return ActionResult.fail(MsgCode.PS027.get());
            }
            String tenantId = StringUtil.isEmpty(UserProvider.getUser().getTenantId()) ? "" : UserProvider.getUser().getTenantId();
            String catchKey = tenantId + "allUser";
            if (redisUtil.exists(catchKey)) {
                redisUtil.remove(catchKey);
            }
            //删除之前进行判断
            List<UserRelationEntity> relationEntities = userRelationService.getListByUserId(id, PermissionConst.ORGANIZE);
            SocialsSysConfig socialsConfig = sysconfigApi.getSocialsConfig();
            String dingDepartment = socialsConfig.getDingDepartment();
            String qyhDepartment = socialsConfig.getQyhDepartment();
            boolean dingChoice;
            boolean qyChoice;
            OrganizeEntity dingOrg = organizeService.getInfo(dingDepartment);
            OrganizeEntity qyQrg = organizeService.getInfo(qyhDepartment);
            if (ObjectUtil.isNotEmpty(dingOrg)) {
                dingChoice = relationEntities.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()));
            } else {
                dingChoice = true;
            }
            if (ObjectUtil.isNotEmpty(qyQrg)) {
                qyChoice = relationEntities.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId()));
            } else {
                qyChoice = true;
            }
            userService.delete(entity);
            ThreadPoolExecutorUtil.getExecutor().execute(() -> {
                try {
                    //删除用户之后判断是否需要同步到企业微信
                    if (!qyChoice) {
                        synThirdQyService.deleteUserSysToQy(false, id, "");
                    }

                    //删除用户之后判断是否需要同步到钉钉
                    if (!dingChoice) {
                        synThirdDingTalkService.deleteUserSysToDing(false, id, "");
                    }

                } catch (Exception e) {
                    log.error("删除用户之后同步失败到企业微信或钉钉失败，异常：" + e.getMessage());
                }
            });
            userService.logoutUser(MsgCode.LOG207.get(), ImmutableList.of(entity.getId()));
            PullUserUtil.syncUser(entity, "delete", UserProvider.getUser().getTenantId());
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    //++++++++++++++++++++++批量操作start+++++++++++++++++++++++++
    @Operation(summary = "批量删除")
    @Parameter(name = "idList", description = "用户id列表", required = true)
    @SaCheckPermission("permission.user")
    @PostMapping("/BatchDelete")
    public ActionResult<Object> batchDelete(@RequestBody UserBatchForm form) {
        List<UserEntity> listByUserIds = userService.getListByUserIds(form.getIds());
        List<String> userIdList = new ArrayList<>();
        if (CollUtil.isNotEmpty(listByUserIds)) {
            List<String> collect = listByUserIds.stream().filter(t ->
                    !Objects.equals("1", t.getIsAdministrator())).map(UserEntity::getId).collect(Collectors.toList());
            userIdList.addAll(collect);
        } else {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        if (CollUtil.isEmpty(userIdList)) {
            return ActionResult.fail(MsgCode.PS026.get());
        }
        if (redisUtil.exists(cacheKeyUtil.getAllUser())) {
            redisUtil.remove(cacheKeyUtil.getAllUser());
        }
        SocialsSysConfig socialsConfig = sysconfigApi.getSocialsConfig();
        String dingDepartment = socialsConfig.getDingDepartment();
        String qyhDepartment = socialsConfig.getQyhDepartment();

        OrganizeEntity dingOrg = organizeService.getInfo(dingDepartment);
        OrganizeEntity qyQrg = organizeService.getInfo(qyhDepartment);

        for (UserEntity userItem : listByUserIds) {
            if (userIdList.contains(userItem.getId())) {
                String id = userItem.getId();
                List<UserRelationEntity> relationEntities = userRelationService.getListByUserId(id, PermissionConst.ORGANIZE);

                boolean dingChoice;
                boolean qyChoice;
                if (ObjectUtil.isNotEmpty(dingOrg)) {
                    dingChoice = relationEntities.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()));
                } else {
                    dingChoice = true;
                }
                if (ObjectUtil.isNotEmpty(qyQrg)) {
                    qyChoice = relationEntities.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId()));
                } else {
                    qyChoice = true;
                }

                ThreadPoolExecutorUtil.getExecutor().execute(() -> {
                    try {
                        //删除用户之后判断是否需要同步到企业微信
                        if (!qyChoice) {
                            synThirdQyService.deleteUserSysToQy(false, id, "");
                        }

                        //删除用户之后判断是否需要同步到钉钉
                        if (!dingChoice) {
                            synThirdDingTalkService.deleteUserSysToDing(false, id, "");
                        }

                    } catch (Exception e) {
                        log.error("删除用户之后同步失败到企业微信或钉钉失败，异常：" + e.getMessage());
                    }
                });
                PullUserUtil.syncUser(userItem, "delete", UserProvider.getUser().getTenantId());
            }
        }
        userService.batchDelete(userIdList);
        userService.logoutUser(MsgCode.LOG207.get(), userIdList);


        return ActionResult.success(MsgCode.SU003.get());
    }

    @Operation(summary = "批量更新状态：锁定、禁用、删除")
    @Parameter(name = "idList", description = "用户id列表", required = true)
    @Parameter(name = "enableMark", description = "用户id列表", required = true)
    @SaCheckPermission("permission.user")
    @PostMapping("/BatchUpdateState")
    public ActionResult<String> batchUpdateState(@RequestBody UserBatchForm form) {
        if (CollUtil.isNotEmpty(form.getIds())) {
            List<UserEntity> listByUserIds = userService.getListByUserIds(form.getIds());
            if (CollUtil.isNotEmpty(listByUserIds)) {
                try {
                    for (UserEntity entity : listByUserIds) {
                        if (Objects.equals(entity.getIsAdministrator(), 1)) {
                            continue;
                        }
                        entity.setEnabledMark(form.getEnabledMark());
                        userService.update(entity.getId(), entity);
                        //禁用移除在线用户
                        if (Objects.equals(form.getEnabledMark(), 0)) {
                            userService.logoutUser(MsgCode.LOG208.get(), ImmutableList.of(entity.getId()));
                        } else if (Objects.equals(form.getEnabledMark(), 2)) {
                            userService.logoutUser(MsgCode.LOG209.get(), ImmutableList.of(entity.getId()));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ActionResult.success(MsgCode.SU005.get());
            }
        }
        return ActionResult.success(MsgCode.FA001.get());
    }
    //++++++++++++++++++++++批量操作end+++++++++++++++++++++++++

    /**
     * 获取用户列表
     *
     * @return ignore
     */
    @Operation(summary = "获取所有用户列表")
    @GetMapping("/All")
    public ActionResult<ListVO<UserAllVO>> getAllUsers(PaginationUser pagination) {
        List<UserEntity> list = userService.getList(pagination, null, false, false, null, null);
        List<UserAllVO> user = JsonUtil.getJsonToList(list, UserAllVO.class);
        ListVO<UserAllVO> vo = new ListVO<>();
        vo.setList(user);
        return ActionResult.success(vo);
    }

    /**
     * IM通讯获取用户接口
     *
     * @param pagination 分页参数
     * @return ignore
     */
    @Operation(summary = "IM通讯获取用户")
    @GetMapping("/ImUser")
    public ActionResult<PageListVO<ImUserListVo>> getAllImUserUsers(Pagination pagination) {
        PageUser pageUser = JsonUtil.getJsonToBean(pagination, PageUser.class);
        List<UserEntity> data = userService.getList(pageUser, true);
        List<ImUserListVo> list = new ArrayList<>();
        Map<String, OrganizeEntity> orgMaps = organizeService.getOrganizeName(data.stream().map(t -> t.getOrganizeId()).collect(Collectors.toList()), null, false, null);
        for (UserEntity entity : data) {
            ImUserListVo user = JsonUtil.getJsonToBean(entity, ImUserListVo.class);
            OrganizeEntity organize = orgMaps.get(entity.getOrganizeId());
            user.setDepartment(organize != null ? organize.getFullName() : "");
            user.setHeadIcon(UploaderUtil.uploaderImg(entity.getHeadIcon()));
            list.add(user);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 获取用户下拉框列表
     *
     * @return ignore
     */
    @Operation(summary = "获取用户下拉框列表")
    @GetMapping("/Selector")
    public ActionResult<PageListVO<UserVO>> selector(UserPagination pagination) {
        List<UserEntity> userList = userService.getList(pagination);
        List<UserVO> list = new ArrayList<>();
        Map<String, String> positionMap = positionService.getPosFullNameMap();
        for (UserEntity userEntity : userList) {
            UserVO userVO = JsonUtil.getJsonToBean(userEntity, UserVO.class);
            userVO.setFullName(userVO.getRealName() + "/" + userVO.getAccount());
            userVO.setHeadIcon(UploaderUtil.uploaderImg(userVO.getHeadIcon()));
            StringJoiner positionJoiner = new StringJoiner(",");
            StringJoiner organizeJoiner = new StringJoiner(",");
            List<UserRelationEntity> allPostion = userRelationService.getListByObjectType(userEntity.getId(), PermissionConst.POSITION);
            if (CollUtil.isNotEmpty(allPostion)) {
                for (UserRelationEntity item : allPostion) {
                    String posName = positionMap.get(item.getObjectId());
                    if (posName != null) {
                        positionJoiner.add(posName);
                        organizeJoiner.add(posName.substring(0, posName.lastIndexOf("/")));
                    }
                }
            }
            userVO.setPosition(positionJoiner.toString());
            userVO.setOrganize(organizeJoiner.toString());
            list.add(userVO);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 通过部门、岗位、用户、角色、分组id获取用户列表
     *
     * @param userConditionModel 用户选择模型
     * @return
     */
    @Operation(summary = "通过部门、岗位、用户、角色、分组id获取用户列表")
    @Parameter(name = "userConditionModel", description = "用户选择模型", required = true)
    @PostMapping("/UserCondition")
    public ActionResult<PageListVO<UserIdListVo>> userCondition(@RequestBody UserConditionModel userConditionModel) {
        List<String> list = new ArrayList<>(16);
        if (userConditionModel.getDepartIds() != null) {
            list.addAll(userConditionModel.getDepartIds());
        }
        if (userConditionModel.getRoleIds() != null) {
            list.addAll(userConditionModel.getRoleIds());
        }
        if (userConditionModel.getPositionIds() != null) {
            list.addAll(userConditionModel.getPositionIds());
        }
        if (userConditionModel.getGroupIds() != null) {
            list.addAll(userConditionModel.getGroupIds());
        }
        if (list.isEmpty()) {
            list = userRelationService.getListByObjectType(userConditionModel.getType()).stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList());
            if (PermissionConst.GROUP.equals(userConditionModel.getType())) {
                List<GroupEntity> groupList = groupService.getListByIds(list);
                list = groupList.stream().map(GroupEntity::getId).collect(Collectors.toList());
            }
            if (PermissionConst.ORGANIZE.equals(userConditionModel.getType())) {
                List<OrganizeEntity> orgList = organizeService.getOrgEntityList(list, true);
                list = orgList.stream().map(OrganizeEntity::getId).collect(Collectors.toList());
            }
            if (PermissionConst.ROLE.equals(userConditionModel.getType())) {
                List<RoleEntity> roleList = roleService.getListByIds(list, null, false);
                list = roleList.stream().filter(t -> t.getEnabledMark() == 1).map(RoleEntity::getId).collect(Collectors.toList());
            }
            if (PermissionConst.POSITION.equals(userConditionModel.getType())) {
                List<PositionEntity> positionList = positionService.getPosList(list);
                list = positionList.stream().filter(t -> t.getEnabledMark() == 1).map(PositionEntity::getId).collect(Collectors.toList());
            }
        }
        List<String> collect = userRelationService.getListByObjectIdAll(list).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
        if (userConditionModel.getUserIds() != null) {
            collect.addAll(userConditionModel.getUserIds());
        }
        collect = collect.stream().distinct().collect(Collectors.toList());
        List<UserEntity> userName = userService.getUserName(collect, userConditionModel.getPagination());
        List<UserIdListVo> jsonToList = JsonUtil.getJsonToList(userName, UserIdListVo.class);
        Map<String, String> orgIdNameMaps = organizeService.getInfoList();
        jsonToList.forEach(t -> {
            t.setHeadIcon(UploaderUtil.uploaderImg(t.getHeadIcon()));
            t.setFullName(t.getRealName() + "/" + t.getAccount());
            List<UserRelationEntity> listByUserId = userRelationService.getListByUserId(t.getId(), PermissionConst.ORGANIZE);
            List<String> orgId = listByUserId.stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList());
            List<OrganizeEntity> organizeName = new ArrayList<>(organizeService.getOrganizeName(orgId, null, false, null).values());
            StringBuilder stringBuilder = new StringBuilder();
            organizeName.forEach(org -> {
                if (StringUtil.isNotEmpty(org.getOrganizeIdTree())) {
                    String fullNameByOrgIdTree = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, org.getOrganizeIdTree(), "/");
                    stringBuilder.append(",");
                    stringBuilder.append(fullNameByOrgIdTree);
                }
            });
            if (stringBuilder.length() > 0) {
                t.setOrganize(stringBuilder.toString().replaceFirst(",", ""));
            }
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(userConditionModel.getPagination(), PaginationVO.class);
        return ActionResult.page(jsonToList, paginationVO);
    }

    /**
     * 获取用户下拉框列表
     *
     * @param organizeIdForm 组织id
     * @param pagination     分页模型
     * @return
     */
    @Operation(summary = "获取用户下拉框列表")
    @Parameter(name = KeyConst.ORGANIZE_ID, description = "组织id", required = true)
    @Parameter(name = "pagination", description = "分页模型", required = true)
    @PostMapping("/ImUser/Selector/{organizeId}")
    public ActionResult<?> imUserSelector(@PathVariable(KeyConst.ORGANIZE_ID) String organizeIdForm, @RequestBody Pagination pagination) {
        String organizeId = XSSEscape.escape(organizeIdForm);
        List<UserSelectorVO> jsonToList = new ArrayList<>();
        Map<String, String> orgIdNameMaps = organizeService.getInfoList();
        Map<String, OrganizeEntity> orgMaps = organizeService.getOrgMaps(null, true, null);
        //判断是否搜索关键字
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            //通过关键字查询
            PageUser pageUser = JsonUtil.getJsonToBean(pagination, PageUser.class);
            List<UserEntity> list = userService.getList(pageUser, false);
            //遍历用户给要返回的值插入值
            for (UserEntity entity : list) {
                UserSelectorVO vo = JsonUtil.getJsonToBean(entity, UserSelectorVO.class);
                vo.setParentId(entity.getOrganizeId());
                vo.setFullName(entity.getRealName() + "/" + entity.getAccount());
                vo.setType("user");
                vo.setIcon(PermissionConst.USER_ICON);
                vo.setHeadIcon(UploaderUtil.uploaderImg(vo.getHeadIcon()));
                List<UserRelationEntity> listByUserId = userRelationService.getListByUserId(entity.getId()).stream().filter(t -> t != null && PermissionConst.ORGANIZE.equals(t.getObjectType())).collect(Collectors.toList());
                StringJoiner stringJoiner = new StringJoiner(",");
                listByUserId.forEach(t -> {
                    OrganizeEntity organizeEntity = orgMaps.get(t.getObjectId());
                    if (organizeEntity != null) {
                        String fullNameByOrgIdTree = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/");
                        if (StringUtil.isNotEmpty(fullNameByOrgIdTree)) {
                            stringJoiner.add(fullNameByOrgIdTree);
                        }
                    }
                });
                vo.setOrganize(stringJoiner.toString());
                vo.setHasChildren(false);
                vo.setIsLeaf(true);
                jsonToList.add(vo);
            }
            PaginationVO jsonToBean = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
            return ActionResult.page(jsonToList, jsonToBean);
        }
        //获取所有组织
        List<OrganizeEntity> collect = new ArrayList<>(orgMaps.values());
        //判断时候传入组织id
        //如果传入组织id，则取出对应的子集
        if (!"0".equals(organizeId)) {
            //通过组织查询部门及人员
            //单个组织
            OrganizeEntity organizeEntity = orgMaps.get(organizeId);
            if (organizeEntity != null) {
                //取出组织下的部门
                List<OrganizeEntity> collect1 = collect.stream().filter(t -> t.getParentId().equals(organizeEntity.getId())).collect(Collectors.toList());
                for (OrganizeEntity entitys : collect1) {
                    UserSelectorVO vo = JsonUtil.getJsonToBean(entitys, UserSelectorVO.class);
                    if (KeyConst.DEPARTMENT.equals(entitys.getCategory())) {
                        vo.setIcon(PermissionConst.DEPARTMENT_ICON);
                    } else if (KeyConst.COMPANY.equals(entitys.getCategory())) {
                        vo.setIcon(PermissionConst.COMPANY_ICON);
                    }
                    vo.setOrganize(organizeService.getFullNameByOrgIdTree(orgIdNameMaps, entitys.getOrganizeIdTree(), "/"));
                    // 判断组织下是否有人
                    jsonToList.add(vo);
                    vo.setHasChildren(true);
                    vo.setIsLeaf(false);
                }
                //取出组织下的人员
                List<UserEntity> entityList = userService.getListByOrganizeId(organizeId, null);
                for (UserEntity entity : entityList) {
                    if ("0".equals(String.valueOf(entity.getEnabledMark()))) {
                        continue;
                    }
                    UserSelectorVO vo = JsonUtil.getJsonToBean(entity, UserSelectorVO.class);
                    vo.setParentId(organizeId);
                    vo.setFullName(entity.getRealName() + "/" + entity.getAccount());
                    vo.setType("user");
                    vo.setIcon(PermissionConst.USER_ICON);
                    List<UserRelationEntity> listByUserId = userRelationService.getListByUserId(entity.getId()).stream().filter(t -> t != null && PermissionConst.ORGANIZE.equals(t.getObjectType())).collect(Collectors.toList());
                    StringBuilder stringBuilder = new StringBuilder();
                    listByUserId.forEach(t -> {
                        OrganizeEntity organizeEntity1 = orgMaps.get(t.getObjectId());
                        if (organizeEntity1 != null) {
                            String fullNameByOrgIdTree = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity1.getOrganizeIdTree(), "/");
                            if (StringUtil.isNotEmpty(fullNameByOrgIdTree)) {
                                stringBuilder.append(",").append(fullNameByOrgIdTree);
                            }
                        }
                    });
                    if (stringBuilder.length() > 0) {
                        vo.setOrganize(stringBuilder.toString().replaceFirst(",", ""));
                    }
                    vo.setHeadIcon(UploaderUtil.uploaderImg(vo.getHeadIcon()));
                    vo.setHasChildren(false);
                    vo.setIsLeaf(true);
                    jsonToList.add(vo);
                }
            }
            ListVO<UserSelectorVO> vo = new ListVO<>();
            vo.setList(jsonToList);
            return ActionResult.success(vo);
        }

        //如果没有组织id，则取出所有组织
        jsonToList = JsonUtil.getJsonToList(collect.stream().filter(t -> "-1".equals(t.getParentId())).collect(Collectors.toList()), UserSelectorVO.class);
        //添加图标
        for (UserSelectorVO userSelectorVO : jsonToList) {
            userSelectorVO.setIcon(PermissionConst.COMPANY_ICON);
            userSelectorVO.setHasChildren(true);
            userSelectorVO.setIsLeaf(false);
            userSelectorVO.setOrganize(organizeService.getFullNameByOrgIdTree(orgIdNameMaps, orgMaps.get(userSelectorVO.getId()).getOrganizeIdTree(), "/"));
        }
        ListVO<UserSelectorVO> vo = new ListVO<>();
        vo.setList(jsonToList);
        return ActionResult.success(vo);
    }

    @Operation(summary = "个人权限")
    @Parameter(name = "id", description = "主键值", required = true)
    @GetMapping("/Authorize")
    public ActionResult<UserAuthorizeVO> getAuthorize(UserAuthForm param) {
        return ActionResult.success(authPermUtil.getUserAuth(param));
    }

    @Operation(summary = "角色岗位权限列表")
    @GetMapping("/getAllPermission")
    public ActionResult<List<PositionVo>> getAllPermission(@RequestParam(value = "userId", required = false) String userId) {
        if (StringUtil.isEmpty(userId)) {
            userId = UserProvider.getUser().getUserId();
        }
        UserEntity info = userService.getInfo(userId);
        List<PositionVo> listRes = new ArrayList<>();
        listRes.add(new PositionVo("all", "全部权限"));

        //获取用户关系
        List<String> posIds = userRelationService.getListByUserId(userId, PermissionConst.POSITION).stream().map(u -> u.getObjectId()).collect(Collectors.toList());
        Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
        List<PositionEntity> pList = positionService.getListByIds(posIds);
        List<String> roleIds = roleRelationService.getListByObjectId(userId, PermissionConst.USER)
                .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        List<RoleEntity> rList = roleService.getListByIds(roleIds);
        List<String> objectIds = new ArrayList<>();
        objectIds.addAll(posIds);
        objectIds.addAll(roleIds);
        List<StandingModel> allStand = standingService.getByObjectIds(objectIds);
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        if (Objects.equals(sysInfo.getStandingSwitch(), 1) && !allStand.isEmpty()) {
            getStanding(allStand, info, rList, pList, allOrgsTreeName, listRes);
        } else {
            getPosition(posIds, pList, allOrgsTreeName, listRes, roleIds, rList);
        }
        return ActionResult.success(listRes);
    }

    private static void getPosition(List<String> posIds, List<PositionEntity> pList, Map<String, Object> allOrgsTreeName, List<PositionVo> listRes, List<String> roleIds, List<RoleEntity> rList) {
        //添加岗位
        if (CollUtil.isNotEmpty(posIds)) {
            PositionVo pv = new PositionVo(PermissionConst.POSITION, "所属岗位");
            pv.setHasChildren(true);
            List<PositionVo> child = new ArrayList<>();
            for (PositionEntity item : pList) {
                PositionVo vo = JsonUtil.getJsonToBean(item, PositionVo.class);
                vo.setFullName(allOrgsTreeName.get(item.getOrganizeId()) + "/" + item.getFullName());
                vo.setType(PermissionConst.POSITION);
                child.add(vo);
            }
            pv.setChildren(child);
            listRes.add(pv);
        }
        //添加角色
        if (CollUtil.isNotEmpty(roleIds)) {
            PositionVo pv = new PositionVo(PermissionConst.ROLE, "所属角色");
            pv.setHasChildren(true);
            List<PositionVo> child = new ArrayList<>();
            for (RoleEntity item : rList) {
                PositionVo vo = JsonUtil.getJsonToBean(item, PositionVo.class);
                vo.setType(PermissionConst.ROLE);
                child.add(vo);
            }
            pv.setChildren(child);
            listRes.add(pv);
        }
    }

    private static void getStanding(List<StandingModel> allStand, UserEntity info, List<RoleEntity> rList, List<PositionEntity> pList, Map<String, Object> allOrgsTreeName, List<PositionVo> listRes) {
        PositionVo pv = new PositionVo(PermissionConst.STAND, "所属身份");
        pv.setType(PermissionConst.STAND);
        pv.setHasChildren(true);
        List<PositionVo> child = new ArrayList<>();
        for (StandingModel standItem : allStand) {
            PositionVo itemPv = new PositionVo(standItem.getId(), standItem.getFullName());
            itemPv.setType(PermissionConst.STAND);
            itemPv.setHasChildren(true);
            if (StringUtil.isNotEmpty(info.getStanding()) && info.getStanding().equals(standItem.getId())) {
                itemPv.setPcCurStand(true);
            }
            if (StringUtil.isNotEmpty(info.getAppStanding()) && info.getAppStanding().equals(standItem.getId())) {
                itemPv.setAppCurStand(true);
            }
            List<PositionVo> childItem = new ArrayList<>();
            if (!standItem.getRoleIds().isEmpty()) {
                for (RoleEntity item : rList) {
                    if (standItem.getRoleIds().contains(item.getId())) {
                        PositionVo vo = JsonUtil.getJsonToBean(item, PositionVo.class);
                        vo.setType(PermissionConst.ROLE);
                        childItem.add(vo);
                    }
                }
            }
            if (!standItem.getPosIds().isEmpty()) {
                for (PositionEntity item : pList) {
                    if (standItem.getPosIds().contains(item.getId())) {
                        PositionVo vo = JsonUtil.getJsonToBean(item, PositionVo.class);
                        vo.setFullName(allOrgsTreeName.get(item.getOrganizeId()) + "/" + item.getFullName());
                        vo.setType(PermissionConst.POSITION);
                        childItem.add(vo);
                    }
                }
            }
            itemPv.setChildren(childItem);
            child.add(itemPv);
        }
        pv.setChildren(child);
        listRes.add(pv);
    }

    @Operation(summary = "用户获取岗位")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.user")
    @GetMapping("/{id}/GetPosition")
    public ActionResult<List<PositionListVO>> getPosition(@PathVariable("id") String id) {
        UserEntity entity = userService.getInfo(id);
        List<PositionListVO> list = new ArrayList<>();
        if (entity != null) {
            List<UserRelationEntity> relationList = userRelationService.getListByUserId(id, PermissionConst.POSITION);
            Map<String, Object> allOrgsTreeName = organizeService.getAllOrgsTreeName();
            List<PositionEntity> positionList = positionService.getListByIds(relationList.stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList()));
            for (PositionEntity item : positionList) {
                PositionListVO vo = JsonUtil.getJsonToBean(item, PositionListVO.class);
                vo.setOrgNameTree(allOrgsTreeName.get(item.getOrganizeId()) + "/" + item.getFullName());
                list.add(vo);
            }
        }
        return ActionResult.success(list);
    }

    @Operation(summary = "同岗位用户列表")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.user")
    @GetMapping("/{id}/SelectorByUserPosId")
    public ActionResult<PageListVO<UserListVO>> selectorByUserPosId(@PathVariable("id") String id, Pagination pagination) {
        UserEntity entity = userService.getInfo(id);
        PageUser pageUser = JsonUtil.getJsonToBean(pagination, PageUser.class);
        List<UserListVO> list = new ArrayList<>();
        if (entity != null) {
            List<UserRelationEntity> relationList = userRelationService.getListByUserId(id, PermissionConst.POSITION);
            List<String> posIds = relationList.stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList());
            List<String> userIds = userRelationService.getListByObjectIdAll(posIds).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(userIds)) {
                pageUser.setIdList(userIds);
                List<UserEntity> userList = userService.getList(pageUser, true);
                for (UserEntity item : userList) {
                    UserListVO vo = JsonUtil.getJsonToBean(item, UserListVO.class);
                    vo.setFullName(item.getRealName() + "/" + item.getAccount());
                    vo.setHeadIcon(UploaderUtil.uploaderImg(item.getHeadIcon()));
                    list.add(vo);
                }
            }
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pageUser, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    @Operation(summary = "用户调整岗位")
    @Parameter(name = "id", description = "用户主键值", required = true)
    @SaCheckPermission("permission.user")
    @PostMapping("/{id}/SetPosition")
    public ActionResult<Object> setPosition(@PathVariable("id") String id, @RequestBody UserBatchForm form) {
        UserEntity entity = userService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        if (Objects.equals(entity.getIsAdministrator(), 1)) {
            return ActionResult.success(MsgCode.PS023.get());
        }

        List<String> posIds = form.getIds();
        List<UserRelationEntity> relationList = userRelationService.getListByUserId(id, PermissionConst.POSITION);
        //直接打开不编辑保存不执行代码
        if (!posIds.isEmpty() && posIds.equals(relationList.stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList()))) {
            return ActionResult.success(MsgCode.SU023.get());
        }

        List<PositionEntity> posList = positionService.getListByIds(posIds);
        //判断约束  -- 逐一判断，能成功的就添加res
        List<String> res = new ArrayList<>();
        Set<String> huchi = new HashSet<>();
        Set<String> xianjue = new HashSet<>();
        List<String> errList1 = new ArrayList<>();
        filterPos(posIds, posList, errList1, huchi, res, xianjue);
        //先决第一个判断不存在，再次判断后续有没有添加上先决的
        if (CollUtil.isNotEmpty(xianjue)) {
            filterXianJuePos(new ArrayList<>(xianjue), posList, errList1, res);
        }
        //只有出现约束异常就直接修改失败
        if (CollUtil.isNotEmpty(errList1)) {
            return ActionResult.fail(MsgCode.FA055.get(MsgCode.PS004.get()));
        }

        List<UserRelationEntity> orgList = userRelationService.getListByUserId(id, PermissionConst.ORGANIZE);
        //移除全部岗位、组织和用户得关系
        List<UserRelationEntity> deleteAll = new ArrayList<>();
        deleteAll.addAll(relationList);
        deleteAll.addAll(orgList);
        //删除之前进行判断
        SocialsSysConfig socialsConfig = sysconfigApi.getSocialsConfig();
        String dingDepartment = socialsConfig.getDingDepartment();
        String qyhDepartment = socialsConfig.getQyhDepartment();
        boolean dingChoice;
        boolean qyChoice;
        OrganizeEntity dingOrg = organizeService.getInfo(dingDepartment);
        OrganizeEntity qyQrg = organizeService.getInfo(qyhDepartment);


        for (UserRelationEntity relationEntity : deleteAll) {
            userRelationService.removeById(relationEntity);
        }

        List<UserRelationEntity> addAll = new ArrayList<>();
        if (CollUtil.isNotEmpty(res)) {
            for (String positionId : res) {
                PositionEntity info = posList.stream().filter(t -> t.getId().equals(positionId)).findFirst().orElse(null);
                UserRelationEntity userRelation = new UserRelationEntity();

                userRelation.setObjectId(positionId);
                userRelation.setUserId(id);
                userRelation.setObjectType(PermissionConst.POSITION);
                addAll.add(userRelation);
                //岗位添加组织关系方便使用
                UserRelationEntity userOrgRelation = new UserRelationEntity();
                if (null != info) {
                    userOrgRelation.setObjectId(info.getOrganizeId());
                }
                userOrgRelation.setUserId(id);
                userOrgRelation.setObjectType(PermissionConst.ORGANIZE);
                addAll.add(userOrgRelation);
            }
            userRelationService.save(addAll);
            if (ObjectUtil.isNotEmpty(dingOrg)) {
                dingChoice = deleteAll.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()))
                        && addAll.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()));
            } else {
                dingChoice = true;
            }
            if (ObjectUtil.isNotEmpty(qyQrg)) {
                qyChoice = deleteAll.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId()))
                        && addAll.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()));
            } else {
                qyChoice = true;
            }

            ThreadPoolExecutorUtil.getExecutor().execute(() -> {
                try {
                    //获取公司关联
                    List<String> collect = deleteAll.stream()
                            .filter(t -> t.getObjectType().equals(PermissionConst.ORGANIZE))
                            .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
                    if (!dingChoice) {
                        synThirdDingTalkService.deleteUserSysToDing(false, entity, "", collect);
                    }
                    if (!qyChoice) {
                        synThirdQyService.deleteUserSysToQy(false, entity, "", collect);
                    }


                } catch (Exception e) {
                    log.error("创建组织后同步失败到钉钉失败，异常：{}", e.getMessage());
                }
            });
            ThreadPoolExecutorUtil.getExecutor().execute(() -> {
                try {
                    //获取公司关联
                    List<String> collect = addAll.stream()
                            .filter(t -> t.getObjectType().equals(PermissionConst.ORGANIZE))
                            .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
                    if (deleteAll.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId()))) {
                        synThirdQyService.createUserSysToQy(false, entity, "", collect);
                    } else {
                        synThirdQyService.updateUserSysToQy(false, entity, "", 1);
                    }

                    if (deleteAll.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()))) {
                        synThirdDingTalkService.createUserSysToDing(false, entity, "", collect);
                    } else {
                        synThirdDingTalkService.updateUserSysToDing(false, entity, "");
                    }


                } catch (Exception e) {
                    log.error("创建组织后同步失败到企业微信或钉钉失败，异常：{}", e.getMessage());
                }
            });
            userService.delCurUser(MsgCode.PS010.get(), Collections.singletonList(id));
        }
        return ActionResult.success(MsgCode.SU023.get());
    }

    private void filterPos(List<String> posIds, List<PositionEntity> posList, List<String> errList1, Set<String> huchi, List<String> res, Set<String> xianjue) {
        for (String positionId : posIds) {
            List<String> errList2 = new ArrayList<>();
            PositionEntity info = posList.stream().filter(t -> t.getId().equals(positionId)).findFirst().orElse(null);
            List<String> userIds = userRelationService.getListByObjectId(positionId, PermissionConst.POSITION)
                    .stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
            PosConModel posConModel = new PosConModel();
            if (null != info && Objects.equals(info.getIsCondition(), 1)) {
                posConModel = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
                posConModel.init();
            }
            //超出权限基数的截取
            if (posConModel.getNumFlag() && posConModel.getUserNum() < (1 + userIds.size())) {
                errList2.add(MsgCode.SYS135.get(MsgCode.PS004.get()));
            }

            //在已有岗位的互斥信息里
            if (huchi.contains(positionId)) {
                errList2.add(MsgCode.SYS137.get(MsgCode.PS004.get()));
            }
            //当前的岗位互斥信息-存在互斥岗位
            List<String> mutualExclusion = posConModel.getMutualExclusion();
            if (posConModel.getMutualExclusionFlag()) {
                //互斥
                List<String> collect = res.stream().filter(mutualExclusion::contains).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(collect)) {
                    errList2.add(MsgCode.SYS137.get(MsgCode.PS004.get()));
                }
            }
            if (posConModel.getPrerequisiteFlag()) {
                //先决
                xianjue.add(positionId);
            }
            if (errList2.isEmpty()) {
                res.add(positionId);
                if (CollUtil.isNotEmpty(mutualExclusion)) {
                    huchi.addAll(mutualExclusion);
                }
            } else {
                errList1.addAll(errList2);
            }
        }
    }

    private void filterXianJuePos(List<String> posIds, List<PositionEntity> posList, List<String> errList1, List<String> res) {
        for (String positionId : posIds) {
            List<String> errList2 = new ArrayList<>();
            PositionEntity info = posList.stream().filter(t -> t.getId().equals(positionId)).findFirst().orElse(null);
            PosConModel posConModel = new PosConModel();
            if (null != info && Objects.equals(info.getIsCondition(), 1)) {
                posConModel = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
                posConModel.init();
            }
            if (posConModel.getPrerequisiteFlag() && !new HashSet<>(res).containsAll(posConModel.getPrerequisite())) {
                //先决
                errList2.add(MsgCode.SYS138.get(MsgCode.PS004.get()));
            }
            if (errList2.isEmpty()) {
                res.add(positionId);
            } else {
                errList1.addAll(errList2);
            }
        }
    }

    @Operation(summary = "批量调整岗位")
    @SaCheckPermission("permission.user")
    @PostMapping("/SetPositionBatch")
    public ActionResult<Object> setPositionBatch(@RequestBody UserBatchForm form) {
        List<UserEntity> userList = userService.getListByUserIds(form.getUserIds());
        List<String> posIds = form.getIds();
        List<PositionEntity> positionList = positionService.getListByIds(posIds);
        if (CollUtil.isEmpty(userList)) {
            return ActionResult.fail(MsgCode.FA001.get());
        }

        SocialsSysConfig socialsConfig = sysconfigApi.getSocialsConfig();
        String dingDepartment = socialsConfig.getDingDepartment();
        String qyhDepartment = socialsConfig.getQyhDepartment();
        OrganizeEntity dingOrg = organizeService.getInfo(dingDepartment);
        OrganizeEntity qyQrg = organizeService.getInfo(qyhDepartment);
        List<String> userIds = userList.stream().filter(t -> !Objects.equals(t.getIsAdministrator(), 1)).map(UserEntity::getId).collect(Collectors.toList());
        //再加
        List<String> addUserIds = new ArrayList<>();
        List<String> skipUserIds = new ArrayList<>();
        for (String userId : userIds) {
            List<UserRelationEntity> relationList = userRelationService.getListByObjectType(userId, null);
            //直接打开不编辑保存不执行代码
            if (!posIds.isEmpty() && posIds.equals(relationList.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType()))
                    .map(UserRelationEntity::getObjectId).collect(Collectors.toList()))) {
                skipUserIds.add(userId);
                continue;
            }

            //判断约束
            List<String> res = new ArrayList<>();
            Set<String> huchi = new HashSet<>();
            Set<String> xianjue = new HashSet<>();
            List<String> errList1 = new ArrayList<>();
            filterPos(posIds, positionList, errList1, huchi, res, xianjue);
            //先决第一个判断不存在，再次判断后续有没有添加上先决的
            if (CollUtil.isNotEmpty(xianjue)) {
                filterXianJuePos(new ArrayList<>(xianjue), positionList, errList1, res);
            }
            //岗位有约束异常就不添加
            if (CollUtil.isEmpty(errList1)) {
                addUserIds.add(userId);
                List<UserRelationEntity> addAll = new ArrayList<>();
                for (String positionId : posIds) {
                    PositionEntity info = positionList.stream().filter(t -> t.getId().equals(positionId)).findFirst().orElse(null);
                    UserRelationEntity userRelation = new UserRelationEntity();
                    UserRelationEntity userOrgRelation = new UserRelationEntity();
                    if (null != info) {
                        userRelation.setObjectId(info.getId());
                        userOrgRelation.setObjectId(info.getOrganizeId());
                    }

                    userRelation.setUserId(userId);
                    userRelation.setObjectType(PermissionConst.POSITION);
                    addAll.add(userRelation);
                    //岗位添加组织关系方便使用


                    userOrgRelation.setUserId(userId);
                    userOrgRelation.setObjectType(PermissionConst.ORGANIZE);
                    addAll.add(userOrgRelation);
                }

                //先删
                List<UserRelationEntity> deleteList = relationList.stream().filter(t -> PermissionConst.POSITION.equals(t.getObjectType())
                        || PermissionConst.ORGANIZE.equals(t.getObjectType())).collect(Collectors.toList());
                //删除之前进行判断
                delThird(dingOrg, deleteList, addAll, qyQrg);
                userRelationService.save(addAll);
                synThird(addAll, deleteList, qyQrg, dingOrg);
            }
        }

        if (CollUtil.isNotEmpty(addUserIds)) {
            userService.delCurUser(MsgCode.PS010.get(), addUserIds);
        }

        if (addUserIds.isEmpty() && skipUserIds.isEmpty()) {
            return ActionResult.fail(MsgCode.FA054.get());
        } else if (!addUserIds.isEmpty() && addUserIds.size() != userIds.size()) {
            return ActionResult.success(MsgCode.SU024.get());
        }
        return ActionResult.success(MsgCode.SU023.get());
    }

    private void delThird(OrganizeEntity dingOrg, List<UserRelationEntity> deleteList, List<UserRelationEntity> addAll, OrganizeEntity qyQrg) {
        boolean dingChoice;
        boolean qyChoice;

        if (ObjectUtil.isNotEmpty(dingOrg)) {
            dingChoice = deleteList.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId())) &&
                    addAll.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()));
        } else {
            dingChoice = true;
        }
        if (ObjectUtil.isNotEmpty(qyQrg)) {
            qyChoice = deleteList.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId())) &&
                    addAll.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId()));
        } else {
            qyChoice = true;
        }


        for (UserRelationEntity relationEntity : deleteList) {
            userRelationService.removeById(relationEntity);
        }
        try {
            List<String> collect = deleteList.stream()
                    .map(UserRelationEntity::getUserId)
                    .collect(Collectors.toList());
            if (!qyChoice) {
                synThirdQyService.deleteUserSysToQy(false, collect, "", "");
            }
            if (!dingChoice) {
                synThirdDingTalkService.deleteUserSysToDing(false, collect, "", "");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    //同步第三方
    private void synThird(List<UserRelationEntity> addAll, List<UserRelationEntity> deleteList, OrganizeEntity qyQrg, OrganizeEntity dingOrg) {
        try {
            Map<String, List<UserRelationEntity>> collect = addAll
                    .stream()
                    .filter(t -> t.getObjectType().equals(PermissionConst.POSITION))
                    .collect(Collectors.groupingBy(UserRelationEntity::getObjectId));
            for (Map.Entry<String, List<UserRelationEntity>> entry : collect.entrySet()) {
                String string = entry.getKey();
                PositionEntity info = positionService.getInfo(string);
                List<UserRelationEntity> userRelationEntities = collect.get(string);
                List<String> userEntityIds = userRelationEntities.stream().map(UserRelationEntity::getUserId)
                        .collect(Collectors.toList());
                List<UserEntity> userEntities = userService.getUserList(userEntityIds);
                for (UserEntity userEntity : userEntities) {
                    userEntity.setOrganizeId(info.getOrganizeId());
                }

                if (deleteList.stream().noneMatch(t -> qyQrg.getOrganizeIdTree().contains(t.getObjectId()))) {
                    synThirdQyService.createUserSysToQy(false, userEntities, "", info.getId());
                } else {
                    for (UserEntity userEntity : userEntities) {
                        synThirdQyService.updateUserSysToQy(false, userEntity, "", info.getId());
                    }

                }
                if (deleteList.stream().noneMatch(t -> dingOrg.getOrganizeIdTree().contains(t.getObjectId()))) {
                    synThirdDingTalkService.createUserSysToDing(false, userEntities, "", info.getId());
                } else {
                    for (UserEntity userEntity : userEntities) {
                        synThirdDingTalkService.updateUserSysToDing(false, userEntity, "", 1);
                    }
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Operation(summary = "修改用户密码")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "userResetPasswordForm", description = "修改密码模型", required = true)
    @SaCheckPermission("permission.user")
    @PostMapping("/{id}/Actions/ResetPassword")
    public ActionResult<String> modifyPassword(@PathVariable("id") String id, @RequestBody @Valid UserResetPasswordForm userResetPasswordForm) {
        UserEntity entity = userService.getInfo(id);
        if (entity != null) {
            entity.setPassword(userResetPasswordForm.getUserPassword());
            userService.updatePassword(entity);
            userService.logoutUser(MsgCode.PS011.get(), ImmutableList.of(entity.getId()));
            entity.setPassword(userResetPasswordForm.getUserPassword());
            PullUserUtil.syncUser(entity, "modifyPassword", UserProvider.getUser().getTenantId());
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.success(MsgCode.FA001.get());
    }

    /**
     * 更新用户状态
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "更新用户状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.user")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<String> disable(@PathVariable("id") String id) {
        UserEntity entity = userService.getInfo(id);
        if (entity != null) {
            if ("1".equals(String.valueOf(entity.getIsAdministrator()))) {
                return ActionResult.fail(MsgCode.PS029.get());
            }
            if (entity.getEnabledMark() != null) {
                if ("1".equals(String.valueOf(entity.getEnabledMark()))) {
                    entity.setEnabledMark(0);
                    userService.delCurUser(null, ImmutableList.of(entity.getId()));
                    userService.update(id, entity);
                } else {
                    entity.setEnabledMark(1);
                    userService.update(id, entity);
                }
            } else {
                entity.setEnabledMark(1);
                userService.update(id, entity);
            }
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.success(MsgCode.FA001.get());
    }

    /**
     * 解除锁定
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "解除锁定")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.user")
    @PutMapping("/{id}/Actions/unlock")
    public ActionResult<String> unlock(@PathVariable("id") String id) {
        UserEntity entity = userService.getInfo(id);
        if (entity != null) {
            // 状态变成正常
            entity.setEnabledMark(1);
            entity.setUnlockTime(null);
            entity.setLogErrorCount(0);
            entity.setId(id);
            userService.updateById(entity);
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.success(MsgCode.FA001.get());
    }

    /**
     * 获取用户基本信息
     *
     * @param userIdModel 用户id
     * @return ignore
     */
    @Operation(summary = "获取用户基本信息")
    @Parameter(name = "userIdModel", description = "用户id", required = true)
    @PostMapping("/getUserList")
    public ActionResult<ListVO<UserIdListVo>> getUserList(@RequestBody UserIdModel userIdModel) {
        List<UserEntity> userList = userService.getUserName(userIdModel.getIds(), true);
        List<UserEntity> userName = new ArrayList<>();
        for (String id : userIdModel.getIds()) {
            UserEntity userEntity = userList.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
            if (userEntity != null) {
                userName.add(userEntity);
            }
        }
        List<UserIdListVo> list = JsonUtil.getJsonToList(userName, UserIdListVo.class);
        List<UserRelationEntity> listByUserIds = userRelationService.getRelationByUserIds(list.stream().map(UserIdListVo::getId).collect(Collectors.toList()));
        Map<String, String> orgIdNameMaps = organizeService.getInfoList();
        for (UserIdListVo entity : list) {
            if (entity == null) {
                break;
            }
            entity.setFullName(entity.getRealName() + "/" + entity.getAccount());
            List<UserRelationEntity> listByUserId = listByUserIds.stream().filter(t -> t.getUserId().equals(entity.getId())).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder();
            List<OrganizeEntity> orgEntityList = organizeService.getOrgEntityList(listByUserId.stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList()), false);
            listByUserId.forEach(t -> {
                OrganizeEntity organizeEntity = orgEntityList.stream().filter(org -> org.getId().equals(t.getObjectId())).findFirst().orElse(null);
                if (organizeEntity != null) {
                    String fullNameByOrgIdTree = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/");
                    if (StringUtil.isNotEmpty(fullNameByOrgIdTree)) {
                        stringBuilder.append(",").append(fullNameByOrgIdTree);
                    }
                }
            });
            if (stringBuilder.length() > 0) {
                entity.setOrganize(stringBuilder.toString().replaceFirst(",", ""));
            }
            entity.setHeadIcon(UploaderUtil.uploaderImg(entity.getHeadIcon()));
        }
        ListVO<UserIdListVo> listVO = new ListVO<>();
        listVO.setList(list);
        return ActionResult.success(listVO);
    }

    /**
     * 获取选中组织、岗位、角色、用户基本信息
     *
     * @param userIdModel 用户id
     * @return ignore
     */
    @Operation(summary = "获取选中组织、岗位、角色、用户基本信息")
    @Parameter(name = "userIdModel", description = "用户id", required = true)
    @PostMapping("/getSelectedList")
    public ActionResult<ListVO<BaseInfoVo>> getSelectedList(@RequestBody UserIdModel userIdModel) {
        List<String> ids = userIdModel.getIds();
        List<BaseInfoVo> list = userService.selectedByIds(ids);

        ListVO<BaseInfoVo> listVO = new ListVO<>();
        listVO.setList(list);
        return ActionResult.success(listVO);
    }


    /**
     * 获取用户基本信息
     *
     * @param userIdModel 用户id
     * @return ignore
     */
    @Operation(summary = "获取选中用户基本信息")
    @Parameter(name = "userIdModel", description = "用户id", required = true)
    @PostMapping("/getSelectedUserList")
    public ActionResult<PageListVO<BaseInfoVo>> getSelectedUserList(@RequestBody UserIdModelByPage userIdModel) {
        List<BaseInfoVo> jsonToList = userService.getObjList(userIdModel.getIds(), userIdModel);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(userIdModel, PaginationVO.class);
        return ActionResult.page(jsonToList, paginationVO);

    }

    /**
     * 获取组织下的人员
     *
     * @param page 页面信息
     * @return ignore
     */
    @Operation(summary = "获取组织下的人员")
    @GetMapping("/getOrganization")
    public ActionResult<List<UserIdListVo>> getOrganization(PageUser page) {
        String departmentId = page.getOrganizeId();
        // 判断是否获取当前组织下的人员
        if ("0".equals(departmentId)) {
            departmentId = UserProvider.getUser().getDepartmentId();
            // 为空则取组织id
            if (StringUtil.isEmpty(departmentId)) {
                departmentId = UserProvider.getUser().getOrganizeId();
            }
        }
        Map<String, OrganizeEntity> orgMaps = organizeService.getOrgMaps(null, true, null);
        List<UserEntity> list = userService.getListByOrganizeId(departmentId, page.getKeyword());
        List<UserIdListVo> jsonToList = JsonUtil.getJsonToList(list, UserIdListVo.class);
        Map<String, String> orgIdNameMaps = organizeService.getInfoList();
        List<UserRelationEntity> listByObjectType = userRelationService.getListByObjectType(PermissionConst.ORGANIZE);
        jsonToList.forEach(t -> {
            t.setRealName(t.getRealName() + "/" + t.getAccount());
            t.setFullName(t.getRealName());
            List<String> collect = listByObjectType.stream().filter(userRelationEntity -> userRelationEntity.getUserId().equals(t.getId())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());
            StringJoiner stringJoiner = new StringJoiner(",");
            collect.forEach(objectId -> {
                OrganizeEntity organizeEntity = orgMaps.get(objectId);
                if (organizeEntity != null) {
                    String fullNameByOrgIdTree = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/");
                    if (StringUtil.isNotEmpty(fullNameByOrgIdTree)) {
                        stringJoiner.add(fullNameByOrgIdTree);
                    }
                }
            });
            t.setOrganize(stringJoiner.toString());
            t.setHeadIcon(UploaderUtil.uploaderImg(t.getHeadIcon()));
        });
        return ActionResult.success(jsonToList);
    }

    /**
     * 获取人员，委托选人接口
     *
     * @param type       范围类型
     * @param pagination 参数
     */
    @Operation(summary = "获取人员")
    @GetMapping("/ReceiveUserList")
    public ActionResult<PageListVO<UserIdListVo>> receiveUserList(@RequestParam("type") Integer type, Pagination pagination) {
        UserInfo userInfo = UserProvider.getUser();
        UserEntity user = userService.getInfo(userInfo.getUserId());
        List<String> userId = new ArrayList<>();
        List<Integer> ruleList = ImmutableList.of(ExtraRuleEnum.ORGANIZE.getCode(), ExtraRuleEnum.POSITION.getCode(),
                ExtraRuleEnum.DEPARTMENT.getCode());
        boolean isDepartmeng = false;
        if (ruleList.contains(type)) {
            isDepartmeng = getuserIds(type, user, userId, isDepartmeng);
        }
        List<UserEntity> list = isDepartmeng ? userService.getUserPage(pagination) : userService.getUserName(userId, pagination);
        List<UserIdListVo> jsonToList = JsonUtil.getJsonToList(list, UserIdListVo.class);
        if (!jsonToList.isEmpty()) {
            List<String> userIdList = list.stream().map(UserEntity::getId).collect(Collectors.toList());
            List<UserRelationEntity> userRelationList = userRelationService.getRelationByUserIds(userIdList);
            Map<String, List<UserRelationEntity>> userMap = userRelationList.stream()
                    .filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).collect(Collectors.groupingBy(UserRelationEntity::getUserId));
            jsonToList.forEach(t -> {
                t.setRealName(t.getRealName() + "/" + t.getAccount());
                t.setFullName(t.getRealName());
                t.setHeadIcon(UploaderUtil.uploaderImg(t.getHeadIcon()));
                List<UserRelationEntity> listByUserId = userMap.get(user.getId()) != null ? userMap.get(user.getId()) : new ArrayList<>();
                StringJoiner joiner = new StringJoiner(",");
                for (UserRelationEntity relation : listByUserId) {
                    setOrgName(t, relation, joiner);
                }
            });
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(jsonToList, paginationVO);
    }

    private boolean getuserIds(Integer type, UserEntity user, List<String> userId, boolean isDepartmeng) {
        List<String> positionList = new ArrayList<>();
        List<String> organizeList = new ArrayList<>();
        List<UserRelationEntity> userPositionList = userRelationService.getListByUserIdAll(ImmutableList.of(user.getId())).stream()
                .filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).collect(Collectors.toList());
        for (UserRelationEntity relation : userPositionList) {
            PositionEntity positionInfo = positionService.getInfo(relation.getObjectId());
            if (positionInfo != null) {
                positionList.add(positionInfo.getId());
                OrganizeEntity organizeInfo = organizeService.getInfo(positionInfo.getOrganizeId());
                if (organizeInfo != null) {
                    organizeList.add(organizeInfo.getId());
                }
            }
        }

        switch (ExtraRuleEnum.getByCode(type)) {
            case ORGANIZE:
                // 委托范围为同一部门，但委托人的所属组织是公司，无需选人
                for (String organizeId : organizeList) {
                    OrganizeEntity organizeInfo = organizeService.getInfo(organizeId);
                    if (null != organizeInfo && Objects.equals(organizeInfo.getCategory(), PermissionConst.DEPARTMENT)) {
                        userId.addAll(userRelationService.getListByObjectIdAll(ImmutableList.of(organizeId)).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                    }
                }
                break;
            case POSITION:
                for (String positionId : positionList) {
                    userId.addAll(userRelationService.getListByObjectIdAll(ImmutableList.of(positionId)).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                }
                break;
            case DEPARTMENT:
                List<String> categoryList = ImmutableList.of("agency", "office");
                int num = 0;
                for (String organizeId : organizeList) {
                    OrganizeEntity organizeInfo = organizeService.getInfo(organizeId);
                    if (organizeInfo == null || categoryList.contains(organizeInfo.getCategory())) {
                        continue;
                    }
                    List<String> orgList = organizeService.getDepartmentAll(organizeId).stream().map(OrganizeEntity::getId).collect(Collectors.toList());
                    List<String> departmentAll = positionService.getListByOrgIds(orgList).stream().map(PositionEntity::getId).collect(Collectors.toList());
                    for (String id : departmentAll) {
                        userId.addAll(userRelationService.getListByObjectIdAll(ImmutableList.of(id)).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                    }
                    num++;
                }
                isDepartmeng = num == 0;
                break;
            default:
                break;
        }
        return isDepartmeng;
    }

    private void setOrgName(UserIdListVo t, UserRelationEntity relation, StringJoiner joiner) {
        StringJoiner name = new StringJoiner("/");
        PositionEntity position = positionService.getInfo(relation.getObjectId());
        if (position != null) {
            OrganizeEntity organize = organizeService.getInfo(position.getOrganizeId());
            if (organize != null) {
                List<String> organizeIdTree = new ArrayList<>(Arrays.asList(organize.getOrganizeIdTree().split(",")));
                List<OrganizeEntity> organizeList = organizeService.getOrganizeName(organizeIdTree);
                for (String organizeId : organizeIdTree) {
                    OrganizeEntity entity = organizeList.stream().filter(e -> Objects.equals(e.getId(), organizeId)).findFirst().orElse(null);
                    if (entity != null) {
                        name.add(entity.getFullName());
                    }
                }
            }
            List<String> positionIdTree = new ArrayList<>(Arrays.asList(position.getPositionIdTree().split(",")));
            List<PositionEntity> positionList = positionService.getPosList(positionIdTree);
            for (String positionId : positionIdTree) {
                PositionEntity entity = positionList.stream().filter(e -> Objects.equals(e.getId(), positionId)).findFirst().orElse(null);
                if (entity != null) {
                    name.add(entity.getFullName());
                }
            }
        }
        joiner.add(name.toString());
        t.setOrganize(joiner.toString());
    }

    /**
     * 获取岗位人员
     *
     * @param page 页面信息
     * @return ignore
     */
    @Operation(summary = "获取岗位人员")
    @GetMapping("/GetUsersByPositionId")
    public ActionResult<List<UserByRoleVO>> getUsersByPositionId(UsersByPositionModel page) {
        List<UserByRoleVO> list = new ArrayList<>(1);
        String keyword = page.getKeyword();
        // 岗位id
        String positionId = page.getPositionId();
        // 得到关联的组织id
        PositionEntity positionEntity = positionService.getInfo(positionId);
        if (positionEntity != null) {
            UserByRoleVO vo = new UserByRoleVO();
            String organizeId = positionEntity.getOrganizeId();
            // 得到组织信息
            OrganizeEntity organizeEntity = organizeService.getInfo(organizeId);
            if (Objects.nonNull(organizeEntity)) {
                vo.setId(organizeEntity.getId());
                vo.setType(organizeEntity.getCategory());
                if (KeyConst.DEPARTMENT.equals(organizeEntity.getCategory())) {
                    vo.setIcon(PermissionConst.DEPARTMENT_ICON);
                } else {
                    vo.setIcon(PermissionConst.COMPANY_ICON);
                }
                vo.setEnabledMark(organizeEntity.getEnabledMark());
                Map<String, String> orgIdNameMaps = organizeService.getInfoList();
                // 组装组织名称
                String orgName = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/");
                vo.setFullName(orgName);
                // 赋予子集
                List<UserByRoleVO> userByRoleVOS = new ArrayList<>(16);
                List<UserEntity> lists = userService.getListByOrganizeId(organizeEntity.getId(), keyword);
                if (!lists.isEmpty()) {
                    vo.setHasChildren(true);
                    vo.setIsLeaf(false);
                    lists.forEach(t -> {
                        UserByRoleVO userByRoleVO = new UserByRoleVO();
                        userByRoleVO.setParentId(organizeEntity.getId());
                        userByRoleVO.setId(t.getId());
                        userByRoleVO.setFullName(t.getRealName() + "/" + t.getAccount());
                        userByRoleVO.setEnabledMark(t.getEnabledMark());
                        userByRoleVO.setHeadIcon(UploaderUtil.uploaderImg(t.getHeadIcon()));
                        userByRoleVO.setOrganize(organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/"));
                        userByRoleVO.setIsLeaf(true);
                        userByRoleVO.setHasChildren(false);
                        userByRoleVO.setIcon(PermissionConst.USER_ICON);
                        userByRoleVO.setType("user");
                        userByRoleVOS.add(userByRoleVO);
                    });
                    vo.setChildren(userByRoleVOS);
                } else {
                    vo.setHasChildren(false);
                    vo.setIsLeaf(true);
                    vo.setChildren(new ArrayList<>());
                }
                list.add(vo);
            }
        }
        return ActionResult.success(list);
    }

    /**
     * 获取我的下属(不取子集)
     *
     * @param page 页面信息
     * @return ignore
     */
    @Operation(summary = "获取我的下属(不取子集)")
    @Parameter(name = "page", description = "关键字", required = true)
    @PostMapping("/getSubordinates")
    public ActionResult<List<UserIdListVo>> getSubordinates(@RequestBody Page page) {
        Map<String, OrganizeEntity> orgMaps = organizeService.getOrgMaps(null, false, null);
        List<UserEntity> list = userService.getListByManagerId(UserProvider.getUser().getUserId(), page.getKeyword());
        List<UserIdListVo> jsonToList = JsonUtil.getJsonToList(list, UserIdListVo.class);
        Map<String, String> orgIdNameMaps = organizeService.getInfoList();
        jsonToList.forEach(t -> {
            t.setRealName(t.getRealName() + "/" + t.getAccount());
            t.setFullName(t.getRealName());
            List<UserRelationEntity> listByUserId = userRelationService.getListByUserId(t.getId()).stream().filter(ur -> PermissionConst.ORGANIZE.equals(ur.getObjectType())).collect(Collectors.toList());
            StringJoiner stringJoiner = new StringJoiner(",");
            listByUserId.forEach(tt -> {
                OrganizeEntity organizeEntity = orgMaps.get(tt.getObjectId());
                if (organizeEntity != null) {
                    String fullNameByOrgIdTree = organizeService.getFullNameByOrgIdTree(orgIdNameMaps, organizeEntity.getOrganizeIdTree(), "/");
                    if (StringUtil.isNotEmpty(fullNameByOrgIdTree)) {
                        stringJoiner.add(fullNameByOrgIdTree);
                    }
                }
            });
            t.setOrganize(stringJoiner.toString());
            t.setHeadIcon(UploaderUtil.uploaderImg(t.getHeadIcon()));
        });
        return ActionResult.success(jsonToList);
    }

    /**
     * 获取默认当前值用户ID
     *
     * @param userConditionModel 参数
     * @return 执行结构
     * @throws DataException ignore
     */
    @Operation(summary = "获取默认当前值用户ID")
    @Parameter(name = "userConditionModel", description = "参数", required = true)
    @PostMapping("/getDefaultCurrentValueUserId")
    public ActionResult<Map<String, Object>> getDefaultCurrentValueUserId(@RequestBody UserConditionModel userConditionModel) throws DataException {
        String userId = userService.getDefaultCurrentValueUserId(userConditionModel);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("userId", userId);
        return ActionResult.success(MsgCode.SU022.get(), dataMap);
    }

    /**
     * 工作交接
     *
     * @param workHandoverModel 模型
     * @return 执行结构
     */
    @Operation(summary = "工作交接")
    @SaCheckPermission("permission.user")
    @Parameter(name = "workHandoverModel", description = "模型", required = true)
    @PostMapping("/workHandover")
    public ActionResult<Object> workHandover(@RequestBody @Valid WorkHandoverModel workHandoverModel) {
        if (CollUtil.isEmpty(workHandoverModel.getAppList())
                && CollUtil.isEmpty(workHandoverModel.getFlowList())
                && CollUtil.isEmpty(workHandoverModel.getFlowTaskList())) {
            return ActionResult.fail(MsgCode.PS042.get());
        }
        // 开始交接就禁用用户
        UserEntity entity = userService.getInfo(workHandoverModel.getFromId());
        if (StringUtil.isEmpty(workHandoverModel.getHandoverUser())) {
            return ActionResult.fail(MsgCode.PS043.get());
        }

        List<SystemEntity> authListByUser = systemService.getAuthListByUser(workHandoverModel.getFromId(), false);
        List<FlowWorkModel> sysList = JsonUtil.getJsonToList(authListByUser, FlowWorkModel.class);
        Boolean isAppShow = CollUtil.isNotEmpty(sysList) ? true : false;
        if (isAppShow && StringUtil.isEmpty(workHandoverModel.getAppHandoverUser())) {
            return ActionResult.fail(MsgCode.PS044.get());
        }

        UserEntity entitys = userService.getInfo(workHandoverModel.getHandoverUser());
        if (entity == null || entitys == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        if (workHandoverModel.getFromId().equals(workHandoverModel.getHandoverUser()) || workHandoverModel.getFromId().equals(workHandoverModel.getAppHandoverUser())) {
            return ActionResult.fail(MsgCode.PS035.get());
        }
        if (ADMIN_KEY.equals(entitys.getAccount())) {
            return ActionResult.fail(MsgCode.PS034.get());
        }
        try {
            boolean flag = templateApi.flowWork(workHandoverModel);
            if (!flag) {
                return ActionResult.fail(MsgCode.FA101.get());
            }
            systemService.workHandover(workHandoverModel);
            entity.setHandoverMark(1);
            return ActionResult.success(MsgCode.PS033.get());
        } finally {
            userService.updateById(entity);
        }
    }

    /**
     * 获取用户工作详情
     *
     * @return 执行结构
     */
    @Operation(summary = "获取用户工作详情")
    @SaCheckPermission("permission.user")
    @Parameter(name = "userId", description = "主键", required = true)
    @GetMapping("/getWorkByUser")
    public ActionResult<WorkHandoverVo> getWorkByUser(@RequestParam("fromId") String fromId) {
        FlowWorkListVO flowWorkListVO = templateApi.flowWork(fromId);
        if (flowWorkListVO == null) {
            log.error("用户：" + fromId + "，待办事宜及负责流程获取失败");
            flowWorkListVO = new FlowWorkListVO();
        }
        List<SystemEntity> authListByUser = systemService.getAuthListByUser(fromId, false);
        List<FlowWorkModel> sysList = JsonUtil.getJsonToList(authListByUser, FlowWorkModel.class);
        boolean isAppShow = CollUtil.isNotEmpty(sysList);
        WorkHandoverVo workHandoverVo = new WorkHandoverVo(flowWorkListVO.getFlow(), flowWorkListVO.getFlowTask(), sysList, isAppShow);
        return ActionResult.success(workHandoverVo);
    }


    // ----------------------------- 多租户调用

    /**
     * 重置管理员密码
     *
     * @param userResetPasswordForm 修改密码模型
     * @return ignore
     */
    @Operation(summary = "重置管理员密码")
    @Parameter(name = "userResetPasswordForm", description = "修改密码模型", required = true)
    @PutMapping("/Tenant/ResetPassword")
    @NoDataSourceBind
    public ActionResult<String> resetPassword(@RequestBody @Valid TenantReSetPasswordForm userResetPasswordForm) {
        if (configValueUtil.isMultiTenancy()) {
            TenantDataSourceUtil.switchTenant(userResetPasswordForm.getTenantId());
        }
        UserEntity entity = userService.getUserByAccount(ADMIN_KEY);
        if (entity != null) {
            entity.setPassword(userResetPasswordForm.getUserPassword());
            userService.updatePassword(entity);
            userService.logoutUser(MsgCode.PS011.get(), ImmutableList.of(entity.getId()));
            entity.setPassword(userResetPasswordForm.getUserPassword());
            PullUserUtil.syncUser(entity, "modifyPassword", userResetPasswordForm.getTenantId());
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.fail(MsgCode.FA001.get());
    }

    /**
     * 获取用户信息
     *
     * @param tenantId 租户号
     * @return ignore
     */
    @Operation(summary = "获取用户信息")
    @Parameter(name = "tenantId", description = "租户号", required = true)
    @NoDataSourceBind
    @GetMapping("/Tenant/AdminInfo")
    public AdminInfoVO adminInfo(@RequestParam("tenantId") String tenantId) throws DataException {
        if (configValueUtil.isMultiTenancy()) {
            TenantDataSourceUtil.switchTenant(tenantId);
        }
        UserEntity entity = userService.getUserByAccount(ADMIN_KEY);
        return JsonUtil.getJsonToBean(entity, AdminInfoVO.class);
    }

    /**
     * 修改管理员信息
     *
     * @param adminInfoVO 模型
     * @return ignore
     */
    @Operation(summary = "修改管理员信息")
    @Parameter(name = "adminInfoVO", description = "模型", required = true)
    @NoDataSourceBind
    @PutMapping("/Tenant/UpdateAdminInfo")
    public ActionResult<Object> adminInfo(@RequestBody AdminInfoVO adminInfoVO) throws DataException {
        if (configValueUtil.isMultiTenancy()) {
            TenantDataSourceUtil.switchTenant(adminInfoVO.getTenantId());
        }
        UserEntity entity = userService.getUserByAccount(ADMIN_KEY);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        entity.setRealName(adminInfoVO.getRealName());
        entity.setMobilePhone(adminInfoVO.getMobilePhone());
        entity.setEmail(adminInfoVO.getEmail());
        userService.updateById(entity);
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            try {
                //修改用户之后判断是否需要同步到企业微信
                synThirdQyService.updateUserSysToQy(false, entity, "", "");
                //修改用户之后判断是否需要同步到钉钉
                synThirdDingTalkService.updateUserSysToDing(false, entity, "");
            } catch (Exception e) {
                log.error("修改用户之后同步失败到企业微信或钉钉失败，异常：{}", e.getMessage());
            }
        });
        // 删除在线的用户
        PullUserUtil.syncUser(entity, "update", adminInfoVO.getTenantId());
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 移除租户账号在线用户
     *
     * @param tenantId 租户号
     * @return ignore
     */
    @Operation(summary = "移除租户账号在线用户")
    @Parameter(name = "tenantId", description = "租户号", required = true)
    @NoDataSourceBind
    @GetMapping("/Tenant/RemoveOnlineByTenantId")
    public void removeOnlineByTenantId(@RequestParam("tenantId") String tenantId) throws DataException {
        List<String> tokenList = new ArrayList<>();
        List<String> tokens = UserProvider.getLoginUserListToken();
        tokens.forEach(token -> {
            UserInfo userInfo = UserProvider.getUser(token);
            if (tenantId.equals(userInfo.getTenantId())) {
                tokenList.add(token);
            }
        });
        authService.kickoutByToken(tokenList.toArray(new String[0]));
    }

    //--------------------以下导入导出-----

    @Operation(summary = "模板下载")
    @SaCheckPermission("permission.user")
    @GetMapping("/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload() {
        UserColumnMap columnMap = new UserColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(false);
        List<Map<String, Object>> list = columnMap.getDefaultList();
        Map<String, String[]> optionMap = getOptionMap();
        ExcelModel excelModel = ExcelModel.builder().models(models).selectKey(new ArrayList<>(keyMap.keySet())).optionMap(optionMap).build();
        DownloadVO vo = ExcelTool.getImportTemplate(FileTypeConstant.TEMPORARY, excelName, keyMap, list, excelModel);
        return ActionResult.success(vo);
    }

    @Operation(summary = "上传导入Excel")
    @SaCheckPermission("permission.user")
    @PostMapping("/Uploader")
    public ActionResult<Object> uploader() {
        return ExcelTool.uploader();
    }

    @Operation(summary = "导入预览")
    @SaCheckPermission("permission.user")
    @GetMapping("/ImportPreview")
    public ActionResult<Map<String, Object>> importPreview(String fileName) {
        // 导入字段
        UserColumnMap columnMap = new UserColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        Map<String, Object> headAndDataMap = ExcelTool.importPreview(FileTypeConstant.TEMPORARY, fileName, keyMap);
        return ActionResult.success(headAndDataMap);
    }

    /**
     * 导出异常报告
     *
     * @return
     */
    @Operation(summary = "导出异常报告")
    @SaCheckPermission("permission.user")
    @PostMapping("/ExportExceptionData")
    public ActionResult<DownloadVO> exportExceptionData(@RequestBody ExcelImportForm visualImportModel) {
        List<Map<String, Object>> dataList = visualImportModel.getList();
        UserColumnMap columnMap = new UserColumnMap();
        String excelName = columnMap.getExcelName();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(true);
        ExcelModel excelModel = ExcelModel.builder().optionMap(getOptionMap()).models(models).build();
        DownloadVO vo = ExcelTool.exportExceptionReport(FileTypeConstant.TEMPORARY, excelName, keyMap, dataList, excelModel);
        return ActionResult.success(vo);
    }

    /**
     * 导入数据
     *
     * @return
     */
    @Operation(summary = "导入数据")
    @SaCheckPermission("permission.user")
    @PostMapping("/ImportData")
    public ActionResult<ExcelImportVO> importData(@RequestBody ExcelImportForm visualImportModel) {
        List<Map<String, Object>> listData = new ArrayList<>();
        List<Map<String, Object>> headerRow = new ArrayList<>();
        if (visualImportModel.isType()) {
            ActionResult<Map<String, Object>> result = importPreview(visualImportModel.getFileName());
            if (result == null) {
                return ActionResult.fail(MsgCode.FA018.get());
            }
            if (result.getCode() != 200) {
                return ActionResult.fail(result.getMsg());
            }
            if (result.getData() instanceof Map) {
                Map<String, Object> data = result.getData();
                listData = (List<Map<String, Object>>) data.get("dataRow");
                headerRow = (List<Map<String, Object>>) data.get("headerRow");
            }
        } else {
            listData = visualImportModel.getList();
        }
        List<UserEntity> addList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        // 对数据做校验
        this.validateImportData(listData, addList, failList);

        //正常数据插入
        for (UserEntity each : addList) {
            userService.create(each);
        }
        ExcelImportVO importModel = new ExcelImportVO();
        importModel.setSnum(addList.size());
        importModel.setFnum(failList.size());
        importModel.setResultType(!failList.isEmpty() ? 1 : 0);
        importModel.setFailResult(failList);
        importModel.setHeaderRow(headerRow);
        return ActionResult.success(importModel);
    }

    /**
     * 导出Excel
     *
     * @return
     */
    @Operation(summary = "导出Excel")
    @SaCheckPermission("permission.user")
    @GetMapping("/ExportData")
    public ActionResult<Object> exportData(PaginationUser pagination) {
        if (StringUtil.isEmpty(pagination.getSelectKey())) {
            return ActionResult.fail(MsgCode.IMP011.get());
        }

        List<UserEntity> list = userService.getList(pagination, null, false, false, null, null);
        Map<String, String> roleIdAndNameMap = roleService.getList(false, PermissionConst.USER, null)
                .stream().collect(Collectors.toMap(RoleEntity::getId, t -> t.getFullName() + "/" + t.getEnCode()));
        Map<Object, String> posIdAndName = positionService.getPosEncodeAndName(true).entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        //性别
        Map<String, String> sexMap = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.SEX_TYPE.getDictionaryTypeId())
                .stream().collect(Collectors.toMap(DictionaryDataEntity::getEnCode, DictionaryDataEntity::getFullName));
        //职级
        Map<String, String> ranksMap = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.RANK.getDictionaryTypeId())
                .stream().collect(Collectors.toMap(DictionaryDataEntity::getId, DictionaryDataEntity::getFullName));
        //民族
        Map<String, String> nationMap = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.NATION.getDictionaryTypeId())
                .stream().collect(Collectors.toMap(DictionaryDataEntity::getId, DictionaryDataEntity::getFullName));
        //证件类型
        Map<String, String> certificatesTypeMap = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.CERTIFICATE_TYPE.getDictionaryTypeId())
                .stream().collect(Collectors.toMap(DictionaryDataEntity::getId, DictionaryDataEntity::getFullName));
        //文化程度
        Map<String, String> educationMap = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.EDUCATION.getDictionaryTypeId())
                .stream().collect(Collectors.toMap(DictionaryDataEntity::getId, DictionaryDataEntity::getFullName));

        List<RoleRelationEntity> roleRelationList = roleRelationService.getListByRoleId("", PermissionConst.USER);
        List<UserRelationEntity> userRelationList = userRelationService.getListByObjectType(PermissionConst.POSITION);

        List<Map<String, Object>> realList = new ArrayList<>();
        for (UserEntity entity : list) {
            entity.setHeadIcon(UploaderUtil.uploaderImg(entity.getHeadIcon()));
            //获取角色
            List<RoleRelationEntity> userRoleRelation = roleRelationList.stream().filter(t -> t.getObjectId().equals(entity.getId())).collect(Collectors.toList());
            List<String> roleIdList = new ArrayList<>();
            for (RoleRelationEntity ure : userRoleRelation) {
                if (StringUtil.isNotEmpty(roleIdAndNameMap.get(ure.getRoleId()))) {
                    roleIdList.add(roleIdAndNameMap.get(ure.getRoleId()));
                }
            }
            entity.setRoleId(String.join(",", roleIdList));

            // 岗位装配
            List<UserRelationEntity> userPosRelation = userRelationList.stream().filter(t -> t.getUserId().equals(entity.getId())).collect(Collectors.toList());
            List<String> positionIds = new ArrayList<>();
            for (UserRelationEntity ure : userPosRelation) {
                if (posIdAndName.containsKey(ure.getObjectId())) {
                    positionIds.add(posIdAndName.get(ure.getObjectId()));
                }
            }
            entity.setPositionId(String.join(",", positionIds));

            //性别
            entity.setGender(sexMap.get(entity.getGender()));
            entity.setRanks(ranksMap.get(entity.getRanks()));
            entity.setNation(nationMap.get(entity.getNation()));
            entity.setCertificatesType(certificatesTypeMap.get(entity.getCertificatesType()));
            entity.setEducation(educationMap.get(entity.getEducation()));

            Map<String, Object> obj = JsonUtil.entityToMap(entity);
            if (obj.get(KeyConst.ENABLED_MARK) != null) {
                String stateName = "";
                if (Objects.equals(obj.get(KeyConst.ENABLED_MARK), 0)) {
                    stateName = "禁用";
                } else if (Objects.equals(obj.get(KeyConst.ENABLED_MARK), 1)) {
                    stateName = "启用";
                } else if (Objects.equals(obj.get(KeyConst.ENABLED_MARK), 2)) {
                    stateName = "锁定";
                }
                obj.put(KeyConst.ENABLED_MARK, stateName);
            }
            if (obj.get(KeyConst.BIRTHDAY) != null) {
                obj.put(KeyConst.BIRTHDAY, DateUtil.daFormat(Long.parseLong(obj.get(KeyConst.BIRTHDAY).toString())));
            }
            if (obj.get(KeyConst.ENTRY_DATE) != null) {
                obj.put(KeyConst.ENTRY_DATE, DateUtil.daFormat(Long.parseLong(obj.get(KeyConst.ENTRY_DATE).toString())));
            }
            realList.add(obj);
        }
        String[] keys = !StringUtil.isEmpty(pagination.getSelectKey()) ? pagination.getSelectKey() : new String[0];
        UserColumnMap columnMap = new UserColumnMap();
        String excelName = columnMap.getExcelName();
        List<ExcelColumnAttr> models = columnMap.getFieldsModel(false);
        Map<String, String> keyMap = columnMap.getColumnByType(null);
        ExcelModel excelModel = ExcelModel.builder().selectKey(Arrays.asList(keys)).models(models).build();
        DownloadVO vo = ExcelTool.creatModelExcel(FileTypeConstant.TEMPORARY, excelName, keyMap, realList, excelModel);
        return ActionResult.success(vo);
    }

    /**
     * 导入验证
     *
     * @param listData
     * @param addList
     * @param failList
     */
    private void validateImportData(List<Map<String, Object>> listData, List<UserEntity> addList, List<Map<String, Object>> failList) {
        UserColumnMap columnMap = new UserColumnMap();
        Map<String, String> keyMap = columnMap.getColumnByType(0);
        List<DictionaryDataEntity> sexList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.SEX_TYPE.getDictionaryTypeId());
        Map<String, String> genderMap = sexList.stream().collect(Collectors.toMap(DictionaryDataEntity::getFullName, DictionaryDataEntity::getEnCode));
        List<DictionaryDataEntity> rankList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.RANK.getDictionaryTypeId());
        Map<String, String> ranksMap = rankList.stream().collect(Collectors.toMap(DictionaryDataEntity::getFullName, DictionaryDataEntity::getId));
        List<DictionaryDataEntity> nationList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.NATION.getDictionaryTypeId());
        Map<String, String> nationMap = nationList.stream().collect(Collectors.toMap(DictionaryDataEntity::getFullName, DictionaryDataEntity::getId));
        List<DictionaryDataEntity> certificateList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.CERTIFICATE_TYPE.getDictionaryTypeId());
        Map<String, String> certificateMap = certificateList.stream().collect(Collectors.toMap(DictionaryDataEntity::getFullName, DictionaryDataEntity::getId));
        List<DictionaryDataEntity> educationList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.EDUCATION.getDictionaryTypeId());
        Map<String, String> educationMap = educationList.stream().collect(Collectors.toMap(DictionaryDataEntity::getFullName, DictionaryDataEntity::getId));

        List<PositionEntity> allPos = positionService.getList(true);
        Map<String, Object> posNameMap = positionService.getPosEncodeAndName(true);
        List<RoleEntity> allUserRole = roleService.getList(true, PermissionConst.USER, null);
        Map<String, String> roleMap = allUserRole.stream().collect(Collectors.toMap(t -> t.getFullName(), RoleEntity::getId));
        //所有岗位的用户列表
        List<UserRelationEntity> posAllUser = userRelationService.getListByObjectType(PermissionConst.POSITION);
        //所有用户角色的用户列表
        List<RoleRelationEntity> roleAllUser = roleRelationService.getListByRoleId("", PermissionConst.USER);

        for (int i = 0, len = listData.size(); i < len; i++) {
            Map<String, Object> eachMap = listData.get(i);
            Map<String, Object> realMap = JsonUtil.getJsonToBean(eachMap, Map.class);
            StringJoiner errInfo = new StringJoiner(",");

            // 开启多租住的-判断用户额度
            String tenantId = UserProvider.getUser().getTenantId();
            if (StringUtil.isNotEmpty(tenantId)) {
                TenantVO cacheTenantInfo = TenantDataSourceUtil.getCacheTenantInfo(tenantId);
                long count = userService.count() + addList.size();
                if (cacheTenantInfo.getAccountNum() != 0 && cacheTenantInfo.getAccountNum() < count) {
                    eachMap.put("errorsInfo", MsgCode.PS009.get());
                    failList.add(eachMap);
                    continue;
                }
            }

            //组织多选id，用于查询角色和岗位
            for (Map.Entry<String, String> columnItem : keyMap.entrySet()) {
                String column = columnItem.getKey();
                String columnName = columnItem.getValue();
                Object valueObj = eachMap.get(column);
                String value = valueObj == null ? null : String.valueOf(valueObj);
                switch (column) {
                    case KeyConst.GENDER:
                        if (StringUtils.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if (genderMap.containsKey(valueObj.toString())) {
                            realMap.put(KeyConst.GENDER, genderMap.get(value));
                        } else {
                            errInfo.add("找不到该" + columnName + "值");
                        }
                        break;
                    case KeyConst.RANKS:
                        if (StringUtils.isNotEmpty(value)) {
                            if (ranksMap.containsKey(valueObj.toString())) {
                                realMap.put(KeyConst.RANKS, ranksMap.get(value));
                            } else {
                                errInfo.add("找不到该" + columnName + "值");
                            }
                        }
                        break;
                    case KeyConst.NATION:
                        if (StringUtils.isNotEmpty(value)) {
                            if (nationMap.containsKey(valueObj.toString())) {
                                realMap.put(KeyConst.NATION, nationMap.get(value));
                            } else {
                                errInfo.add("找不到该" + columnName + "值");
                            }
                        }
                        break;
                    case KeyConst.CERTIFICATES_TYPE:
                        if (StringUtils.isNotEmpty(value)) {
                            if (certificateMap.containsKey(valueObj.toString())) {
                                realMap.put(KeyConst.CERTIFICATES_TYPE, certificateMap.get(value));
                            } else {
                                errInfo.add("找不到该" + columnName + "值");
                            }
                        }
                        break;
                    case KeyConst.EDUCATION:
                        if (StringUtils.isNotEmpty(value)) {
                            if (educationMap.containsKey(valueObj.toString())) {
                                realMap.put(KeyConst.EDUCATION, educationMap.get(value));
                            } else {
                                errInfo.add("找不到该" + columnName + "值");
                            }
                        }
                        break;
                    case "account":
                        //账号
                        if (StringUtils.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if (50 < value.length()) {
                            errInfo.add(columnName + "值超出最多输入字符限制");
                        }
                        //值不能含有特殊符号
                        if (!RegexUtils.checkSpecoalSymbols(value)) {
                            errInfo.add(columnName + "值不能含有特殊符号");
                        }
                        //库里重复
                        if (userService.isExistByAccount(value)) {
                            errInfo.add(columnName + "值已存在");
                            break;
                        }
                        //表格内重复
                        long enCodeCount = addList.stream().filter(t -> t.getAccount().equals(value)).count();
                        if (enCodeCount > 0) {
                            errInfo.add(columnName + "值已存在");
                            break;
                        }
                        break;
                    case "realName":
                        if (StringUtils.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if (50 < value.length()) {
                            errInfo.add(columnName + "值超出最多输入字符限制");
                        }
                        //值不能含有特殊符号
                        if (!RegexUtils.checkSpecoalSymbols(value)) {
                            errInfo.add(columnName + "值不能含有特殊符号");
                        }
                        break;
                    case KeyConst.POSITION_ID:
                        if (StringUtils.isNotEmpty(value)) {
                            List<String> posIds = new ArrayList<>();
                            List<String> posErrList = new ArrayList<>();
                            List<String> listName = Arrays.asList(value.split(","));
                            for (String item : listName) {
                                if (posNameMap.containsKey(item)) {
                                    posIds.add(posNameMap.get(item).toString());
                                } else {
                                    posErrList.add(item);
                                }
                            }
                            if (!posErrList.isEmpty()) {
                                if (listName.size() > 1) {
                                    errInfo.add("找不到该" + columnName + "(" + String.join("、", posErrList) + ")");
                                    break;
                                } else {
                                    errInfo.add("找不到该" + columnName);
                                    break;
                                }
                            }
                            if (CollUtil.isNotEmpty(posIds)) {
                                //判断约束
                                for (String item : posIds) {
                                    PositionEntity info = allPos.stream().filter(t -> t.getId().equals(item)).findFirst().orElse(null);
                                    //库里岗位用户数
                                    List<UserRelationEntity> thisPosUsers = posAllUser.stream().filter(t -> t.getObjectId().equals(item)).collect(Collectors.toList());
                                    //表格里的岗位用户数
                                    List<UserEntity> excelUserList = addList.stream().filter(t -> t.getPositionId() != null && t.getPositionId().contains(item)).collect(Collectors.toList());
                                    int userNum = thisPosUsers.size() + excelUserList.size();
                                    if (null != info && Objects.equals(info.getIsCondition(), 1)) {
                                        PosConModel conModelP = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
                                        conModelP.init();
                                        if (conModelP.getMutualExclusionFlag() && conModelP.getMutualExclusion().stream().anyMatch(posIds::contains)) {
                                            errInfo.add(info.getFullName() + "存在互斥岗位");
                                        } else if (conModelP.getNumFlag() && userNum >= conModelP.getUserNum()) {
                                            errInfo.add(info.getFullName() + "岗位已达用户限制个数");
                                        } else if (conModelP.getPrerequisiteFlag() && !new HashSet<>(posIds).containsAll(conModelP.getPrerequisite())) {
                                            errInfo.add(info.getFullName() + "存在先决岗位");
                                        }
                                    }
                                }
                                realMap.put(KeyConst.POSITION_ID, String.join(",", posIds));
                                Set<String> orgList = allPos.stream().filter(t -> posIds.contains(t.getId())).map(PositionEntity::getOrganizeId).collect(Collectors.toSet());
                                realMap.put(KeyConst.ORGANIZE_ID, String.join(",", orgList));
                                break;
                            }
                            break;
                        }
                        break;
                    case KeyConst.ROLE_ID:
                        if (StringUtils.isNotEmpty(value)) {
                            List<String> roleIds = new ArrayList<>();
                            List<String> roleErrList = new ArrayList<>();
                            List<String> listName = Arrays.asList(value.split(","));
                            for (String item : listName) {
                                if (roleMap.containsKey(item)) {
                                    roleIds.add(roleMap.get(item));
                                } else {
                                    roleErrList.add(item);
                                }
                            }
                            if (!roleErrList.isEmpty()) {
                                if (listName.size() > 1) {
                                    errInfo.add("找不到该" + columnName + "(" + String.join("、", roleErrList) + ")");
                                    break;
                                } else {
                                    errInfo.add("找不到该" + columnName);
                                    break;
                                }
                            }
                            if (CollUtil.isNotEmpty(roleIds)) {
                                //判断约束
                                for (String item : roleIds) {
                                    RoleEntity info = allUserRole.stream().filter(t -> t.getId().equals(item)).findFirst().orElse(null);
                                    List<RoleRelationEntity> thisRoleUsers = roleAllUser.stream().filter(t -> t.getRoleId().equals(item)).collect(Collectors.toList());
                                    //表格里的岗位用户数
                                    List<UserEntity> excelUserList = addList.stream().filter(t -> t.getRoleId() != null && t.getRoleId().contains(item)).collect(Collectors.toList());
                                    int userNum = thisRoleUsers.size() + excelUserList.size();
                                    if (null != info && Objects.equals(info.getIsCondition(), 1)) {
                                        PosConModel conModelP = JsonUtil.getJsonToBean(info.getConditionJson(), PosConModel.class);
                                        conModelP.init();
                                        if (conModelP.getMutualExclusionFlag() && conModelP.getMutualExclusion().stream().anyMatch(roleIds::contains)) {
                                            errInfo.add(info.getFullName() + "存在互斥角色");
                                        } else if (conModelP.getNumFlag() && userNum >= conModelP.getUserNum()) {
                                            errInfo.add(info.getFullName() + "角色已达用户限制个数");
                                        } else if (conModelP.getPrerequisiteFlag() && !new HashSet<>(roleIds).containsAll(conModelP.getPrerequisite())) {
                                            errInfo.add(info.getFullName() + "存在先决角色");
                                        }
                                    }
                                }
                                realMap.put(KeyConst.ROLE_ID, String.join(",", roleIds));
                                break;
                            }
                        }
                        break;
                    case KeyConst.ENTRY_DATE:
                        if (StringUtil.isNotEmpty(value)) {
                            Date date = DateUtil.checkDate(value, "yyyy-MM-dd");
                            if (date == null) {
                                errInfo.add(columnName + "值不正确");
                                break;
                            }
                            realMap.put(KeyConst.ENTRY_DATE, date);
                        }
                        break;
                    case KeyConst.BIRTHDAY:
                        if (StringUtil.isNotEmpty(value)) {
                            Date date = DateUtil.checkDate(value, "yyyy-MM-dd");
                            if (date == null) {
                                errInfo.add(columnName + "值不正确");
                                break;
                            }
                            realMap.put(KeyConst.BIRTHDAY, date);
                        }
                        break;
                    case KeyConst.SORT_CODE:
                        if (StringUtil.isEmpty(value)) {
                            realMap.put(KeyConst.SORT_CODE, 0);
                            break;
                        }
                        Long numValue = 0l;
                        try {
                            numValue = Long.parseLong(value);
                        } catch (Exception e) {
                            errInfo.add(columnName + "值不正确");
                            break;
                        }
                        if (numValue < 0) {
                            errInfo.add(columnName + "值不能小于0");
                            break;
                        }
                        if (numValue > 1000000) {
                            errInfo.add(columnName + "值不能大于999999");
                            break;
                        }
                        break;
                    case KeyConst.ENABLED_MARK:
                        if (StringUtil.isEmpty(value)) {
                            errInfo.add(columnName + "不能为空");
                            break;
                        }
                        if ("启用".equals(value)) {
                            realMap.put(KeyConst.ENABLED_MARK, 1);
                        } else if ("禁用".equals(value)) {
                            realMap.put(KeyConst.ENABLED_MARK, 0);
                        } else if ("锁定".equals(value)) {
                            realMap.put(KeyConst.ENABLED_MARK, 2);
                        } else {
                            errInfo.add("找不到该" + columnName + "值");
                        }
                        break;
                    default:
                        break;
                }

            }

            if (errInfo.length() == 0) {
                UserEntity entity = JsonUtil.getJsonToBean(realMap, UserEntity.class);
                entity.setCreatorTime(new Date());
                addList.add(entity);
            } else {
                eachMap.put("errorsInfo", errInfo.toString());
                failList.add(eachMap);
            }
        }
    }


    /**
     * 获取下拉框
     *
     * @return
     */
    private Map<String, String[]> getOptionMap() {
        Map<String, String[]> optionMap = new HashMap<>();
        //性别
        List<DictionaryDataEntity> sexList = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.SEX_TYPE.getDictionaryTypeId());
        String[] gender = sexList.stream().map(DictionaryDataEntity::getFullName).toArray(String[]::new);
        optionMap.put(KeyConst.GENDER, gender);
        //职级
        List<DictionaryDataEntity> ranksList = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.RANK.getDictionaryTypeId());
        String[] ranks = ranksList.stream().map(DictionaryDataEntity::getFullName).toArray(String[]::new);
        optionMap.put(KeyConst.RANKS, ranks);
        //状态
        optionMap.put(KeyConst.ENABLED_MARK, new String[]{"启用", "禁用", "锁定"});
        //民族
        List<DictionaryDataEntity> nationList = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.NATION.getDictionaryTypeId());
        String[] nation = nationList.stream().map(DictionaryDataEntity::getFullName).toArray(String[]::new);
        optionMap.put(KeyConst.NATION, nation);
        //证件类型
        List<DictionaryDataEntity> certificatesTypeList = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.CERTIFICATE_TYPE.getDictionaryTypeId());
        String[] certificatesType = certificatesTypeList.stream().map(DictionaryDataEntity::getFullName).toArray(String[]::new);
        optionMap.put(KeyConst.CERTIFICATES_TYPE, certificatesType);
        //文化程度
        List<DictionaryDataEntity> educationList = dictionaryDataApi.getByTypeCodeEnable(DictionaryDataEnum.EDUCATION.getDictionaryTypeId());
        String[] education = educationList.stream().map(DictionaryDataEntity::getFullName).toArray(String[]::new);
        optionMap.put(KeyConst.EDUCATION, education);
        return optionMap;
    }
}
