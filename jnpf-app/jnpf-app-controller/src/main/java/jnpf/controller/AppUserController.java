package jnpf.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.model.AppUserInfoVO;
import jnpf.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-08
 */
@Tag(name = "app用户信息", description = "User")
@RestController
@RequestMapping("/api/app/User")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AppUserController {


    private final AppService appService;



    @Operation(summary = "通讯录详情")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<AppUserInfoVO> userInfo(@PathVariable("id") String id) {
        AppUserInfoVO userInfoVO = appService.getInfo(id);
        return ActionResult.success(userInfoVO);
    }

}
