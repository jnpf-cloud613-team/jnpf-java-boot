package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品明细
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 10:40:59
 */
@Data
@TableName("ext_product_entry")
public class ProductEntryEntity extends SuperExtendEntity.SuperExtendDescriptionEntity<String> {

    /**
     * 订单主键
     */
    @TableField("F_PRODUCT_ID")
    private String productId;

    /**
     * 产品编码
     */
    @TableField("F_PRODUCT_CODE")
    private String productCode;

    /**
     * 产品名称
     */
    @TableField("F_PRODUCT_NAME")
    private String productName;

    /**
     * 产品规格
     */
    @TableField("F_PRODUCT_SPECIFICATION")
    private String productSpecification;

    /**
     * 数量
     */
    @TableField("F_QTY")
    private Integer qty;

    /**
     * 订货类型
     */
    @TableField("F_TYPE")
    private String type;

    /**
     * 单价
     */
    @TableField("F_MONEY")
    private BigDecimal money;

    /**
     * 折后单价
     */
    @TableField("F_PRICE")
    private BigDecimal price;

    /**
     * 单位
     */
    @TableField("F_UTIL")
    private String util;

    /**
     * 控制方式
     */
    @TableField("F_COMMAND_TYPE")
    private String commandType;

    /**
     * 金额
     */
    @TableField("F_AMOUNT")
    private BigDecimal amount;

    /**
     * 活动
     */
    @TableField("F_ACTIVITY")
    private String activity;

}
