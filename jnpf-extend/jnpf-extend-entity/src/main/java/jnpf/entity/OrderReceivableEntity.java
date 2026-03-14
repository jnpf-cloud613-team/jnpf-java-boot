package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单收款
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("ext_order_receivable")
public class OrderReceivableEntity extends SuperExtendEntity.SuperExtendDescriptionEntity<String> {

    /**
     * 订单主键
     */
    @TableField("F_ORDER_ID")
    private String orderId;

    /**
     * 收款摘要
     *
     */
    @TableField("F_ABSTRACT")
    private String fabstract;

    /**
     * 收款日期
     */
    @TableField("F_RECEIVABLE_DATE")
    private Date receivableDate;

    /**
     * 收款比率
     */
    @TableField("F_RECEIVABLE_RATE")
    private BigDecimal receivableRate;

    /**
     * 收款金额
     */
    @TableField("F_RECEIVABLE_MONEY")
    private BigDecimal receivableMoney;

    /**
     * 收款方式
     */
    @TableField("F_RECEIVABLE_MODE")
    private String receivableMode;

    /**
     * 收款状态
     */
    @TableField("F_RECEIVABLE_STATE")
    private Integer receivableState;

}
