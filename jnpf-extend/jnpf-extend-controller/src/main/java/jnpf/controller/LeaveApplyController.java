package jnpf.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.entity.LeaveApplyEntity;
import jnpf.model.leaveapply.LeaveApplyForm;
import jnpf.model.leaveapply.LeaveApplyInfoVO;
import jnpf.service.LeaveApplyService;
import jnpf.util.GeneraterSwapUtil;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 请假申请
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "请假申请", description = "LeaveApply")
@RestController
@RequestMapping("/api/extend/Form/LeaveApply")
@RequiredArgsConstructor
public class LeaveApplyController extends SuperController<LeaveApplyService, LeaveApplyEntity> {


    private final LeaveApplyService leaveApplyService;

    private final GeneraterSwapUtil generaterSwapUtil;



    /**
     * 获取请假申请信息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取请假申请信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<LeaveApplyInfoVO> info(@PathVariable("id") String id) {
        LeaveApplyEntity entity = leaveApplyService.getInfo(id);
        LeaveApplyInfoVO vo = JsonUtil.getJsonToBean(entity, LeaveApplyInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建请假申请
     *
     * @param leaveApplyForm 表单对象
     * @return
     */
    @Operation(summary = "新建请假申请")
    @PostMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "leaveApplyForm", description = "请假模型", required = true)
    public ActionResult<Object> create(@RequestBody LeaveApplyForm leaveApplyForm, @PathVariable("id") String id) {
        LeaveApplyEntity entity = JsonUtil.getJsonToBean(leaveApplyForm, LeaveApplyEntity.class);
        leaveApplyService.submit(id, entity, leaveApplyForm);
        return ActionResult.success(MsgCode.SU006.get());
    }

    /**
     * 修改请假申请
     *
     * @param leaveApplyForm 表单对象
     * @param id             主键
     * @return
     */
    @Operation(summary = "修改请假申请")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "leaveApplyForm", description = "请假模型", required = true)
    public ActionResult<Object> update(@RequestBody LeaveApplyForm leaveApplyForm, @PathVariable("id") String id) {
        LeaveApplyEntity entity = JsonUtil.getJsonToBean(leaveApplyForm, LeaveApplyEntity.class);
        entity.setId(id);
        leaveApplyService.submit(id, entity, leaveApplyForm);
        return ActionResult.success(MsgCode.SU006.get());
    }

    /**
     * 删除请假申请信息
     *
     * @param id 主键
     */
    @Operation(summary = "删除请假申请信息")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object> delete(@PathVariable("id") String id, @RequestParam(name = "forceDel", defaultValue = "false") Boolean forceDel) {
        LeaveApplyEntity entity = leaveApplyService.getInfo(id);
        if (null != entity) {
            if (Boolean.FALSE.equals(forceDel)) {
                String errMsg = generaterSwapUtil.deleteFlowTask(entity.getId());
                if (StringUtils.isNotBlank(errMsg)) {
                    throw new IllegalArgumentException(errMsg);
                }
            }
            leaveApplyService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }
}
