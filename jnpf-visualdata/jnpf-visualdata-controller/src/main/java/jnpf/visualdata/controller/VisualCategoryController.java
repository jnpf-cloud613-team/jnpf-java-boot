package jnpf.visualdata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.SystemEntity;
import jnpf.base.service.SystemService;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.util.context.RequestContext;
import jnpf.visualdata.entity.VisualCategoryEntity;
import jnpf.visualdata.model.VisualPageVO;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.model.visualcategory.VisualCategoryCrForm;
import jnpf.visualdata.model.visualcategory.VisualCategoryInfoVO;
import jnpf.visualdata.model.visualcategory.VisualCategoryListVO;
import jnpf.visualdata.model.visualcategory.VisualCategoryUpForm;
import jnpf.visualdata.service.VisualCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 大屏分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@RestController
@Tag(name = "大屏分类", description = "category")
@RequestMapping("/api/blade-visual/category")
@RequiredArgsConstructor
public class VisualCategoryController extends SuperController<VisualCategoryService, VisualCategoryEntity> {


    private final VisualCategoryService categoryService;

    private final SystemService systemService;

    /**
     * 列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "列表")
    @GetMapping("/page")
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<VisualPageVO<VisualCategoryListVO>> page(VisualPaginationModel pagination) {
        List<VisualCategoryEntity> data = categoryService.getList(pagination,true);
        List<VisualCategoryListVO> list = JsonUtil.getJsonToList(data, VisualCategoryListVO.class);
        VisualPageVO<VisualCategoryListVO> paginationVO = JsonUtil.getJsonToBean(pagination, VisualPageVO.class);
        paginationVO.setRecords(list);
        return ActionResult.success(paginationVO);
    }

    /**
     * 列表
     *
     * @return
     */
    @Operation(summary = "列表")
    @GetMapping("/list")
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<List<VisualCategoryListVO>> list(VisualPaginationModel pagination) {
        List<VisualCategoryEntity> data = categoryService.getList(pagination,false);
        List<VisualCategoryListVO> list = JsonUtil.getJsonToList(data, VisualCategoryListVO.class);
        return ActionResult.success(list);
    }

    /**
     * 详情
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "详情")
    @GetMapping("/detail")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<VisualCategoryInfoVO> info(@RequestParam("id") String id) {
        VisualCategoryEntity entity = categoryService.getInfo(id);
        VisualCategoryInfoVO vo = JsonUtil.getJsonToBean(entity, VisualCategoryInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新增
     *
     * @param categoryCrForm 大屏分类模型
     * @return
     */
    @Operation(summary = "新增")
    @PostMapping("/save")
    @Parameter(name = "categoryCrForm", description = "大屏分类模型",required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> create(@RequestBody @Valid VisualCategoryCrForm categoryCrForm) {
        VisualCategoryEntity entity = JsonUtil.getJsonToBean(categoryCrForm, VisualCategoryEntity.class);
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        if (categoryService.isExistByValue(entity.getCategoryvalue(), entity.getId(), sysInfo.getId())) {
            return ActionResult.fail(MsgCode.EXIST003.get());
        }
        categoryService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @param categoryUpForm 大屏分类模型
     * @return
     */
    @Operation(summary = "修改")
    @PostMapping("/update")
    @Parameter(name = "categoryUpForm", description = "大屏分类模型",required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> update(@RequestBody VisualCategoryUpForm categoryUpForm) {
        VisualCategoryEntity entity = JsonUtil.getJsonToBean(categoryUpForm, VisualCategoryEntity.class);
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        if (categoryService.isExistByValue(entity.getCategoryvalue(), entity.getId(), sysInfo.getId())) {
            return ActionResult.fail(MsgCode.EXIST003.get());
        }
        boolean flag = categoryService.update(categoryUpForm.getId(), entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param ids 主键
     * @return
     */
    @Operation(summary = "删除")
    @PostMapping("/remove")
    @Parameter(name = "ids", description = "主键", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> delete(String ids) {
        VisualCategoryEntity entity = categoryService.getInfo(ids);
        if (entity != null) {
            categoryService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

}
