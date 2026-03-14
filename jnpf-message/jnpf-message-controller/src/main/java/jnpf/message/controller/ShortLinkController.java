package jnpf.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.consts.DeviceType;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.LoginException;
import jnpf.message.entity.ShortLinkEntity;
import jnpf.message.service.ShortLinkService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "短链接跳转", description = "message")
@RequestMapping("/api/message/ShortLink")
public class ShortLinkController extends SuperController<ShortLinkService, ShortLinkEntity> {

    private final ShortLinkService shortLinkService;
    private final ConfigValueUtil configValueUtil;
    protected final AuthUtil authUtil;

    /**
     * 消息发送配置弹窗列表
     *
     * @return
     */
    @NoDataSourceBind
    @Operation(summary = "根据短链接获取实际链接地址")
    @Parameter(name = "shortLink", description = "短链接", required = true)
    @Parameter(name = "tenant", description = "租户")
    @GetMapping(value = {"/{shortLink}/{tenant}", "/{shortLink}"})
    public ActionResult<Object> getShortUrl(@PathVariable("shortLink") String shortLink, @PathVariable(value = "tenant", required = false) String tenant, HttpServletResponse response) throws LoginException, IOException {
        String tokenStr = "&token=";
        if (configValueUtil.isMultiTenancy()) {
            if (StringUtil.isNotEmpty(tenant)) {
                //切换成租户库
                TenantDataSourceUtil.switchTenant(tenant);
            } else {
                return ActionResult.fail(MsgCode.LOG115.get());
            }
        }
        String link;
        ShortLinkEntity entity = shortLinkService.getInfoByLink(shortLink);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA039.get());
        }
        String frontDomain = configValueUtil.getFrontDomain();
        String appDomain = configValueUtil.getAppDomain();
        String realPcLink = entity.getRealPcLink();
        String realAppLink = entity.getRealAppLink();
        if (!realPcLink.contains("http")) {
            realPcLink = frontDomain + realPcLink;
        }
        if (!realAppLink.contains("http")) {
            realAppLink = appDomain + realAppLink;
        }
        DeviceType type = UserProvider.getDeviceForAgent();
        String token = AuthUtil.loginTempUser(entity.getUserId(), tenant);
        if (StringUtil.isEmpty(token)) {
            return ActionResult.fail(MsgCode.AD104.get());
        }

        if (entity.getIsUsed() == 1) {
            if (entity.getClickNum() < entity.getUnableNum() && entity.getUnableTime().after(DateUtil.getNowDate())) {
                if (DeviceType.PC.equals(type)) {
                    link = realPcLink + tokenStr + token;
                    entity.setClickNum(entity.getClickNum() + 1);
                    shortLinkService.updateById(entity);
                } else {
                    link = realAppLink + tokenStr + token;
                    entity.setClickNum(entity.getClickNum() + 1);
                    shortLinkService.updateById(entity);
                }
            } else {
                return ActionResult.fail(MsgCode.FA039.get());
            }
        } else {
            if (entity.getUnableTime().after(DateUtil.getNowDate())) {
                if (DeviceType.PC.equals(type)) {
                    link = realPcLink + tokenStr + token;
                    entity.setClickNum(entity.getClickNum() + 1);
                    shortLinkService.updateById(entity);
                } else {
                    link = realAppLink + tokenStr + token;
                    entity.setClickNum(entity.getClickNum() + 1);
                    shortLinkService.updateById(entity);
                }
            } else {
                return ActionResult.fail(MsgCode.FA039.get());
            }
        }

        response.sendRedirect(link);
        return ActionResult.success("");
    }

}
