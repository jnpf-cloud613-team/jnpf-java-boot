package jnpf.permission.model.user.mod;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Page;
import lombok.Data;

/**
 * 获取岗位成员
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/4/28 14:44
 */
@Data
public class UsersByPositionModel extends Page {
    @Schema(description = "岗位id")
    private String positionId;
}
