package jnpf.visualdata.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.util.DesUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.visualdata.entity.VisualDbEntity;
import jnpf.visualdata.model.VisualPageVO;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.model.visualdb.*;
import jnpf.visualdata.service.VisualDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 大屏数据源配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@RestController
@Tag(name = "大屏数据源配置", description = "db")
@RequestMapping("/api/blade-visual/db")
@RequiredArgsConstructor
public class VisualDbController extends SuperController<VisualDbService, VisualDbEntity> {


    private final VisualDbService dbService;

    /**
     * 分页
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "分页")
    @GetMapping("/list")
    public ActionResult<VisualPageVO<VisualDbListVO>> list(VisualPaginationModel pagination) {
        List<VisualDbEntity> data = dbService.getList(pagination);
        List<VisualDbListVO> list = JsonUtil.getJsonToList(data, VisualDbListVO.class);
        VisualPageVO<VisualDbListVO> paginationVO = JsonUtil.getJsonToBean(pagination, VisualPageVO.class);
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
    public ActionResult<VisualDbInfoVO> info(@RequestParam("id")String id) {
        VisualDbEntity entity = dbService.getInfo(id);
        VisualDbInfoVO vo = JsonUtil.getJsonToBean(entity, VisualDbInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新增或修改
     *
     * @param dbUpForm 数据模型
     * @return
     */
    @Operation(summary = "新增或修改")
    @PostMapping("/submit")
    @Parameter(name = "dbUpForm", description = "数据模型",required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> submit(@RequestBody VisualDbUpForm dbUpForm) {
        VisualDbEntity entity = JsonUtil.getJsonToBean(dbUpForm, VisualDbEntity.class);
        if (StringUtil.isEmpty(entity.getId())) {
            dbService.create(entity);
            return ActionResult.success(MsgCode.SU001.get());
        } else {
            dbService.update(entity.getId(), entity);
            return ActionResult.success(MsgCode.SU004.get());
        }
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
        VisualDbEntity entity = dbService.getInfo(ids);
        if (entity != null) {
            dbService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 下拉数据源
     *
     * @return
     */
    @Operation(summary = "下拉数据源")
    @GetMapping("/db-list")
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<List<VisualDbSelectVO>> list() {
        List<VisualDbEntity> data = dbService.getList();
        List<VisualDbSelectVO> list = JsonUtil.getJsonToList(data, VisualDbSelectVO.class);
        return ActionResult.success(list);
    }

    /**
     * 数据源测试连接
     *
     * @param dbCrForm 数据源模型
     * @return
     */
    @Operation(summary = "数据源测试连接")
    @PostMapping("/db-test")
    @Parameter(name = "dbCrForm", description = "数据源模型",required = true)
    @SaCheckPermission("onlineDev.dataScreen")
    public ActionResult<Object> test(@RequestBody VisualDbCrForm dbCrForm) {
        VisualDbEntity entity = JsonUtil.getJsonToBean(dbCrForm, VisualDbEntity.class);
        entity.setPassword(DesUtil.aesOrDecode(entity.getPassword(), false, true));
        boolean flag = dbService.dbTest(entity);
        if (flag) {
            return ActionResult.success(MsgCode.DB301.get());
        }
        return ActionResult.fail(MsgCode.DB302.get());
    }

    /**
     * 动态执行SQL
     *
     * @param queryForm 数据模型
     * @return
     */
    @Operation(summary = "动态执行SQL")
    @PostMapping("/dynamic-query")
    @Parameter(name = "queryForm", description = "数据模型",required = true)
    public ActionResult<Object> query(@RequestBody VisualDbQueryForm queryForm) {
        VisualDbEntity entity = dbService.getInfo(queryForm.getId());
        List<Map<String, Object>> data = new ArrayList<>();
        if (entity != null) {
            entity.setPassword(DesUtil.aesOrDecode(entity.getPassword(), false, true));
            queryForm.setSql(DesUtil.aesOrDecode(queryForm.getSql(), false, true));
            data = dbService.query(entity, queryForm.getSql());
        }
        return ActionResult.success(data);
    }

}
