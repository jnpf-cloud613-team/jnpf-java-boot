package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_sys_log")
public class LogEntity extends SuperExtendEntity.SuperExtendDescriptionEntity<String> implements Serializable {
    /**
     * 用户主键
     */
    @TableField("F_USER_ID")
    private String userId;

    /**
     * 用户主键
     */
    @TableField("F_USER_NAME")
    private String userName;

    /**
     * 日志类型
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 日志级别
     */
    @TableField("F_LEVEL")
    private Integer levels;

    /**
     * IP地址
     */
    @TableField("F_IP_ADDRESS")
    private String ipAddress;

    /**
     * IP所在城市
     */
    @TableField("F_IP_ADDRESS_NAME")
    private String ipAddressName;

    /**
     * 请求地址
     */
    @TableField("F_REQUEST_URL")
    private String requestUrl;

    /**
     * 请求方法
     */
    @TableField("F_REQUEST_METHOD")
    private String requestMethod;

    /**
     * 请求耗时
     */
    @TableField("F_REQUEST_DURATION")
    private Integer requestDuration;

    /**
     * 日志内容
     */
    @TableField("F_JSON")
    private String jsons;

    /**
     * 平台设备
     */
    @TableField("F_PLAT_FORM")
    private String platForm;

    /**
     * 功能主键
     */
    @TableField("F_MODULE_ID")
    private String moduleId;

    /**
     * 功能名称
     */
    @TableField("F_MODULE_NAME")
    private String moduleName;

    /**
     * 对象Id
     */
    @TableField("F_OBJECT_ID")
    private String objectId;

    /**
     * 浏览器
     */
    @TableField("f_browser")
    private String browser;

    /**
     * 请求参数
     */
    @TableField("f_request_param")
    private String requestParam;

    /**
     * 请求方法
     */
    @TableField("f_request_target")
    private String requestTarget;

    /**
     * 是否登录成功标志
     */
    @TableField("f_login_mark")
    private Integer loginMark;

    /**
     * 登录类型
     */
    @TableField("f_login_type")
    private Integer loginType;

}
