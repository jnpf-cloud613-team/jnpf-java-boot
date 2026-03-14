package jnpf.model.order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 订单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 8:46
 */
@Data
public class OrderInfoVO  {
    @Schema(description ="有效标志", example = "1")
    private Integer enabledMark;
    @Schema(description ="制单人员")
    private String creatorUserId;
    @Schema(description ="附件信息")
    private String fileJson;
    @Schema(description ="付款方式")
    private String paymentMode;
    @Schema(description ="制单时间")
    private Long creatorTime;
    @Schema(description ="业务员Id")
    private String salesmanId;
    @Schema(description ="预付定金")
    private String prepayEarnest;
    @Schema(description ="运输方式")
    private String transportMode;
    @Schema(description ="客户名称")
    private String customerName;
    @Schema(description ="发货日期")
    private Long deliveryDate;
    @Schema(description ="订单主键")
    private String id;
    @Schema(description ="业务员")
    private String salesmanName;
    @Schema(description ="客户Id")
    private String customerId;
    @Schema(description ="修改时间")
    private Long lastModifyTime;
    @Schema(description ="应收金额")
    private String receivableMoney;
    @Schema(description ="发货地址")
    private String deliveryAddress;
    @Schema(description ="定金比率")
    private String earnestRate;
    @Schema(description ="描述")
    private String description;
    @Schema(description ="修改用户")
    private String lastModifyUserId;
    @Schema(description ="订单日期")
    private Long orderDate;
    @Schema(description ="订单编号")
    private String orderCode;
    @Schema(description ="订单明细")
    List<OrderInfoOrderEntryModel> goodsList;
    @Schema(description ="收款计划")
    List<OrderInfoOrderReceivableModel> collectionPlanList;
}
