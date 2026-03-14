package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.io.Serializable;

/**
 * 会签流转配置
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/16 10:09
 */
@Data
public class CounterSignConfig implements Serializable {
    /**
     * 通过类型 0.无 1.百分比 2.人数
     */
    @Schema(description = "通过类型")
    private Integer auditType = FlowNature.PERCENT;
    /**
     * 通过百分比
     */
    @Schema(description = "通过百分比")
    private Integer auditRatio = 100;
    /**
     * 通过人数
     */
    @Schema(description = "通过人数")
    private Integer auditNum = 1;
    /**
     * 拒绝类型 0.无 1.百分比 2.人数
     */
    @Schema(description = "拒绝类型")
    private Integer rejectType = FlowNature.PERCENT;
    /**
     * 拒绝百分比
     */
    @Schema(description = "拒绝百分比")
    private Integer rejectRatio = 10;
    /**
     * 拒绝人数
     */
    @Schema(description = "拒绝人数")
    private Integer rejectNum = 1;
    /**
     * 计算方式，1.实时计算  2.延后计算
     */
    @Schema(description = "计算方式")
    private Integer calculateType = FlowNature.DELAY;
}
