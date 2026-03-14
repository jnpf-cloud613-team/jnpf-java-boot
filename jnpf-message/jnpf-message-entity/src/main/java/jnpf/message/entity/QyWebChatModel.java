package jnpf.message.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业微信的模型
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/5/25 14:18
 */
@Data
public class QyWebChatModel {
    @Schema(description = "CorpId")
    private String qyhCorpId;
    @Schema(description = "AgentId")
    private String qyhAgentId;
    @Schema(description = "AgentSecret")
    private String qyhAgentSecret;
    @Schema(description = "CorpSecret")
    private String qyhCorpSecret;
}
