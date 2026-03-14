package jnpf.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.entity.SalesOrderEntity;
import jnpf.entity.SalesOrderEntryEntity;
import jnpf.exception.WorkFlowException;
import jnpf.model.salesorder.SalesOrderEntryEntityInfoModel;
import jnpf.model.salesorder.SalesOrderForm;
import jnpf.model.salesorder.SalesOrderInfoVO;
import jnpf.service.SalesOrderService;
import jnpf.util.GeneraterSwapUtil;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 销售订单
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月29日 上午9:18
 */
@Tag(name = "销售订单", description = "SalesOrder")
@RestController
@RequestMapping("/api/extend/Form/SalesOrder")
@RequiredArgsConstructor
public class SalesOrderController extends SuperController<SalesOrderService, SalesOrderEntity> {


    private final SalesOrderService salesOrderService;

    private final GeneraterSwapUtil generaterSwapUtil;

    /**
     * 获取销售订单信息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取销售订单信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object>info(@PathVariable("id") String id) {
        SalesOrderEntity entity = salesOrderService.getInfo(id);
        List<SalesOrderEntryEntity> entityList = salesOrderService.getSalesEntryList(id);
        SalesOrderInfoVO vo = JsonUtil.getJsonToBean(entity, SalesOrderInfoVO.class);
        if (vo != null) {
            vo.setEntryList(JsonUtil.getJsonToList(entityList, SalesOrderEntryEntityInfoModel.class));
        }
        return ActionResult.success(vo);
    }

    /**
     * 新建销售订单
     *
     * @param salesOrderForm 表单对象
     * @return
     * @throws WorkFlowException
     */
    @Operation(summary = "新建销售订单")
    @PostMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "salesOrderForm", description = "销售模型", required = true)
    public ActionResult<Object>create(@RequestBody SalesOrderForm salesOrderForm, @PathVariable("id") String id){
        SalesOrderEntity sales = JsonUtil.getJsonToBean(salesOrderForm, SalesOrderEntity.class);
        List<SalesOrderEntryEntity> salesEntryList = JsonUtil.getJsonToList(salesOrderForm.getEntryList(), SalesOrderEntryEntity.class);
        salesOrderService.submit(id, sales, salesEntryList, salesOrderForm);
        return ActionResult.success(MsgCode.SU006.get());
    }

    /**
     * 修改销售订单
     *
     * @param salesOrderForm 表单对象
     * @param id             主键
     * @return
     * @throws WorkFlowException
     */
    @Operation(summary = "修改销售订单")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "salesOrderForm", description = "销售模型", required = true)
    public ActionResult<Object>update(@RequestBody SalesOrderForm salesOrderForm, @PathVariable("id") String id)  {
        SalesOrderEntity sales = JsonUtil.getJsonToBean(salesOrderForm, SalesOrderEntity.class);
        sales.setId(id);
        List<SalesOrderEntryEntity> salesEntryList = JsonUtil.getJsonToList(salesOrderForm.getEntryList(), SalesOrderEntryEntity.class);
        salesOrderService.submit(id, sales, salesEntryList, salesOrderForm);
        return ActionResult.success(MsgCode.SU006.get());
    }

    /**
     * 删除销售订单信息
     *
     * @param id 主键
     */
    @Operation(summary = "删除销售订单信息")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object>delete(@PathVariable("id") String id, @RequestParam(name = "forceDel", defaultValue = "false") Boolean forceDel) {
        SalesOrderEntity entity = salesOrderService.getInfo(id);
        if (null != entity) {
            if (Boolean.FALSE.equals(forceDel)) {
                String errMsg = generaterSwapUtil.deleteFlowTask(entity.getId());
                if (StringUtils.isNotBlank(errMsg)) {
                    throw new IllegalArgumentException(errMsg);
                }
            }
            salesOrderService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }
}
