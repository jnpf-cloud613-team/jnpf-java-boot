package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class TemplatePageLisVO {
    @Schema(description = "流程编码")
    private String enCode;
    @Schema(description = "流程名称")
    private String fullName;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "流程分类")
    private String category;
    @Schema(description = "流程类型")
    private Integer type;
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "图标背景色")
    private String iconBackground;
    @Schema(description = "创建人")
    private String creatorUser;
    @Schema(description = "创建时间")
    private Date creatorTime;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "有效标志")
    private Integer visibleType;
    @Schema(description = "版本主键")
    private String flowId;
    @Schema(description = "流程显示类型（0-全局 1-流程 2-菜单）")
    private Integer showType;
    @Schema(description = "状态(0.未上架,1.上架,2.下架-继续审批，3.下架-隐藏审批)")
    private Integer status;
}
