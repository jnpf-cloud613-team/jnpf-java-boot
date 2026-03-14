package jnpf.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 产品商品
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 15:57:50
 */
@Data
@TableName("ext_product_goods")
public class ProductGoodsEntity extends SuperExtendEntity<String> {

    /**
     * 分类主键
     */
    @TableField("F_CLASSIFY_ID")
    private String classifyId;

    /**
     * 产品编号
     */
    @TableField("F_EN_CODE")
    @JSONField(name = "code")
    private String enCode;

    /**
     * 产品名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 订货类型
     */
    @TableField("F_TYPE")
    private String type;

    /** 产品规格 */
    @TableField("F_PRODUCT_SPECIFICATION")
    private String productSpecification;

    /** 单价 */
    @TableField("F_MONEY")
    private String money;

    /**
     * 库存数
     */
    @TableField("F_QTY")
    private Integer qty;

    /** 金额 */
    @TableField("F_AMOUNT")
    private String amount;

}
