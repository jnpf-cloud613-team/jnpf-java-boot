package jnpf.base.model.base;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/21 15:50
 */
@Data
public class SystemListVO implements Serializable {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序码")
    private Long sortCode;

    @Schema(description = "图标背景色")
    private String backgroundColor;

    @Schema(description = "所属用户id")
    private String userId;

    @Schema(description = "有修改")
    private Integer hasUpdate = 0;

    @Schema(description = "有删除")
    private Integer hasDelete = 0;

    @Schema(description = "是否置顶")
    private Boolean isTop = false;
}
