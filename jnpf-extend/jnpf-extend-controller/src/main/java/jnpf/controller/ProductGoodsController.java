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
import jnpf.entity.ProductGoodsEntity;
import jnpf.model.productgoods.*;
import jnpf.service.ProductGoodsService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 产品商品
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 15:57:50
 */
@Slf4j
@RestController
@Tag(name = "产品商品", description = "Goods")
@RequestMapping("/api/extend/saleOrder/Goods")
@RequiredArgsConstructor
public class ProductGoodsController extends SuperController<ProductGoodsService, ProductGoodsEntity> {


    private final ProductGoodsService productgoodsService;

    /**
     * 列表
     *
     * @param type 类型
     * @return
     */
    @GetMapping("/getGoodList")
    @Operation(summary = "列表")
    @Parameter(name = "type", description = "类型")
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<ListVO<ProductGoodsListVO>> list(@RequestParam("type")String type) {
        List<ProductGoodsEntity> list = productgoodsService.getGoodList(type);
        List<ProductGoodsListVO> listVO = JsonUtil.getJsonToList(list, ProductGoodsListVO.class);
        ListVO<ProductGoodsListVO> vo = new ListVO<>();
        vo.setList(listVO);
        return ActionResult.success(vo);
    }

    /**
     * 列表
     *
     * @param goodsPagination 分页模型
     * @return
     */
    @GetMapping
    @Operation(summary = "列表")
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<PageListVO<ProductGoodsListVO>> list(ProductGoodsPagination goodsPagination) {
        List<ProductGoodsEntity> list = productgoodsService.getList(goodsPagination);
        List<ProductGoodsListVO> listVO = JsonUtil.getJsonToList(list, ProductGoodsListVO.class);
        PageListVO<ProductGoodsListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(goodsPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param goodsCrForm 商品模型
     * @return
     */
    @PostMapping
    @Operation(summary = "创建")
    @Parameter(name = "goodsCrForm", description = "商品模型",required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>create(@RequestBody @Valid ProductGoodsCrForm goodsCrForm) {
        ProductGoodsEntity entity = JsonUtil.getJsonToBean(goodsCrForm, ProductGoodsEntity.class);
        productgoodsService.create(entity);
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
    public ActionResult<ProductGoodsInfoVO> info(@PathVariable("id") String id) {
        ProductGoodsEntity entity = productgoodsService.getInfo(id);
        ProductGoodsInfoVO vo = JsonUtil.getJsonToBean(entity, ProductGoodsInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 更新
     *
     * @param id                主键
     * @param goodsCrFormUpForm 商品模型
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "goodsCrFormUpForm", description = "商品模型",required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid ProductGoodsUpForm goodsCrFormUpForm) {
        ProductGoodsEntity entity = JsonUtil.getJsonToBean(goodsCrFormUpForm, ProductGoodsEntity.class);
        boolean ok = productgoodsService.update(id, entity);
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
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ProductGoodsEntity entity = productgoodsService.getInfo(id);
        if (entity != null) {
            productgoodsService.delete(entity);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 下拉
     *
     * @param goodsPagination 下拉模型
     * @return
     */
    @GetMapping("/Selector")
    @Operation(summary = "下拉")
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<ListVO<ProductGoodsListVO>> listSelect(ProductGoodsPagination goodsPagination) {
        goodsPagination.setCurrentPage(1);
        goodsPagination.setPageSize(50);
        List<ProductGoodsEntity> list = productgoodsService.getList(goodsPagination);
        List<ProductGoodsListVO> listVO = JsonUtil.getJsonToList(list, ProductGoodsListVO.class);
        ListVO<ProductGoodsListVO> vo = new ListVO<>();
        vo.setList(listVO);
        return ActionResult.success(vo);
    }

}
