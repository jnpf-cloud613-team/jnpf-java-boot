package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据接口
 */
@Data
@TableName("base_data_interface")
public class DataInterfaceEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 分类
     */
    @TableField("f_category")
    private String category;

    /**
     * 接口名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 接口编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 类型(1-sql，2-静态数据，3-api)
     */
    @TableField("f_type")
    private Integer type;

    /**
     * 动作(3-查询)
     */
    @TableField("f_action")
    private Integer action;

    /**
     * 分页(0-禁用，1-启用)
     */
    @TableField("f_has_page")
    private Integer hasPage;

    /**
     * 后置接口(0-否 1-是)
     */
    @TableField("f_is_postposition")
    private Integer isPostPosition;

    /**
     * 数据配置json
     */
    @TableField("f_data_config_json")
    private String dataConfigJson;

    /**
     * 数据统计json
     */
    @TableField("f_data_count_json")
    private String dataCountJson;

    /**
     * 数据回显json
     */
    @TableField("f_data_echo_json")
    private String dataEchoJson;

    /**
     * 异常验证json
     */
    @TableField("f_data_exception_json")
    private String dataExceptionJson;

    /**
     * 数据处理json
     */
    @TableField("f_data_js_json")
    private String dataJsJson;

    /**
     * 参数json
     */
    @TableField("f_parameter_json")
    private String parameterJson;

    /**
     * 字段JSON
     */
    @TableField("f_field_json")
    private String fieldJson;

}
