package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 用户关系
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_user_relation")
public class UserRelationEntity extends SuperExtendEntity<String> {

    /**
     * 用户主键
     */
    @TableField("F_USER_ID")
    private String userId;

    /**
     * 对象类型
     */
    @TableField("F_OBJECT_TYPE")
    private String objectType;

    /**
     * 对象主键
     */
    @TableField("F_OBJECT_ID")
    private String objectId;

}
