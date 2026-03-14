package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印模板-实体类
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
@EqualsAndHashCode
@TableName("base_print_version")
public class PrintVersionEntity extends SuperExtendEntity<String> {

    /**
     * 打印模板id
     */
    @TableField("F_TEMPLATE_ID")
    private String templateId;

    /**
     * 打印版本
     */
    @TableField("F_VERSION")
    private Integer version;

    /**
     * 状态(0.设计中,1.启用中,2.已归档)
     */
    @TableField("F_STATE")
    private Integer state;

    /**
     * 打印模板内容
     */
    @TableField("F_PRINT_TEMPLATE")
    private String printTemplate;

    /**
     * 转换配置
     */
    @TableField("F_CONVERT_CONFIG")
    private String convertConfig;

    /**
     * 全局配置
     */
    @TableField("F_GLOBAL_CONFIG")
    private String globalConfig;
}
