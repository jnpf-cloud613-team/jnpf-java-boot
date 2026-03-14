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
@TableName("base_msg_wechat_user")
public class WechatUserEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /** 公众号元素id **/
    @TableField("f_gzh_id")
    private String gzhId;

    /** 用户id **/
    @TableField("f_user_id")
    private String userId;

    /** 公众号用户id **/
    @TableField("f_open_id")
    private String openId;

    /** 是否关注公众号 **/
    @TableField("f_close_mark")
    private Integer closeMark;

}
