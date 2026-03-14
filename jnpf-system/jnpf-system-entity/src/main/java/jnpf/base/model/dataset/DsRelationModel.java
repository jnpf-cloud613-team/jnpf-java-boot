package jnpf.base.model.dataset;

import lombok.Data;

/**
 * 关联字段属性
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/9 17:22:21
 */
@Data
public class DsRelationModel {
    /**
     * 左表字段
     */
    private String pField;
    /**
     * 右表字段
     */
    private String field;
}
