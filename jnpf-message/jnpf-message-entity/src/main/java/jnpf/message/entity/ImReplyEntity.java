package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.util.Date;

/**
 * 聊天会话表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-05-28
 */
@Data
@TableName("base_im_reply")
public class ImReplyEntity extends SuperExtendEntity<String> {

    /**
     * 发送者
     */
    @TableField("f_user_id")
    private String userId;

    /**
     * 接收者
     */
    @TableField("f_receive_user_id")
    private String receiveUserId;

    /**
     * 发送时间
     */
    @TableField("f_receive_time")
    private Date receiveTime;

}
