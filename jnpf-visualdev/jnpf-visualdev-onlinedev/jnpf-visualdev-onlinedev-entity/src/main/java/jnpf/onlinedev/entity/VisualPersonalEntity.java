package jnpf.onlinedev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * 列表个性视图
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/5 16:47:24
 */
@Data
@TableName("base_visual_personal")
public class VisualPersonalEntity extends SuperEntity<String> {
    /**
     * 菜单id
     */
    @TableField("F_MENU_ID")
    private String menuId;
    /**
     * 个性视图名称
     */
    @TableField("F_full_name")
    private String fullName;
    /**
     * 类型：0-系统，1-其他
     */
    @TableField("F_TYPE")
    private Integer type;
    /**
     * 状态：0-其他，1-默认
     */
    @TableField("F_STATUS")
    private Integer status;
    /**
     * 查询字段
     */
    @TableField("F_SEARCH_LIST")
    private String searchList;
    /**
     * 列表字段
     */
    @TableField("F_COLUMN_LIST")
    private String columnList;
}
