package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_dictionary_data")
public class DictionaryDataEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

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
     * 拼音
     */
    @TableField("f_simple_spelling")
    private String simpleSpelling;

    /**
     * 默认
     */
    @TableField("f_is_default")
    private Integer isDefault;

    /**
     * 类别主键
     */
    @TableField("f_dictionary_type_id")
    private String dictionaryTypeId;

}
