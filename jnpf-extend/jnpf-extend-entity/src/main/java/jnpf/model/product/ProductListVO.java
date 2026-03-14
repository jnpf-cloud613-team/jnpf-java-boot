

package jnpf.model.product;
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
public class ProductListVO{
    @Schema(description ="主键")
    private String id;
    @Schema(description ="订单编号")
    private String code;
    @Schema(description ="客户名称")
    private String customerName;
    @Schema(description ="业务员")
    private String business;
    @Schema(description ="送货地址")
    private String address;
    @Schema(description ="联系方式")
    private String contactTel;
    @Schema(description ="制单人")
    private String salesmanName;
    @Schema(description ="审核状态")
    private Integer auditState;
    @Schema(description ="发货状态")
    private Integer goodsState;
    @Schema(description ="关闭状态")
    private Integer closeState;
    @Schema(description ="关闭日期")
    private Long  closeDate;
    @Schema(description ="联系人")
    private String contactName;
}
