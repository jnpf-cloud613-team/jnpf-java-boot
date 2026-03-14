package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * 知识文档删除记录
 *
 * @author JNPF开发平台组
 * @version V5.0.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2024年4月10日 上午9:18
 */
@Data
@TableName("ext_document_log")
public class DocumentLogEntity extends SuperEntity<String> {

    /**
     * 文档主键
     */
    @TableField("F_DOCUMENT_ID")
    private String documentId;

    /**
     * 共享人员
     */
    @TableField("F_CHILD_DOCUMENT")
    private String childDocument;


}
