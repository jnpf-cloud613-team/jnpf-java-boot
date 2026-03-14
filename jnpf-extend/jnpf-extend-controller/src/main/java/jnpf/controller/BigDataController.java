package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.entity.BigDataEntity;
import jnpf.exception.WorkFlowException;
import jnpf.model.bidata.BigBigDataListVO;
import jnpf.service.BigDataService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 大数据测试
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "大数据测试", description = "BigData")
@RestController
@RequestMapping("/api/extend/BigData")
@RequiredArgsConstructor
public class BigDataController extends SuperController<BigDataService, BigDataEntity> {


    private final BigDataService bigDataService;



    /**
     * 列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "列表")
    @GetMapping
    @SaCheckPermission("extend.bigData")
    public ActionResult<PageListVO<BigBigDataListVO>> list(Pagination pagination) {
        List<BigDataEntity> data = bigDataService.getList(pagination);
        List<BigBigDataListVO> list = JsonUtil.getJsonToList(data, BigBigDataListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 新建
     *
     * @return
     */
    @Operation(summary = "添加大数据测试")
    @PostMapping
    @SaCheckPermission("extend.bigData")
    public ActionResult<Object> create() throws WorkFlowException {
        bigDataService.create(10000);
        return ActionResult.success(MsgCode.ETD105.get());
    }
}
