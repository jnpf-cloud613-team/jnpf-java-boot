package jnpf.base.model.dataset;

import lombok.Data;

import java.util.List;

/**
 * 数据转换模型
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/7/15 11:42:03
 */
@Data
public class DataSetConfig {
    private String dataType;
    private List<DataSetOptions> options;
    private String dictionaryType;
    private String propsUrl;
    private String propsValue;
    private String format;
    private String propsLabel;
    /**
     * 精度位数
     */
    private Integer precision = 0;
    /**
     * 千位符是否开启
     */
    private boolean thousands;
}
