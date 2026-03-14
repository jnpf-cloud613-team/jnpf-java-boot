package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 操作权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技AuthorizeController术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_authorize")
public class AuthorizeEntity extends SuperExtendEntity<String> {

    /**
     * 项目类型:
     * 角色、岗位：module、column、button、resource、portal
     * 身份：role、position
     */
    @TableField("f_item_type")
    private String itemType;

    /**
     * 项目主键
     */
    @TableField("f_item_id")
    private String itemId;

    /**
     * 对象类型：
     * 角色、岗位、身份
     * role、position、standing
     */
    @TableField("f_object_type")
    private String objectType;

    /**
     * 对象主键
     */
    @TableField("f_object_id")
    private String objectId;

}
