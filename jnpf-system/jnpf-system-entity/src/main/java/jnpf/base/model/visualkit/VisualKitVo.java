package jnpf.base.model.visualkit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 表单套件列表信息
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:20:49
 */
@Data
@Schema(description = "套件列表信息")
public class VisualKitVo {

    @Schema(description = "id")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "分类（数据字典）")
    private String category;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "创建时间")
    private Long creatorTime;

    @Schema(description = "创建人")
    private String creatorUser;

    @Schema(description = "创建人id")
    private String creatorUserId;

    @Schema(description = "修改时间")
    private Long lastModifyTime;

    @Schema(description = "修改人")
    private String lastModifyUser;

    @Schema(description = "修改人id")
    private String lastModifyUserId;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer enabledMark;
}
