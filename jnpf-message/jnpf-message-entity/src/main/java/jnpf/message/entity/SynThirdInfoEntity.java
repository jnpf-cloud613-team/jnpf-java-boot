package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 第三方工具的公司-部门-用户同步表模型
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/23 17:06
 */
@Data
@TableName("base_syn_third_info")
public class SynThirdInfoEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 第三方类型(1:企业微信;2:钉钉)
     */
    @TableField("F_THIRD_TYPE")
    private Integer thirdType;

    /**
     * 数据类型(1:组织(公司与部门);2:用户)
     */
    @TableField("F_DATA_TYPE")
    private Integer dataType;

    /**
     * 系统对象ID(公司ID、部门ID、用户ID)
     */
    @TableField("F_SYS_OBJ_ID")
    private String sysObjId;

    /**
     * 第三对象ID(公司ID、部门ID、用户ID)
     */
    @TableField("F_THIRD_OBJ_ID")
    private String thirdObjId;

    /**
     * 第三对象名称
     */
    @TableField("F_THIRD_NAME")
    private String thirdName;

}
