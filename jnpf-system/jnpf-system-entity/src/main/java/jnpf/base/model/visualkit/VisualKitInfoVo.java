package jnpf.base.model.visualkit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 套件详细信息
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:50:38
 */
@Data
@Schema(description = "套件详细信息")
@AllArgsConstructor
@NoArgsConstructor
public class VisualKitInfoVo implements Serializable {

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

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "状态")
    private Integer enabledMark;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "套件设计内容")
    private String formData;

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

    public VisualKitInfoVo(String id, String enCode) {
        this.id = id;
        this.enCode = enCode;
    }
}
