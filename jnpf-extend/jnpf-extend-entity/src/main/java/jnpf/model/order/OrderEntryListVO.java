package jnpf.model.order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 8:46
 */
@Data
public class OrderEntryListVO {
    @Schema(description ="自然主键")
    private String id;
    @Schema(description ="商品名称")
    private String goodsName;
    @Schema(description ="规格型号")
    private String specifications;
    @Schema(description ="单位")
    private String unit;
    @Schema(description ="数量")
    private String qty;
    @Schema(description ="单价")
    private String price;
    @Schema(description ="金额")
    private String amount;
    @Schema(description =" 折扣%")
    private String discount;
    @Schema(description =" 税率%")
    private String cess;
    @Schema(description ="实际单价")
    private String actualPrice;
    @Schema(description ="实际金额")
    private String actualAmount;
    @Schema(description ="描述")
    private String description;
}
