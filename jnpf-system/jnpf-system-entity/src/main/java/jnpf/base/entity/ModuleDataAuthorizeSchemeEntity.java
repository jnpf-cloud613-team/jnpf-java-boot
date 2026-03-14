package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据权限方案
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_module_scheme")
public class ModuleDataAuthorizeSchemeEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 方案编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 方案名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 条件规则Json
     */
    @TableField("f_condition_json")
    private String conditionJson;

    /**
     * 条件规则描述
     */
    @TableField("f_condition_text")
    private String conditionText;

    /**
     * 功能主键
     */
    @TableField("f_module_id")
    private String moduleId;

    /**
     * 全部数据标识
     */
    @TableField("f_all_data")
    private Integer allData;

    /**
     * 分组匹配逻辑
     */
    @TableField("f_match_logic")
    private String matchLogic;

}
