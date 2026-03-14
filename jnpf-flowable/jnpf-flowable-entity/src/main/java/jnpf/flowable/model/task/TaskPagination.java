package jnpf.flowable.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.PaginationTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/22 9:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskPagination extends PaginationTime {
    @Schema(description = "所属名称")
    private String flowId;
    @Schema(description = "分类")
    private String flowCategory;
    @Schema(description = "紧急程度")
    private Integer flowUrgent;
    @Schema(description = "所属流程")
    private String templateId;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "用户主键")
    private String creatorUserId;
    @Schema(description = "编码")
    private String nodeCode;
    @Schema(description = "应用主键")
    private String systemId;
    /**
     * 待签、待办、在办、已办、批量在办
     */
    private String category;

    @JsonIgnore
    private Boolean delegateType = false;
    @JsonIgnore
    private String userId;
}
