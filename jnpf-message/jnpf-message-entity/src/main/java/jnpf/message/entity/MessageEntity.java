package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperBaseEntity;
import jnpf.constant.TableFieldsNameConst;
import lombok.Data;

import java.util.Date;

/**
 * 消息实例
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_notice")
public class MessageEntity extends SuperBaseEntity<String> {

    /**
     * 标题
     */
    @TableField("f_title")
    private String title;

    /**
     * 正文
     */
    @TableField("f_body_text")
    private String bodyText;

    /**
     * 收件用户
     */
    @TableField("f_to_user_ids")
    private String toUserIds;

    /**
     * 附件
     */
    @TableField("f_files")
    private String files;

    /**
     * 封面图片
     */
    @TableField("f_cover_image")
    private String coverImage;

    /**
     * 过期时间
     */
    @TableField("f_expiration_time")
    private Date expirationTime;

    /**
     * 分类 1-公告 2-通知
     */
    @TableField("f_category")
    private String category;

    /**
     * 提醒方式 1-站内信 2-自定义 3-不通知
     */
    @TableField("f_type")
    private Integer remindCategory;

    /**
     * 发送配置
     */
    @TableField("f_send_config_id")
    private String sendConfigId;

    /**
     * 描述
     */
    @TableField("f_description")
    private String excerpt;

    /**
     * 有效标志 (-1-删除，0-草稿，1-已发送，2-已过期，)
     */
    @TableField(value = "f_enabled_mark", fill = FieldFill.INSERT)
    private Integer enabledMark;

    /**
     * 排序码
     */
    @TableField("f_sort_code")
    private Long sortCode;

    /**
     * 删除标志
     */
    @TableField(value = "f_delete_mark", updateStrategy = FieldStrategy.ALWAYS)
    private Integer deleteMark;

    /**
     * 删除时间
     */
    @TableField(value = "f_delete_time", fill = FieldFill.UPDATE)
    private Date deleteTime;

    /**
     * 删除用户
     */
    @TableField(value = "f_delete_user_id", fill = FieldFill.UPDATE)
    private String deleteUserId;

    /**
     * 修改时间
     */
    @TableField(value = "f_last_modify_time", updateStrategy = FieldStrategy.ALWAYS)
    private Date lastModifyTime;

    /**
     * 修改用户
     */
    @TableField(value = "f_last_modify_user_id", updateStrategy = FieldStrategy.ALWAYS)
    private String lastModifyUserId;

    /**
     * 创建时间
     */
    @TableField(value = TableFieldsNameConst.F_CREATOR_TIME , fill = FieldFill.INSERT)
    private Date creatorTime;

    /**
     * 创建用户
     */
    @TableField(value = TableFieldsNameConst.F_CREATOR_USER_ID , fill = FieldFill.INSERT)
    private String creatorUserId;

    /**
     * 租户id
     */
    @TableField(value = TableFieldsNameConst.F_TENANT_ID , fill = FieldFill.INSERT_UPDATE)
    private String tenantId;

}
