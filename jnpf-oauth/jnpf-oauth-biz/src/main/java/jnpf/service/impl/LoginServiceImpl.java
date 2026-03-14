package jnpf.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jnpf.base.KeyNameModel;
import jnpf.base.UserInfo;
import jnpf.base.UserOrgPosModel;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.button.ButtonModel;
import jnpf.base.model.column.ColumnModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.resource.ResourceModel;
import jnpf.base.service.SignService;
import jnpf.base.service.SysconfigService;
import jnpf.base.service.SystemService;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.EventConst;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.LoginException;
import jnpf.exception.NoPermiLoginException;
import jnpf.exception.TenantDatabaseException;
import jnpf.granter.UserDetailsServiceBuilder;
import jnpf.message.entity.MessageTemplateConfigEntity;
import jnpf.message.service.MessageService;
import jnpf.message.service.MessageTemplateConfigService;
import jnpf.model.BaseSystemInfo;
import jnpf.model.BuildUserCommonInfoModel;
import jnpf.model.login.*;
import jnpf.model.tenant.TenantVO;
import jnpf.module.ProjectEventBuilder;
import jnpf.permission.entity.*;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.model.authorize.OtherModel;
import jnpf.permission.service.*;
import jnpf.portal.service.PortalDataService;
import jnpf.properties.SecurityProperties;
import jnpf.service.LoginService;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static cn.hutool.core.collection.CollUtil.isEmpty;
import static jnpf.util.Constants.ADMIN_KEY;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（<a href="https://www.jnpfsoft.com">...</a>）
 * @date 2021/3/16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final ConfigValueUtil configValueUtil;

    private final SecurityProperties securityProperties;

    private final RedisUtil redisUtil;

    private final CacheKeyUtil cacheKeyUtil;

    private final SysconfigService sysconfigApi;


    private final UserService userApi;

    private final UserRelationService userRelationApi;

    private final RoleRelationService roleRelationApi;

    private final OrganizeService organizeApi;

    private final PositionService positionApi;

    private final RoleService roleApi;

    private final GroupService groupApi;

    private final AuthorizeService authorizeApi;

    private final PortalDataService portalDataService;

    private final SystemService systemApi;

    private final UserDetailsServiceBuilder userDetailsServiceBuilder;

    private final SignService signService;

    private final MessageTemplateConfigService messageTemplateApi;

    private final MessageService sentMessageApi;

    private final UserExtraService userExtraService;

    @Override
    public UserInfo getTenantAccount(UserInfo userInfo) throws LoginException {
        String tenantId = null;
        if (configValueUtil.isMultiTenancy()) {
            String[] tenantAccount = userInfo.getUserAccount().split("\\@");
            if (tenantAccount.length == 1) {
                //只输入账号, 1:配置的二级域名下只输入账号, 2:主域名下输入了租户号
                String referer = ServletUtil.getHeader("Referer");
                if (StringUtil.isNotEmpty(referer)) {
                    String remoteHost = UrlBuilder.of(referer).getHost();
                    String apiHost = UrlBuilder.of(RequestContext.isOrignPc() ? configValueUtil.getFrontDomain() : configValueUtil.getAppDomain()).getHost();
                    if (!ObjectUtil.equals(remoteHost, apiHost)
                            && remoteHost.endsWith(apiHost)) {
                        //二级域名访问, 输入的是账号
                        tenantId = remoteHost.split("\\.")[0];
                        userInfo.setUserAccount(tenantAccount[0]);
                    }
                }
                if (tenantId == null) {
                    if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
                        //主域名访问, 新租户输入的是账号-走默认租户
                        tenantId = "defaulttenant";
                        userInfo.setUserAccount(tenantAccount[0]);
                    } else {
                        //主域名访问, 输入的是租户号
                        tenantId = tenantAccount[0];
                        userInfo.setUserAccount(ADMIN_KEY);
                    }
                }
            } else {
                if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
                    //账号@租户号
                    tenantId = tenantAccount[tenantAccount.length - 1];
                    userInfo.setUserAccount(String.join("@", Arrays.copyOfRange(tenantAccount, 0, tenantAccount.length - 1)));
                } else {
                    //租户号@账号
                    tenantId = tenantAccount[0];
                    userInfo.setUserAccount(String.join("@", Arrays.copyOfRange(tenantAccount, 1, tenantAccount.length)));
                }
            }
            if (StringUtil.isEmpty(tenantId) || tenantAccount.length > 3 || StringUtil.isEmpty(userInfo.getUserAccount())) {
                throw new LoginException(MsgCode.LOG102.get());
            }
            TenantVO tenantVO = TenantDataSourceUtil.getRemoteTenantInfo(tenantId);
            TenantDataSourceUtil.switchTenant(tenantId, tenantVO);
            //切换成租户库
            userInfo.setTenantId(tenantId);
            userInfo.setTenantDbConnectionString(tenantVO.getDbName());
            userInfo.setTenantDbType(tenantVO.getType());
            //查库测试
            BaseSystemInfo baseSystemInfo = null;
            try {
                baseSystemInfo = getBaseSystemConfig(userInfo.getTenantId());
            } catch (Exception e) {
                log.error("登录获取系统配置失败: {}", e.getMessage());
            }
            if (baseSystemInfo == null || baseSystemInfo.getSingleLogin() == null) {
                throw new TenantDatabaseException();
            }
        }
        return userInfo;
    }

    @Override
    public UserInfo userInfo(UserInfo userInfo, BaseSystemInfo sysConfigInfo) throws LoginException {
        //获取账号信息
        UserEntity userEntity = LoginHolder.getUserEntity();
        if (userEntity == null) {
            userEntity = userDetailsServiceBuilder.getUserDetailService(userInfo.getUserDetailKey()).loadUserEntity(userInfo);
            LoginHolder.setUserEntity(userEntity);
        }

        checkUser(userEntity, userInfo, sysConfigInfo);

        userInfo.setUserId(userEntity.getId());
        userInfo.setUserAccount(userEntity.getAccount());
        userInfo.setUserName(userEntity.getRealName());
        userInfo.setUserIcon(userEntity.getHeadIcon());
        userInfo.setTheme(userEntity.getTheme());
        userInfo.setOrganizeId(userEntity.getOrganizeId());
        userInfo.setPortalId(userEntity.getPortalId());
        userInfo.setIsAdministrator(BooleanUtil.toBoolean(String.valueOf((userEntity.getIsAdministrator()))));
        if (!ADMIN_KEY.equals(userInfo.getUserAccount()) && ObjectUtil.isNotEmpty(userEntity.getStanding())) {

            userInfo.setIsAdministrator(ObjectUtil.equals(userEntity.getStanding(), 1));

        }
        // 添加过期时间
        String time = sysConfigInfo.getTokenTimeout();
        if (StringUtil.isNotEmpty(time)) {
            int minu = Integer.parseInt(time);
            userInfo.setOverdueTime(DateUtil.dateAddMinutes(null, minu));
            userInfo.setTokenTimeout(minu);
        }

        String ipAddr = IpUtil.getIpAddr();
        userInfo.setLoginIpAddress(ipAddr);
        userInfo.setLoginIpAddressName(IpUtil.getIpCity(ipAddr));
        userInfo.setLoginTime(DateUtil.getmmNow());
        UserAgent userAgent = UserAgentUtil.parse(ServletUtil.getUserAgent());
        if (userAgent != null) {
            userInfo.setLoginPlatForm(userAgent.getPlatform().getName() + " " + userAgent.getOsVersion());
            userInfo.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
        }
        userInfo.setPrevLoginTime(userEntity.getPrevLogTime());
        userInfo.setPrevLoginIpAddress(userEntity.getPrevLogIp());
        userInfo.setPrevLoginIpAddressName(IpUtil.getIpCity(userEntity.getPrevLogIp()));
        // 生成id
        String token = RandomUtil.uuId();
        userInfo.setId(cacheKeyUtil.getLoginToken(userInfo.getTenantId()) + token);

        createUserOnline(userInfo);
        return userInfo;
    }

    @Override
    public void updatePasswordMessage() {
        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = userApi.getInfo(userInfo.getUserId());
        BaseSystemInfo baseSystemInfo = sysconfigApi.getSysInfo();
        if (baseSystemInfo.getPasswordIsUpdatedRegularly() == 1) {
            Date changePasswordDate = userEntity.getCreatorTime();
            if (userEntity.getChangePasswordDate() != null) {
                changePasswordDate = userEntity.getChangePasswordDate();
            }
            //当前时间
            Date nowDate = DateUtil.getNowDate();
            //更新周期
            Integer updateCycle = baseSystemInfo.getUpdateCycle();
            //提前N天提醒
            Integer updateInAdvance = baseSystemInfo.getUpdateInAdvance();
            Integer day = DateUtil.getDiffDays(changePasswordDate, nowDate);
            if (day >= (updateCycle - updateInAdvance)) {
                MessageTemplateConfigEntity entity = messageTemplateApi.getInfoByEnCode("XTXXTX001", "1");
                if (entity != null) {
                    List<String> toUserIds = new ArrayList<>();
                    toUserIds.add(userInfo.getUserId());
                    sentMessageApi.sentMessage(toUserIds, entity.getTitle(), entity.getContent(), userInfo, Integer.parseInt(entity.getMessageSource()), Integer.parseInt(entity.getMessageType()));
                }
            }
        }
    }

    /**
     * 创建用户在线信息
     *
     * @param userInfo
     */
    private void createUserOnline(UserInfo userInfo) {
        String userId = userInfo.getUserId();


        String authorize = String.valueOf(redisUtil.getString(cacheKeyUtil.getUserAuthorize() + userId));
        redisUtil.remove(authorize);
        //记录在线
        if (ServletUtil.getIsMobileDevice() && ServletUtil.getHeader("clientId") != null) {
            //记录移动设备CID,用于消息推送

            String clientId = ServletUtil.getHeader("clientId");
            Map<String, String> map = new HashMap<>(16);
            map.put(userInfo.getUserId(), clientId);
            redisUtil.insert(cacheKeyUtil.getMobileDeviceList(), map);

        }
    }

    private UserCommonInfoVO data(BuildUserCommonInfoModel buildUserCommonInfoModel) {
        UserInfo userInfo = buildUserCommonInfoModel.getUserInfo();
        UserEntity userEntity = buildUserCommonInfoModel.getUserEntity();
        UserExtraEntity userExtraByUserId = userExtraService.getUserExtraByUserId(userInfo.getUserId());
        boolean firstLogTime = userEntity.getFirstLogTime() == null;//第一次登录，不返回上一次登录时间
        //userInfo 填充信息
        UserOrgPosModel uopm = this.userInfo(userInfo, userEntity);
        //返回前端vo
        BaseSystemInfo baseSystemInfo = buildUserCommonInfoModel.getBaseSystemInfo();
        UserCommonInfoVO infoVO = JsonUtil.getJsonToBean(userInfo, UserCommonInfoVO.class);
        infoVO.setOrganizeList(uopm.getOrganizeList());
        infoVO.setPositionList(uopm.getPositionList());
        infoVO.setGroupList(uopm.getGroupList());
        infoVO.setRoleList(uopm.getRoleList());
        infoVO.setPrevLogin(baseSystemInfo.getLastLoginTimeSwitch() == 1 ? 1 : 0);
        infoVO.setPrevLoginTime(firstLogTime ? null : userEntity.getPrevLogTime().getTime());
        if (BeanUtil.isNotEmpty(userExtraByUserId)) {
            infoVO.setPreferenceJson(userExtraByUserId.getPreferenceJson());
        }
        //最后一次修改密码时间
        infoVO.setChangePasswordDate(userEntity.getChangePasswordDate());
        // 姓名
        infoVO.setUserName(userEntity.getRealName());
        // 组织名称
        KeyNameModel defaultOrg = uopm.getOrganizeList().stream().filter(t -> t.getId().equals(userInfo.getOrganizeId())).findFirst().orElse(new KeyNameModel());
        infoVO.setOrganizeName(defaultOrg.getFullName());
        // 岗位名称
        KeyNameModel defaultPos = uopm.getPositionList().stream().filter(t -> t.getId().equals(userInfo.getPositionId())).findFirst().orElse(new KeyNameModel());
        infoVO.setPositionName(defaultPos.getFullName());
        //是否超级管理员
        infoVO.setIsAdministrator(BooleanUtil.toBoolean(String.valueOf(userEntity.getIsAdministrator())));
        if (!ADMIN_KEY.equals(userEntity.getAccount()) && ObjectUtil.isNotEmpty(userEntity.getStanding())) {

            userInfo.setIsAdministrator(ObjectUtil.equals(userEntity.getStanding(), 1));
            infoVO.setIsAdministrator(ObjectUtil.equals(userEntity.getStanding(), 1));

        }
        infoVO.setSecurityKey(userInfo.getSecurityKey());
        //添加当前应用信息
        infoVO.setSaasAppId(configValueUtil.getMultiTenancyAppCode());
        infoVO.setSaasAppName(TenantHolder.getCurrentAppName());
        if (configValueUtil.isMultiTenancy()) {
            infoVO.setIsTenant(true);
            if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
                infoVO.setIsSaas(true);
            }
        }
        return infoVO;
    }

    public UserEntity checkUser(UserEntity userEntity, UserInfo userInfo, BaseSystemInfo sysConfigInfo) throws LoginException {
        if (userEntity == null) {
            throw new LoginException(MsgCode.LOG101.get());
        }
        //判断是否组织、岗位、角色、部门主管是否为空，为空则抛出异常
        //判断是否为管理员，是否为Admin(Admin为最高账号，不受限制)
        if (!ADMIN_KEY.equals(userEntity.getAccount()) || userEntity.getIsAdministrator() != 1) {
            List<String> posAndRole = new ArrayList<>();
            //没岗位，且没用户角色时直接提示没权限
            List<UserRelationEntity> userPos = userRelationApi.getListByUserIdAndObjType(userEntity.getId(), PermissionConst.POSITION);
            List<String> userPosIds = userPos.stream().map(UserRelationEntity::getObjectId).collect(Collectors.toList());
            userPosIds.add(userEntity.getId());
            List<RoleRelationEntity> userRole = roleRelationApi.getListByObjectId(userPosIds, null);
            posAndRole.addAll(userPosIds);
            posAndRole.addAll(userRole.stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList()));
            //有岗位角色但是没有权限
            if (isEmpty(posAndRole) || isEmpty(authorizeApi.getListByObjectId(posAndRole))) {
                throw new LoginException(MsgCode.LOG004.get());
            }

        }
        if (userEntity.getIsAdministrator() == 0) {
            if (userEntity.getEnabledMark() == null) {
                throw new LoginException(MsgCode.LOG005.get());
            }
            if (userEntity.getEnabledMark() == 0) {
                throw new LoginException(MsgCode.LOG006.get());
            }
        }
        if (userEntity.getDeleteMark() != null && userEntity.getDeleteMark() == 1) {
            throw new LoginException(MsgCode.LOG007.get());
        }
        //安全验证
        String ipAddr = IpUtil.getIpAddr();
        userInfo.setLoginIpAddress(IpUtil.getIpAddr());
        // 判断白名单
        if (!ADMIN_KEY.equals(userEntity.getAccount()) && "1".equals(sysConfigInfo.getWhitelistSwitch())) {
            List<String> ipList = Arrays.asList(sysConfigInfo.getWhitelistIp().split(","));
            if (!ipList.contains(ipAddr)) {
                throw new LoginException(MsgCode.LOG010.get());
            }
        }
        // 判断当前账号是否被锁定
        Integer lockMark = userEntity.getEnabledMark();
        if (Objects.nonNull(lockMark) && lockMark == 2) {
            // 获取解锁时间
            Date unlockTime = userEntity.getUnlockTime();
            // 账号锁定
            if (sysConfigInfo.getLockType() == 1 || Objects.isNull(unlockTime)) {
                throw new LoginException(MsgCode.LOG012.get());
            }
            // 延迟登陆锁定
            long millis = System.currentTimeMillis();
            // 系统设置的错误次数
            int passwordErrorsNumber = sysConfigInfo.getPasswordErrorsNumber() != null ? sysConfigInfo.getPasswordErrorsNumber() : 0;
            // 用户登录错误次数
            int logErrorCount = userEntity.getLogErrorCount() != null ? userEntity.getLogErrorCount() : 0;
            if (unlockTime.getTime() > millis) {
                // 转成分钟
                int time = (int) ((unlockTime.getTime() - millis) / (1000 * 60));
                throw new LoginException(MsgCode.LOG108.get(time + 1));
            } else if (unlockTime.getTime() < millis && logErrorCount >= passwordErrorsNumber) {
                // 已经接触错误时间锁定的话就重置错误次数
                userEntity.setLogErrorCount(0);
                userEntity.setEnabledMark(1);
                userApi.updateById(userEntity);
            }
        }
        return userEntity;
    }

    /**
     * 获取用户登陆信息
     *
     * @return
     */
    @Override
    public PcUserVO getCurrentUser(String type, String systemCode, Integer isBackend) {
        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = userApi.getInfo(userInfo.getUserId());
        if (userEntity == null) {
            return null;
        }
        userInfo.setIsBackend(isBackend);
        BaseSystemInfo baseSystemInfo = sysconfigApi.getSysInfo();
        BuildUserCommonInfoModel buildUserCommonInfoModel = new BuildUserCommonInfoModel(userInfo, userEntity, baseSystemInfo, type);
        //添加userInfo信息
        UserCommonInfoVO infoVO = this.data(buildUserCommonInfoModel);
        //获取权限
        if (StringUtil.isEmpty(systemCode) && JnpfConst.WEB.equals(type)) {
            systemCode = JnpfConst.MAIN_SYSTEM_CODE;
        } else if (StringUtil.isEmpty(systemCode) && JnpfConst.APP.equals(type)) {
            SystemEntity sysInfo = systemApi.getInfo(userEntity.getAppSystemId());
            systemCode = sysInfo != null ? sysInfo.getEnCode() : null;
        }
        AuthorizeVO authorizeModel = authorizeApi.getAuthorize(false, systemCode, isBackend);

        OtherModel otherModel = authorizeModel.getOtherModel();
        userInfo.setIsManageRole(otherModel.getIsManageRole());
        userInfo.setIsDevRole(otherModel.getIsDevRole());
        userInfo.setIsUserRole(otherModel.getIsUserRole());
        userInfo.setIsOtherRole(otherModel.getIsOtherRole());
        userInfo.setWorkflowEnabled(otherModel.getWorkflowEnabled());
        infoVO.setIsManageRole(userInfo.getIsManageRole());
        infoVO.setIsDevRole(userInfo.getIsDevRole());
        infoVO.setIsUserRole(userInfo.getIsUserRole());
        infoVO.setIsOtherRole(userInfo.getIsOtherRole());
        infoVO.setWorkflowEnabled(userInfo.getWorkflowEnabled());
        //当前系统信息
        SystemEntity currentSystem = authorizeModel.getCurrentSystem();
        if (currentSystem != null) {
            userInfo.setAppSystemId(currentSystem.getId());
            infoVO.setSystemId(currentSystem.getId());
            infoVO.setSystemName(currentSystem.getFullName());
            infoVO.setSystemCode(currentSystem.getEnCode());
            infoVO.setSystemIcon(currentSystem.getIcon());
            infoVO.setSystemColor(currentSystem.getBackgroundColor());
            if (JnpfConst.APP.equals(type) && !currentSystem.getId().equals(userEntity.getAppSystemId())) {
                userEntity.setAppSystemId(currentSystem.getId());
                userApi.updateById(userEntity);
            }
        }


        //身份
        infoVO.setStandingList(authorizeModel.getStandingList());
        List<SystemBaeModel> systemList = authorizeModel.getSystemList();
        if (!authorizeModel.getStandingList().isEmpty()) {
            UserSystemVO userSystemVO = authorizeModel.getStandingList().stream().filter(t -> t.isCurrentStanding()).findFirst().orElse(null);
            if (userSystemVO != null) {
                userInfo.setCurrentStandId(userSystemVO.getId());
            }
        }

        // 获取菜单权限
        List<ModuleModel> moduleList = new ArrayList<>(authorizeModel.getModuleList());
        //当前pc或app权限过滤
        List<String> appComModule = new ArrayList<>();
        appComModule.add(JnpfConst.APP_BACKEND);
        appComModule.addAll(JnpfConst.APP_CONFIG_MODULE);
        appComModule.addAll(JnpfConst.ONLINE_DEV_MODULE);
        List<ModuleModel> moduleListRes = moduleList.stream().filter(t -> type.equals(t.getCategory()) && !Objects.equals(t.getNoShow(), 1)
                && (Objects.equals(isBackend, 1) || !appComModule.contains(t.getEnCode()))
                && !JnpfConst.APP_BACKEND.equals(t.getEnCode())).sorted(Comparator.comparing(ModuleModel::getSortCode)).collect(Collectors.toList());
        List<PermissionModel> models = new ArrayList<>();
        for (ModuleModel moduleModel : moduleListRes) {
            if (JnpfConst.APP_CONFIG_CODE.equals(moduleModel.getEnCode()) || JnpfConst.ONLINE_DEV_CODE.equals(moduleModel.getEnCode())) {
                moduleModel.setParentId("-1");
            }
            PermissionModel model = new PermissionModel();
            model.setModelId(moduleModel.getId());
            model.setModuleName(moduleModel.getFullName());
            List<ButtonModel> buttonModels = authorizeModel.getButtonList().stream().filter(t -> moduleModel.getId().equals(t.getModuleId())).collect(Collectors.toList());
            List<ColumnModel> columnModels = authorizeModel.getColumnList().stream().filter(t -> moduleModel.getId().equals(t.getModuleId())).collect(Collectors.toList());
            List<ResourceModel> resourceModels = authorizeModel.getResourceList().stream().filter(t -> moduleModel.getId().equals(t.getModuleId())).collect(Collectors.toList());
            List<ModuleFormModel> moduleFormModels = authorizeModel.getFormsList().stream().filter(t -> moduleModel.getId().equals(t.getModuleId())).collect(Collectors.toList());
            model.setButton(JsonUtil.getJsonToList(buttonModels, PermissionVO.class));
            model.setColumn(JsonUtil.getJsonToList(columnModels, PermissionVO.class));
            model.setResource(JsonUtil.getJsonToList(resourceModels, PermissionVO.class));
            model.setForm(JsonUtil.getJsonToList(moduleFormModels, PermissionVO.class));
            if (moduleModel.getType() != 1) {
                models.add(model);
            }
        }

        // 获取签名信息
        SignEntity signEntity = signService.getDefaultByUserId(userEntity.getId());
        infoVO.setSignImg(signEntity != null ? signEntity.getSignImg() : "");
        infoVO.setSignId(signEntity != null ? signEntity.getId() : "");

        List<ModuleModel> collect = moduleListRes.stream().sorted(Comparator.comparing(ModuleModel::getSystemId).thenComparing(ModuleModel::getSortCode)).collect(Collectors.toList());
        List<AllUserMenuModel> needList = JsonUtil.getJsonToList(collect, AllUserMenuModel.class);
        List<SumTree<AllUserMenuModel>> needTree = TreeDotUtils.convertListToTreeDotFilter(needList);
        List<AllMenuSelectVO> menuvo = JsonUtil.getJsonToList(needTree, AllMenuSelectVO.class);

        SystemInfo jsonToBean = JsonUtil.getJsonToBean(baseSystemInfo, SystemInfo.class);
        jsonToBean.setJnpfDomain(configValueUtil.getApiDomain());
        PcUserVO userVO = new PcUserVO(menuvo, models, infoVO, jsonToBean);
        userVO.setCurrentSystemId(currentSystem != null ? currentSystem.getId() : null);
        userVO.getUserInfo().setHeadIcon(UploaderUtil.uploaderImg(userInfo.getUserIcon()));
        // 更新userInfo对象
        if (StringUtil.isNotEmpty(userInfo.getId())) {
            UserProvider.setLoginUser(userInfo);
            UserProvider.setLocalLoginUser(userInfo);
        }

        if (JnpfConst.WEB.equals(type)) {
            if (!JnpfConst.MAIN_SYSTEM_CODE.equals(systemCode) && isEmpty(systemList)) {

                throw new NoPermiLoginException(MsgCode.PS032.get());

            }
        } else {
            if (isEmpty(systemList) || systemList.stream().allMatch(t -> Objects.equals(t.getIsMain(), 1))) {
                userVO.setCurrentSystemId(null); //如果需要自动切不提示替换成这段代码
            }
        }

        //判断开发者-有无后台
        List<String> sysIdList = systemApi.getAuthListByUser(userInfo.getUserId(), true).stream().map(SystemEntity::getId).collect(Collectors.toList());
        if (currentSystem != null && sysIdList.contains(currentSystem.getId())) {
            infoVO.setHasBackend(true);
        }
        //无后台权限
        if (Objects.equals(isBackend, 1) && Boolean.TRUE.equals(!userInfo.getIsAdministrator())
                && !sysIdList.contains(currentSystem.getId())) {
            throw new LoginException(MsgCode.PS039.get());
        }

        //获取默认门户
        if (currentSystem != null && !JnpfConst.MAIN_SYSTEM_CODE.equals(currentSystem.getEnCode())) {
            List<String> webPortalIds = authorizeModel.getModuleList().stream().filter(t -> Objects.equals(t.getType(), 8)
                            && t.getCategory().equals(JnpfConst.WEB))
                    .map(ModuleModel::getId).collect(Collectors.toList());
            List<String> appPortalIds = authorizeModel.getModuleList().stream().filter(t -> Objects.equals(t.getType(), 8)
                            && t.getCategory().equals(JnpfConst.APP))
                    .map(ModuleModel::getId).collect(Collectors.toList());
            // 门户Web
            infoVO.setPortalId(portalDataService.getCurrentDefault(webPortalIds, currentSystem.getId(), userEntity.getId(), JnpfConst.WEB));
            // 门户App
            infoVO.setAppPortalId(portalDataService.getCurrentDefault(appPortalIds, currentSystem.getId(), userEntity.getId(), JnpfConst.APP));
        }

        //初始化接口权限
        if (securityProperties.isEnablePreAuth()) {
            // 如需使用远程事件, 改用publish
            PublishEventUtil.publishLocalEvent(new ProjectEventBuilder(EventConst.EVENT_INIT_LOGIN_PERMISSION, authorizeModel).setAsync(false));
        }
        return userVO;
    }

    @Override
    public BaseSystemInfo getBaseSystemConfig(String tenantId) {
        if (tenantId != null) {
            TenantDataSourceUtil.switchTenant(tenantId);
        }
        return sysconfigApi.getSysInfo();
    }

    /**
     * userInfo添加组织、岗位、分组、角色的关系
     * 默认组织和默认岗位
     *
     * @param userInfo
     * @param userEntity
     */
    private UserOrgPosModel userInfo(UserInfo userInfo, UserEntity userEntity) {
        UserOrgPosModel uopm = new UserOrgPosModel();
        // 得到用户和组织、岗位、分组、角色的关系
        List<UserRelationEntity> data = userRelationApi.getListByUserId(userInfo.getUserId());
        List<String> positionIds = data.stream().filter(t -> PermissionConst.POSITION.equalsIgnoreCase(t.getObjectType())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());
        List<String> groupIds = data.stream().filter(t -> PermissionConst.GROUP.equalsIgnoreCase(t.getObjectType())).map(UserRelationEntity::getObjectId).collect(Collectors.toList());

        List<PositionEntity> positionList = positionApi.getListByIds(positionIds);
        List<String> orgIds = new ArrayList<>(positionList.stream().map(PositionEntity::getOrganizeId).collect(Collectors.toSet()));
        List<OrganizeEntity> organizeList = organizeApi.getListByIds(orgIds);
        List<GroupEntity> groupList = groupApi.getListByIds(groupIds);

        List<String> allIds = new ArrayList<>();
        allIds.addAll(orgIds);
        allIds.addAll(positionIds);
        allIds.add(userEntity.getId());
        List<RoleRelationEntity> roleRelationList = roleRelationApi.getListByObjectId(allIds, null);
        List<String> roleIds = roleRelationList.stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
        List<RoleEntity> roleList = roleApi.getListByIds(roleIds);

        userInfo.setOrganizeIds(orgIds);
        userInfo.setPositionIds(positionIds);
        userInfo.setGroupIds(groupIds);
        userInfo.setRoleIds(roleIds);

        //组织全名，岗位全名
        List<KeyNameModel> organizeListRes = organizeList.stream().map(t -> {
            KeyNameModel jsonb = JsonUtil.getJsonToBean(t, KeyNameModel.class);
            jsonb.setTreeName(t.getOrgNameTree());
            jsonb.setTreeId(t.getOrganizeIdTree());
            return jsonb;
        }).collect(Collectors.toList());
        List<KeyNameModel> positionListRes = positionList.stream().map(t -> {
            KeyNameModel jsonb = JsonUtil.getJsonToBean(t, KeyNameModel.class);
            OrganizeEntity organizeEntity = organizeList.stream().filter(m -> m.getId().equals(t.getOrganizeId())).findFirst().orElse(new OrganizeEntity());
            jsonb.setTreeName(organizeEntity.getOrgNameTree() + "/" + t.getFullName());
            jsonb.setTreeId(t.getPositionIdTree());
            return jsonb;
        }).collect(Collectors.toList());
        uopm.setOrganizeList(organizeListRes);
        uopm.setPositionList(positionListRes);
        uopm.setGroupList(JsonUtil.getJsonToList(groupList, KeyNameModel.class));
        uopm.setRoleList(JsonUtil.getJsonToList(roleList, KeyNameModel.class));
        userInfo.setIsManageRole(false);
        userInfo.setIsDevRole(false);
        userInfo.setIsUserRole(false);
        userInfo.setIsOtherRole(false);
        for (RoleEntity roleEntity : roleList) {
            if (PermissionConst.MANAGER_CODE.equals(roleEntity.getEnCode())) {
                userInfo.setIsManageRole(true);
            } else if (PermissionConst.DEVELOPER_CODE.equals(roleEntity.getEnCode())) {
                userInfo.setIsDevRole(true);
            } else if (PermissionConst.USER_CODE.equals(roleEntity.getEnCode())) {
                userInfo.setIsUserRole(true);
            } else {
                userInfo.setIsOtherRole(true);
            }
        }

        //默认组织和默认岗位
        String organizeId = userEntity.getOrganizeId();
        String positionId = userEntity.getPositionId();
        if (CollUtil.isNotEmpty(orgIds) && !orgIds.contains(userEntity.getOrganizeId())) {

            organizeId = orgIds.get(0);

        }
        if (CollUtil.isNotEmpty(positionIds) && !positionIds.contains(userEntity.getPositionId())) {

            positionId = positionIds.get(0);

        }
        userInfo.setOrganizeId(organizeId);
        userInfo.setPositionId(positionId);

        // 修改用户信息
        userEntity.setOrganizeId(organizeId);
        userEntity.setPositionId(positionId);
        if (userEntity.getFirstLogTime() == null) {
            userEntity.setFirstLogIp(userEntity.getPrevLogIp());
            userEntity.setFirstLogTime(userEntity.getPrevLogTime());
        }
        userApi.updateById(userEntity);


        userInfo.setManagerId(userEntity.getManagerId());
        //获取岗位
        List<UserRelationEntity> listByObjectType = userRelationApi.getListByObjectType(userInfo.getUserId(), PermissionConst.POSITION);
        if (CollUtil.isNotEmpty(listByObjectType)) {
            List<String> collect = listByObjectType.stream()
                    .map(UserRelationEntity::getObjectId)
                    .collect(Collectors.toList());
            //获取子岗位
            List<String> sonPositionIdList = positionApi.getListByParentIds(collect).stream()
                    .map(PositionEntity::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(sonPositionIdList)) {
                List<String> userIds = userRelationApi.getListByObjectIdAll(sonPositionIdList).stream()
                        .map(UserRelationEntity::getUserId)
                        .collect(Collectors.toList());
                userIds.add(userInfo.getUserId());
                userInfo.setSubordinateIds(userIds);

            }

        }
        userInfo.setLoginTime(DateUtil.getmmNow());
        return uopm;
    }

}
