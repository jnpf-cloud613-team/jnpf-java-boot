package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 行政区划-地图
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18m
 */
@Data
@TableName("base_province_atlas")
public class ProvinceAtlasEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 区域上级
     */
    @TableField("F_PARENT_ID")
    private String parentId;

    /**
     * 区域编号
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 区域名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 快速查询
     */
    @TableField("F_QUICK_QUERY")
    private String quickQuery;

    /**
     * 区域类型
     */
    @TableField("F_TYPE")
    private String type;

    /**
     * 行政区划编码
     */
    @TableField("F_DIVISION_CODE")
    private String divisionCode;

    /**
     * 中心经纬度
     */
    @TableField("F_ATLAS_CENTER")
    private String atlasCenter;
}
