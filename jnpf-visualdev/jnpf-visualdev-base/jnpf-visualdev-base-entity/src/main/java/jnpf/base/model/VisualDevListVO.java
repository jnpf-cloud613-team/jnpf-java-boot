package jnpf.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * json格式化对象（在线开发对象）
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class VisualDevListVO {
    @Schema(description = "主键" )
    private String id;
    @Schema(description = "名称" )
    private String fullName;
    @Schema(description = "编码" )
    private String enCode;
    @Schema(description = "是否启用流程" )
    private Integer enableFlow;

    @Schema(description = "是否引用：0-否，1-是" )
    private Integer isQuote;
    @Schema(description = "类型：1-自定义表单，2-系统表单" )
    private Integer type;

    @Schema(description = "回显名称")
    private String showName;
}
