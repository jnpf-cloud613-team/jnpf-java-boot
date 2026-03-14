package jnpf.message.model.websocket;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息分页参数模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-05
 */
@Data
@Builder
public class PaginationMessageModel implements Serializable {

    /**
     * 当前页
     */
    private Integer currentPage;


    private Integer pageSize;


    private Long total;

}
