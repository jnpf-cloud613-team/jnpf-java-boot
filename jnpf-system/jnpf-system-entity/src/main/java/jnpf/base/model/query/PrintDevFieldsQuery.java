package jnpf.base.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 打印模板-数查询对象
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
public class PrintDevFieldsQuery {

    /**
     * sql语句
     */
    @NotBlank(message = "必填")
    @Schema(description = "sql语句")
    private String sqlTemplate;

    /**
     * 连接id
     */
    @NotBlank(message = "必填")
    @Schema(description = "连接id")
    private String dbLinkId;

}
