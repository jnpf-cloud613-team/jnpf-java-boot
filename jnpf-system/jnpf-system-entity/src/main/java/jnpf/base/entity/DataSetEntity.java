package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 数据集管理
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/6 18:21:15
 */
@Data
@TableName("base_data_set")
public class DataSetEntity extends SuperExtendEntity<String> {

    /**
     * 关联的数据类型
     */
    @TableField("F_OBJECT_TYPE")
    private String objectType;

    /**
     * 关联的数据id
     */
    @TableField("F_OBJECT_ID")
    private String objectId;

    /**
     * 数据集名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 数据库连接
     */
    @TableField("F_DB_LINK_ID")
    private String dbLinkId;


    /**
     * 数据sql语句
     */
    @TableField("F_DATA_CONFIG_JSON")
    private String dataConfigJson;

    /**
     * 参数json
     */
    @TableField("F_PARAMETER_JSON")
    private String parameterJson;

    /**
     * 字段json
     */
    @TableField("F_FIELD_JSON")
    private String fieldJson;

    /**
     * 类型：1-sql语句，2-配置式
     */
    @TableField("f_type")
    private Integer type;

    /**
     * 配置json
     */
    @TableField("f_visual_config_json")
    private String visualConfigJson;

    /**
     * 筛选设置json
     */
    @TableField("f_filter_config_json")
    private String filterConfigJson;

    /**
     * 数据接口id
     */
    @TableField("f_interface_id")
    private String interfaceId;
}
