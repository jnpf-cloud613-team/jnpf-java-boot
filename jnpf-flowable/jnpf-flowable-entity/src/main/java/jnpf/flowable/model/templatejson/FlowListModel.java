package jnpf.flowable.model.templatejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/7/5 15:40
 */
@Data
public class FlowListModel {
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "编码")
    private String enCode;
}
