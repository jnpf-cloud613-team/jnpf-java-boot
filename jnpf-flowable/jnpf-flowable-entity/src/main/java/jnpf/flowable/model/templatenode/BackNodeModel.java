package jnpf.flowable.model.templatenode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/8 11:33
 */
@Data
public class BackNodeModel {
    /**
     * 节点编码
     */
    @Schema(description = "节点编码")
    private String nodeCode;
    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String nodeName;
}
