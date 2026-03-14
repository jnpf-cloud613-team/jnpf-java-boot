package jnpf.portal.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
@Data
public class PortalSelectVO {
    private String id;
    private List<PortalSelectVO> children;
    private Boolean hasChildren;
    private String  parentId;
    private Long sortCode;

    @Schema(description = "门户名")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "字典类型")
    private String category;
    @Schema(description = "分类ID")
    private String categoryId;
    @Schema(description = "分类名")
    private String categoryName;

}
