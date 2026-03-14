package jnpf.base.model.dbtable.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbtable.DbTableFieldModel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@NoArgsConstructor
public class DbTableInfoVO {

    @Schema(description = "表信息")
    private DbTableVO tableInfo;
    @Schema(description = "字段信息集合")
    private List<DbFieldVO> tableFieldList;
    @Schema(description = "表是否存在信息")
    private Boolean hasTableData;

    public DbTableInfoVO(DbTableFieldModel dbTableModel, List<DbFieldModel> dbFieldModelList){
        if(dbTableModel != null){
            List<DbFieldVO> list = new ArrayList<>();
            for (DbFieldModel dbFieldModel : dbFieldModelList) {
                list.add(new DbFieldVO(dbFieldModel));
            }
            this.tableFieldList = list;
            this.tableInfo = new DbTableVO(dbTableModel);
            this.hasTableData = dbTableModel.getHasTableData();
        }
    }

}
