package jnpf.flowable.model.delegate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/2 16:31
 */
@Data
public class DelegateInfoModel {
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
    private Date startTime;
    @Schema(description = "结束日期")
    private Date endTime;
    @Schema(description = "委托流程id")
    private String flowId;
    @Schema(description = "委托流程名称")
    private String flowName;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "确认状态")
    private Integer confirmStatus;
    @Schema(description = "委托id")
    private String delegateId;
}
