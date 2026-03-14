package jnpf.permission.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;


@Data
@TableName("base_user_extra")
public class UserExtraEntity extends SuperExtendEntity<String> {



    /**
     * 用户id
     */
    @TableField("f_user_id")
    private String userId;

    /**
     * 常用菜单
     */
    @TableField("f_common_menu")
    private String commonMenu;

    /**
     * 扩展属性
     */
    @TableField("f_property_json")
    private String propertyJson;


    /**
     * 外观属性
     */
    @TableField("f_preference_json")
    private String preferenceJson;


}
