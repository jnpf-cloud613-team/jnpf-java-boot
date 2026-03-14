package jnpf.permission.controller;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.LoginException;
import jnpf.permission.entity.SocialsUserEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.socails.SocialsUserInfo;
import jnpf.permission.model.socails.SocialsUserModel;
import jnpf.permission.model.socails.SocialsUserVo;
import jnpf.permission.service.SocialsUserService;
import jnpf.permission.service.UserService;
import jnpf.socials.config.CustomAuthConfig;
import jnpf.socials.config.SocialsConfig;
import jnpf.socials.enums.SocialsAuthEnum;
import jnpf.socials.model.AuthCallbackNew;
import jnpf.socials.utils.AuthSocialsUtil;
import jnpf.util.JsonUtil;
import jnpf.util.NoDataSourceBind;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.wxutil.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 单点登录
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/14 10:48:00
 */
@Tag(name = "第三方登录和绑定", description = KeyConst.SOCIALS)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/socials")
@Slf4j
public class SocialsUserController extends SuperController<SocialsUserService, SocialsUserEntity> {

    private final SocialsUserService socialsUserService;
    private final AuthSocialsUtil authSocialsUtil;
    private final UserService userService;
    private final SocialsConfig socialsConfig;
    private final ConfigValueUtil configValueUtil;

    /**
     * 获取用户列表
     *
     * @param
     * @return ignore
     */
    @Operation(summary = "获取用户授权列表")
    @Parameter(name = "userId", description = "用户id")
    @GetMapping
    public ActionResult<List<SocialsUserVo>> getList(@RequestParam(value = "userId", required = false) String userId) {
        if (StringUtil.isEmpty(userId)) {
            userId = UserProvider.getUser().getUserId();
        }
        List<Map<String, Object>> platformInfos = SocialsAuthEnum.getPlatformInfos();
        String s = JSON.toJSONString(platformInfos);
        List<SocialsUserVo> socialsUserVos = JsonUtil.getJsonToList(s, SocialsUserVo.class);
        List<CustomAuthConfig> config = socialsConfig.getConfig();
        List<SocialsUserVo> res = new ArrayList<>();
        if (config == null) {
            return ActionResult.fail(MsgCode.PS019.get());
        }
        config.stream().forEach(item ->
                socialsUserVos.stream().forEach(item2 -> {
                    if (item2.getEnname().toLowerCase().equals(item.getProvider())) {
                        res.add(item2);
                    }
                })
        );
        //查询绑定信息
        List<SocialsUserEntity> listByUserId = socialsUserService.getListByUserId(userId);
        List<SocialsUserModel> listModel = JsonUtil.getJsonToList(listByUserId, SocialsUserModel.class);
        res.forEach(item ->
                listModel.stream()
                        .filter(item2 -> item.getEnname().equals(item2.getSocialType()))
                        .findFirst()
                        .ifPresent(item::setEntity)
        );
        return ActionResult.success(res);
    }

    /**
     * 绑定：重定向第三方登录页面
     *
     * @return ignore
     */
    @Operation(summary = "重定向第三方登录页面")
    @Parameter(name = "source", description = "地址", required = true)
    @GetMapping("/render/{source}")
    public ActionResult<Object> render(@PathVariable String source) {
        AuthRequest authRequest = authSocialsUtil.getAuthRequest(source, UserProvider.getUser().getUserId(), false, null, UserProvider.getUser().getTenantId());
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return ActionResult.success(authorizeUrl);
    }


    /**
     * 设置租户库
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/9/8
     */
    private boolean setTenantData(String tenantId) {
        try {
            TenantDataSourceUtil.switchTenant(tenantId);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 解绑
     *
     * @param userId 用户id
     * @param id     主键
     * @return ignore
     */
    @Operation(summary = "解绑")
    @Parameter(name = "userId", description = "用户id")
    @Parameter(name = "id", description = "主键", required = true)
    @DeleteMapping("/{id}")
    public ActionResult<Object> deleteSocials(@RequestParam(value = "userId", required = false) String userId, @PathVariable("id") String id) {
        SocialsUserEntity byId = socialsUserService.getById(id);
        UserInfo userInfo = UserProvider.getUser();
        boolean b = socialsUserService.removeById(id);
        if (b) {
            //多租户开启-解除绑定
            if (configValueUtil.isMultiTenancy()) {
                String param = "?userId=" + byId.getUserId() + "&tenantId=" + userInfo.getTenantId() + "&socialsType=" + byId.getSocialType();
                JSONObject object = HttpUtil.httpRequest(configValueUtil.getMultiTenancyUrl() + KeyConst.SOCIALS + param, "DELETE", null);
                if (object == null || "500".equals(object.get("code").toString()) || "400".equals(object.getString("code"))) {
                    return ActionResult.fail(MsgCode.PS018.get());
                }
            }
            return ActionResult.success(MsgCode.SU005.get());
        }
        return ActionResult.fail(MsgCode.PS018.get());
    }


    @GetMapping("/list")
    @NoDataSourceBind
    public List<SocialsUserVo> getLoginList(@RequestParam("ticket") String ticket) {
        if (!socialsConfig.isSocialsEnabled()) return Collections.emptyList();
        List<Map<String, Object>> platformInfos = SocialsAuthEnum.getPlatformInfos();
        String s = JSON.toJSONString(platformInfos);
        List<SocialsUserVo> socialsUserVos = JsonUtil.getJsonToList(s, SocialsUserVo.class);
        List<CustomAuthConfig> config = socialsConfig.getConfig();
        List<SocialsUserVo> res = new ArrayList<>();
        config.forEach(item ->
                socialsUserVos.stream()
                        .filter(item2 -> item2.getEnname().toLowerCase().equals(item.getProvider()))
                        .forEach(item2 -> {
                            AuthRequest authRequest = authSocialsUtil.getAuthRequest(item2.getEnname(), null, true, ticket, null);
                            String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
                            item2.setRenderUrl(authorizeUrl);
                            res.add(item2);
                        })
        );
        return res;
    }

    @GetMapping("/getSocialsUserInfo")
    @NoDataSourceBind
    public SocialsUserInfo getSocialsUserInfo(@RequestParam("source") String source, @RequestParam("code") String code,
                                              @RequestParam(value = "state", required = false) String state) throws LoginException {
        //获取第三方请求
        AuthCallbackNew callback = setAuthCallback(code, state);
        AuthRequest authRequest = authSocialsUtil.getAuthRequest(source, null, false, null, null);
        AuthResponse<AuthUser> res = authRequest.login(callback);
        if (AuthResponseStatus.FAILURE.getCode() == res.getCode()) {
            throw new LoginException("连接失败！");
        } else if (AuthResponseStatus.SUCCESS.getCode() != res.getCode()) {
            throw new LoginException("授权失败:" + res.getMsg());
        }
        //登录用户第三方id
        String uuid = getSocialUuid(res);
        String socialName = StringUtil.isNotEmpty(res.getData().getUsername()) ? res.getData().getUsername() : res.getData().getNickname();
        return getUserInfo(source, uuid, socialName);
    }

    /**
     * 获取用户绑定信息列表
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/9/20
     */
    @NoDataSourceBind
    public SocialsUserInfo getUserInfo(String source, String uuid, String socialName) throws LoginException {
        SocialsUserInfo socialsUserInfo = new SocialsUserInfo();
        UserInfo userInfo = UserProvider.getLocalLoginUser();
        if (userInfo == null) {
            userInfo = new UserInfo();
        }
        //查询租户绑定
        if ("wechat_applets".equals(source)) {
            source = "wechat_open";
        }
        if (configValueUtil.isMultiTenancy()) {
            JSONObject object = HttpUtil.httpRequest(configValueUtil.getMultiTenancyUrl() + "socials/list?socialsId=" + uuid, "GET", null);
            if (object == null || "500".equals(object.get("code").toString()) || "400".equals(object.getString("code"))) {
                throw new LoginException("租户绑定信息查询错误！");
            }
            if ("200".equals(object.get("code").toString())) {
                JSONArray data = JSON.parseArray(object.get("data").toString());
                int size = data.size();
                if (size == 0) {
                    socialsUserInfo.setSocialUnionid(uuid);
                    socialsUserInfo.setSocialName(socialName);
                    return socialsUserInfo;
                } else if (data.size() == 1) {
                    //租户开启时-切换租户库
                    JSONObject oneUser = (JSONObject) data.get(0);
                    setTenantData(oneUser.get(KeyConst.TENANT_ID).toString());
                    List<SocialsUserEntity> list = socialsUserService.getUserIfnoBySocialIdAndType(uuid, source);
                    if (CollUtil.isEmpty(list)) {
                        throw new LoginException("第三方未绑定账号！");
                    }
                    UserEntity infoById = userService.getInfo(list.get(0).getUserId());
                    BeanUtils.copyProperties(infoById, userInfo);
                    userInfo.setTenantId(oneUser.get(KeyConst.TENANT_ID).toString());
                    userInfo.setUserId(infoById.getId());
                    userInfo.setUserAccount(userInfo.getTenantId() + "@" + infoById.getAccount());
                    socialsUserInfo.setTenantUserInfo(data);
                    socialsUserInfo.setUserInfo(userInfo);
                } else {
                    socialsUserInfo.setTenantUserInfo(data);
                }
            }
        } else {//非多租户
            //查询绑定
            List<SocialsUserEntity> list = socialsUserService.getUserIfnoBySocialIdAndType(uuid, source);
            if (CollUtil.isNotEmpty(list)) {
                UserEntity infoById = userService.getInfo(list.get(0).getUserId());
                BeanUtils.copyProperties(infoById, userInfo);
                userInfo.setUserId(infoById.getId());
                userInfo.setUserAccount(infoById.getAccount());
                socialsUserInfo.setUserInfo(userInfo);
            } else {
                socialsUserInfo.setSocialUnionid(uuid);
                socialsUserInfo.setSocialName(socialName);
            }
        }
        return socialsUserInfo;
    }

    /**
     * 绑定
     *
     * @return ignore
     */
    @GetMapping("/callback")
    @NoDataSourceBind
    public JSONObject binding(@RequestParam("source") String source,
                              @RequestParam(value = "userId", required = false) String userId,
                              @RequestParam(value = KeyConst.TENANT_ID, required = false) String tenantId,
                              @RequestParam(value = "code", required = false) String code,
                              @RequestParam(value = "state", required = false) String state) {
        log.info("进入callback：" + source + " callback params：");
        //获取第三方请求
        AuthCallbackNew callback = setAuthCallback(code, state);
        //租户开启时-切换租户库
        if (configValueUtil.isMultiTenancy()) {
            boolean b = setTenantData(tenantId);
            if (!b) {
                return resultJson(201, "查询租户信息错误！");
            }

        }
        //获取第三方请求
        AuthRequest authRequest = authSocialsUtil.getAuthRequest(source, userId, false, null, null);
        AuthResponse<AuthUser> res = authRequest.login(callback);
        log.info(JSON.toJSONString(res));
        if (res.ok()) {
            String uuid = getSocialUuid(res);
            List<SocialsUserEntity> userIfnoBySocialIdAndType = socialsUserService.getUserIfnoBySocialIdAndType(uuid, source);
            if (CollUtil.isNotEmpty(userIfnoBySocialIdAndType)) {
                UserEntity info = userService.getInfo(userIfnoBySocialIdAndType.get(0).getUserId());
                return resultJson(201, "当前账户已被" + info.getRealName() + "/" + info.getAccount() + "绑定，不能重复绑定");
            }
            SocialsUserEntity socialsUserEntity = new SocialsUserEntity();
            socialsUserEntity.setUserId(userId);
            socialsUserEntity.setSocialType(source);
            socialsUserEntity.setSocialName(res.getData().getUsername());
            socialsUserEntity.setSocialId(uuid);
            socialsUserEntity.setCreatorTime(new Date());
            boolean save = socialsUserService.save(socialsUserEntity);

            //租户开启时-添加租户库绑定数据
            if (configValueUtil.isMultiTenancy() && save) {
                JSONObject params = (JSONObject) JSON.toJSON(socialsUserEntity);
                UserEntity info = userService.getInfo(userId);
                params.put(KeyConst.TENANT_ID, tenantId);
                params.put("account", info.getAccount());
                params.put("accountName", info.getRealName() + "/" + info.getAccount());
                JSONObject object = HttpUtil.httpRequest(configValueUtil.getMultiTenancyUrl() + KeyConst.SOCIALS, "POST", params.toJSONString());
                if (object == null || "500".equals(object.get("code").toString()) || "400".equals(object.getString("code"))) {
                    return resultJson(201, "用户租户绑定错误!");
                }
            }
            return resultJson(200, "绑定成功!");

        }
        return resultJson(201, "第三方回调失败！");
    }

    /**
     * 设置第三方code state参数
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/9/8
     */
    private AuthCallbackNew setAuthCallback(String code, String state) {
        AuthCallbackNew callback = new AuthCallbackNew();
        callback.setAuthCode(code);
        callback.setAuth_code(code);
        callback.setAuthorization_code(code);
        callback.setCode(code);
        callback.setState(state);
        return callback;
    }

    /**
     * 返回json
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/9/8
     */
    private JSONObject resultJson(int code, String message) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("message", message);
        return jsonObject;
    }

    private String getSocialUuid(AuthResponse<AuthUser> res) {
        String uuid = res.getData().getUuid();
        if (res.getData().getToken() != null && StringUtil.isNotEmpty(res.getData().getToken().getUnionId())) {
            uuid = res.getData().getToken().getUnionId();
        }
        return uuid;
    }

    /**
     * 绑定
     *
     * @return ignore
     */
    @GetMapping("/loginbind")
    @NoDataSourceBind
    public void loginAutoBinding(@RequestParam("socialType") String socialType,
                                 @RequestParam("socialUnionid") String socialUnionid,
                                 @RequestParam("socialName") String socialName,
                                 @RequestParam("userId") String userId,
                                 @RequestParam(value = KeyConst.TENANT_ID, required = false) String tenantId) {
        //查询租户绑定
        if ("wechat_applets".equals(socialType)) {
            socialType = "wechat_open";
        }
        //租户开启时-切换租户库
        if (configValueUtil.isMultiTenancy()) {
            setTenantData(tenantId);
        }
        List<SocialsUserEntity> list = socialsUserService.getListByUserIdAndSource(userId, socialType);
        if (CollUtil.isNotEmpty(list)) {//账号已绑定该第三方其他账号，则不绑定
            return;
        }
        SocialsUserEntity socialsUserEntity = new SocialsUserEntity();
        socialsUserEntity.setUserId(userId);
        socialsUserEntity.setSocialType(socialType);
        socialsUserEntity.setSocialName(socialName);
        socialsUserEntity.setSocialId(socialUnionid);
        socialsUserEntity.setCreatorTime(new Date());
        boolean save = socialsUserService.save(socialsUserEntity);
        //租户开启时-添加租户库绑定数据
        if (configValueUtil.isMultiTenancy() && save) {
            JSONObject params = (JSONObject) JSON.toJSON(socialsUserEntity);
            UserEntity info = userService.getInfo(userId);
            params.put(KeyConst.TENANT_ID, tenantId);
            params.put("account", info.getAccount());
            params.put("accountName", info.getRealName() + "/" + info.getAccount());
            HttpUtil.httpRequest(configValueUtil.getMultiTenancyUrl() + KeyConst.SOCIALS, "POST", params.toJSONString());
        }
    }
}
