package jnpf.base.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("base_module_use_num")
public class ModuleUserNumEntity extends SuperExtendEntity<String>{



    /**
     * 用户id
     */
    @TableField("f_user_id")
    private String userId;

    /**
     * 功能id
     */
    @TableField("f_module_id")
    private String moduleId;

    /**
     * 使用次数
     */
    @TableField("f_use_num")
    private Integer useNum;

    /**
     * 系统标识
     */
    @TableField("f_system_code")
    private String systemCode;

}
