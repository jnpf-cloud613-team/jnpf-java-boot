package jnpf.visualdata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.visualdata.entity.VisualMapEntity;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.model.visualmap.*;
import jnpf.visualdata.service.VisualMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 大屏地图
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@RestController
@Tag(name = "大屏地图", description = "map")
@RequestMapping("/api/blade-visual/map")
@RequiredArgsConstructor
public class VisualMapController extends SuperController<VisualMapService, VisualMapEntity> {


    private final VisualMapService mapService;

    /**
     * 分页
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "分页")
    @GetMapping("/lazy-list")
    public ActionResult<List<VisualMapListVO>> list(VisualPaginationModel pagination) {
        List<VisualMapEntity> data = mapService.getListWithColnums(pagination, VisualMapEntity::getId, VisualMapEntity::getName
                , VisualMapEntity::getCode, VisualMapEntity::getParentCode, VisualMapEntity::getParentId
                , VisualMapEntity::getAncestors, VisualMapEntity::getMapLevel);
        List<VisualMapListVO> list = JsonUtil.getJsonToList(data, VisualMapListVO.class);
        VisualMapEntity parent = getBaseService().getInfo(pagination.getParentId());
        list.forEach(m -> {
            m.setHasChildren(getBaseService().hasChild(m.getId()));
            if(parent != null){
                m.setParentName(parent.getName());
            }
        });
        return ActionResult.success(MsgCode.SU005.get(), list);
    }

    /**
     * 详情
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "详情")
    @GetMapping("/detail")
    public ActionResult<VisualMapInfoVO> info(@RequestParam("id") String id) {
        VisualMapEntity entity = mapService.getInfo(id);
        VisualMapInfoVO vo = JsonUtil.getJsonToBean(entity, VisualMapInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新增
     *
     * @param mapCrForm 地图模型
     * @return
     */
    @Operation(summary = "新增")
    @PostMapping("/save")
    @Parameter(name = "mapCrForm", description = "地图模型", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> create(@RequestBody VisualMapCrForm mapCrForm) {
        VisualMapEntity entity = JsonUtil.getJsonToBean(mapCrForm, VisualMapEntity.class);
        mapService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @param mapUpForm 地图模型
     * @return
     */
    @Operation(summary = "修改")
    @PostMapping("/update")
    @Parameter(name = "mapUpForm", description = "地图模型", required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object>update(@RequestBody VisualMapUpForm mapUpForm) {
        VisualMapEntity entity = JsonUtil.getJsonToBean(mapUpForm, VisualMapEntity.class);
        boolean flag = mapService.update(mapUpForm.getId(), entity);
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
    public ActionResult<Object> delete(@RequestParam("ids") String ids) {
        VisualMapEntity entity = mapService.getInfo(ids);
        if (entity != null) {
            mapService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 数据详情
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "数据详情")
    @GetMapping("/data")
    @Parameter(name = "id", description = "主键", required = true)
    public String dataInfo(@RequestParam("id") String id) {
        VisualMapEntity entity = mapService.getInfo(id);
        Assert.notNull(entity, MsgCode.FA001::get);
        return entity.getData();
    }

}
