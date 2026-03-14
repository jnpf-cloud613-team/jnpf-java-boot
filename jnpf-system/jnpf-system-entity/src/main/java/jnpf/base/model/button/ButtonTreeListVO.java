package jnpf.base.model.button;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ButtonTreeListVO {
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父级id")
    private String parentId;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "是否有子集")
    private Boolean hasChildren;
    @Schema(description = "子集集合")
    private List<ButtonTreeListVO> children;
}
