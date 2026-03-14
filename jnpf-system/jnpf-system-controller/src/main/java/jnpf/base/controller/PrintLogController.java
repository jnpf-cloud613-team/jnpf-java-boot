package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.PaginationTime;
import jnpf.base.UserInfo;
import jnpf.base.model.vo.PrintLogVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import jnpf.base.ActionResult;
import jnpf.base.entity.PrintLogEntity;
import jnpf.base.service.PrintLogService;
import org.springframework.validation.annotation.Validated;
import jnpf.base.model.printlog.PrintLogInfo;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "打印模板日志", description = "PrintLogController")
@RestController
@RequestMapping("/api/system/printLog")
@RequiredArgsConstructor
public class PrintLogController {

    private final PrintLogService printLogService;

    


    private final UserService userService;

    /**
     * 获取列表
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "获取列表")
    @Parameter(name = "id", description = "打印模板ID", required = true)
    @SaCheckPermission("system.printDev")
    @GetMapping("/{id}")
    public ActionResult<PageListVO<PrintLogVO>> list(@PathVariable("id") String printId, PaginationTime page) {
        List<PrintLogVO> list = printLogService.list(printId, page);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(page, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 保存信息
     *
     * @param info 实体对象
     * @return
     */
    @Operation(summary = "保存信息")
    @Parameter(name = "info", description = "实体对象", required = true)
    @SaCheckPermission("system.printDev")
    @PostMapping("save")
    public ActionResult<Object> save(@RequestBody @Validated PrintLogInfo info) {
        PrintLogEntity printLogEntity = BeanUtil.copyProperties(info, PrintLogEntity.class);
        UserInfo userInfo = UserProvider.getUser();

        printLogEntity.setId(RandomUtil.uuId());
        printLogEntity.setCreatorTime(new Date());
        printLogEntity.setCreatorUserId(userInfo.getUserId());
        printLogService.save(printLogEntity);
        return ActionResult.success(MsgCode.SU002);
    }


}
