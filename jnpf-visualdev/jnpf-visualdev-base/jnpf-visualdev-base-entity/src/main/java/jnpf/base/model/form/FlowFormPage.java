package jnpf.base.model.form;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/1 9:48
 */
@Schema(description="表单列表查询参数")
@Data
public class FlowFormPage  extends Pagination {
    private String keyword;
    @Schema(description = "流程类型:0-发起流程，1-功能流程")
    private Integer flowType;
    @Schema(description = "表单类型:1-系统表单，2-自定义表单")
    private Integer formType;
    @Schema(description = "该参数下拉列表无效")
    private Integer enabledMark;
    @Schema(description = "状态")
    private Integer isRelease;
}
