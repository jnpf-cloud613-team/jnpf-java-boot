package jnpf.flowable.model.record;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/29 10:55
 */
@Data
public class RecordVo  implements Serializable {
    /**
     * 主键
     */
    @Schema(description = "主键")
    private String id;
    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String nodeName;
    /**
     * 节点编码
     */
    @Schema(description = "节点编码")
    private String nodeCode;
    /**
     * 经办类型
     */
    @Schema(description = "经办类型")
    private Integer handleType;
    /**
     * 经办人员
     */
    @Schema(description = "经办人员")
    private String handleId;
    /**
     * 经办时间
     */
    @Schema(description = "经办时间")
    private Date handleTime;
    /**
     * 经办理由
     */
    @Schema(description = "经办理由")
    private String handleOpinion;
    /**
     * 经办主键
     */
    @Schema(description = "经办主键")
    private String operatorId;
    /**
     * 任务id
     */
    @Schema(description = "任务id")
    private String taskId;
    /**
     * 签名图片
     */
    @Schema(description = "签名图片")
    private String signImg;
    /**
     * 状态
     */
    @Schema(description = "状态")
    private Integer status;
    /**
     * 流转操作人名称
     */
    @Schema(description = "流转操作人名称")
    private String handleUserName;
    /**
     * 经办文件
     */
    @Schema(description = "经办文件")
    private String fileList;
    /**
     * 用户名称
     */
    @Schema(description = "用户名称")
    private String userName;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date creatorTime;
    /**
     * 头像
     */
    @Schema(description = "头像")
    private String headIcon;
    /**
     * 是否外部节点
     */
    @Schema(description = "是否外部节点")
    private Boolean isOutSideNode;
    /**
     * 拓展字段
     */
    @Schema(description = "拓展字段")
    private List<Map<String, Object>> approvalField = new ArrayList<>();
}
