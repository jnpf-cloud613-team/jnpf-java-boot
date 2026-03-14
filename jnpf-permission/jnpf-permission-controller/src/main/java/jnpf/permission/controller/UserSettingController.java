package jnpf.permission.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.UserInfo;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.sign.SignForm;
import jnpf.base.model.sign.SignListVO;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SignService;
import jnpf.base.service.SysconfigService;
import jnpf.base.service.SystemService;
import jnpf.base.service.impl.UserOnlineServiceImpl;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.consts.DeviceType;
import jnpf.database.util.LoginSaasUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.entity.LogEntity;
import jnpf.exception.LoginException;
import jnpf.message.model.UserOnlineModel;
import jnpf.message.model.websocket.UserOnLineModelVo;
import jnpf.model.BaseSystemInfo;
import jnpf.model.PaginationLogModel;
import jnpf.model.UserLogVO;
import jnpf.model.login.LoginSaasVo;
import jnpf.model.login.TenantInfo;
import jnpf.model.tenant.TenantVO;
import jnpf.permission.entity.*;
import jnpf.permission.mapper.UserOldPasswordMapper;
import jnpf.permission.model.position.PosistionCurrentModel;
import jnpf.permission.model.tenant.JoinCompanyModel;
import jnpf.permission.model.tenant.TenantJoinForm;
import jnpf.permission.model.user.UserAuthForm;
import jnpf.permission.model.user.form.*;
import jnpf.permission.model.user.vo.UserAuthorizeVO;
import jnpf.permission.model.user.vo.UserBaseInfoVO;
import jnpf.permission.model.user.vo.UserSubordinateVO;
import jnpf.permission.rest.PullUserUtil;
import jnpf.permission.service.*;
import jnpf.permission.util.AuthPermUtil;
import jnpf.permission.util.PermissionUtil;
import jnpf.service.LogService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static jnpf.util.Constants.ADMIN_KEY;

/**
 * 个人资料
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "个人资料", description = "CurrentUsersInfo")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Users/Current")
@Slf4j
public class UserSettingController {
    public static final String APP_CODE = "APP";
    public static final String PC_CODE = "PC";
    private final UserService userService;
    private final AuthorizeService authorizeService;
    private final LogService logService;
    private final RedisUtil redisUtil;
    private final PositionService positionService;
    private final OrganizeService organizeService;
    private final CacheKeyUtil cacheKeyUtil;
    private final UserRelationService userRelationService;
    private final SystemService systemService;
    private final SignService signService;
    private final SysconfigService sysConfigApi;
    private final UserOldPasswordMapper userOldPasswordMapper;
    private final ConfigValueUtil configValueUtil;
    private final DictionaryDataService dictionaryDataApi;
    private final UserExtraService userExtraService;
    private final AuthPermUtil authPermUtil;
    private final RoleRelationService roleRelationService;

    /**
     * 我的信息
     *
     * @return
     */
    @Operation(summary = "个人资料")
    @GetMapping("/BaseInfo")
    public ActionResult<UserBaseInfoVO> get() {


        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = userService.getInfo(userInfo.getUserId());

        //获取用户额外信息
        UserExtraEntity userExtraEntity = userExtraService.getUserExtraByUserId(userEntity.getId());

        String catchKey = cacheKeyUtil.getAllUser();
        if (redisUtil.exists(catchKey)) {
            redisUtil.remove(catchKey);
        }

        UserBaseInfoVO vo = JsonUtil.getJsonToBean(userEntity, UserBaseInfoVO.class);
        BeanUtil.copyProperties(userExtraEntity, vo, "id");

        if (StringUtil.isNotEmpty(userEntity.getManagerId())) {
            UserEntity menager = userService.getInfo(userEntity.getManagerId());
            vo.setManager(menager != null && !ObjectUtil.equal(menager.getEnabledMark(), 0) ? menager.getRealName() + "/" + menager.getAccount() : "");
        }

        //设置语言和主题
        vo.setLanguage(userEntity.getLanguage() != null ? userEntity.getLanguage() : "zh-CN");
        vo.setTheme(userEntity.getTheme() != null ? userEntity.getTheme() : "W-001");

        // 获取组织
        vo.setOrganize(PermissionUtil.getLinkInfoByOrgId(userInfo.getOrganizeId(), organizeService, false));

        // 获取主要岗位
        List<PositionEntity> positionEntityList = positionService.getListByOrgIdAndUserId(userInfo.getOrganizeId(), userEntity.getId());
        if (!positionEntityList.isEmpty()) {
            List<String> fullNames = positionEntityList.stream().map(PositionEntity::getFullName).collect(Collectors.toList());
            vo.setPosition(String.join(",", fullNames));
        }

        // 获取用户
        if (StringUtil.isNotEmpty(userInfo.getTenantId())) {
            String account = userInfo.getTenantId() + "@" + vo.getAccount();
            if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
                account = vo.getAccount() + "@" + userInfo.getTenantId();
            }
            vo.setAccount(account);
        }

        // 获取用户头像
        if (!StringUtil.isEmpty(userInfo.getUserIcon())) {
            vo.setAvatar(UploaderUtil.uploaderImg(userInfo.getUserIcon()));
        }
        vo.setBirthday(userEntity.getBirthday() != null ? userEntity.getBirthday().getTime() : null);
        DictionaryDataEntity dictionaryDataEntity3 = dictionaryDataApi.getInfo(userEntity.getRanks());
        vo.setRanks(dictionaryDataEntity3 != null && ObjectUtil.equal(dictionaryDataEntity3.getEnabledMark(), 1) ? dictionaryDataEntity3.getFullName() : null);
        // 多租户
        String tenantId = UserProvider.getUser().getTenantId();
        if (StringUtil.isNotEmpty(tenantId)) {
            vo.setIsTenant(true);
        }

        //获取在线用户
        List<UserOnlineModel> userOnlineModels = UserOnlineServiceImpl.getUserOnlineModels();
        Map<String, List<UserOnLineModelVo>> collect = userOnlineModels.stream()
                .filter(it -> it.getUserId().equals(userInfo.getUserId()))
                .map(it -> {
                    UserOnLineModelVo userOnLineModelVo = JsonUtil.getJsonToBean(it, UserOnLineModelVo.class);
                    userOnLineModelVo.setIsCurrent(it.getToken().equals(UserProvider.getUser().getToken().replace("bearer ", "")));
                    return userOnLineModelVo;
                })
                .collect(Collectors.groupingBy(UserOnLineModelVo::getDevice));

        vo.setPcOnlineModelList(Optional.ofNullable(collect.get(PC_CODE)).orElse(Collections.emptyList()));
        vo.setAppOnlineModelList(Optional.ofNullable(collect.get(APP_CODE)).orElse(Collections.emptyList()));


        return ActionResult.success(vo);
    }

    /**
     * 我的权限
     *
     * @return
     */
    @Operation(summary = "系统权限")
    @GetMapping("/Authorize")
    public ActionResult<UserAuthorizeVO> getList() {
//        全部应用的权限
        return ActionResult.success(authPermUtil.getUserAuth(new UserAuthForm()));

    }

    /**
     * 系统日志
     *
     * @param pagination 页面参数
     * @return
     */
    @Operation(summary = "系统日志")
    @GetMapping("/SystemLog")
    public ActionResult<PageListVO<UserLogVO>> getLogList(PaginationLogModel pagination) {
        List<LogEntity> data = logService.getList(pagination.getCategory(), pagination, true);
        List<UserLogVO> loginLogVOList = JsonUtil.getJsonToList(data, UserLogVO.class);
        for (int i = 0; i < loginLogVOList.size(); i++) {
            loginLogVOList.get(i).setAbstracts(data.get(i).getDescription());
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(loginLogVOList, paginationVO);
    }

    /**
     * 修改用户资料
     *
     * @param userInfoForm 页面参数
     * @return
     */
    @Operation(summary = "修改用户资料")
    @Parameter(name = "userInfoForm", description = "页面参数", required = true)
    @PutMapping("/BaseInfo")
    public ActionResult<String> updateInfo(@RequestBody UserInfoForm userInfoForm) {
        UserEntity userEntity = userService.getInfo(UserProvider.getUser().getUserId());
        userEntity.setBirthday(userInfoForm.getBirthday() == null ? null : new Date(userInfoForm.getBirthday()));
        userEntity.setCertificatesNumber(userInfoForm.getCertificatesNumber());
        userEntity.setCertificatesType(userInfoForm.getCertificatesType());
        userEntity.setEducation(userInfoForm.getEducation());
        userEntity.setEmail(userInfoForm.getEmail());
        userEntity.setGender(userInfoForm.getGender());
        userEntity.setLandline(userInfoForm.getLandline());
        userEntity.setMobilePhone(userInfoForm.getMobilePhone());
        userEntity.setNation(userInfoForm.getNation());
        userEntity.setNativePlace(userInfoForm.getNativePlace());
        userEntity.setPostalAddress(userInfoForm.getPostalAddress());
        userEntity.setRealName(userInfoForm.getRealName());
        userEntity.setSignature(userInfoForm.getSignature());
        userEntity.setTelePhone(userInfoForm.getTelePhone());
        userEntity.setUrgentContacts(userInfoForm.getUrgentContacts());
        userEntity.setUrgentTelePhone(userInfoForm.getUrgentTelePhone());
        UserExtraEntity userExtraEntity = BeanUtil.copyProperties(userInfoForm, UserExtraEntity.class);
        userExtraEntity.setUserId(userEntity.getId());
        UserExtraEntity userExtraByUserId = userExtraService.getUserExtraByUserId(userExtraEntity.getUserId());
        if (null != userExtraByUserId) {
            userExtraEntity.setId(userExtraByUserId.getId());
        }
        userExtraService.saveOrUpdate(userExtraEntity);
        userService.updateById(userEntity);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 修改用户密码
     *
     * @param userModifyPasswordForm 用户修改密码表单
     * @return
     */
    @Operation(summary = "修改用户密码")
    @Parameter(name = "userModifyPasswordForm", description = "用户修改密码表单", required = true)
    @PostMapping("/Actions/ModifyPassword")
    public ActionResult<Object> modifyPassword(@RequestBody @Valid UserModifyPasswordForm userModifyPasswordForm) {
        UserEntity userEntity = userService.getInfo(UserProvider.getUser().getUserId());
        if (userEntity != null) {
            String timestamp = String.valueOf(redisUtil.getString(userModifyPasswordForm.getTimestamp()));
            if (!userModifyPasswordForm.getCode().equalsIgnoreCase(timestamp)) {
                return ActionResult.fail(MsgCode.LOG104.get());
            }
            if (!Md5Util.getStringMd5((userModifyPasswordForm.getOldPassword().toLowerCase() + userEntity.getSecretkey().toLowerCase())).equals(userEntity.getPassword())) {
                return ActionResult.fail(MsgCode.LOG201.get());
            }
            //禁用旧密码
            String disableOldPassword = sysConfigApi.getValueByKey("disableOldPassword");
            if (disableOldPassword.equals("1")) {
                String disableTheNumberOfOldPasswords = sysConfigApi.getValueByKey("disableTheNumberOfOldPasswords");
                List<UserOldPasswordEntity> userOldPasswordList = userOldPasswordMapper.getList(UserProvider.getLoginUserId());
                userOldPasswordList = userOldPasswordList.stream().limit(Long.valueOf(disableTheNumberOfOldPasswords)).collect(Collectors.toList());
                for (UserOldPasswordEntity userOldPassword : userOldPasswordList) {
                    String newPassword = Md5Util.getStringMd5(userModifyPasswordForm.getPassword().toLowerCase() + userOldPassword.getSecretkey().toLowerCase());
                    if (userOldPassword.getOldPassword().equals(newPassword)) {
                        return ActionResult.fail(MsgCode.LOG204.get());
                    }
                }
            }
            userEntity.setPassword(userModifyPasswordForm.getPassword());
            userService.updatePassword(userEntity);
            userService.logoutUser(MsgCode.PS011.get(), Arrays.asList(userEntity.getId()));
            UserProvider.logoutByUserId(userEntity.getId());
            userEntity.setPassword(userModifyPasswordForm.getPassword());
            PullUserUtil.syncUser(userEntity, "modifyPassword", UserProvider.getUser().getTenantId());
            return ActionResult.success(MsgCode.PS011.get());
        }
        return ActionResult.fail(MsgCode.LOG203.get());

    }

    /**
     * 我的下属
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "我的下属")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/Subordinate/{id}")
    public ActionResult<List<UserSubordinateVO>> getSubordinate(@PathVariable("id") String id) {
        List<UserEntity> userName = new ArrayList<>(16);
        List<UserSubordinateVO> list = new ArrayList<>();
        if ("0".equals(id)) {
            if (Objects.isNull(UserProvider.getUser()) || StringUtil.isEmpty(UserProvider.getUser().getUserId())) {
                return ActionResult.success(list);
            }
            userName.add(userService.getInfo(UserProvider.getUser().getUserId()));
        } else {
            userName = new ArrayList<>(userService.getListByManagerId(id, null));
        }
        List<String> department = userName.stream().map(t -> t.getOrganizeId()).collect(Collectors.toList());
        List<OrganizeEntity> departmentList = organizeService.getOrganizeName(department);
        for (UserEntity user : userName) {
            String departName = departmentList.stream().filter(
                    t -> String.valueOf(user.getOrganizeId()).equals(String.valueOf(t.getId()))
            ).findFirst().orElse(new OrganizeEntity()).getFullName();
            PositionEntity entity = null;
            if (StringUtil.isNotEmpty(user.getPositionId())) {
                String[] split = user.getPositionId().split(",");
                for (String positionId : split) {
                    entity = positionService.getInfo(positionId);
                    if (Objects.nonNull(entity)) {
                        break;
                    }
                }
            }
            UserSubordinateVO subordinateVO = UserSubordinateVO.builder()
                    .id(user.getId())
                    .avatar(UploaderUtil.uploaderImg(user.getHeadIcon()))
                    .department(departName)
                    .userName(user.getRealName() + "/" + user.getAccount())
                    .position(entity != null ? entity.getFullName() : null)
                    .isLeaf(false).build();
            list.add(subordinateVO);
        }
        return ActionResult.success(list);
    }

    /**
     * 修改系统主题
     *
     * @param userThemeForm 主题模板
     * @return
     */
    @Operation(summary = "修改系统主题")
    @Parameter(name = "userThemeForm", description = "主题模板", required = true)
    @PutMapping("/SystemTheme")
    public ActionResult<Object> updateTheme(@RequestBody @Valid UserThemeForm userThemeForm) {
        UserEntity entity = JsonUtil.getJsonToBean(userThemeForm, UserEntity.class);
        entity.setId(UserProvider.getUser().getUserId());
        userService.updateById(entity);
        return ActionResult.success(MsgCode.SU016.get());
    }

    /**
     * 修改头像
     *
     * @param name 名称
     * @return
     */
    @Operation(summary = "修改头像")
    @Parameter(name = "name", description = "名称", required = true)
    @PutMapping("/Avatar/{name}")
    public ActionResult<Object> updateAvatar(@PathVariable("name") String name) {
        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = userService.getInfo(userInfo.getUserId());
        userEntity.setHeadIcon(name);
        userService.update(userEntity.getId(), userEntity);
        if (!StringUtil.isEmpty(userInfo.getId())) {
            userInfo.setUserIcon(name);
            UserProvider.setLoginUser(userInfo);
            UserProvider.setLocalLoginUser(userInfo);
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 修改系统语言
     *
     * @param userLanguageForm 修改语言模型
     * @return
     */
    @Operation(summary = "修改系统语言")
    @Parameter(name = "userLanguageForm", description = "修改语言模型", required = true)
    @PutMapping("/SystemLanguage")
    public ActionResult<Object> updateLanguage(@RequestBody @Valid UserLanguageForm userLanguageForm) {
        UserEntity userEntity = userService.getInfo(UserProvider.getUser().getUserId());
        userEntity.setLanguage(userLanguageForm.getLanguage());
        userService.updateById(userEntity);
        return ActionResult.success(MsgCode.SU016.get());
    }

    @Operation(summary = "设置默认岗位/切换身份(岗位不切换权限)")
    @Parameter(name = "userSettingForm", description = "页面参数", required = true)
    @PutMapping("/major")
    public ActionResult<String> defaultOrganize(@RequestBody UserSettingForm userSettingForm) {
        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = userService.getInfo(userInfo.getUserId());
        if (userEntity == null) {
            return ActionResult.fail(ActionResultCode.SESSIONOVERDUE.getCode(), ActionResultCode.SESSIONOVERDUE.getMessage());
        }
        UserEntity updateUser = new UserEntity();
        switch (userSettingForm.getMajorType()) {
            //切岗位（组织也被切了）
            case PermissionConst.POSITION:
                PositionEntity info = positionService.getInfo(userSettingForm.getMajorId());
                updateUser.setOrganizeId(info.getOrganizeId());
                updateUser.setPositionId(userSettingForm.getMajorId());
                break;
            case PermissionConst.STAND:
                ActionResult<String> res = setStand(userSettingForm, userInfo, updateUser, userEntity);
                if (res != null) return res;
                break;
            case PermissionConst.SYSTEM:
                //app切换系统
                SystemEntity systemEntity = systemService.getInfo(userSettingForm.getMajorId());
                if (systemEntity == null) {
                    return ActionResult.fail(MsgCode.PS031.get());
                }
                if (systemEntity.getEnabledMark() == 0) {
                    return ActionResult.fail(MsgCode.PS014.get());
                }
                List<ModuleModel> moduleList = authorizeService.getAuthorizeByUser(false).getModuleList()
                        .stream().filter(t -> StringUtil.isNotEmpty(t.getSystemId()) && t.getSystemId().equals(userSettingForm.getMajorId())).collect(Collectors.toList());
                Map<String, List<ModuleModel>> map = moduleList.stream().collect(Collectors.groupingBy(t -> {
                    if (JnpfConst.WEB.equals(t.getCategory())) {
                        return JnpfConst.WEB;
                    } else {
                        return JnpfConst.APP;
                    }
                }));
                List<ModuleModel> appModule = map.containsKey(JnpfConst.APP) ? map.get(JnpfConst.APP) : new ArrayList<>();
                if (Objects.equals(userSettingForm.getMenuType(), 1) && appModule.isEmpty()) {
                    return ActionResult.fail(MsgCode.FA027.get());
                }
                if (userSettingForm.getMenuType() != null && userSettingForm.getMenuType() == 1) {
                    updateUser.setAppSystemId(userSettingForm.getMajorId());
                    userInfo.setAppSystemId(userSettingForm.getMajorId());
                    UserProvider.setLoginUser(userInfo);
                    UserProvider.setLocalLoginUser(userInfo);
                }
                updateUser.setId(userEntity.getId());
                userService.updateById(updateUser);

                return ActionResult.success(MsgCode.SU005.get());
            default:
                break;
        }
        updateUser.setId(userEntity.getId());
        userService.updateById(updateUser);
        authorizeService.removeAuthByUserOrMenu(Arrays.asList(userInfo.getUserId()), null);
        if (PermissionConst.STAND.equals(userSettingForm.getMajorType())) {
            userService.majorStandFreshUser();
        }
        return ActionResult.success(MsgCode.SU016.get());
    }

    private @Nullable ActionResult<String> setStand(UserSettingForm userSettingForm, UserInfo userInfo, UserEntity updateUser, UserEntity userEntity) {
        if (DeviceType.PC.getDevice().equals(userInfo.getLoginDevice())) {
            updateUser.setStanding(userSettingForm.getMajorId());
        } else {
            updateUser.setAppStanding(userSettingForm.getMajorId());

            List<AuthorizeEntity> standingList = authorizeService.getAuthorizeByItem(PermissionConst.STAND, userSettingForm.getMajorId());
            List<String> posAndRoles = new ArrayList<>();
            List<String> stdPos = standingList.stream().filter(t -> userInfo.getPositionIds().contains(t.getObjectId())
                    && PermissionConst.POSITION.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
            //获取当前岗位角色
            List<String> stdPosRole = roleRelationService.getListByObjectId(stdPos, null)
                    .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
            List<String> stdRole = standingList.stream().filter(t -> userInfo.getRoleIds().contains(t.getObjectId())
                    && PermissionConst.ROLE.equals(t.getObjectType())).map(AuthorizeEntity::getObjectId).collect(Collectors.toList());
            posAndRoles.addAll(stdPos);
            posAndRoles.addAll(stdPosRole);
            posAndRoles.addAll(stdRole);

            List<SystemEntity> allSysList = systemService.getList();
            List<String> mainSysIds = allSysList.stream().filter(t -> Objects.equals(t.getIsMain(), 1)).map(SystemEntity::getId).collect(Collectors.toList());

            List<AuthorizeEntity> sysList = authorizeService.getListByObjectId(posAndRoles).stream()
                    .filter(t -> PermissionConst.SYSTEM.equals(t.getItemType()) && !mainSysIds.contains(t.getItemId())).collect(Collectors.toList());
            if (CollUtil.isEmpty(sysList)) {
                return ActionResult.fail(MsgCode.FA052.get());
            }
            List<String> collect = sysList.stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            if (StringUtil.isEmpty(userEntity.getAppSystemId()) || !collect.contains(userEntity.getAppSystemId())) {
                updateUser.setAppSystemId(collect.get(0));
            }
        }
        return null;
    }

    @Operation(summary = "获取当前用户所有岗位")
    @GetMapping("/getUserPositions")
    public ActionResult<List<PosistionCurrentModel>> getUserPositions() {
        return ActionResult.success(userRelationService.getObjectVoList());
    }

    /*= different =*/

    /**
     * 修改app常用
     *
     * @param userAppDataForm 页面参数
     * @return
     */
    @Operation(summary = "修改app常用数据")
    @Parameter(name = "userAppDataForm", description = "页面参数", required = true)
    @PutMapping("/SystemAppData")
    public ActionResult<Object> updateAppData(@RequestBody @Valid UserAppDataForm userAppDataForm) {
        UserInfo userInfo = UserProvider.getUser();
        UserEntity entity = userService.getInfo(userInfo.getUserId());
        UserExtraEntity userExtraByUserId = userExtraService.getUserExtraByUserId(userInfo.getUserId());
        userExtraByUserId.setPropertyJson(userAppDataForm.getData());
        userService.updateById(entity);
        userExtraService.updateById(userExtraByUserId);
        return ActionResult.success(MsgCode.SU016.get());
    }


    /**
     * 列表
     *
     * @return ignore
     */
    @Operation(summary = "获取个性签名列表")
    @GetMapping("/SignImg")
    public ActionResult<List<SignListVO>> getListSignImg() {
        List<SignEntity> list = signService.getList();
        List<SignListVO> data = JsonUtil.getJsonToList(list, SignListVO.class);
        return ActionResult.success(data);
    }


    /**
     * 新建
     *
     * @param signForm 实体对象
     * @return ignore
     */
    @Operation(summary = "添加个性签名")
    @Parameter(name = "signForm", description = "实体对象", required = true)
    @PostMapping("/SignImg")
    public ActionResult<Object> create(@RequestBody @Valid SignForm signForm) {
        SignEntity entity = JsonUtil.getJsonToBean(signForm, SignEntity.class);
        boolean b = signService.create(entity);
        if (b) {
            return ActionResult.success(MsgCode.SU001.get());
        }
        return ActionResult.fail(MsgCode.SU001.get());
    }

    /**
     * 删除
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除个性签名")
    @Parameter(name = "id", description = "主键值", required = true)
    @DeleteMapping("/{id}/SignImg")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        boolean delete = signService.delete(id);
        if (delete) {
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.SU003.get());
    }

    /**
     * 设置默认
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "设置默认")
    @Parameter(name = "id", description = "主键值", required = true)
    @PutMapping("/{id}/SignImg")
    public ActionResult<Object> uptateDefault(@PathVariable("id") String id) {
        boolean b = signService.updateDefault(id);
        if (b) {
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.fail(MsgCode.SU004.get());
    }

    //*****************************以下新多租户对接接口************************************

    @Operation(summary = "获取可切换租户列表")
    @GetMapping("/saasList")
    public ActionResult<List<LoginSaasVo>> saasList() {
        if (configValueUtil.isMultiTenancy() && Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
            return ActionResult.success(LoginSaasUtil.getSaasList());
        }
        return ActionResult.success();
    }

    @Operation(summary = "获取租户信息(RSA)")
    @GetMapping("/saasinfo")
    public ActionResult<TenantInfo> saasInfo() {
        TenantInfo vo = null;
        // 多租户-旧租户
        if (configValueUtil.isMultiTenancy() && Objects.equals(configValueUtil.getMultiTenancyVersion(), 1)) {
            String tenantId = UserProvider.getUser().getTenantId();
            Map<String, String> headers = Collections.emptyMap();
            try {
                String ip = IpUtil.getIpAddr();
                if (StringUtil.isNotEmpty(ip) && !Objects.equals("127.0.0.1", ip)) {
                    headers = ImmutableMap.of("X-Forwarded-For", ip);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            if (StringUtil.isNotEmpty(tenantId)) {
                try (HttpResponse execute = HttpRequest.get(configValueUtil.getMultiTenancyUrl() + "GetTenantInfo/" + tenantId)
                        .addHeaders(headers)
                        .execute()) {
                    vo = JsonUtil.getJsonToBean(execute.body(), TenantInfo.class);
                } catch (Exception e) {
                    log.error("获取远端多租户信息失败: {}", e.getMessage());
                }
            }
            return ActionResult.success(vo);
        } else {
            vo = LoginSaasUtil.getTenantInfo();
        }
        return ActionResult.success(vo);
    }

    @Operation(summary = "获取租户信息(AES)")
    @GetMapping("/saasTenantName/{tenantCode}")
    public ActionResult<String> saasTenantName(@PathVariable("tenantCode") String tenantCode) {
        TenantVO remoteTenantInfoNew = LoginSaasUtil.getRemoteTenantInfoNew(tenantCode);
        if (remoteTenantInfoNew == null) {
            return ActionResult.fail(MsgCode.LOG115.get());
        }
        return ActionResult.success(MsgCode.SU019.get(), remoteTenantInfoNew.getCompanyName());
    }

    @Operation(summary = "加入企业")
    @PostMapping("/saasjoin")
    public ActionResult<String> saasjoin(@RequestBody TenantJoinForm form) {
        String sourceTenantId = TenantHolder.getDatasourceId();
        String targetTenantId = form.getTenantId();
        TenantDataSourceUtil.switchTenant(form.getTenantId());
        UserEntity userEntity = userService.getUserByAccount(form.getAccount());
        if (userEntity == null) {
            throw new LoginException(MsgCode.LOG101.get());
        }
        BaseSystemInfo sysInfo = sysConfigApi.getSysInfo();
        try {
            //密码验证
            authenticateLock(userEntity, sysInfo);
            authenticatePassword(userEntity, form.getPassword());
        } catch (Exception e) {
            //验证失败多次锁定
            authenticateFailure(userEntity, sysInfo);
            return ActionResult.fail(e.getMessage());
        } finally {
            TenantDataSourceUtil.switchTenant(sourceTenantId);
        }
        JoinCompanyModel model = JsonUtil.getJsonToBean(form, JoinCompanyModel.class);
        model.setSourceTenantId(sourceTenantId);
        model.setSourceAccount(UserProvider.getUser().getUserAccount());
        model.setTargetTenantId(targetTenantId);
        model.setTargetAccount(form.getAccount());
        boolean flag = LoginSaasUtil.saasJoin(JSON.toJSONString(model));
        if (flag) {
            return ActionResult.success("加入成功");
        }
        return ActionResult.fail("加入失败");
    }

    protected void authenticatePassword(UserEntity userEntity, String password) throws LoginException {
        try {
            //前端md5后进行aes加密
            password = DesUtil.aesOrDecode(password, false, true);
        } catch (Exception e) {
            log.error(MsgCode.OA013.get() + ":{}", password, e);
            password = "";
        }
        if (!userEntity.getPassword().equals(Md5Util.getStringMd5(password + userEntity.getSecretkey().toLowerCase()))) {
            throw new LoginException(MsgCode.LOG101.get());
        }
    }

    protected void authenticateLock(UserEntity userEntity, BaseSystemInfo systemInfo) throws LoginException {
        // 判断当前账号是否被锁定
        Integer lockMark = userEntity.getEnabledMark();
        if (Objects.nonNull(lockMark) && lockMark == 2) {
            // 获取解锁时间
            Date unlockTime = userEntity.getUnlockTime();
            // 账号锁定
            if (systemInfo.getLockType() == 1 || Objects.isNull(unlockTime)) {
                throw new LoginException(MsgCode.LOG012.get());
            }
            // 延迟登陆锁定
            long millis = System.currentTimeMillis();
            if (unlockTime.getTime() > millis) {
                // 转成分钟
                int time = (int) ((unlockTime.getTime() - millis) / (1000 * 60));
                throw new LoginException(MsgCode.LOG108.get(time + 1));
            } else if (unlockTime.getTime() < millis && userEntity.getLogErrorCount() >= systemInfo.getPasswordErrorsNumber()) {
                // 已经接触错误时间锁定的话就重置错误次数
                userEntity.setLogErrorCount(0);
                userEntity.setEnabledMark(1);
                userService.updateById(userEntity);
            }
        }
    }

    protected void authenticateFailure(UserEntity entity, BaseSystemInfo sysConfigInfo) {
        // 超级管理员特权，不会锁定
        if (entity != null && !ADMIN_KEY.equals(entity.getAccount())) {

            // 判断是否需要锁定账号，哪种锁定方式
            // 大于2则判断有效
            Integer errorsNumber = sysConfigInfo.getPasswordErrorsNumber();
            // 判断是否开启
            if (errorsNumber != null && errorsNumber > 2) {
                // 加入错误次数
                Integer errorCount = entity.getLogErrorCount() != null ? entity.getLogErrorCount() + 1 : 1;
                entity.setLogErrorCount(errorCount);
                Integer lockType = sysConfigInfo.getLockType();
                if (errorCount >= errorsNumber) {
                    entity.setEnabledMark(2);
                    // 如果是延时锁定
                    if (Objects.nonNull(lockType) && lockType == 2) {
                        Integer lockTime = sysConfigInfo.getLockTime();
                        Date date = new Date((System.currentTimeMillis() + (lockTime * 60 * 1000)));
                        entity.setUnlockTime(date);
                    }
                }
                if (null != lockType && lockType == 1) {
                    entity.setUnlockTime(null);
                }
                userService.updateById(entity);
            }
        }
    }

    @Operation(summary = "退出企业")
    @DeleteMapping("/{tenantCode}/saas")
    public ActionResult<String> saasjoin(@PathVariable String tenantCode) {
        UserInfo user = UserProvider.getUser();
        JoinCompanyModel model = new JoinCompanyModel();
        model.setAppId(configValueUtil.getMultiTenancyAppCode());
        model.setSourceTenantId(user.getTenantId());
        model.setSourceAccount(UserProvider.getUser().getUserAccount());
        model.setTargetTenantId(tenantCode);
        boolean flag = LoginSaasUtil.saasExit(JSON.toJSONString(model));
        if (flag) {
            return ActionResult.success("退出成功");
        }
        return ActionResult.success("退出失败");
    }
}
