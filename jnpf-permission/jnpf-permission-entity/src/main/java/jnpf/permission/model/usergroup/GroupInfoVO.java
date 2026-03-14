package jnpf.permission.model.usergroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/11 9:21
 */
@Data
public class GroupInfoVO implements Serializable {
    /**
     * 主键
     **/
    @Schema(description = "主键")
    private String id;

    /**
     * 名称
     **/
    @Schema(description = "名称")
    private String fullName;

    /**
     * 编码
     **/
    @Schema(description = "编码")
    private String enCode;

    /**
     * 说明
     **/
    @Schema(description = "说明")
    private String description;

    /**
     * 类型
     **/
    @Schema(description = "类型")
    private String type;

    /**
     * 排序
     **/
    @Schema(description = "排序")
    private String sortCode;

    @Schema(description = "状态")
    private Integer enabledMark;
}
