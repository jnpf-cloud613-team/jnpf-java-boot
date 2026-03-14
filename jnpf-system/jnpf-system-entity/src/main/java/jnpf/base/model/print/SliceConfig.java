package jnpf.base.model.print;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多联配置属性
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/26 15:04:41
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SliceConfig {
    private String dataSet;
    private Integer limit;
}
