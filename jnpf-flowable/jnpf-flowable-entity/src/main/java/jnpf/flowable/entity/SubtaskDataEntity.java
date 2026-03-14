package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子流程发起参数(依次发起)
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/6 15:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_subtask_data")
public class SubtaskDataEntity extends SuperExtendEntity<String> {
    /**
     * 子流程发起参数
     */
    @TableField("F_SUBTASK_JSON")
    private String subtaskJson;
    /**
     * 父流程id
     */
    @TableField("F_PARENT_ID")
    private String parentId;
    /**
     * 节点编码
     */
    @TableField("F_NODE_CODE")
    private String nodeCode;
}
