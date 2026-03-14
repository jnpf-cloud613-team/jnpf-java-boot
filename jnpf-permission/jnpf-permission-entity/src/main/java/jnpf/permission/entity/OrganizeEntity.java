package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 组织机构
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_organize")
public class OrganizeEntity extends PermissionEntityBase {
    /**
     * 机构上级
     */
    @TableField("F_PARENT_ID")
    private String parentId;

    /**
     * 机构分类
     */
    @TableField("F_CATEGORY")
    private String category;

    /**
     * 机构编号
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 机构名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 责任岗位
     */
    @TableField("F_DUTY_POSITION")
    private String dutyPosition;

    /**
     * 机构主管
     */
    @TableField("F_MANAGER_ID")
    private String managerId;

    /**
     * 扩展属性
     */
    @TableField("F_PROPERTY_JSON")
    private String propertyJson;

    /**
     * 父级组织
     */
    @TableField("F_ORGANIZE_ID_TREE")
    private String organizeIdTree;

    /**
     * 机构路径全名
     */
    @TableField("F_ORG_NAME_TREE")
    private String orgNameTree;
}
