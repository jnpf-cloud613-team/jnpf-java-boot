package jnpf.controller;

import jnpf.base.controller.SuperController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.vo.PageListVO;
import jnpf.constant.MsgCode;
import jnpf.entity.CustomerEntity;
import jnpf.model.customer.CustomerCrForm;
import jnpf.model.customer.CustomerInfoVO;
import jnpf.model.customer.CustomerListVO;
import jnpf.model.customer.CustomerUpForm;
import jnpf.service.CustomerService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 客户信息
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 14:09:05
 */
@Slf4j
@RestController
@Tag(name = "客户信息", description = "Customer")
@RequestMapping("/api/extend/saleOrder/Customer")
@RequiredArgsConstructor
public class CustomerController extends SuperController<CustomerService, CustomerEntity> {


    private final CustomerService customerService;



    /**
     * 列表
     *
     * @param pagination 分页模型
     * @return
     */
    @GetMapping
    @Operation(summary = "列表")
    public ActionResult<PageListVO<CustomerListVO>> list(Pagination pagination) {
        pagination.setPageSize(50);
        pagination.setCurrentPage(1);
        List<CustomerEntity> list = customerService.getList(pagination);
        List<CustomerListVO> listVO = JsonUtil.getJsonToList(list, CustomerListVO.class);
        PageListVO<CustomerListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param customerCrForm 新建模型
     * @return
     */
    @PostMapping
    @Operation(summary = "创建")
    @Parameter(name = "customerCrForm", description = "客户模型", required = true)
    public ActionResult<Object>create(@RequestBody @Valid CustomerCrForm customerCrForm) {
        CustomerEntity entity = JsonUtil.getJsonToBean(customerCrForm, CustomerEntity.class);
        customerService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "信息")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<CustomerInfoVO> info(@PathVariable("id") String id) {
        CustomerEntity entity = customerService.getInfo(id);
        CustomerInfoVO vo = JsonUtil.getJsonToBean(entity, CustomerInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 更新
     *
     * @param id             主键
     * @param customerUpForm 修改模型
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "customerUpForm", description = "客户模型", required = true)
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid CustomerUpForm customerUpForm) {
        CustomerEntity entity = JsonUtil.getJsonToBean(customerUpForm, CustomerEntity.class);
        boolean ok = customerService.update(id, entity);
        if (ok) {
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.fail(MsgCode.FA002.get());
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        CustomerEntity entity = customerService.getInfo(id);
        if (entity != null) {
            customerService.delete(entity);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }


}
