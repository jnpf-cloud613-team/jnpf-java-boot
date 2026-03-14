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
public class ProductEntryMdoel {
    @Schema(description ="产品规格")
    private String productSpecification;
    @Schema(description ="数量")
    private String qty;
    @Schema(description ="单价")
    private String money;
    @Schema(description ="折后单价")
    private String price;
    @Schema(description ="单位")
    private String util;
    @Schema(description ="控制方式")
    private String commandType;
}
