package jnpf.base.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 可视化开发功能表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-04-02
 */
@Data
@TableName("base_visual_dev")
public class VisualdevEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {
    /**
     * 名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 编码
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 状态(0-暂存（默认），1-发布)
     */
    @TableField("F_STATE")
    private Integer state;

    /**
     * 类型：1-自定义表单，2-系统表单（有实体代码的）
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 关联的表
     */
    @TableField("F_TABLES_DATA")
    @JSONField(name = "tables")
    private String visualTables;

    /**
     * 分类（数据字典）
     */
    @TableField("F_CATEGORY")
    private String category;

    /**
     * 表单配置JSON
     */
    @TableField("F_FORM_DATA")
    private String formData;

    /**
     * 列表配置JSON
     */
    @TableField("F_COLUMN_DATA")
    private String columnData;

    /**
     * 关联数据连接id
     */
    @TableField("F_DB_LINK_ID")
    private String dbLinkId;

    /**
     * 页面类型（1、纯表单，2、表单加列表，4、数据视图）
     */
    @TableField("F_WEB_TYPE")
    private Integer webType;

    /**
     * app列表配置JSON
     */
    @TableField("F_APP_COLUMN_DATA")
    private String appColumnData;

    /**
     * 接口id
     */
    @TableField("F_INTERFACE_ID")
    private String interfaceId;

    /**
     * 接口参数
     */
    @TableField("F_INTERFACE_PARAM")
    private String interfaceParam;

    /**
     * 发布时勾选平台类型
     */
    @TableField("F_PLATFORM_RELEASE")
    private String platformRelease;

    //以下系统表单属性
    /**
     * Web地址
     */
    @TableField("F_URL_ADDRESS")
    private String urlAddress;
    /**
     * APP地址
     */
    @TableField("F_APP_URL_ADDRESS")
    private String appUrlAddress;
    /**
     * 接口路径
     */
    @TableField("F_INTERFACE_URL")
    private String interfaceUrl;

    /**
     * 系统id
     */
    @TableField("F_SYSTEM_ID")
    private String systemId;

    /**
     * 启用流程
     */
    @TableField("F_ENABLE_FLOW")
    private Integer enableFlow;

    /**
     * web页面地址
     */
    @TableField("F_WEB_ADDRESS")
    private String webAddress;

    /**
     * app页面地址
     */
    @TableField("F_APP_ADDRESS")
    private String appAddress;

    /**
     * 按钮设置json
     */
    @TableField("F_BUTTON_DATA")
    private String buttonData;

    /**
     * APP按钮设置json
     */
    @TableField("F_APP_BUTTON_DATA")
    private String appButtonData;
}
