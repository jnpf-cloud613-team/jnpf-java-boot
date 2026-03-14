package jnpf.flowable.model.flowable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * flowable节点
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/19 17:52
 */
@Data
public class FlowableNodeModel implements Serializable {
    /**
     * 元素ID
     */
    @Schema(name = "id", description = "元素ID")
    private String id;
    /**
     * 元素名称
     */
    @Schema(name = "name", description = "元素名称")
    private String name;
    /**
     * 进线ID
     */
    @Schema(name = "incoming", description = "进线ID")
    private List<String> incomingList;
    /**
     * 出线ID
     */
    @Schema(name = "outgoingList", description = "出线ID")
    private List<String> outgoingList;
}
