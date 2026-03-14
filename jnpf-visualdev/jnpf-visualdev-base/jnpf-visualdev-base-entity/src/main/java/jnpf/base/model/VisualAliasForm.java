package jnpf.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.visualjson.TableModel;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "命名规范表单")
public class VisualAliasForm {
    List<TableModel> tableList;
}
