package jnpf.permission.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.FlowWorkModel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作交接展示
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/4/3 11:48:33
 */
@Data
@Schema(description = "工作交接")
@AllArgsConstructor
public class WorkHandoverVo {
    @Schema(description = "流程列表")
    private List<FlowWorkModel> flow = new ArrayList<>();
    @Schema(description = "流程实例")
    private List<FlowWorkModel> flowTask = new ArrayList<>();
    @Schema(description = "流程列表")
    private List<FlowWorkModel> app = new ArrayList<>();

    @Schema(description = "是否有应用交接权限")
    private Boolean isAppShow = false;
}
