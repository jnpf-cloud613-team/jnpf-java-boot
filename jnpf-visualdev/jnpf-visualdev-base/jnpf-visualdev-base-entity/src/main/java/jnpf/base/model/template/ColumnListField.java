package jnpf.base.model.template;


import jnpf.model.visualjson.FieLdsModel;
import lombok.Data;

/**
 * 列表字段
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Data
public class ColumnListField extends FieLdsModel {
    /**
     * 字段
     */
    private String prop;
    /**
     * 列名
     */
    private String label;
    /**
     * 对齐
     */
    private String align;

    private String jnpfKey;
    /**
     * 是否勾选
     */
    private Boolean checked;

    private String fixed;
}
