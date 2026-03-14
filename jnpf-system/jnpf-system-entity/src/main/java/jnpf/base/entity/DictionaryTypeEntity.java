package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_dictionary_type")
public class DictionaryTypeEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 上级
     */
    @TableField("f_parent_id")
    private String parentId;

    /**
     * 名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 树形
     */
    @TableField("f_is_tree")
    private Integer isTree;

    /**
     * 类型
     */
    @TableField("f_type")
    private Integer category;

}
