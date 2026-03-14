package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * 账号配置使用记录表
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-09-21
 */
@Data
@TableName("base_message_send_record")
public class SendConfigRecordEntity extends SuperEntity<String> {

    @TableField("F_SENDCONFIGID")
    private String sendConfigId;

    @TableField("F_MESSAGESOURCE")
    private String messageSource;

    @TableField("F_USEDID")
    private String usedId;

    /**
     * 状态
     */
    @TableField("F_ENABLEDMARK")
    private Integer enabledMark;

}
