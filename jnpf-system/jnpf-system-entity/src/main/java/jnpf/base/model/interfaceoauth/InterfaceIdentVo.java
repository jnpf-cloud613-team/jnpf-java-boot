package jnpf.base.model.interfaceoauth;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.datainterface.DataInterfaceVo;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 接口认证vo
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/8 10:16
 */
@Data
public class InterfaceIdentVo {

    @Schema(description = "id")
    private String id;

    @Schema(description = "应用id")
    private String appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用秘钥")
    private String appSecret;

    @Schema(description = "验证签名")
    private Integer verifySignature;

    @Schema(description = "使用期限")
    private Date usefulLife;

    @Schema(description = "白名单")
    private String whiteList;

    @Schema(description = "黑名单")
    private String blackList;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "状态")
    private Integer enabledMark;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "创建人id")
    private String creatorUserId;

    @Schema(description = "创建人")
    private String creatorUser;

    @Schema(description = "创建时间")
    private Long creatorTime;

    @Schema(description = "修改人id")
    private String lastModifyUserId;

    @Schema(description = "修改人")
    private String lastModifyUser;

    @Schema(description = "修改时间")
    private Long lastModifyTime;

    /**
     * 接口列表
     */
    @Schema(description = "接口列表字符串")
    private String dataInterfaceIds;
    /**
     * 接口列表
     */
    @Schema(description = "接口列表")
    private List<DataInterfaceVo> list;

    @Schema(description = "授权用户列表")
    private List<InterfaceUserVo> userList;
}
