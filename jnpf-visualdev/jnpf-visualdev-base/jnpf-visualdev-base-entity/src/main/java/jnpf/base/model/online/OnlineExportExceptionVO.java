package jnpf.base.model.online;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年3月15日09:47:19
 */
@Data
public class OnlineExportExceptionVO {
    private String tableField;
    private String field;
    private String label;
}
