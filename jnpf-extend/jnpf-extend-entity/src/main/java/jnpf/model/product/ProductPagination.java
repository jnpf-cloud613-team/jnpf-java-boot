package jnpf.model.product;
import jnpf.base.Pagination;
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
public class ProductPagination extends Pagination {
    @Schema(description ="订单编号")
    private String code;
    @Schema(description ="客户名称")
    private String customerName;
    @Schema(description ="联系方式")
    private String contactTel;



}
