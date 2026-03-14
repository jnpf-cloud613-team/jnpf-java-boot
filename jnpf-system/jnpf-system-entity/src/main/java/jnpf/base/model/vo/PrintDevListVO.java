package jnpf.base.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页列表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-11-20
 */
@Data
@Schema(description = "打印列表")
public class PrintDevListVO {
    @Schema(description = "主键id")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "创建人")
    private String creatorUser;

    @Schema(description = "创建时间")
    private Long creatorTime;

    @Schema(description = "修改人")
    private String lastModifyUser;

    @Schema(description = "修改时")
    private Long lastModifyTime;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "状态：0-未发布，1-已发布，2-已修改")
    private Integer state;

    @Schema(description = "通用-将该模板设为通用(0-表单用，1-业务打印模板用)")
    private Integer commonUse;

    @Schema(description = "发布范围：1-公开，2-权限设置")
    private Integer visibleType;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "图标颜色")
    private String iconBackground;
}
