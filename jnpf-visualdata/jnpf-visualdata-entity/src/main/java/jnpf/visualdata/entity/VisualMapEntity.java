package jnpf.visualdata.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 大屏地图配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Data
@TableName("blade_visual_map")
public class VisualMapEntity {
    /** 主键 */
    @TableId("id")
    private String id;

    /** 地图名称 */
    @TableField("name")
    private String name;

    /** 地图数据 */
    @TableField("data")
    private String data;

    /** 地图编码 */
    @TableField("code")
    private String code;

    /** 地图级别 0:国家 1:省份 2:城市 3:区县 */
    @TableField("map_level")
    @JSONField(name = "level")
    private Integer mapLevel;

    /** 上级ID */
    @TableField("parent_id")
    private String parentId;

    /** 上级编码 */
    @TableField("parent_code")
    private String parentCode;

    /** 祖编码 */
    @TableField("ancestors")
    private String ancestors;

    /**
     * 租户id
     */
    @TableField(value = "f_tenant_id" , fill = FieldFill.INSERT)
    private String tenantId;

}
