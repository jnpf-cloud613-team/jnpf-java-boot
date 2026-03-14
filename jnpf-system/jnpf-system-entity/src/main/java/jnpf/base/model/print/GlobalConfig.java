package jnpf.base.model.print;

import lombok.Data;

import java.util.List;

/**
 * 全局配置属性
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/26 15:04:29
 */
@Data
public class GlobalConfig {
    private List<SliceConfig> sliceConfig;
}
