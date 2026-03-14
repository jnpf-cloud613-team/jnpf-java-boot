package jnpf.base.model.dbtable.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.database.model.dbtable.DbTableFieldModel;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@NoArgsConstructor
public class DbTableVO {

    @NotBlank(message = "必填")
    @Schema(description = "表名")
    private String table;
    @NotBlank(message = "必填")
    @Schema(description = "表注释")
    private String tableName;

    public DbTableVO(DbTableFieldModel dbTableFieldModel){
        this.table = dbTableFieldModel.getTable();
        this.tableName = dbTableFieldModel.getComment();
    }

}
