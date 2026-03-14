package jnpf.flowable.model.delegate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:18
 */
@Data
public class DelegateInfoVO {
    @Schema(description = "主键id")
    private String id;
    @Schema(description = "委托类型0-发起委托，1-审批委托")
    private Integer type;
    @Schema(description = "委托人id")
    private String userId;
    @Schema(description = "委托人")
    private String userName;
    @Schema(description = "流程分类")
    private String flowCategory;
    @Schema(description = "被委托人")
    private String toUserName;
    @Schema(description = "被委托人")
    private List<String> toUserId;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "开始日期")
    private Long startTime;
    @Schema(description = "结束日期")
    private Long endTime;
    @Schema(description = "委托流程id")
    private String flowId;
    @Schema(description = "委托流程名称")
    private String flowName;
}
