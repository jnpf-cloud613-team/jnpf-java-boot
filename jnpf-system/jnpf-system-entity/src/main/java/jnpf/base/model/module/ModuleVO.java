package jnpf.base.model.module;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class ModuleVO {
    @Schema(description = "功能主键")
    private String id;
    @Schema(description = "功能上级")
    private String parentId;
    @Schema(description = "功能类别【1-类别、2-页面】")
    private Integer type;
    @Schema(description = "功能名称")
    private String fullName;
    @Schema(description = "功能编码")
    private String enCode;
    @Schema(description = "功能地址")
    private String urlAddress;
}
