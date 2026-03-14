package jnpf.flowable.model.delegate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:18
 */
@Data
public class DelegateListVO {
    @Schema(description = "主键id")
    private String id;
    @Schema(description = "委托类型0-发起委托，1-审批委托")
    private String type;
    @Schema(description = "委托人id")
    private String userId;
    @Schema(description = "委托人")
    private String userName;
    @Schema(description = "被委托人id")
    private String toUserId;
    @Schema(description = "被委托人")
    private String toUserName;
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
    @Schema(description = "有效标志")
    private Integer enabledMark;
    /**
     * 生效状态，0.未生效 1.生效中 2.已失效
     */
    private Integer status;
    /**
     * 确认状态，0.待确认 1.已接受 2.已拒绝
     */
    private Integer confirmStatus;
    /**
     * 是否可编辑
     */
    private Boolean isEdit = true;
}
