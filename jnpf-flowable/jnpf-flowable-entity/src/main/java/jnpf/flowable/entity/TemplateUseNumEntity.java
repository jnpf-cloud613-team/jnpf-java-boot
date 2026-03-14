package jnpf.flowable.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

@Data
@TableName("workflow_template_use_num")
public class TemplateUseNumEntity extends SuperExtendEntity<String> {

    /**
     * 用户id
     */
    @TableField("f_user_id")
    private String userId;

    /**
     * 功能id
     */
    @TableField("f_template_id")
    private String templateId;

    /**
     * 使用次数
     */
    @TableField("f_use_num")
    private Integer useNum;
}
