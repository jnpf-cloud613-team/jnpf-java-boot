package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 消息接收
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_message")
public class MessageReceiveEntity extends SuperExtendEntity<String> {

    /**
     * 用户主键
     */
    @TableField("f_user_id")
    private String userId;

    /**
     * 是否阅读
     */
    @TableField("f_is_read")
    private Integer isRead;

    /**
     * 站内信息
     */
    @TableField("f_body_text")
    private String bodyText;

    /**
     * 标题
     */
    @TableField("f_title")
    private String title;

    /**
     * 类型(1-公告 2-流程 3-系统 4-日程)
     */
    @TableField("f_type")
    private Integer type;

    /**
     * 流程类型(1:审批 2:委托)
     */
    @TableField("f_flow_type")
    private Integer flowType;

}
