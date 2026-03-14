package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.util.Date;

/**
 * 聊天内容
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_im_content")
public class ImContentEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /**
     * 发送者
     */
    @TableField("f_send_user_id")
    private String sendUserId;

    /**
     * 发送时间
     */
    @TableField("f_send_time")
    private Date sendTime;

    /**
     * 接收者
     */
    @TableField("f_receive_user_id")
    private String receiveUserId;

    /**
     * 接收时间
     */
    @TableField("f_receive_time")
    private Date receiveTime;

    /**
     * 内容
     */
    @TableField("f_content")
    private String content;

    /**
     * 内容
     */
    @TableField("f_content_type")
    private String contentType;

}
