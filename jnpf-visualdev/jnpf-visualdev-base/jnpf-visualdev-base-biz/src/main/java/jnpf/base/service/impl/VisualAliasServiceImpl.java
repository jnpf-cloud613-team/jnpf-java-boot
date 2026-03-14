package jnpf.base.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.google.common.base.CaseFormat;
import jnpf.base.entity.VisualAliasEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.mapper.VisualAliasMapper;
import jnpf.base.mapper.VisualdevMapper;
import jnpf.base.mapper.VisualdevReleaseMapper;
import jnpf.base.model.VisualAliasForm;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualAliasService;
import jnpf.base.util.common.AliasModel;
import jnpf.constant.GenerateConstant;
import jnpf.model.visualjson.TableFields;
import jnpf.model.visualjson.TableModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/13 14:05:48
 */
@Service
@RequiredArgsConstructor
public class VisualAliasServiceImpl extends SuperServiceImpl<VisualAliasMapper, VisualAliasEntity> implements VisualAliasService {

    private final VisualdevReleaseMapper visualdevReleaseMapper;
    private final VisualdevMapper visualdevMapper;


    @Override
    public List<VisualAliasEntity> getList(String visualId) {
        return this.baseMapper.getList(visualId);
    }

    @Override
    public List<TableModel> getAliasInfo(String id) {
        VisualdevReleaseEntity visualdevEntity = visualdevReleaseMapper.selectById(id);
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        List<VisualAliasEntity> list = this.getList(id);
        for (TableModel tableModel : tableModels) {
            tableModel.setComment(tableModel.getTableName());
            VisualAliasEntity tableAlias = list.stream().filter(t -> tableModel.getTable().equals(t.getTableName()) && t.getFieldName() == null).findFirst().orElse(null);
            if (tableAlias != null) {
                tableModel.setAliasName(tableAlias.getAliasName());
            }
            List<TableFields> fields = tableModel.getFields();
            List<TableFields> newFields = fields.stream().filter(t -> !GenerateConstant.getSysKeyWord().contains(t.getField().toLowerCase())).collect(Collectors.toList());
            for (TableFields field : newFields) {
                VisualAliasEntity fieldAlias = list.stream().filter(t -> tableModel.getTable().equals(t.getTableName()) && t.getFieldName() != null
                        && field.getField().equals(t.getFieldName())).findFirst().orElse(null);
                if (fieldAlias != null) {
                    field.setAliasName(fieldAlias.getAliasName());
                }
            }
            tableModel.setFields(newFields);
        }
        return tableModels;
    }

    @DSTransactional
    @Override
    public void aliasSave(String id, VisualAliasForm form) {
        this.baseMapper.aliasSave(id, form);
    }

    @Override
    public Map<String, AliasModel> getAllFiledsAlias(String id) {
        Map<String, AliasModel> tableMap = new HashMap<>();
        VisualdevReleaseEntity visualdevEntity = visualdevReleaseMapper.selectById(id);
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        List<String> collect = tableModels.stream().map(TableModel::getTable).collect(Collectors.toList());
        //修改时有表不存在发布里补充
        VisualdevEntity ve = visualdevMapper.selectById(id);
        List<TableModel> tableModels2 = StringUtil.isNotEmpty(ve.getVisualTables()) ? JsonUtil.getJsonToList(ve.getVisualTables(), TableModel.class) : Collections.emptyList();
        tableModels.addAll(tableModels2.stream().filter(t -> !collect.contains(t.getTable())).collect(Collectors.toList()));
        List<VisualAliasEntity> list = this.getList(id);
        for (TableModel tableModel : tableModels) {
            AliasModel aliasModel = new AliasModel();
            aliasModel.setTableName(tableModel.getTable());
            VisualAliasEntity tableAlias = list.stream().filter(t -> tableModel.getTable().equals(t.getTableName()) && t.getFieldName() == null).findFirst().orElse(null);
            if (tableAlias != null) {
                aliasModel.setAliasName(tableAlias.getAliasName());
            } else {
                aliasModel.setAliasName(tableModel.getTable());
            }
            List<TableFields> fields = tableModel.getFields();
            Map<String, String> fieldMap = new HashMap<>();
            for (TableFields field : fields) {
                VisualAliasEntity fieldAlias = list.stream().filter(t -> tableModel.getTable().equals(t.getTableName()) && t.getFieldName() != null
                        && field.getField().equals(t.getFieldName())).findFirst().orElse(null);
                if (fieldAlias != null) {
                    fieldMap.put(field.getField(), fieldAlias.getAliasName());
                }
                //系统字段-以驼峰形式给别名
                else if (GenerateConstant.getSysKeyWord().contains(field.getField().toLowerCase())) {
                    String name = field.getField().toLowerCase();
                    name = name.startsWith("f_") ? name.substring(2) : name;
                    String fieldAliasName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
                    fieldMap.put(field.getField(), fieldAliasName);
                } else {
                    String name = field.getField();
                    String fieldAliasName = twoToLowerCase(name);
                    fieldMap.put(field.getField(), fieldAliasName);
                }
            }
            aliasModel.setFieldsMap(fieldMap);
            tableMap.put(tableModel.getTable(), aliasModel);
        }
        return tableMap;
    }

    @Override
    public void copy(String visualId, String uuid) {
        this.baseMapper.copy(visualId, uuid);
    }

    @Override
    public void copyEntity(VisualAliasEntity copy, String visualId) {
        this.baseMapper.copyEntity(copy, visualId);
    }

    @Override
    public void removeByVisualId(String visualId) {
        this.baseMapper.removeByVisualId(visualId);
    }

    /**
     * 两字母强制小写前
     *
     * @return
     */
    public String twoToLowerCase(String name) {
        if (StringUtil.isEmpty(name)) {
            return name;
        }
        name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
        if (name.length() <= 2) {
            return name.toLowerCase();
        }
        if (name.length() > 2) {
            return name.substring(0, 2).toLowerCase() + name.substring(2);
        }
        return name;
    }
}
