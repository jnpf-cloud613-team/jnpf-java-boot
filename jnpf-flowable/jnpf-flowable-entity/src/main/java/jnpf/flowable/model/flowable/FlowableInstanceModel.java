package jnpf.flowable.model.flowable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/25 8:57
 */
@Data
public class FlowableInstanceModel {
    /**
     * 实例ID
     */
    @Schema(name = "instanceId", description = "实例ID")
    private String instanceId;
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(name = "startTime", description = "开始时间")
    private Date startTime;
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(name = "endTime", description = "结束时间")
    private Date endTime;
    /**
     * 耗时
     */
    @Schema(name = "durationInMillis", description = "耗时")
    private Long durationInMillis;
    /**
     * 删除原因
     */
    @Schema(name = "deleteReason", description = "删除原因")
    private String deleteReason;
}
