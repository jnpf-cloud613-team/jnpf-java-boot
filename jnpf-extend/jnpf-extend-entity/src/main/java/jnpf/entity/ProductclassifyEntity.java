package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * 产品分类
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 14:34:04
 */
@Data
@TableName("ext_product_classify")
public class ProductclassifyEntity extends SuperEntity<String> {

    /**
     * 上级
     */
    @TableField("F_PARENT_ID")
    private String parentId;

    /**
     * 名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

}
