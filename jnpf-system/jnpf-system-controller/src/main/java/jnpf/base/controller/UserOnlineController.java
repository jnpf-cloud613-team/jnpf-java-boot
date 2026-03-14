package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.model.online.BatchOnlineModel;
import jnpf.base.service.UserOnlineService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.message.model.UserOnlineModel;
import jnpf.message.model.UserOnlineVO;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线用户
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "在线用户", description = "Online")
@RestController
@RequestMapping("/api/system/OnlineUser")
@RequiredArgsConstructor
public class UserOnlineController {


    private final UserOnlineService userOnlineService;

    /**
     * 列表
     *
     * @param page 分页参数
     * @return ignore
     */
    @Operation(summary = "获取在线用户列表")
    @SaCheckPermission(value = {"permission.userOnline", "monitor.userOnline"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult<PageListVO<UserOnlineVO>> list(Pagination page) {
        List<UserOnlineModel> data = userOnlineService.getList(page);
        List<UserOnlineVO> voList = data.stream().map(online -> {
            UserOnlineVO vo = JsonUtil.getJsonToBean(online, UserOnlineVO.class);
            vo.setUserId(online.getToken());
            return vo;
        }).collect(Collectors.toList());
        PaginationVO paginationVO = JsonUtil.getJsonToBean(page, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 注销
     *
     * @param token 主键值
     * @return
     */
    @Operation(summary = "强制下线")
    @Parameter(name = "token", description = "token", required = true)
    @SaCheckPermission("permission.userOnline")
    @DeleteMapping("/{token}")
    public ActionResult<Object> delete(@PathVariable("token") String token) {
        userOnlineService.delete(token);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 批量下线用户
     *
     * @param model 主键值
     * @return ignore
     */
    @Operation(summary = "批量下线用户")
    @Parameter(name = "model", description = "在线用户id集合", required = true)
    @SaCheckPermission("permission.userOnline")
    @DeleteMapping
    public ActionResult<Object> clear(@RequestBody BatchOnlineModel model) {
        userOnlineService.delete(model.getIds());
        return ActionResult.success(MsgCode.SU005.get());
    }
}
