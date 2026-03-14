package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("base_print_log")
public class PrintLogEntity extends SuperExtendEntity<String> {

    /**
     * 打印条数
     */
    @TableField("F_PRINT_NUM")
    private Integer printNum;

    /**
     * 打印功能名称
     */
    @TableField("F_PRINT_TITLE")
    private String printTitle;

    /**
     * 模板id
     */
    @TableField("F_PRINT_ID")
    private String printId;

}
