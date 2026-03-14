package jnpf.base.model.dataset;

import lombok.Data;

/**
 * 配置式字段属性
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/9 17:27:00
 */
@Data
public class DsConfigFields {
    /**
     * 字段名
     */
    private String field;
    /**
     * 字段备注
     */
    private String fieldName;
    /**
     * 字段类型
     */
    private String dataType;

    //一下用于组装sql
    /**
     * 表名
     */
    private String table;
    /**
     * 字段别名
     */
    private String fieldAlias;
}
