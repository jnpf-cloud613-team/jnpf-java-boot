package jnpf.flowable.model.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import jnpf.permission.model.authorize.AuthorizeVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "分页模型")
public class TemplatePagination extends Pagination {
    @Schema(description = "分类")
    private String category;
    @Schema(description = "标志")
    private Integer enabledMark;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "权限过滤")
    private Integer isAuthority = 1;
    @Schema(description = "是否委托代理列表，1-委托代理列表")
    private Integer isDelegate = 0;
    @Schema(description = "委托人")
    private String delegateUser;
    @Schema(description = "是否发起列表（0-否 1-是）")
    private Integer isLaunch = 0;
    @Schema(description = "应用主建")
    private String systemId;
    @JsonIgnore
    private List<String> templateIdList = new ArrayList<>();

    @Schema(description = "流程有权限列表")
    private AuthorizeVO authorize;
    @Schema(description = "表单类型")
    private Integer isSystemFrom = 0;
    @Schema(description = "自由流程")
    private Integer isFree = 0;
}
