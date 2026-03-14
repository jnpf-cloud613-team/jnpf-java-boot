package jnpf.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售订单
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 10:40:59
 */
@Data
@TableName("ext_product")
public class ProductEntity extends SuperExtendEntity.SuperExtendDescriptionEntity<String> {

    /**
     * 订单编号
     */
    @TableField("F_EN_CODE")
    @JSONField(name = "code")
    private String enCode;

    /**
     * 客户类别
     */
    @TableField("F_TYPE")
    private String type;

    /**
     * 客户id
     */
    @TableField("F_CUSTOMER_ID")
    private String customerId;

    /**
     * 客户名称
     */
    @TableField("F_CUSTOMER_NAME")
    private String customerName;

    /**
     * 制单人
     */
    @TableField("F_SALESMAN_ID")
    private String salesmanId;

    /**
     * 制单人
     */
    @TableField("F_SALESMAN_NAME")
    private String salesmanName;

    /**
     * 制单日期
     */
    @TableField("F_SALESMAN_DATE")
    private Date salesmanDate;

    /**
     * 审核人
     */
    @TableField("F_AUDIT_NAME")
    private String auditName;

    /**
     * 审核日期
     */
    @TableField("F_AUDIT_DATE")
    private Date auditDate;

    /**
     * 审核状态
     */
    @TableField("F_AUDIT_STATE")
    private Integer auditState;

    /**
     * 发货仓库
     */
    @TableField("F_GOODS_WAREHOUSE")
    private String goodsWarehouse;

    /**
     * 发货日期
     */
    @TableField("F_GOODS_DATE")
    private Date goodsDate;

    /**
     * 发货通知人
     */
    @TableField("F_CONSIGNOR")
    private String consignor;

    /**
     * 发货状态
     */
    @TableField("F_GOODS_STATE")
    private Integer goodsState;

    /**
     * 关闭状态
     */
    @TableField("F_CLOSE_STATE")
    private Integer closeState;

    /**
     * 关闭日期
     */
    @TableField("F_CLOSE_DATE")
    private Date closeDate;

    /**
     * 收款方式
     */
    @TableField("F_GATHERING_TYPE")
    private String gatheringType;

    /**
     * 业务员
     */
    @TableField("F_BUSINESS")
    private String business;

    /**
     * 送货地址
     */
    @TableField("F_ADDRESS")
    private String address;

    /**
     * 联系方式
     */
    @TableField("F_CONTACT_TEL")
    private String contactTel;

    /**
     * 联系人
     */
    @TableField("F_CONTACT_NAME")
    private String contactName;

    /**
     * 收货消息
     */
    @TableField("F_HARVEST_MSG")
    private Integer harvestMsg;

    /**
     * 收货仓库
     */
    @TableField("F_HARVEST_WAREHOUSE")
    private String harvestWarehouse;

    /**
     * 代发客户
     */
    @TableField("F_ISSUING_NAME")
    private String issuingName;

    /**
     * 让利金额
     */
    @TableField("F_PART_PRICE")
    private BigDecimal partPrice;

    /**
     * 优惠金额
     */
    @TableField("F_REDUCED_PRICE")
    private BigDecimal reducedPrice;

    /**
     * 折后金额
     */
    @TableField("F_DISCOUNT_PRICE")
    private BigDecimal discountPrice;

}
