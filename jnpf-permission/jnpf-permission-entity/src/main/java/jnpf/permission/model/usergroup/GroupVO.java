package jnpf.permission.model.usergroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户分组管理列表返回
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/11 8:58
 */
@Data
public class GroupVO implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "创建时间")
    private Long creatorTime;
    @Schema(description = "创建时间")
    private Integer enabledMark;
}
