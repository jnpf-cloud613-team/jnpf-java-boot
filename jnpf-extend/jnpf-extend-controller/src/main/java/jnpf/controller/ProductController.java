package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.entity.ProductEntity;
import jnpf.entity.ProductEntryEntity;
import jnpf.exception.DataException;
import jnpf.model.product.*;
import jnpf.model.productentry.ProductEntryInfoVO;
import jnpf.model.productentry.ProductEntryListVO;
import jnpf.model.productentry.ProductEntryMdoel;
import jnpf.service.ProductEntryService;
import jnpf.service.ProductService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 销售订单
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 10:40:59
 */
@Slf4j
@RestController
@Tag(name = "销售订单", description = "Product")
@RequestMapping("/api/extend/saleOrder/Product")
@RequiredArgsConstructor
public class ProductController extends SuperController<ProductService, ProductEntity> {


    private final ProductService productService;

    private final ProductEntryService productEntryService;

    private final Random random = new Random();

    /**
     * 列表
     *
     * @param productPagination 分页模型
     * @return
     */
    @Operation(summary = "列表")
    @GetMapping
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<PageListVO<ProductListVO>> list(ProductPagination productPagination) {
        List<ProductEntity> list = productService.getList(productPagination);
        List<ProductListVO> listVO = JsonUtil.getJsonToList(list, ProductListVO.class);
        PageListVO<ProductListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(productPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param productCrForm 销售模型
     * @return
     */
    @Operation(summary = "创建")
    @PostMapping
    @Parameter(name = "productCrForm", description = "销售模型", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>create(@RequestBody @Valid ProductCrForm productCrForm) throws DataException {
        ProductEntity entity = JsonUtil.getJsonToBean(productCrForm, ProductEntity.class);
        List<ProductEntryEntity> productEntryList = JsonUtil.getJsonToList(productCrForm.getProductEntryList(), ProductEntryEntity.class);
        productService.create(entity, productEntryList);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<ProductInfoVO> info(@PathVariable("id") String id) {
        ProductEntity entity = productService.getInfo(id);
        ProductInfoVO vo = JsonUtil.getJsonToBean(entity, ProductInfoVO.class);
        List<ProductEntryEntity> productEntryList = productEntryService.getProductentryEntityList(id);
        List<ProductEntryInfoVO> productList = JsonUtil.getJsonToList(productEntryList, ProductEntryInfoVO.class);
        vo.setProductEntryList(productList);
        return ActionResult.success(vo);
    }

    /**
     * 更新
     *
     * @param productUpForm 销售模型
     * @param id            主键
     * @return
     */
    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "productUpForm", description = "销售模型", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid ProductUpForm productUpForm) {
        ProductEntity entity = productService.getInfo(id);
        if (entity != null) {
            List<ProductEntryEntity> productEntryList = JsonUtil.getJsonToList(productUpForm.getProductEntryList(), ProductEntryEntity.class);
            productService.update(id, entity, productEntryList);
            return ActionResult.success(MsgCode.SU004.get());
        } else {
            return ActionResult.fail(MsgCode.FA002.get());
        }
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ProductEntity entity = productService.getInfo(id);
        if (entity != null) {
            productService.delete(entity);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 获取销售产品明细
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取销售明细")
    @GetMapping("/ProductEntry/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<ListVO<ProductEntryListVO>> productEntryList(@PathVariable("id") String id) {
        String data = "[{\"id\":\"37c995b4044541009fb7e285bcf9845d\",\"productSpecification\":\"120ml\",\"qty\":16,\"money\":510,\"price\":120,\"commandType\":\"唯一码\",\"util\":\"盒\"},{\"id\":\"2dbb11d3cde04c299985ac944d130ba0\",\"productSpecification\":\"150ml\",\"qty\":15,\"money\":520,\"price\":310,\"commandType\":\"唯一码\",\"util\":\"盒\"},{\"id\":\"f8ec261ccdf045e5a2e1f0e5485cda76\",\"productSpecification\":\"40ml\",\"qty\":13,\"money\":530,\"price\":140,\"commandType\":\"唯一码\",\"util\":\"盒\"},{\"id\":\"6c110b57ae56445faa8ce9be501c8997\",\"productSpecification\":\"103ml\",\"qty\":2,\"money\":504,\"price\":150,\"commandType\":\"唯一码\",\"util\":\"盒\"},{\"id\":\"f2ee981aaf934147a4d090a0eed2203f\",\"productSpecification\":\"120ml\",\"qty\":21,\"money\":550,\"price\":160,\"commandType\":\"唯一码\",\"util\":\"盒\"}]";
        List<ProductEntryMdoel> dataAll = JsonUtil.getJsonToList(data, ProductEntryMdoel.class);
        List<ProductEntryEntity> productEntryList = productEntryService.getProductentryEntityList(id);
        List<ProductEntryListVO> productList = JsonUtil.getJsonToList(productEntryList, ProductEntryListVO.class);
        for (ProductEntryListVO entry : productList) {
            List<ProductEntryMdoel> dataList = new ArrayList<>();

            int num = random.nextInt(dataAll.size());
            for (int i = 0; i < num; i++) {
                dataList.add(dataAll.get(num));
            }
            entry.setDataList(dataList);
        }
        ListVO<ProductEntryListVO> vo = new ListVO<>();
        vo.setList(productList);
        return ActionResult.success(vo);
    }

}
