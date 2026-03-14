package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * 客户信息
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 14:09:05
 */
@Data
@TableName("ext_customer")
public class CustomerEntity extends SuperEntity<String> {

    /**
     * 编码
     */
    @TableField("F_EN_CODE")
    private String code;

    /**
     * 客户名称
     */
    @TableField("F_CUSTOMER_NAME")
    private String customerName;

    /**
     * 地址
     */
    @TableField("F_ADDRESS")
    private String address;

    /**
     * 姓名
     */
    @TableField("F_FULL_NAME")
    private String name;

    /**
     * 联系方式
     */
    @TableField("F_CONTACT_TEL")
    private String contactTel;

}
