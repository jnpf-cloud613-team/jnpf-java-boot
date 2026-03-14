package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/28 11:08:05
 */
@Data
@TableName("base_code_num")
public class CodeNumEntity extends SuperEntity<String> {
    /**
     * 编码类型：ZZ-组织,GW-岗位，YHZ-用户组，YHJS-用户角色，
     * ZZJS-组织角色，GWJS-岗位角色,SF-身份编码，LC-流程，MH-门户，BDHC-表单回传
     */
    @TableField("F_TYPE")
    private String type;

    /**
     * 日期（用户重置序号）
     */
    @TableField("F_DATE_VALUE")
    private Integer dateValue;

    /**
     * 编号（1开始）
     */
    @TableField("F_NUM")
    private Integer num;
}
