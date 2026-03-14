package jnpf.model.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 订单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 8:46
 */
@Data
public class OrderReceivableModel {
    @Schema(description ="")
    private String remove;
    @NotBlank(message = "必填")
    @Schema(description ="自然主键")
    private String id;
    @NotNull(message = "必填")
    @Schema(description ="收款日期")
    private Long receivableDate;
    @NotNull(message = "必填")
    @Schema(description ="收款比率")
    private int receivableRate;
    @NotBlank(message = "必填")
    @Schema(description ="收款金额")
    private String receivableMoney;
    @NotBlank(message = "必填")
    @Schema(description ="收款方式")
    private String receivableMode;
    @Schema(description ="收款摘要")
    @JsonProperty("abstract")
    private String fabstract;
    @Schema(description ="")
    private String index;
    @Schema(description ="描述")
    private String description;

}
