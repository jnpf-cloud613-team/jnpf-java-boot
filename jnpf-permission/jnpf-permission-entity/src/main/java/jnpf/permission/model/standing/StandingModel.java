package jnpf.permission.model.standing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 身份详情
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:39:14
 */
@Data
public class StandingModel extends StandingVO implements Serializable {

    @Schema(description = "岗位列表")
    private List<String> posIds = new ArrayList<>();
    @Schema(description = "角色列表")
    private List<String> roleIds = new ArrayList<>();

}
