package jnpf.base.model.dblink;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 数据连接表单对象
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DbLinkCreUpForm extends DbLinkBaseForm{

    @Schema(description = "有效标识")
    @NotNull(message = "必填")
    private boolean enabledMark;

}
