package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.EmailConfigEntity;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.model.systemconfig.EmailTestForm;
import jnpf.base.model.systemconfig.SocialsSysVo;
import jnpf.base.model.systemconfig.SysConfigModel;
import jnpf.base.service.SysconfigService;
import jnpf.base.service.SystemConfigApi;
import jnpf.constant.MsgCode;
import jnpf.message.entity.QyWebChatModel;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.model.message.DingTalkModel;
import jnpf.message.service.SynThirdInfoService;
import jnpf.message.util.QyWebChatUtil;
import jnpf.message.util.SynThirdConsts;
import jnpf.model.SocialsSysConfig;
import jnpf.permission.model.user.form.UserUpAdminForm;
import jnpf.permission.model.user.vo.UserAdminVO;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.third.DingTalkUtil;
import jnpf.util.wxutil.HttpUtil;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "系统配置", description = "SysConfig")
@RestController
@RequestMapping("/api/system/SysConfig")
@RequiredArgsConstructor
public class SysConfigController extends SuperController<SysconfigService, SysConfigEntity> implements SystemConfigApi {

    
    private final SysconfigService sysconfigService;

    
    private final UserService userService;

    
    private final WorkFlowApi workFlowApi;

    
    private final SynThirdInfoService synThirdInfoService;

    /**
     * 列表
     *
     * @return ignore
     */
    @Operation(summary = "列表")
    @GetMapping
    @SaCheckPermission(value = {"sysConfig.parameter", "sysConfig.strategy"}, mode = SaMode.OR)
    public ActionResult<SysConfigModel> list() {
        List<SysConfigEntity> list = sysconfigService.getList("SysConfig");
        HashMap<String, String> map = new HashMap<>(16);
        for (SysConfigEntity sys : list) {
            map.put(sys.getFkey(), sys.getValue());
        }
        SysConfigModel sysConfigModel = JsonUtil.getJsonToBean(map, SysConfigModel.class);
        return ActionResult.success(sysConfigModel);
    }

    /**
     * 保存设置
     *
     * @param sysConfigModel 系统配置模型
     * @return ignore
     */
    @Operation(summary = "更新系统配置")
    @Parameter(name = "sysConfigModel", description = "系统模型", required = true)
    @SaCheckPermission(value = {"sysConfig.parameter", "sysConfig.strategy"}, mode = SaMode.OR)
    @PutMapping
    public ActionResult<Object> save(@RequestBody SysConfigModel sysConfigModel) {
        if (Objects.nonNull(sysConfigModel.getVerificationCodeNumber())) {
            if (sysConfigModel.getVerificationCodeNumber() > 6) {
                return ActionResult.fail(MsgCode.SYS029.get());
            }
            if (sysConfigModel.getVerificationCodeNumber() < 3) {
                return ActionResult.fail(MsgCode.SYS030.get());
            }
        }
        String flowTodo = sysconfigService.getValueByKey("flowTodo");
        if (ObjectUtil.equals(flowTodo, "1") && ObjectUtil.equals(sysConfigModel.getFlowTodo(), 0) && workFlowApi.checkTodo()) {
                return ActionResult.fail(MsgCode.WF141.get());
            }

        String flowSign = sysconfigService.getValueByKey("flowSign");
        if (ObjectUtil.equals(flowSign, "1") && ObjectUtil.equals(sysConfigModel.getFlowSign(), 0) && workFlowApi.checkSign()) {
                return ActionResult.fail(MsgCode.WF138.get());
            }

        if (sysConfigModel.getAddSignLevel() > 6 || sysConfigModel.getAddSignLevel() < 1) {
            return ActionResult.fail("加签层级的范围不在1-6之间");
        }
        List<SysConfigEntity> entitys = new ArrayList<>();
        Map<String, Object> map = JsonUtil.entityToMap(sysConfigModel);
        map.put("isLog", "1");
        map.put("sysTheme", "1");
        map.put("pageSize", "30");
        map.put("lastLoginTime", 1);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            SysConfigEntity entity = new SysConfigEntity();
            entity.setId(RandomUtil.uuId());
            entity.setFkey(entry.getKey());
            entity.setValue(String.valueOf(entry.getValue()));
            entitys.add(entity);
        }
        sysconfigService.save(entitys);
        return ActionResult.success(MsgCode.SU005.get());
    }

    @Operation(summary = "获取第三方配置")
    @SaCheckPermission(value = {"sysConfig.parameter", "integrationCenter.dingTalk"}, mode = SaMode.OR)
    @GetMapping("/socials")
    public ActionResult<SocialsSysVo> getSocials() {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        SocialsSysVo vo = JsonUtil.getJsonToBean(socialsConfig, SocialsSysVo.class);
        List<SynThirdInfoEntity> qySynList = synThirdInfoService.getList(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG);
        List<SynThirdInfoEntity> dingSynList = synThirdInfoService.getList(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG);
        if (CollUtil.isNotEmpty(qySynList)) {
            vo.setQyhDisabled(true);
        }
        if (CollUtil.isNotEmpty(dingSynList)) {
            vo.setDingDisabled(true);
        }
        return ActionResult.success(vo);
    }

    @Operation(summary = "更新第三方配置")
    @Parameter(name = "SocialsSysConfig", description = "第三方参数模型", required = true)
    @SaCheckPermission(value = {"sysConfig.parameter", "integrationCenter.dingTalk"}, mode = SaMode.OR)
    @PutMapping("/socials")
    public ActionResult<Object> saveSocials(@RequestBody SocialsSysConfig sysConfigModel) {
        List<SysConfigEntity> entitys = new ArrayList<>();
        Map<String, Object> map = JsonUtil.entityToMap(sysConfigModel);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            SysConfigEntity entity = new SysConfigEntity();
            entity.setId(RandomUtil.uuId());
            entity.setFkey(entry.getKey());
            entity.setValue(String.valueOf(entry.getValue()));
            entity.setCategory("SocialsConfig");
            entitys.add(entity);
        }
        sysconfigService.saveSocials(entitys);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 邮箱账户密码验证
     *
     * @param emailTestForm 邮箱测试模型
     * @return ignore
     */
    @Operation(summary = "邮箱连接测试")
    @Parameter(name = "emailTestForm", description = "邮箱测试模型", required = true)
    @SaCheckPermission(value = {"sysConfig.parameter", "sysConfig.strategy"}, mode = SaMode.OR)
    @PostMapping("/Email/Test")
    public ActionResult<Object> checkLogin(@RequestBody EmailTestForm emailTestForm) {
        EmailConfigEntity entity = JsonUtil.getJsonToBean(emailTestForm, EmailConfigEntity.class);
        entity.setEmailSsl(Integer.valueOf(emailTestForm.getSsl()));
        String result = sysconfigService.checkLogin(entity);
        if ("true".equals(result)) {
            return ActionResult.success(MsgCode.SU017.get());
        } else {
            return ActionResult.fail(result);
        }
    }


    //=====================================测试企业微信、钉钉的连接=====================================

    /**
     * 测试企业微信配置的连接功能
     *
     * @param type           0-发送消息,1-同步组织
     * @param qyWebChatModel 企业微信模型
     * @return ignore
     */
    @Operation(summary = "测试企业微信配置的连接")
    @Parameter(name = "type", description = "0-发送消息,1-同步组织", required = true)
    @Parameter(name = "qyWebChatModel", description = "企业微信模型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @PostMapping("{type}/testQyWebChatConnect")
    public ActionResult<Object> testQyWebChatConnect(@PathVariable("type") String type, @RequestBody @Valid QyWebChatModel qyWebChatModel) {
        JSONObject retMsg;
        // 测试发送消息、组织同步的连接
        String corpId = qyWebChatModel.getQyhCorpId();
        String agentSecret = qyWebChatModel.getQyhAgentSecret();
        String corpSecret = qyWebChatModel.getQyhCorpSecret();
        // 测试发送消息的连接
        if ("0".equals(type)) {
            retMsg = QyWebChatUtil.getAccessToken(corpId, agentSecret);
            if (HttpUtil.isWxError(retMsg)) {
                return ActionResult.fail(MsgCode.SYS031.get(retMsg.getString("errmsg")));
            }
            return ActionResult.success(MsgCode.SYS032.get());
        } else if ("1".equals(type)) {
            retMsg = QyWebChatUtil.getAccessToken(corpId, corpSecret);
            if (HttpUtil.isWxError(retMsg)) {
                return ActionResult.fail(MsgCode.SYS033.get(retMsg.getString("errmsg")));
            }
            return ActionResult.success(MsgCode.SYS037.get());
        }
        return ActionResult.fail(MsgCode.SYS035.get());
    }

    /**
     * 测试钉钉配置的连接功能
     *
     * @param dingTalkModel 钉钉模板
     * @return ignore
     */
    @Operation(summary = "测试钉钉配置的连接")
    @Parameter(name = "dingTalkModel", description = "钉钉模型", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @PostMapping("/testDingTalkConnect")
    public ActionResult<Object> testDingTalkConnect(@RequestBody @Valid DingTalkModel dingTalkModel) {
        JSONObject retMsg;
        // 测试钉钉配置的连接
        String appKey = dingTalkModel.getDingSynAppKey();
        String appSecret = dingTalkModel.getDingSynAppSecret();
        // 测试钉钉的连接
        retMsg = DingTalkUtil.getAccessToken(appKey, appSecret);
        if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
            return ActionResult.fail(MsgCode.SYS036.get(retMsg.getString("error")));
        }

        return ActionResult.success(MsgCode.SYS037.get());
    }

    /**
     * 获取管理员集合
     *
     * @return
     */
    @Operation(summary = "获取管理员集合")
    @SaCheckPermission("sysConfig.parameter")
    @GetMapping("/getAdminList")
    public ActionResult<List<UserAdminVO>> getAdminList() {
        List<UserAdminVO> admins = JsonUtil.getJsonToList(userService.getAdminList(), UserAdminVO.class);
        return ActionResult.success(admins);
    }

    /**
     * 获取管理员集合
     *
     * @param userUpAdminForm 超级管理员设置表单参数
     * @return
     */
    @Operation(summary = "获取管理员集合")
    @Parameter(name = "userUpAdminForm", description = "超级管理员设置表单参数", required = true)
    @SaCheckPermission("sysConfig.parameter")
    @PutMapping("/setAdminList")
    public ActionResult<String> setAdminList(@RequestBody UserUpAdminForm userUpAdminForm) {
        userService.setAdminListByIds(userUpAdminForm.getAdminIds());
        return ActionResult.success(MsgCode.SU004.get());
    }

}
