package jnpf.base.util.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 别名对象
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/16 11:21:03
 */
@Data
public class AliasModel {
    /**
     * 表名
     */
    private String tableName;
    /**
     * 表别名
     */
    private String aliasName;
    /**
     * 字段别名map
     */
    private Map<String, String> fieldsMap = new HashMap<>();
}
