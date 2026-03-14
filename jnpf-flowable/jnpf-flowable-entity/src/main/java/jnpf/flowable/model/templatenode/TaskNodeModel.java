package jnpf.flowable.model.templatenode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/26 10:05
 */
@Data
public class TaskNodeModel implements Serializable {

    /**
     * 节点类型
     */
    @Schema(description = "节点类型")
    private String nodeType;
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

    /**
     * 审核用户
     */
    @Schema(description = "审核用户")
    private String userName;
    /**
     * 节点类型(-1没有经过,0.经过 1.当前 2.未经过  3.异常)
     */
    @Schema(description = "节点类型(-1没有经过,0.经过 1.当前 2.未经过 3.异常)")
    private String type;
}
