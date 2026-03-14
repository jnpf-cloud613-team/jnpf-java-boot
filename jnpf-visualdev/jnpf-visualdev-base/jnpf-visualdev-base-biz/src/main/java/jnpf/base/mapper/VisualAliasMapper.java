package jnpf.base.mapper;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.VisualAliasEntity;
import jnpf.base.model.VisualAliasForm;
import jnpf.constant.GenerateConstant;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.model.visualjson.TableFields;
import jnpf.model.visualjson.TableModel;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/13 14:03:56
 */
public interface VisualAliasMapper extends SuperMapper<VisualAliasEntity> {


    default List<VisualAliasEntity> getList(String visualId) {
        QueryWrapper<VisualAliasEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualAliasEntity::getVisualId, visualId);
        return this.selectList(queryWrapper);
    }

    default void aliasSave(String id, VisualAliasForm form) {
        List<TableModel> tableList = form.getTableList();
        List<VisualAliasEntity> list = this.getList(id);
        List<String> tableNameList = new ArrayList<>();
        List<String> listKeyword = GenerateConstant.getAllKeyWord();
        String regex = "[a-zA-Z_]\\w*";
        for (TableModel tableModel : tableList) {
            saveOrUpdateTableMode(id, tableModel, list, tableNameList, listKeyword, regex);

            List<String> fieldNameList = new ArrayList<>();
            for (TableFields field : tableModel.getFields()) {
                VisualAliasEntity fieldAlias = list.stream().filter(t -> tableModel.getTable().equals(t.getTableName()) && t.getFieldName() != null
                        && field.getField().equals(t.getFieldName())).findFirst().orElse(null);
                if (StringUtil.isNotEmpty(field.getAliasName())) {
                    if (listKeyword.contains(field.getAliasName().toLowerCase())) {
                        throw new DataException(MsgCode.SYS128.get(field.getField() + "-" + field.getAliasName()));
                    }
                    if (!field.getAliasName().matches(regex)) {
                        throw new DataException(MsgCode.VS021.get(field.getField()));
                    }
                    if (twoCharactesUpperCase(field.getAliasName())) {
                        throw new DataException(MsgCode.VS026.get());
                    }
                    if (fieldNameList.contains(field.getAliasName())) {
                        throw new DataException(field.getAliasName() + MsgCode.VS020.get());
                    } else {
                        fieldNameList.add(field.getAliasName());
                    }
                    if (fieldAlias != null) {
                        fieldAlias.setAliasName(field.getAliasName());
                    } else {
                        fieldAlias = new VisualAliasEntity();
                        fieldAlias.setId(RandomUtil.uuId());
                        fieldAlias.setVisualId(id);
                        fieldAlias.setTableName(tableModel.getTable());
                        fieldAlias.setFieldName(field.getField());
                        fieldAlias.setAliasName(field.getAliasName());
                    }
                    this.insertOrUpdate(fieldAlias);
                } else if (fieldAlias != null) {
                    this.deleteById(fieldAlias);
                }
            }
        }
    }

    //保存或者修改表数据
    default void saveOrUpdateTableMode(String id, TableModel tableModel, List<VisualAliasEntity> list, List<String> tableNameList, List<String> listKeyword, String regex) {
        VisualAliasEntity tableAlias = list.stream().filter(t -> tableModel.getTable().equals(t.getTableName()) && t.getFieldName() == null).findFirst().orElse(null);
        if (StringUtil.isNotEmpty(tableModel.getAliasName())) {
            if (tableNameList.contains(tableModel.getAliasName())) {
                throw new DataException(tableModel.getAliasName() + MsgCode.VS018.get());
            } else {
                tableNameList.add(tableModel.getAliasName());
            }

            if (listKeyword.contains(tableModel.getAliasName().toLowerCase())) {
                throw new DataException(MsgCode.SYS128.get(tableModel.getTable() + "-" + tableModel.getAliasName()));
            }

            if (!tableModel.getAliasName().matches(regex)) {
                throw new DataException(MsgCode.VS021.get(tableModel.getTable()));
            }
            if (twoCharactesUpperCase(tableModel.getAliasName())) {
                throw new DataException(MsgCode.VS026.get());
            }
            if (tableAlias != null) {
                tableAlias.setAliasName(tableModel.getAliasName());
            } else {
                tableAlias = new VisualAliasEntity();
                tableAlias.setId(RandomUtil.uuId());
                tableAlias.setVisualId(id);
                tableAlias.setTableName(tableModel.getTable());
                tableAlias.setAliasName(tableModel.getAliasName());
            }
            this.insertOrUpdate(tableAlias);
        } else if (tableAlias != null) {
            this.deleteById(tableAlias);
        }
    }

    default void copy(String visualId, String uuid) {
        List<VisualAliasEntity> list = this.getList(visualId);
        if (CollectionUtils.isNotEmpty(list)) {
            for (VisualAliasEntity entity : list) {
                VisualAliasEntity copy = BeanUtil.copyProperties(entity, VisualAliasEntity.class);
                copyEntity(copy, uuid);
            }
        }
    }

    default void copyEntity(VisualAliasEntity copy, String visualId) {
        copy.setId(RandomUtil.uuId());
        copy.setVisualId(visualId);
        copy.setCreatorTime(new Date());
        copy.setCreatorUserId(UserProvider.getUser().getUserId());
        copy.setLastModifyUserId(null);
        copy.setLastModifyTime(null);
        this.insert(copy);
    }

    default void removeByVisualId(String visualId) {
        QueryWrapper<VisualAliasEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualAliasEntity::getVisualId, visualId);
        this.delete(queryWrapper);
    }

    /**
     * 判断前两字母是否大写
     *
     * @return
     */
    default boolean twoCharactesUpperCase(String name) {
        if (StringUtil.isEmpty(name)) {
            return false;
        }
        if (name.length() == 1) {
            return Character.isUpperCase(name.charAt(0));
        }
        if (name.length() >= 2) {
            return Character.isUpperCase(name.charAt(0)) || Character.isUpperCase(name.charAt(1));
        }
        return false;
    }
}
