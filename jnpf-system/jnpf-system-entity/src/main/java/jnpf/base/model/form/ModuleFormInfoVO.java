package jnpf.base.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-09-14
 */
@Data
public class ModuleFormInfoVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "状态")
    private Integer enabledMark;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "备注")
    private String description;

    @Schema(description = "菜单id")
    private String moduleId;

    @Schema(description = "排序码")
    private Long sortCode;

    @Schema(description = "规则")
    private Integer fieldRule;
    @Schema(description = "绑定表")
    private String bindTable;
    private String childTableKey = JnpfKeyConsts.CHILD_TABLE_PREFIX;
}
