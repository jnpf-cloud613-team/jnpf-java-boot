package jnpf.base.model.interfaceoauth;

import jnpf.base.PaginationTime;
import lombok.Data;

/**
 * 日志列表查询
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/10 11:24
 */
@Data
public class PaginationIntrfaceLog extends PaginationTime {
    private String keyword;
}
