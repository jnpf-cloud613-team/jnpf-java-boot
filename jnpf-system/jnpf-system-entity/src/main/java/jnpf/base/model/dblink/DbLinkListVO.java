package jnpf.base.model.dblink;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DbLinkListVO {
    @Schema(description = "连接名称")
    private String fullName;
    @Schema(description = "连接驱动")
    private String dbType;
    @Schema(description = "主机名称")
    private String host;
    @Schema(description = "端口")
    private String port;
    @Schema(description = "创建时间",example = "1")
    private Long creatorTime;
    @Schema(description = "创建人")
    @JSONField(name = "creatorUserId")
    private String creatorUser;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "修改时间")
    private Long lastModifyTime;
    @Schema(description = "修改用户")
    @JSONField(name = "lastModifyUserId")
    private String lastModifyUser;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "数据库名")
    private String dbName;
    @Schema(description = "用户名")
    private String userName;

}
