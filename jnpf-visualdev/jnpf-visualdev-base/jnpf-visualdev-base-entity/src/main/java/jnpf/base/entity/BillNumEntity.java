package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 单据递增序号
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/3 14:08:03
 */
@Data
@TableName("base_bill_num")
public class BillNumEntity extends SuperEntity<String> {

    /**
     * 单据规则id
     */
    @TableField("f_rule_id")
    private String ruleId;

    /**
     * 功能id
     */
    @TableField("f_visual_id")
    private String visualId;

    /**
     * 流程模板id
     */
    @TableField("f_flow_id")
    private String flowId;

    /**
     * 时间规则类型
     */
    @TableField("f_rule_config")
    private String ruleConfig;

    /**
     * 时间规则值：用于判断是否重置
     */
    @TableField("f_date_value")
    private String dateValue;

    /**
     * 单据递增序号
     */
    @TableField("f_num")
    private Integer num;
}
