package jnpf.flowable.model.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/16 10:07
 */
@Data
public class FlowBatchModel {
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "数量")
    private Long num;
}
