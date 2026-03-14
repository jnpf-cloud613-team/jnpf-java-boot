package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateCrForm {

    @Schema(description = "流程编码")
    private String enCode;

    @Schema(description = "流程名称")
    private String fullName;

    /**
     * 0.标准  1.简流
     */
    @Schema(description = "流程类型")
    private Integer type = 0;

    @Schema(description = "流程分类")
    private String category;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "图标背景色")
    private String iconBackground;


    @Schema(description = "说明")
    private String description;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "流程设置")
    private String flowConfig;

    /**
     * 流程显示类型（0-全局 1-流程 2-菜单）
     */
    private Integer showType = 0;
    /**
     * 状态(0.未上架,1.上架,2.下架-继续审批，3.下架-隐藏审批)
     */
    private Integer status = 0;

}
