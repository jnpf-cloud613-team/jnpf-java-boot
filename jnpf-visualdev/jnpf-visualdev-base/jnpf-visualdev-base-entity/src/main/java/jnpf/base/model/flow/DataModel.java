package jnpf.base.model.flow;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.UserInfo;
import jnpf.base.model.VisualLogModel;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.permission.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据模型")
public class DataModel {
    @Schema(description = "功能id")
    private String visualId;
    @Schema(description = "名称")
    private Map<String, Object> dataNewMap;
    @Schema(description = "字段列表")
    private List<FieLdsModel> fieLdsModelList;
    @Schema(description = "表列表")
    private List<TableModel> tableModelList;
    @Schema(description = "解析后字段")
    private List<FormAllModel> formAllModel;
    @Schema(description = "主表id")
    private String mainId;
    @Schema(description = "数据库链接")
    private DbLinkEntity link;
    @Schema(description = "转换")
    private Boolean convert;
    @Schema(description = "数据库类型")
    private String dbType;
    @Schema(description = "用户信息")
    private UserEntity userEntity;
    //是否开启安全锁
    @Schema(description = "安全锁策略")
    @Builder.Default
    private Boolean concurrencyLock = false;
    @Schema(description = "逻辑删除")
    @Builder.Default
    private Boolean logicalDelete = false;
    @Schema(description = "主键策略")
    private Integer primaryKeyPolicy = 1;
    @Schema(description = "用户信息")
    private UserInfo userInfo;
    @Schema(description = "是否外链")
    @Builder.Default
    private boolean linkOpen = false;

    @Schema(description = "流程表单权限")
    private List<Map<String, Object>> flowFormOperates;

    private List<VisualLogModel> listLog;

    @Schema(description = "子表仅修改")
    @Builder.Default
    private Boolean onlyUpdate = false;

    @Schema(description = "旧的主表数据")
    private Map<String, Object> oldMainData;


    @Schema(description = "需要判断权限")
    @Builder.Default
    private Boolean needPermission = false;
    @Schema(description = "表单权限")
    private List<String> formPerList;

}
