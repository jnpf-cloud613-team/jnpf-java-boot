package jnpf.visualdata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.visualdata.entity.VisualComponentEntity;
import jnpf.visualdata.model.VisualPageVO;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.model.visualcomponent.*;
import jnpf.visualdata.service.VisualComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 大屏组件库
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@RestController
@Tag(name = "大屏组件库配置", description = "component")
@RequestMapping("/api/blade-visual/component")
@RequiredArgsConstructor
public class VisualComponentController extends SuperController<VisualComponentService, VisualComponentEntity> {


    private final VisualComponentService componentService;

    /**
     * 分页
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "分页")
    @GetMapping("/list")
    public ActionResult<VisualPageVO<VisualComponentListVO>> list(VisualPaginationModel pagination) {
        List<VisualComponentEntity> data = componentService.getList(pagination);
        List<VisualComponentListVO> list = JsonUtil.getJsonToList(data, VisualComponentListVO.class);
        VisualPageVO<VisualComponentListVO> paginationVO = JsonUtil.getJsonToBean(pagination, VisualPageVO.class);
        paginationVO.setRecords(list);
        return ActionResult.success(paginationVO);
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
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<VisualComponentInfoVO> info(@RequestParam("id")String id) {
        VisualComponentEntity entity = componentService.getInfo(id);
        VisualComponentInfoVO vo = JsonUtil.getJsonToBean(entity, VisualComponentInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新增
     *
     * @param recordCrForm 数据模型
     * @return
     */
    @Operation(summary = "新增")
    @PostMapping("/save")
    @Parameter(name = "recordCrForm", description = "数据模型",required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> create(@RequestBody VisualComponentCrForm recordCrForm) {
        VisualComponentEntity entity = JsonUtil.getJsonToBean(recordCrForm, VisualComponentEntity.class);
        componentService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @param recordUpForm 数据模型
     * @return
     */
    @Operation(summary = "修改")
    @PostMapping("/update")
    @Parameter(name = "recordUpForm", description = "数据模型",required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> update(@RequestBody VisualComponentUpForm recordUpForm) {
        VisualComponentEntity entity = JsonUtil.getJsonToBean(recordUpForm, VisualComponentEntity.class);
        componentService.update(entity.getId(), entity);
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
        VisualComponentEntity entity = componentService.getInfo(ids);
        if (entity != null) {
            componentService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }


}
