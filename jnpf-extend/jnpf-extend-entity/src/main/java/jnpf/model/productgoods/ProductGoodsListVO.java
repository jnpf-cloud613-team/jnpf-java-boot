package jnpf.model.productgoods;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * 产品商品
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 15:57:50
 */
@Data
public class ProductGoodsListVO{
    @Schema(description ="主键")
    private String id;
    @Schema(description ="分类主键")
    private String classifyId;
    @Schema(description ="产品编号")
    private String code;
    @Schema(description ="产品名称")
    private String fullName;
    @Schema(description ="产品规格")
    private String productSpecification;
    @Schema(description ="单价")
    private String money;
    @Schema(description ="金额")
    private String amount;
    @Schema(description ="库存数")
    private String qty;
    @Schema(description ="订货分类")
    private String type;
}
