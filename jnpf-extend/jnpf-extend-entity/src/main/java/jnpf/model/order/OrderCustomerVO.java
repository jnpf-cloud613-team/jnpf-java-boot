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
public class OrderCustomerVO {
    @Schema(description = "主键id")
    private String id;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "内容")
    private String text;
}
