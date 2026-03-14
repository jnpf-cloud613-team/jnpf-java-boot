package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.DataInterfaceLogEntity;
import jnpf.base.model.datainterface.DataInterfaceLogVO;
import jnpf.base.service.DataInterfaceLogService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据接口调用日志控制器
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-06-03
 */
@Tag(description = "DataInterfaceLog", name = "数据接口调用日志")
@RestController
@RequestMapping("/api/system/DataInterfaceLog")
@RequiredArgsConstructor
public class DataInterfaceLogController extends SuperController<DataInterfaceLogService, DataInterfaceLogEntity> {

    private final DataInterfaceLogService dataInterfaceLogService;

    private final UserService userService;

    /**
     * 获取数据接口调用日志列表
     *
     * @param id         主键
     * @param pagination 分页参数
     * @return ignore
     */
    @Operation(summary = "获取数据接口调用日志列表")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.dataInterface")
    @GetMapping("{id}")
    public ActionResult<PageListVO<DataInterfaceLogVO>> getList(@PathVariable("id") String id, Pagination pagination) {
        List<DataInterfaceLogEntity> list = dataInterfaceLogService.getList(id, pagination);
        List<DataInterfaceLogVO> voList = JsonUtil.getJsonToList(list, DataInterfaceLogVO.class);
        for (DataInterfaceLogVO vo : voList) {
            UserEntity entity = userService.getInfo(vo.getUserId());
            if (entity != null) {
                vo.setUserId(entity.getRealName() + "/" + entity.getAccount());
            }
        }
        PaginationVO vo = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, vo);
    }
}
