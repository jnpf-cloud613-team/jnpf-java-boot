package jnpf.model.productentry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * Product模型
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 10:40:59
 */
@Data
public class ProductEntryInfoVO {
    @Schema(description ="产品编号")
    private String productCode;
    @Schema(description ="产品名称")
    private String productName;
    @Schema(description ="产品规格")
    private String productSpecification;
    @Schema(description ="数量")
    private Long qty;
    @Schema(description ="订货类型")
    private String type;
    @Schema(description ="单价")
    private String money;
    @Schema(description ="折后单价")
    private String price;
    @Schema(description ="金额")
    private String amount;
    @Schema(description ="备注")
    private String description;
}
