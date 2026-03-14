package jnpf.visualdata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.visualdata.entity.VisualAssetsEntity;
import jnpf.visualdata.model.VisualPageVO;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.model.visualassets.*;
import jnpf.visualdata.service.VisualAssetsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 静态资源
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@RestController
@Tag(name = "全局变量", description = "assets")
@RequestMapping("/api/blade-visual/assets")
public class VisualAssetsController extends SuperController<VisualAssetsService, VisualAssetsEntity> {


    /**
     * 分页
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "分页")
    @GetMapping("/list")
    public ActionResult<VisualPageVO<VisualAssetsListVO>> list(VisualPaginationModel pagination) {
        List<VisualAssetsEntity> data = getBaseService().getList(pagination);
        List<VisualAssetsListVO> list = JsonUtil.getJsonToList(data, VisualAssetsListVO.class);
        VisualPageVO<VisualAssetsListVO> paginationVO = JsonUtil.getJsonToBean(pagination, VisualPageVO.class);
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
    public ActionResult<VisualAssetsInfoVO> info(@RequestParam("id")String id) {
        VisualAssetsEntity entity = getBaseService().getInfo(id);
        VisualAssetsInfoVO vo = JsonUtil.getJsonToBean(entity, VisualAssetsInfoVO.class);
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
    public ActionResult<Object> create(@RequestBody VisualAssetsCrForm recordCrForm) {
        VisualAssetsEntity entity = JsonUtil.getJsonToBean(recordCrForm, VisualAssetsEntity.class);
        getBaseService().create(entity);
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
    public ActionResult<Object> update(@RequestBody VisualAssetsUpForm recordUpForm) {
        VisualAssetsEntity entity = JsonUtil.getJsonToBean(recordUpForm, VisualAssetsEntity.class);
        getBaseService().update(entity.getId(), entity);
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
        if (getBaseService().delete(ids)) {
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }


}
