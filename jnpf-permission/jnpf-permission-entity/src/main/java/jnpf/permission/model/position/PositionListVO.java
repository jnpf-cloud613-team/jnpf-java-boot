package jnpf.permission.model.position;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class PositionListVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "组织id")
    private String organizeId;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "是否责任岗位(0-否，1-是)")
    private Integer isDutyPosition;
    @Schema(description = "是否默认岗位(0-否，1-是)")
    private Integer defaultMark;
    @Schema(description = "允许设置责任(0-否，1-是)")
    private Integer allowDuty;
    @Schema(description = "子集")
    List<PositionListVO> children = new ArrayList<>();

    //拼接组织+岗位名称
    @Schema(description = "岗位全名")
    private String  orgNameTree;
}
