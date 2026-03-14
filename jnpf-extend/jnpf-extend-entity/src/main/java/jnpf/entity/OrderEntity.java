package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("ext_order")
public class OrderEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 客户Id
     */
    @TableField("F_CUSTOMER_ID")
    private String customerId;

    /**
     * 客户名称
     */
    @TableField("F_CUSTOMER_NAME")
    private String customerName;

    /**
     * 业务员Id
     */
    @TableField("F_SALESMAN_ID")
    private String salesmanId;

    /**
     * 业务员
     */
    @TableField("F_SALESMAN_NAME")
    private String salesmanName;

    /**
     * 订单日期
     */
    @TableField("F_ORDER_DATE")
    private Date orderDate;

    /**
     * 订单编码
     */
    @TableField("F_ORDER_CODE")
    private String orderCode;

    /**
     * 运输方式
     */
    @TableField("F_TRANSPORT_MODE")
    private String transportMode;

    /**
     * 发货日期
     */
    @TableField("F_DELIVERY_DATE")
    private Date deliveryDate;

    /**
     * 发货地址
     */
    @TableField("F_DELIVERY_ADDRESS")
    private String deliveryAddress;

    /**
     * 付款方式
     */
    @TableField("F_PAYMENT_MODE")
    private String paymentMode;

    /**
     * 应收金额
     */
    @TableField("F_RECEIVABLE_MONEY")
    private BigDecimal receivableMoney;

    /**
     * 定金比率
     */
    @TableField("F_EARNEST_RATE")
    private BigDecimal earnestRate;

    /**
     * 预付定金
     */
    @TableField("F_PREPAY_EARNEST")
    private BigDecimal prepayEarnest;

    /**
     * 当前状态
     */
    @TableField("F_CURRENT_STATE")
    private Integer currentState;

    /**
     * 流程引擎
     */
    @TableField("F_FLOW_ID")
    private String flowId;

    /**
     * 附件信息
     */
    @TableField("F_FILE_JSON")
    private String fileJson;

}
