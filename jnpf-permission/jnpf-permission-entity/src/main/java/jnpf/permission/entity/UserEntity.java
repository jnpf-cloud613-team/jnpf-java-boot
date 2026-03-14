package jnpf.permission.entity;

import jnpf.base.entity.SuperExtendEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("base_user")
public class UserEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 账户
     */
    @TableField("F_ACCOUNT")
    private String account;

    /**
     * 姓名
     */
    @TableField("F_REAL_NAME")
    private String realName;

    /**
     * 快速查询
     */
    @TableField("F_QUICK_QUERY")
    private String quickQuery;

    /**
     * 呢称
     */
    @TableField("F_NICK_NAME")
    private String nickName;

    /**
     * 头像
     */
    @TableField("F_HEAD_ICON")
    private String headIcon;

    /**
     * 性别
     */
    @TableField("F_GENDER")
    private String gender;

    /**
     * 生日
     */
    @TableField("F_BIRTHDAY")
    private Date birthday;

    /**
     * 手机
     */
    @TableField("F_MOBILE_PHONE")
    private String mobilePhone;

    /**
     * 电话
     */
    @TableField("F_TELE_PHONE")
    private String telePhone;

    /**
     * F_Landline
     */
    @TableField("F_LANDLINE")
    private String landline;

    /**
     * 邮箱
     */
    @TableField("F_EMAIL")
    private String email;

    /**
     * 民族
     */
    @TableField("F_NATION")
    private String nation;

    /**
     * 籍贯
     */
    @TableField("F_NATIVE_PLACE")
    private String nativePlace;

    /**
     * 入职日期
     */
    @TableField(value = "F_ENTRY_DATE")
    private Date entryDate;

    /**
     * 证件类型
     */
    @TableField("F_CERTIFICATES_TYPE")
    private String certificatesType;

    /**
     * 证件号码
     */
    @TableField("F_CERTIFICATES_NUMBER")
    private String certificatesNumber;

    /**
     * 文化程度
     */
    @TableField("F_EDUCATION")
    private String education;

    /**
     * F_UrgentContacts
     */
    @TableField("F_URGENT_CONTACTS")
    private String urgentContacts;

    /**
     * 紧急电话
     */
    @TableField("F_URGENT_TELE_PHONE")
    private String urgentTelePhone;

    /**
     * 通讯地址
     */
    @TableField("F_POSTAL_ADDRESS")
    private String postalAddress;

    /**
     * 自我介绍
     */
    @TableField("F_SIGNATURE")
    private String signature;

    /**
     * 密码
     */
    @TableField("F_PASSWORD")
    private String password;

    /**
     * 秘钥
     */
    @TableField("F_SECRETKEY")
    private String secretkey;

    /**
     * 首次登录时间
     */
    @TableField("F_FIRST_LOG_TIME")
    private Date firstLogTime;

    /**
     * 首次登录IP
     */
    @TableField("F_FIRST_LOG_IP")
    private String firstLogIp;

    /**
     * 前次登录时间
     */
    @TableField("F_PREV_LOG_TIME")
    private Date prevLogTime;

    /**
     * 前次登录IP
     */
    @TableField("F_PREV_LOG_IP")
    private String prevLogIp;

    /**
     * 最后登录时间
     */
    @TableField("F_LAST_LOG_TIME")
    private Date lastLogTime;

    /**
     * 最后登录IP
     */
    @TableField("F_LAST_LOG_IP")
    private String lastLogIp;

    /**
     * 登录成功次数
     */
    @TableField("F_LOG_SUCCESS_COUNT")
    private Integer logSuccessCount;

    /**
     * 登录错误次数
     */
    @TableField("F_LOG_ERROR_COUNT")
    private Integer logErrorCount;

    /**
     * 最后修改密码时间
     */
    @TableField("F_CHANGE_PASSWORD_DATE")
    private Date changePasswordDate;

    /**
     * 系统语言
     */
    @TableField("F_LANGUAGE")
    private String language;

    /**
     * 系统样式
     */
    @TableField("F_THEME")
    private String theme;

    /**
     * 是否管理员
     */
    @TableField("F_IS_ADMINISTRATOR")
    private Integer isAdministrator;

    /**
     * 主管主键
     */
    @TableField("F_MANAGER_ID")
    private String managerId;

    /**
     * 组织主键
     */
    @TableField("F_ORGANIZE_ID")
    private String organizeId;

    /**
     * 岗位主键
     */
    @TableField("F_POSITION_ID")
    private String positionId;

    /**
     * 角色主键
     */
    @TableField("F_ROLE_ID")
    private String roleId;

    /**
     * 门户主键
     */
    @TableField("F_PORTAL_ID")
    private String portalId;

    /**
     * 是否锁定
     */
    @TableField("F_LOCK_MARK")
    private Integer lockMark;

    /**
     * 解锁时间
     */
    @TableField(value = "F_UNLOCK_TIME",updateStrategy = FieldStrategy.ALWAYS)
    private Date unlockTime;

    /**
     * 分组id
     */
    @TableField("F_GROUP_ID")
    private String groupId;

    /**
     * 交接状态
     */
    @TableField("f_handover_mark")
    private Integer handoverMark;

    /**
     * 职级
     */
    @TableField("f_rank")
    private String ranks;

    /**
     * 身份
     */
    @TableField("F_STANDING")
    private String standing;

    /**
     * 身份
     */
    @TableField("F_APP_STANDING")
    private String appStanding;

    /**
     * App系统id
     */
    @TableField("F_APP_SYSTEM_ID")
    private String appSystemId;

}
