package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 *
 * Contract
 * 版本： V3.0.0
 * 版权： 引迈信息技术有限公司(https://www.jnpfsoft.com)
 * 作者： JNPF开发平台组
 * 日期： 2020-12-31
 */
@Data
@TableName("test_contract")
@EqualsAndHashCode(callSuper = true)
public class ContractEntity extends SuperEntity<String> {

    @TableField("f_contractname")
    private String contractName;

    @TableField("f_mytelephone")
    private String mytelePhone;

    @TableField("f_filejson")
    private String fileJson;
}
