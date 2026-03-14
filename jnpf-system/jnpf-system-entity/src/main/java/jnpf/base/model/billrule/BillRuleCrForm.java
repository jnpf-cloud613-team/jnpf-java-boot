package jnpf.base.model.billrule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:28
 */
@Data
public class BillRuleCrForm {
    @NotBlank(message = "必填")
    @Schema(description = "业务名称")
    private String fullName;
    @NotBlank(message = "必填")
    @Schema(description = "业务编码")
    private String enCode;
//    @NotBlank(message = "必填")
    @Schema(description = "流水前缀")
    private String prefix;
//    @NotBlank(message = "必填")
    @Schema(description = "流水日期格式")
    private String dateFormat;
//    @NotNull(message = "必填")
    @Schema(description = "流水位数")
    private Integer digit;
//    @NotBlank(message = "必填")
    @Schema(description = "流水起始")
    private String startNumber;
    @Schema(description = "流水范例")
    private String example;
    @Schema(description = "状态(0-禁用，1-启用)")
    private Integer enabledMark;
    @Schema(description = "流水说明")
    private String description;
    private long sortCode;
    private String category;
    @Schema(description = "方式 1-时间格式，2-随机数编号，3-UUID")
    private Integer type;
    @Schema(description = "随机数位数")
    private Integer randomDigit;
    @Schema(description = "随机数类型")
    private Integer randomType;
    @Schema(description = "单据后缀")
    private String suffix;
}
