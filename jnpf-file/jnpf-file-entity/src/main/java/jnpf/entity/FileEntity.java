package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * @author JNPF
 */
@Data
@TableName("base_file")
public class FileEntity extends SuperExtendEntity<String> {

    /**
     * 文件编辑版本
     */
    @TableField("f_file_version")
    private String fileVersionId;

    /**
     * 文件名
     */
    @TableField("f_file_name")
    private String fileName;

    /**
     * 文件上传方式
     */
    @TableField("f_type")
    private String type;

    /**
     * 上传的url
     */
    @TableField("f_url")
    private String url;

    /**
     * 上次文件版本
     */
    @TableField("f_old_file_version_id")
    private String oldFileVersionId;
}
