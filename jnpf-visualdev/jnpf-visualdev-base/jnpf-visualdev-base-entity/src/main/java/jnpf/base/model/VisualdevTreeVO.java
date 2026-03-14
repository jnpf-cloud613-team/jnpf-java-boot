package jnpf.base.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
@Data
@Schema(description="功能树形VO" )
@NoArgsConstructor
public class VisualdevTreeVO {
    @Schema(description = "主键" )
    private String id;
    @Schema(description = "名称" )
    private String fullName;
    @Schema(description = "页面地址" )
    private String webAddress;
    @Schema(description = "页面地址" )
    private String appAddress;
    @Schema(description = "是否有子集" )
    private Boolean hasChildren;
    @Schema(description = "排序" )
    private Long sortCode;
    @Schema(description = "表单类型" )
    private Integer webType;
    @Schema(description = "子集对象" )
    private List<VisualdevTreeVO> children;

    public VisualdevTreeVO(String id, String fullName) {
        this.id = id;
        this.fullName = fullName;
        this.hasChildren = true;
    }
}
