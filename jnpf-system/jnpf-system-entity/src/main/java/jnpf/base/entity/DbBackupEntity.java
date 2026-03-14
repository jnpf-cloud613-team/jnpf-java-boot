package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据备份
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
//@TableName("base_dbbackup")
public class DbBackupEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 备份库名
     */
    @TableField("F_BACKUPDBNAME")
    private String backupDbName;

    /**
     * 备份时间
     */
    @TableField("F_BACKUPTIME")
    private Date backupTime;

    /**
     * 文件名称
     */
    @TableField("F_FILENAME")
    private String fileName;

    /**
     * 文件大小
     */
    @TableField("F_FILESIZE")
    private String fileSize;

    /**
     * 文件路径
     */
    @TableField("F_FILEPATH")
    private String filePath;

}
