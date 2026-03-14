package jnpf.base.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 在线开发变量
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/6 10:03:10
 */
public class VisualConst {
    VisualConst() {
    }

    /**
     * 数据库全部的 int bigint字段
     */
    public static final List<String> DB_INT_ALL = Collections.unmodifiableList(
            Arrays.asList("int", "bigint", "number", "integer", "int2", "int4", "int8", "numeric"));
}
