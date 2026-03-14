package jnpf.model.customer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * 客户信息
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 14:09:05
 */
@Data
public class CustomerInfoVO{
    @Schema(description ="主键")
    private String id;
    @Schema(description ="编码")
    private String code;
    @Schema(description ="客户名称")
    private String customerName;
    @Schema(description ="地址")
    private String address;
    @Schema(description ="名称")
    private String name;
    @Schema(description ="联系方式")
    private String contactTel;

}
