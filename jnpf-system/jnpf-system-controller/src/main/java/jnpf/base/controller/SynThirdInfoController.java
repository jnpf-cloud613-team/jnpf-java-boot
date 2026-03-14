package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.dingtalk.api.response.OapiV2UserListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.model.synthird.PaginationSynThirdInfo;
import jnpf.base.model.synthird.SynThirdTotal;
import jnpf.base.service.SysconfigService;
import jnpf.base.util.SynDingTalkUtil;
import jnpf.base.util.SynQyWebChatUtil;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.exception.WxErrorException;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.model.SynThirdInfoVo;
import jnpf.message.model.message.*;
import jnpf.message.service.MessageService;
import jnpf.message.service.SynThirdDingTalkService;
import jnpf.message.service.SynThirdInfoService;
import jnpf.message.service.SynThirdQyService;
import jnpf.message.util.SynThirdConsts;
import jnpf.model.SocialsSysConfig;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.organize.OrganizeModel;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 第三方工具的公司-部门-用户同步表模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/4/25 9:30
 */
@Tag(name = "第三方信息同步", description = "SynThirdInfo")
@RestController
@RequestMapping("/api/system/SynThirdInfo")
@Slf4j
@RequiredArgsConstructor
public class SynThirdInfoController extends SuperController<SynThirdInfoService, SynThirdInfoEntity> {

    public static final String SOCIALS_CONFIG = "SocialsConfig";
    public static final String QYH_DEPARTMENT = "qyhDepartment";
    public static final String DING_DEPARTMENT = "dingDepartment";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SYNCHRONOUS = "同步组织配置未保存";


    private final SynThirdInfoService synThirdInfoService;

    private final  SynThirdQyService synThirdQyService;

    private final  SynThirdDingTalkService synThirdDingTalkService;

    private final  OrganizeService organizeService;

    private final  UserService userService;

    private final  RedisUtil redisUtil;

    private final  ConfigValueUtil configValueUtil;

    private final  MessageService messageService;

    private final  CacheKeyUtil cacheKeyUtil;

    private final  SysconfigService sysconfigService;

    /**
     * 新增同步表信息
     *
     * @param synThirdInfoCrForm 新建模型
     * @return ignore
     */
    @Operation(summary = "新增同步表信息")
    @Parameter(name = "synThirdInfoCrForm", description = "同步信息模型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @PostMapping
    @DSTransactional
    public ActionResult<Object>create(@RequestBody @Valid SynThirdInfoCrForm synThirdInfoCrForm) throws DataException {
        UserInfo userInfo = UserProvider.getUser();
        SynThirdInfoEntity entity = JsonUtil.getJsonToBean(synThirdInfoCrForm, SynThirdInfoEntity.class);
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setId(RandomUtil.uuId());
        synThirdInfoService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 获取同步表信息
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "获取同步表信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/{id}")
    public SynThirdInfoEntity getInfo(@PathVariable("id") String id) {
        return synThirdInfoService.getInfo(id);
    }

    /**
     * 获取指定类型的同步对象
     *
     * @param thirdType 1:企业微信 2:钉钉
     * @param dataType  1:公司 2:部门 3：用户
     * @param id        dataType对应的对象ID
     * @return ignore
     */
    @Operation(summary = "获取指定类型的同步对象")
    @GetMapping("/getInfoBySysObjId/{thirdType}/{dataType}/{id}")
    public SynThirdInfoEntity getInfoBySysObjId(@PathVariable("thirdType") String thirdType, @PathVariable("dataType") String dataType, @PathVariable("id") String id) {
        return synThirdInfoService.getInfoBySysObjId(thirdType, dataType, id);
    }


    /**
     * 更新同步表信息
     *
     * @param id                 主键
     * @param synThirdInfoUpForm 修改对象
     * @return ignore
     * @throws DataException ignore
     */
    @Operation(summary = "更新同步表信息")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "synThirdInfoUpForm", description = "同步模型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @PutMapping("/{id}")
    @DSTransactional
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid SynThirdInfoUpForm synThirdInfoUpForm) throws DataException {
        SynThirdInfoEntity entity = synThirdInfoService.getInfo(id);
        UserInfo userInfo = UserProvider.getUser();
        if (entity != null) {
            SynThirdInfoEntity entityUpd = JsonUtil.getJsonToBean(synThirdInfoUpForm, SynThirdInfoEntity.class);
            entityUpd.setCreatorUserId(entity.getCreatorUserId());
            entityUpd.setCreatorTime(entity.getCreatorTime());
            entityUpd.setLastModifyUserId(userInfo.getUserId());
            entityUpd.setLastModifyTime(DateUtil.getNowDate());
            synThirdInfoService.update(id, entityUpd);

            return ActionResult.success(MsgCode.SU004.get());
        } else {
            return ActionResult.fail(MsgCode.FA002.get());
        }
    }


    /**
     * 删除同步表信息
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "删除同步表信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @DeleteMapping("/{id}")
    @DSTransactional
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        SynThirdInfoEntity entity = synThirdInfoService.getInfo(id);
        if (entity != null) {
            synThirdInfoService.delete(entity);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }


    /**
     * 获取第三方(如：企业微信、钉钉)的组织与用户同步统计信息
     *
     * @param thirdType 第三方类型(1:企业微信;2:钉钉)
     * @return ignore
     */
    @Operation(summary = "获取第三方(如：企业微信、钉钉)的组织与用户同步统计信息")
    @Parameter(name = "thirdType", description = "第三方类型(1:企业微信;2:钉钉)", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/getSynThirdTotal/{thirdType}")
    public ActionResult<List<SynThirdTotal>> getSynThirdTotal(@PathVariable("thirdType") String thirdType) {
        List<SynThirdTotal> synTotalList = new ArrayList<>();
        synTotalList.add(synThirdInfoService.getSynTotal(thirdType, SynThirdConsts.DATA_TYPE_ORG));
        synTotalList.add(synThirdInfoService.getSynTotal(thirdType, SynThirdConsts.DATA_TYPE_USER));
        return ActionResult.success(synTotalList);
    }

    /**
     * 获取第三方(如：企业微信、钉钉)的组织或用户同步统计信息
     *
     * @param thirdType 第三方类型(1:企业微信;2:钉钉)
     * @param dataType  数据类型(1:组织(公司与部门);2:用户)
     * @return ignore
     */
    @Operation(summary = "获取第三方(如：企业微信、钉钉)的组织或用户同步统计信息")
    @Parameter(name = "thirdType", description = "第三方类型(1:企业微信;2:钉钉)", required = true)
    @Parameter(name = "dataType", description = "数据类型(1:组织(公司与部门);2:用户)", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/getSynThirdTotal/{thirdType}/{dataType}")
    public SynThirdTotal getSynThirdTotal(@PathVariable("thirdType") String thirdType, @PathVariable("dataType") String dataType) {
        return synThirdInfoService.getSynTotal(thirdType, dataType);
    }

    //==================================企业微信的公司-部门-用户批量同步到本系统20220609==================================

    /**
     * 本地所有组织信息(包含公司和部门)同步到企业微信
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     * 应用密钥
     *
     * @return ignore
     * @throws WxErrorException ignore
     */
    @Operation(summary = "本地所有组织信息(包含公司和部门)同步到企业微信")
    @Parameter(name = "type", description = "类型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllOrganizeSysToQy")
    public ActionResult<Object> synAllOrganizeSysToQy(@RequestParam("type") String type
            , @RequestParam("departmentId") String departmentId) throws WxErrorException {
        if (StringUtils.isEmpty(departmentId)) {
            return ActionResult.fail(MsgCode.FA057.get());
        }
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String qyhDepartment = config.getQyhDepartment();
        Boolean isDepartChange = saveConfig(departmentId, qyhDepartment, QYH_DEPARTMENT);
        if (Boolean.TRUE.equals(isDepartChange)){
            qyhDepartment=departmentId;
        }
        if ("1".equals(type)) {
            //类型为1走企业微信组织信息同步到本地
            return this.synAllOrganizeQyToSys(qyhDepartment);
        }

        String corpId = config.getQyhCorpId();
        // 向企业微信插入数据需要另外token（凭证密钥）
        String corpSecret = config.getQyhAgentSecret();

        if (StringUtil.isEmpty(qyhDepartment)) {
            return ActionResult.fail(SYNCHRONOUS);
        }
        String accessToken = "";
        try {
            // 获取Token值
            JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
            if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
                return ActionResult.fail(MsgCode.SYS025.get());
            }
            accessToken = tokenObject.getString(ACCESS_TOKEN);
            // 获取同步表、部门表的信息
            List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService.getList(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG);
            List<OrganizeEntity> organizeEntitiesBind = synThirdInfoService.getOrganizeEntitiesBind(qyhDepartment);
            List<String> collect = organizeEntitiesBind.stream()
                    .map(OrganizeEntity::getId)
                    .distinct()
                    .collect(Collectors.toList());
            List<SynThirdInfoEntity> collected = synThirdInfoList.stream()
                    .filter(t -> !collect.contains(t.getSysObjId()))
                    .collect(Collectors.toList());
            synThirdInfoList.removeAll(collected);
            // 根据公司表、同步表进行比较，决定执行创建、还是更新
            for (OrganizeEntity organizeEntity : organizeEntitiesBind) {
                if (synThirdInfoList.stream().anyMatch(t -> t.getSysObjId().equals(organizeEntity.getId()))) {
                    // 执行更新功能,token取应用密钥
                    synThirdQyService.updateDepartmentSysToQy(true, organizeEntity, accessToken);
                } else {
                    // 执行创建功能,token取应用密钥
                    synThirdQyService.createDepartmentSysToQy(true, organizeEntity, accessToken);
                }
            }
        } catch (Exception e) {
            ActionResult.fail(e.toString());
        }
        //获取结果
        SynThirdTotal synThirdTotal = synThirdInfoService.getSynTotal(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG);
        return ActionResult.success(synThirdTotal);
    }


    /**
     * 本地所有用户信息同步到企业微信
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @return ignore
     * @throws WxErrorException ignore
     */
    @Operation(summary = "本地所有用户信息同步到企业微信")
    @Parameter(name = "type", description = "类型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllUserSysToQy")
    public ActionResult<Object>synAllUserSysToQy(@RequestParam("type") String type) throws WxErrorException {
        if ("1".equals(type)) {
            //类型为1走企业微信用户同步到本地
            return this.synAllUserQyToSys();
        }

        SocialsSysConfig config = sysconfigService.getSocialsConfig();

        String corpToken = "";

        try {
            String corpId = config.getQyhCorpId();
            // 向企业微信插入数据需要另外token（凭证密钥）
            String corpSecret = config.getQyhCorpSecret();
            // 获取Token值
            JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
            if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
                return ActionResult.fail(MsgCode.SYS025.get());
            }
            corpToken = tokenObject.getString(ACCESS_TOKEN);

            // 获取同步表、用户表的信息
            List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService
                    .getList(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER);
            List<OrganizeEntity> organizeEntitiesBind = synThirdInfoService
                    .getOrganizeEntitiesBind(config.getQyhDepartment());
            List<UserEntity> userEntities = userService.getListBySyn(organizeEntitiesBind.stream()
                    .map(OrganizeEntity::getId).collect(Collectors.toList()), "");


            // 根据公司表、同步表进行比较，决定执行创建、还是更新
            String needToken = this.getAgentAccessToken();
            for (UserEntity userEntity : userEntities) {
                if (synThirdInfoList.stream().noneMatch(t -> t.getSysObjId().equals(userEntity.getId()))) {
                    // 执行创建功能
                    synThirdQyService.createUserSysToQy(true, userEntity, needToken);
                } else {
                    // 执行更新功能
                    synThirdQyService.updateUserSysToQy(true, userEntity, needToken,corpToken);
                }
            }
        } catch (Exception e) {
            ActionResult.fail(e.toString());
        }

        //获取结果
        SynThirdTotal synThirdTotal = synThirdInfoService.getSynTotal(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER);
        return ActionResult.success(synThirdTotal);
    }

    private String getAgentAccessToken() {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        String corpId = socialsConfig.getQyhCorpId();
        String corpSecret = socialsConfig.getQyhAgentSecret();
        JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
        return tokenObject.getString(ACCESS_TOKEN);
    }

    //==================================企业微信的公司-部门-用户批量同步到本系统20220609==================================

    /**
     * 企业微信所有组织信息(包含公司和部门)同步到本系统
     * 通讯录密钥
     *
     * @return ignore
     */
    @Operation(summary = "企业微信所有组织信息(包含公司和部门)同步到本系统")
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllOrganizeQyToSys")
    public ActionResult<Object> synAllOrganizeQyToSys(@RequestParam(value = "departmentId") String departmentId) {
        // 设置redis的key
        String synDing = "";
        UserInfo userInfo = UserProvider.getUser();
        if (configValueUtil.isMultiTenancy()) {
            synDing = userInfo.getTenantId() + "_" + userInfo.getUserId() + "_synAllOrganizeQyToSys";
        } else {
            synDing = userInfo.getUserId() + "_synAllOrganizeQyToSys";
        }
        // 如果redis中存在key说明同步正在进行
        if (redisUtil.exists(synDing)) {
            return ActionResult.fail(MsgCode.SYS026.get());
        }
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        if (StringUtil.isEmpty(departmentId)) {
            return ActionResult.fail(SYNCHRONOUS);
        }
        OrganizeEntity info = organizeService.getInfo(departmentId);
        if (info == null) {
            return ActionResult.fail("同步组织数据不存在");
        }
        // 获取Token值
        JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(config.getQyhCorpId(), config.getQyhCorpSecret());
        if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
            return ActionResult.fail(MsgCode.SYS025.get());
        }

        // 异步执行
        String finalSynDing = synDing;
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            String userId = userInfo.getUserId();
            try {
                redisUtil.insert(finalSynDing, "true");
                String accessToken = tokenObject.getString(ACCESS_TOKEN);

                List<QyWebChatDeptModel> qyWebChatDeptModels = new ArrayList<>();

                // 获取同步表的信息
                List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService
                        .getListByDepartment(SynThirdConsts.THIRD_TYPE_QY
                                , SynThirdConsts.DATA_TYPE_ORG
                                , config.getQyhDepartment());
                // 获取企业微信上的根目录部门(本系统的组织)
                String departId = SynThirdConsts.QY_ROOT_DEPT_ID;

                //  获取企业微信上的部门列表,需要通讯录密钥
                JSONObject retMsg = SynQyWebChatUtil.getDepartmentList(departId, accessToken);
                if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
                    log.error(retMsg.getString("error"));
                }
                qyWebChatDeptModels = JsonUtil.getJsonToList(retMsg.get("department").toString(), QyWebChatDeptModel.class);

                Map<String, QyWebChatDeptModel> deptModelMap = qyWebChatDeptModels.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), dept -> dept));
                // 部门进行树结构化,固化上下层级序列化
                List<OrganizeModel> organizeModelList = qyWebChatDeptModels.stream().map(t -> {
                    OrganizeModel model = JsonUtil.getJsonToBean(t, OrganizeModel.class);
                    model.setFullName(t.getName());
                    model.setParentId(t.getParentid() + "");
                    return model;
                }).collect(Collectors.toList());
                List<SumTree<OrganizeModel>> trees = TreeDotUtils.convertListToTreeDot(organizeModelList);
                List<OrganizeListVO> listVO = JsonUtil.getJsonToList(trees, OrganizeListVO.class);

                // 转化成为按上下层级顺序排序的列表数据
                List<QyWebChatDeptModel> listByOrder = new ArrayList<>();
                for (OrganizeListVO organizeVo : listVO) {
                    QyWebChatDeptModel entity = deptModelMap.get(organizeVo.getId());
                    listByOrder.add(entity);
                    SynQyWebChatUtil.getOrganizeTreeToList(organizeVo, deptModelMap, listByOrder);
                }
                List<OrganizeEntity> organizeEntitiesBind = synThirdInfoService.getOrganizeEntitiesBind(departmentId);
                List<String> collect = organizeEntitiesBind.stream()
                        .map(OrganizeEntity::getId)
                        .distinct()
                        .collect(Collectors.toList());
                List<SynThirdInfoEntity> list = synThirdInfoService.getList(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG);
                List<SynThirdInfoEntity> synThirdInfoEntities = list.stream()
                        .filter(t -> !collect.contains(t.getSysObjId()))
                        .distinct()
                        .collect(Collectors.toList());
                //如果不存在在最高组织下，就删除
                if (CollUtil.isNotEmpty(synThirdInfoEntities)) {
                    synThirdInfoService.removeBatchByIds(synThirdInfoEntities);
                }
                // 根据公司表、同步表进行比较，决定执行创建、还是更新
                for (QyWebChatDeptModel qyWebChatDeptModel : listByOrder) {
                    if (synThirdInfoList.stream().anyMatch(t -> t.getThirdObjId().equals(String.valueOf(qyWebChatDeptModel.getId())))) {
                        // 执行本地更新功能
                        synThirdQyService.updateDepartmentQyToSys(true, qyWebChatDeptModel, accessToken);
                    } else {
                        // 执行本的创建功能
                        synThirdQyService.createDepartmentQyToSys(true, qyWebChatDeptModel, accessToken);
                    }
                }
            } catch (Exception e) {
                log.error(finalSynDing + "，企业微信所有组织信息同步到本系统失败：" + e.getMessage());
            } finally {
                redisUtil.remove(finalSynDing);
                redisUtil.remove(cacheKeyUtil.getOrganizeList());
                List<String> toUserId = new ArrayList<>(1);
                toUserId.add(userId);
                messageService.sentMessage(toUserId, "企业微信所有组织信息同步到本系统", null, userInfo, 3, 1);
                redisUtil.remove(cacheKeyUtil.getOrganizeInfoList());
            }
        });
        return ActionResult.success(MsgCode.SYS026.get());
    }


    /**
     * 企业微信所有用户信息同步到本系统
     *
     * @return ignore
     */
    @Operation(summary = "企业微信所有用户信息同步到本系统")
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllUserQyToSys")
    @DSTransactional
    public ActionResult<Object>synAllUserQyToSys() {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        String qyhDepartment = socialsConfig.getQyhDepartment();
        // 设置redis的key
        String synDing = "";
        UserInfo userInfo = UserProvider.getUser();
        if (configValueUtil.isMultiTenancy()) {
            synDing = userInfo.getTenantId() + "_" + userInfo.getUserId() + "_synAllUserQyToSys";
        } else {
            synDing = userInfo.getUserId() + "_synAllUserQyToSys";
        }
        // 如果redis中存在key说明同步正在进行
        if (redisUtil.exists(synDing)) {
            return ActionResult.fail(MsgCode.SYS026.get());
        }
        // 获取已同步的部门信息
        List<SynThirdInfoEntity> synThirdOrgList = synThirdInfoService.getListByDepartment(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG,qyhDepartment);
        if (CollUtil.isEmpty(synThirdOrgList)) {
            return ActionResult.fail(MsgCode.SYS027.get());
        }

        // 获取Token值
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(config.getQyhCorpId(), config.getQyhCorpSecret());
        if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
            return ActionResult.fail(MsgCode.SYS025.get());
        }
        // 异步执行
        String finalSynDing = synDing;
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            String userId = userInfo.getUserId();
            try {
                redisUtil.insert(finalSynDing, "true");
                List<QyWebChatUserModel> qyUserAllList = new ArrayList<>();
                String accessToken = tokenObject.getString(ACCESS_TOKEN);

                // 获取企业微信的用户列表
                JSONObject retMsg = SynQyWebChatUtil.getUserList("1", "1", accessToken);
                qyUserAllList = JsonUtil.getJsonToList(retMsg.get("userlist"), QyWebChatUserModel.class);
                // 得到企业微信信息
                List<SynThirdInfoEntity> synThirdInfoEntityList = synThirdInfoService
                        .syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER, SynThirdConsts.THIRD_TYPE_QY);
                // 根据公司表、同步表进行比较，决定执行创建、还是更新
                for (QyWebChatUserModel qyWebChatUserModel : qyUserAllList) {
                    if (synThirdInfoEntityList.stream().noneMatch(t -> t.getThirdObjId().equals(qyWebChatUserModel.getUserid()))) {
                        // 执行创建功能
                        synThirdQyService.createUserQyToSys(true, qyWebChatUserModel, accessToken);
                    } else {
                        // 执行更新功能
                        synThirdQyService.updateUserQyToSystem(true, qyWebChatUserModel, accessToken);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("{}，企业微信所有用户信息同步到本系统失败：{}", finalSynDing, e.getMessage());
            } finally {
                redisUtil.remove(finalSynDing);
                List<String> toUserId = new ArrayList<>(1);
                toUserId.add(userId);
                messageService.sentMessage(toUserId, "企业微信所有用户信息同步到本系统", null, userInfo, 3, 1);
            }
        });
        return ActionResult.success(MsgCode.SYS026.get());
    }

    @Operation(summary = "同步成功或失败数")
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/getHandleNum")
    public ActionResult<PageListVO<SynThirdInfoVo>>getHandleNum(PaginationSynThirdInfo pagination) {
        List<SynThirdInfoVo> synThirdInfoList = synThirdInfoService
                .getListJoin(pagination);
        synThirdInfoList = synThirdInfoList.stream()
                .filter(t -> t.getLastModifyTime() != null)
                .collect(Collectors.toList());
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(synThirdInfoList, paginationVO);
    }

    //==================================本系统的公司-部门-用户批量同步到钉钉==================================

    /**
     * 本地所有组织信息(包含公司和部门)同步到钉钉
     * 不带第三方错误定位判断的功能代码 20210604
     *
     * @return ignore
     */
    @Operation(summary = "本地所有组织信息(包含公司和部门)同步到钉钉")
    @Parameter(name = "type", description = "类型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllOrganizeSysToDing")
    public ActionResult<Object>synAllOrganizeSysToDing(@RequestParam("type") String type
            ,@RequestParam("departmentId") String departmentId) {
        if (StringUtils.isEmpty(departmentId)) {
            return ActionResult.fail(MsgCode.FA057.get());
        }
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String dingDepartment = config.getDingDepartment();
        Boolean isDepartChange = saveConfig(departmentId, dingDepartment, DING_DEPARTMENT);
        if (Boolean.TRUE.equals(isDepartChange)){
            dingDepartment=departmentId;
        }
        if ("1".equals(type)) {
            //类型为1走钉钉组织部门信息同步到本地
            return this.synAllOrganizeDingToSys(dingDepartment);
        }
        //获取配置

        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();

        if (StringUtil.isEmpty(dingDepartment)) {
            return ActionResult.fail(SYNCHRONOUS);
        }
        // 获取Token值
        JSONObject tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
        if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
            return ActionResult.fail(MsgCode.SYS053.get());
        }
        String accessToken = tokenObject.getString(ACCESS_TOKEN);
        //获取绑定部门的下级有序组织
        List<OrganizeEntity> listByOrder = synThirdInfoService.getOrganizeEntitiesBind(dingDepartment);

        try {

            List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService.getList(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG);
            List<String> collect = listByOrder.stream()
                    .map(OrganizeEntity::getId)
                    .distinct()
                    .collect(Collectors.toList());
            //筛选同步表有，但是公司表没有的数据
            List<SynThirdInfoEntity> collected = synThirdInfoList.stream()
                    .filter(t -> !collect.contains(t.getSysObjId()))
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(collected)) {
                for (SynThirdInfoEntity synThirdInfoEntity : collected) {
                    //如果系统中组织不存在同步表，那将钉钉上的数据也删除
                    synThirdDingTalkService.deleteDepartmentSysToDing(true, synThirdInfoEntity.getId(), accessToken);
                }
            }
            synThirdInfoList.removeAll(collected);


            // 根据公司表、同步表进行比较，决定执行创建、还是更新
            for (OrganizeEntity organizeEntity : listByOrder) {
                if (synThirdInfoList.stream().anyMatch(t -> t.getSysObjId().equals(organizeEntity.getId()))) {
                    // 执行更新功能
                    synThirdDingTalkService.updateDepartmentSysToDing(true, organizeEntity, accessToken);
                } else {
                    // 执行创建功能
                    synThirdDingTalkService.createDepartmentSysToDing(true, organizeEntity, accessToken);
                }
            }
        } catch (Exception e) {
            ActionResult.fail(e.toString());
        }

        //获取结果
        SynThirdTotal synThirdTotal = synThirdInfoService.getSynTotal(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG);
        return ActionResult.success(synThirdTotal);
    }

    private Boolean saveConfig(String departmentId, String dingDepartment, String department) {
        if (StringUtil.isNotEmpty(departmentId) && !departmentId.equals(dingDepartment)) {
            LambdaQueryWrapper<SysConfigEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfigEntity::getCategory, SOCIALS_CONFIG);
            wrapper.eq(SysConfigEntity::getFkey, department);
            List<SysConfigEntity> list = sysconfigService.list(wrapper);
            if (CollUtil.isEmpty(list)) {
                SysConfigEntity sysConfigEntity = new SysConfigEntity();
                sysConfigEntity.setCategory(SOCIALS_CONFIG);
                sysConfigEntity.setFkey(department);
                sysConfigEntity.setValue(departmentId);
                sysconfigService.save(sysConfigEntity);

            } else {
                SysConfigEntity configEntity = list.get(0);
                configEntity.setValue(departmentId);
                sysconfigService.updateById(configEntity);
            }

            String cacheKey = cacheKeyUtil.getSocialsConfig();
            redisUtil.remove(cacheKey);
            return true;
        }
        return false;
    }


    /**
     * 本地所有用户信息同步到钉钉
     * 不带第三方错误定位判断的功能代码 20210604
     *
     * @return ignore
     */
    @Operation(summary = "本地所有用户信息同步到钉钉")
    @Parameter(name = "type", description = "类型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllUserSysToDing")
    public ActionResult<Object>synAllUserSysToDing(@RequestParam("type") String type){
        if ("1".equals(type)) {
            //类型为1走钉钉用户信息同步到本地
            return this.synAllUserDingToSys();
        }
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();
        String dingDepartment = config.getDingDepartment();

        try {
            // 获取Token值
            JSONObject tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
            if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
                return ActionResult.success(MsgCode.SYS053.get());
            }
            String accessToken = tokenObject.getString(ACCESS_TOKEN);
            // 获取同步表、用户表的信息
            List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService.getList(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER);
            List<OrganizeEntity> organizeEntitiesBind = synThirdInfoService.getOrganizeEntitiesBind(dingDepartment);

            List<UserEntity> userEntities = userService.getList(organizeEntitiesBind.stream()
                    .map(OrganizeEntity::getId).collect(Collectors.toList()), "");
            // 根据同步表、公司表进行比较，判断不存的执行删除
            for (SynThirdInfoEntity synThirdInfoEntity : synThirdInfoList) {
                if (userEntities.stream().noneMatch(t -> t.getId().equals(synThirdInfoEntity.getSysObjId()))) {
                    // 执行删除操作
                    synThirdDingTalkService.deleteUserSysToDing(true, synThirdInfoEntity.getSysObjId(), accessToken);
                }
            }

            // 根据公司表、同步表进行比较，决定执行创建、还是更新
            for (UserEntity userEntity : userEntities) {
                if (synThirdInfoList.stream()
                        .noneMatch(t -> t.getSysObjId().equals(userEntity.getId()))
                ) {
                    // 执行创建功能
                    synThirdDingTalkService.createUserSysToDing(true, userEntity, accessToken);
                } else {
                    // 执行更新功能
                    synThirdDingTalkService.updateUserSysToDing(true, userEntity, accessToken);
                }
            }
        } catch (Exception e) {
            ActionResult.fail(e.toString());
        }

        //获取结果
        SynThirdTotal synThirdTotal = synThirdInfoService.getSynTotal(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER);
        return ActionResult.success(synThirdTotal);
    }


    //==================================钉钉的公司-部门-用户批量同步到本系统20220330==================================

    /**
     * 钉钉所有组织信息(包含公司和部门)同步到本系统
     * 不带第三方错误定位判断的功能代码 20220330
     *
     * @return ignore
     */
    @Operation(summary = "钉钉所有组织信息(包含公司和部门)同步到本系统")
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllOrganizeDingToSys")
    public ActionResult<Object>synAllOrganizeDingToSys(@RequestParam("departmentId") String departmentId) {
        // 设置redis的key
        String synDing = "";
        UserInfo userInfo = UserProvider.getUser();
        if (configValueUtil.isMultiTenancy()) {
            synDing = userInfo.getTenantId() + "_" + userInfo.getUserId() + "_synAllOrganizeDingToSys";
        } else {
            synDing = userInfo.getUserId() + "_synAllOrganizeDingToSys";
        }
        // 如果redis中存在key说明同步正在进行
        if (redisUtil.exists(synDing)) {
            return ActionResult.fail(MsgCode.SYS026.get());
        }
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        if (StringUtil.isEmpty(departmentId)) {
            return ActionResult.fail(SYNCHRONOUS);
        }
        OrganizeEntity info = organizeService.getInfo(departmentId);
        if (info == null) {
            return ActionResult.fail("同步组织数据不存在");
        }
        // 获取Token值
        JSONObject tokenObject = SynDingTalkUtil.getAccessToken(config.getDingSynAppKey(), config.getDingSynAppSecret());
        if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
            return ActionResult.fail(MsgCode.SYS053.get());
        }

        // 异步执行
        String finalSynDing = synDing;
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            String userId = userInfo.getUserId();
            try {
                redisUtil.insert(finalSynDing, "true");

                List<OapiV2DepartmentListsubResponse.DeptBaseResponse> deptBaseResponses = new ArrayList<>();

                String accessToken = tokenObject.getString(ACCESS_TOKEN);
                //  获取钉钉上的部门列表
                JSONObject retMsg = SynDingTalkUtil.getDepartmentList(SynThirdConsts.DING_ROOT_DEPT_ID, accessToken);
                if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
                    throw new DataException(retMsg.getString("msg"));
                }
                deptBaseResponses = (List<OapiV2DepartmentListsubResponse.DeptBaseResponse>) retMsg.get("department");
                List<DingTalkDeptModel> dingDeptListVo = JsonUtil.getJsonToList(deptBaseResponses, DingTalkDeptModel.class);
                List<DingTalkDeptModel> dingTalkDeptModels = new ArrayList<>(dingDeptListVo);

                Map<String, DingTalkDeptModel> deptModelMap = dingTalkDeptModels.stream().collect(Collectors.toMap(t -> String.valueOf(t.getDeptId()), dept -> dept));
                // 部门进行树结构化,固化上下层级序列化
                List<OrganizeModel> organizeModelList = dingTalkDeptModels.stream().map(t -> {
                    OrganizeModel model = JsonUtil.getJsonToBean(t, OrganizeModel.class);
                    model.setFullName(t.getName());
                    model.setCategory(t.getSourceIdentifier());
                    model.setParentId(t.getParentId() + "");
                    model.setDeptId(t.getDeptId() + "");
                    model.setId(t.getDeptId() + "");
                    model.setParentId(t.getParentId() + "");
                    return model;
                }).collect(Collectors.toList());
                List<SumTree<OrganizeModel>> trees = TreeDotUtils.convertListToTreeDot(organizeModelList);
                List<OrganizeListVO> listVO = JsonUtil.getJsonToList(trees, OrganizeListVO.class);

                // 转化成为按上下层级顺序排序的列表数据
                List<DingTalkDeptModel> listByOrder = new ArrayList<>();
                for (OrganizeListVO organizeVo : listVO) {
                    DingTalkDeptModel entity = deptModelMap.get(organizeVo.getId());
                    listByOrder.add(entity);
                    SynDingTalkUtil.getOrganizeTreeToList(organizeVo, deptModelMap, listByOrder);
                }

                // 钉钉没有最高级得先插入一条最高级
                synThirdInfoService.initBaseDept(SynThirdConsts.DING_ROOT_DEPT_ID, accessToken, SynThirdConsts.THIRD_TYPE_DING);
                // 获取同步表的信息
                List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService.getListByDepartment(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG,departmentId);
                List<String> sysDeptIds = synThirdInfoList.stream().map(SynThirdInfoEntity::getThirdObjId).collect(Collectors.toList());

                // 根据公司表、同步表进行比较，决定执行创建、还是更新
                for (DingTalkDeptModel dingDeptEntity : listByOrder) {
                    if (sysDeptIds.contains(String.valueOf(dingDeptEntity.getDeptId()))) {
                        // 执行本地更新功能
                        synThirdDingTalkService.updateDepartmentDingToSys(true, dingDeptEntity, accessToken);
                    } else {
                        // 执行本的创建功能
                        synThirdDingTalkService.createDepartmentDingToSys(true, dingDeptEntity, accessToken);
                    }
                }
            } catch (Exception e) {
                e.getMessage();
                log.error("{}，钉钉所有组织信息同步到本系统失败：{}", finalSynDing, e.getMessage());
            } finally {
                redisUtil.remove(finalSynDing);
                redisUtil.remove(cacheKeyUtil.getOrganizeList());
                List<String> toUserId = new ArrayList<>(1);
                toUserId.add(userId);
                messageService.sentMessage(toUserId, "钉钉所有组织信息同步到本系统", null, userInfo, 3, 1);
            }
        });
        return ActionResult.success(MsgCode.SYS026.get());
    }


    /**
     * 钉钉所有用户信息同步到本系统
     * 不带第三方错误定位判断的功能代码 20210604
     *
     * @return ignore
     */
    @Operation(summary = "钉钉所有用户信息同步到本系统")
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/synAllUserDingToSys")
    @DSTransactional
    public ActionResult<Object>synAllUserDingToSys() {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        String dingDepartment = socialsConfig.getDingDepartment();
        // 设置redis的key
        String synDing = "";
        UserInfo userInfo = UserProvider.getUser();
        if (configValueUtil.isMultiTenancy()) {
            synDing = userInfo.getTenantId() + "_" + userInfo.getUserId() + "_synAllUserDingToSys";
        } else {
            synDing = userInfo.getUserId() + "_synAllUserDingToSys";
        }
        // 如果redis中存在key说明同步正在进行
        if (redisUtil.exists(synDing)) {
            return ActionResult.fail(MsgCode.SYS026.get());
        }
        // 获取已同步的部门信息
        List<SynThirdInfoEntity> synThirdOrgList = synThirdInfoService.getListByDepartment(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, dingDepartment);
        List<String> dingDeptIdList = new ArrayList<>();
        if (CollUtil.isNotEmpty(synThirdOrgList)) {
            dingDeptIdList = synThirdOrgList.stream().map(SynThirdInfoEntity::getThirdObjId)
                    .filter(StringUtils::isNotBlank)
                    .distinct().collect(Collectors.toList());
        } else {
            return ActionResult.fail(MsgCode.SYS028.get());
        }

        // 获取Token值
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        JSONObject tokenObject = SynDingTalkUtil.getAccessToken(config.getDingSynAppKey(), config.getDingSynAppSecret());
        if (Boolean.FALSE.equals(tokenObject.getBoolean("code"))) {
            return ActionResult.fail(MsgCode.SYS053.get());
        }
        // 异步执行
        List<String> finalDingDeptIdList = dingDeptIdList;
        String finalSynDing = synDing;
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            String userId = userInfo.getUserId();

            try {
                redisUtil.insert(finalSynDing, "true");
                List<OapiV2UserListResponse.ListUserResponse> dingUserList = new ArrayList<>();
                String accessToken = tokenObject.getString(ACCESS_TOKEN);

                // 获取钉钉的用户列表
                JSONObject retMsg = SynDingTalkUtil.getUserDingList(finalDingDeptIdList, accessToken);


                dingUserList = (List<OapiV2UserListResponse.ListUserResponse>) retMsg.get("userlist");

                // 获取同步表、用户表的信息
                List<SynThirdInfoEntity> synThirdInfoList = synThirdInfoService.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, SynThirdConsts.THIRD_TYPE_DING);
                // 根据公司表、同步表进行比较，决定执行创建、还是更新
                for (OapiV2UserListResponse.ListUserResponse dingUserModel : dingUserList) {
                    if (synThirdInfoList.stream().noneMatch(t -> t.getThirdObjId().equals(dingUserModel.getUserid()))) {
                        // 执行创建功能
                        synThirdDingTalkService.createUserDingToSys(true, dingUserModel, accessToken);
                    } else {
                        // 执行更新功能
                        synThirdDingTalkService.updateUserDingToSystem(true, dingUserModel);
                    }
                }
            } catch (Exception e) {
                log.error("{}，钉钉所有用户信息同步到本系统失败：{}", finalSynDing, e.getMessage());
            } finally {
                redisUtil.remove(finalSynDing);
                List<String> toUserId = new ArrayList<>(1);
                toUserId.add(userId);
                messageService.sentMessage(toUserId, "钉钉所有用户信息同步到本系统", null, userInfo, 3, 1);
            }
        });
        return ActionResult.success(MsgCode.SYS026.get());
    }

    @Operation(summary = "解除同步")
    @SaCheckPermission(value = {"sysConfig.parameter", "integrationCenter.dingTalk"}, mode = SaMode.OR)
    @GetMapping("/clearAllSyn")
    @DSTransactional
    public ActionResult<Object>clearAllSyn(String type) {
        synThirdInfoService.clearAllSyn(Integer.valueOf(type));
        return ActionResult.success(MsgCode.SU005.get());
    }
}
