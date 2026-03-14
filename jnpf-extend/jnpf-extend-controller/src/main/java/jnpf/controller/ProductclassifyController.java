package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.entity.ProductclassifyEntity;
import jnpf.model.productclassify.*;
import jnpf.service.ProductclassifyService;
import jnpf.util.JsonUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 产品分类
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 14:34:04
 */
@Slf4j
@RestController
@Tag(name = "产品分类", description = "Classify")
@RequestMapping("/api/extend/saleOrder/Classify")
@RequiredArgsConstructor
public class ProductclassifyController extends SuperController<ProductclassifyService, ProductclassifyEntity> {


    private final ProductclassifyService productclassifyService;

    /**
     * 列表
     *
     * @return
     */
    @GetMapping
    @Operation(summary = "列表")
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<ListVO<ProductclassifyListVO>> list() {
        List<ProductclassifyEntity> data = productclassifyService.getList();
        List<ProductclassifyModel> modelList = JsonUtil.getJsonToList(data, ProductclassifyModel.class);
        List<SumTree<ProductclassifyModel>> sumTrees = TreeDotUtils.convertListToTreeDot(modelList);
        List<ProductclassifyListVO> list = JsonUtil.getJsonToList(sumTrees, ProductclassifyListVO.class);
        ListVO<ProductclassifyListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param classifyCrForm 分类模型
     * @return
     */
    @PostMapping
    @Operation(summary = "创建")
    @Parameter(name = "classifyCrForm", description = "分类模型", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>create(@RequestBody @Valid ProductclassifyCrForm classifyCrForm) {
        ProductclassifyEntity entity = JsonUtil.getJsonToBean(classifyCrForm, ProductclassifyEntity.class);
        productclassifyService.create(entity);
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
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<ProductclassifyInfoVO> info(@PathVariable("id") String id) {
        ProductclassifyEntity entity = productclassifyService.getInfo(id);
        ProductclassifyInfoVO vo = JsonUtil.getJsonToBean(entity, ProductclassifyInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 更新
     *
     * @param id             主键
     * @param classifyUpForm 分类模型
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "classifyUpForm", description = "分类模型", required = true)
    @SaCheckPermission("extend.orderDemo")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid ProductclassifyUpForm classifyUpForm) {
        ProductclassifyEntity entity = JsonUtil.getJsonToBean(classifyUpForm, ProductclassifyEntity.class);
        boolean ok = productclassifyService.update(id, entity);
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
        ProductclassifyEntity entity = productclassifyService.getInfo(id);
        if (entity != null) {
            productclassifyService.delete(entity);
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

}
