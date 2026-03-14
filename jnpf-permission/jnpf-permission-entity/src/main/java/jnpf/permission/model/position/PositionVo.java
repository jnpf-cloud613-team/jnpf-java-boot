package jnpf.permission.model.position;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过组织id获取岗位列表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-21
 */
@Data
public class PositionVo implements Serializable {

    @Schema(description = "id")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "类型：position,role")
    private String type;

    @Schema(description = "是否有子集")
    private Boolean hasChildren = false;

    @Schema(description = "pc当前身份")
    private Boolean pcCurStand = false;

    @Schema(description = "app当前身份")
    private Boolean appCurStand = false;

    @Schema(description = "子集")
    private List<PositionVo> children = new ArrayList<>();

    public PositionVo(String id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }
}
