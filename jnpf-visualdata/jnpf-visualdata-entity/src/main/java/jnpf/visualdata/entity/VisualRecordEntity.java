package jnpf.visualdata.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 大屏数据集
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Data
@TableName("blade_visual_record")
public class VisualRecordEntity {

    /** 主键 */
    @TableId("ID")
    private String id;

    /** 名称 */
    @TableField("name")
    private String name;

    /** 请求地址 */
    @TableField("url")
    private String url;

    /** 数据集类型 */
    @TableField("dataType")
    private Integer dataType;

    /** 请求方法 */
    @TableField("dataMethod")
    private String dataMethod;

    /** 数据集类型 */
    @TableField("dataHeader")
    private String dataHeader;

    /** 请求数据 */
    @TableField("data")
    private String data;

    /** 请求参数 */
    @TableField("dataQuery")
    private String dataQuery;

    /** 请求参数类型 */
    @TableField("dataQueryType")
    private String dataQueryType;

    /** 过滤器 */
    @TableField("dataFormatter")
    private String dataFormatter;

    /** 开启跨域 */
    @TableField("proxy")
    private Integer proxy;

    /** WebSocket地址 */
    @TableField("wsUrl")
    private String wsUrl;

    /** 数据集类型 */
    @TableField("dbsql")
    private String dbsql;

    /** 数据集类型 */
    @TableField("fsql")
    @JSONField(name = "sql")
    private String fsql;

    /** 数据集类型 */
    @TableField("result")
    private String result;

    /**
     * MTQQ 连接地址
     */
    @TableField("mqttUrl")
    private String mqtturl;

    /**
     * MQTT 配置
     */
    @TableField("mqttConfig")
    private String mqttConfig;

    @TableField("SYSTEM_ID")
    private String systemId;
    /**
     * 租户id
     */
    @TableField(value = "f_tenant_id" , fill = FieldFill.INSERT)
    private String tenantId;


}
