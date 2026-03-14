package jnpf.message.model.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 第三方工具的对象同步表
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/25 9:35
 */
@Data
public class SynThirdInfoCrForm {

    /**
     * 第三方类型(1:企业微信;2:钉钉)
     */
    @Schema(description = "第三方类型(1:企业微信;2:钉钉)")
    private Integer thirdtype;

    /**
     * 数据类型(1:公司;2:部门;3:用户)
     */
    @Schema(description = "数据类型(1:公司;2:部门;3:用户)")
    private Integer datatype;

    /**
     * 本地对象ID(公司ID、部门ID、用户ID)
     */
    @Schema(description = "本地对象ID(公司ID、部门ID、用户ID)")
    private String sysObjId;

    /**
     * 第三方对象ID(公司ID、部门ID、用户ID)
     */
    @Schema(description = "第三方对象ID(公司ID、部门ID、用户ID)")
    private String thirdObjId;

    /**
     * 同步状态(0:未同步;1:同步成功;2:同步失败)
     */
    @Schema(description = "同步状态(0:未同步;1:同步成功;2:同步失败)")
    private Integer synstate;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;
}
