package jnpf.flowable.model.delegate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:18
 */
@Data
public class DelegateCrForm {
    @Schema(description = "委托人名称")
    private String userName;
    @Schema(description = "委托人id")
    private String userId;
    /**
     * 委托人id集合
     */
    private List<String> userIdList = new ArrayList<>();
    @Schema(description = "被委托人")
    @NotBlank(message = "必填")
    private String toUserName;
    @Schema(description = "被委托人id")
    private List<String> toUserId = new ArrayList<>();
    @Schema(description = "委托类型（0-发起委托，1-审批委托）")
    @NotBlank(message = "必填")
    private String type;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "开始日期")
    @NotNull(message = "必填")
    private Long startTime;
    @Schema(description = "结束日期")
    @NotNull(message = "必填")
    private Long endTime;
    @Schema(description = "委托流程id")
    private String flowId;
    @Schema(description = "委托流程名称")
    @NotBlank(message = "必填")
    private String flowName;
}
