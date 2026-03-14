package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_signature_user")
public class SignatureUserEntity extends SuperExtendEntity.SuperExtendDescriptionEntity<String> {
    /**
     * 签章主键
     */
    @TableField("f_signature_id")
    private String signatureId;

    /**
     * 用户主键
     */
    @TableField("f_user_id")
    private String userId;



}
