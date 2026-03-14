package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;


/**
 *
 * 短信变量表
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
@TableName("base_user_device")
public class UserDeviceEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /** 用户id **/
    @TableField("F_USERID")
    private String userId;

    /** 设备id **/
    @TableField("F_CLIENTID")
    private String clientId;
}
