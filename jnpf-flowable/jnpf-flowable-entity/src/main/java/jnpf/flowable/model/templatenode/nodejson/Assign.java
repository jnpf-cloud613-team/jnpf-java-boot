package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/16 9:59
 */
@Data
public class Assign implements Serializable {
    /**
     * 节点编码
     */
    @Schema(description = "节点编码")
    private String nodeId;
    /**
     * 传递规则
     */
    @Schema(description = "传递规则")
    private List<AssignRule> ruleList = new ArrayList<>();
}
