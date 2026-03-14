package jnpf.flowable.model.candidates;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 21:17
 */
@Data
public class CandidateCheckFo extends Pagination {
    /**
     * 版本主键
     */
    @Schema(description = "版本主键")
    private String flowId;
    /**
     * 表单数据
     */
    @Schema(description = "表单数据")
    private Map<String, Object> formData = new HashMap<>();
    /**
     * 审批类型 1.同意 0.拒绝
     */
    @Schema(description = "审批类型")
    private Integer handleStatus = FlowNature.AUDIT_COMPLETION;
    /**
     * 任务主键
     */
    private String id;
    /**
     * 节点编码
     */
    @Schema(description = "节点编码")
    private String nodeCode;
    /**
     * 委托人
     */
    @Schema(description = "委托人")
    private String delegateUser;
}
