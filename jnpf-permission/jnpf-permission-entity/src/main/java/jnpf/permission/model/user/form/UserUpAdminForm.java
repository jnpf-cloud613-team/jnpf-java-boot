package jnpf.permission.model.user.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 超级管理员设置表单参数
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.3.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/2/23
 */
@Data
public class UserUpAdminForm {

    @Schema(description = "超级管理id集合")
    List<String> adminIds;

}
