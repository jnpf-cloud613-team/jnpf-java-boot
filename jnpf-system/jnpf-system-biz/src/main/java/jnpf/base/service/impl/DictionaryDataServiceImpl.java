package jnpf.base.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.DictionaryTypeEntity;
import jnpf.base.mapper.DictionaryDataMapper;
import jnpf.base.mapper.DictionaryTypeMapper;
import jnpf.base.model.dictionarydata.DictionaryDataExportModel;
import jnpf.base.model.dictionarytype.DictionaryExportModel;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.vo.DownloadVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.util.FileExport;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class DictionaryDataServiceImpl extends SuperServiceImpl<DictionaryDataMapper, DictionaryDataEntity> implements DictionaryDataService {


    private final DictionaryTypeMapper dictionaryTypeMapper;

    private final FileExport fileExport;

    @Override
    public List<DictionaryDataEntity> getList(String dictionaryTypeId, Boolean enable) {
        return this.baseMapper.getList(dictionaryTypeId, enable);
    }

    @Override
    public List<DictionaryDataEntity> getList(String dictionaryTypeId) {
        return this.baseMapper.getList(dictionaryTypeId);
    }

    @Override
    public List<DictionaryDataEntity> getDicList(String dictionaryTypeId) {
        return this.baseMapper.getDicList(dictionaryTypeId);
    }

    @Override
    public List<DictionaryDataEntity> geDicList(String dictionaryTypeId) {
        return this.baseMapper.geDicList(dictionaryTypeId);
    }

    @Override
    public Boolean isExistSubset(String parentId) {
        return this.baseMapper.isExistSubset(parentId);
    }

    @Override
    public DictionaryDataEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public DictionaryDataEntity getSwapInfo(String value, String dictionaryTypeId) {
        return this.baseMapper.getSwapInfo(value, dictionaryTypeId);
    }

    @Override
    public boolean isExistByFullName(String dictionaryTypeId, String fullName, String id) {
        return this.baseMapper.isExistByFullName(dictionaryTypeId, fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String dictionaryTypeId, String enCode, String id) {
        return this.baseMapper.isExistByEnCode(dictionaryTypeId, enCode, id);
    }

    @Override
    public void delete(DictionaryDataEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public void create(DictionaryDataEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, DictionaryDataEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public boolean first(String id) {
        return this.baseMapper.first(id);
    }

    @Override
    public boolean next(String id) {
        return this.baseMapper.next(id);
    }

    @Override
    public List<DictionaryDataEntity> getDictionName(List<String> id) {
        return this.baseMapper.getDictionName(id);
    }

    @Override
    public DownloadVO exportData(String id) {
        //获取数据分类字段详情
        DictionaryTypeEntity typeEntity = dictionaryTypeMapper.getInfo(id);
        if (typeEntity == null) {
            throw new DataException(MsgCode.FA001.get());
        }
        DictionaryExportModel exportModel = new DictionaryExportModel();
        //递归子分类
        List<DictionaryTypeEntity> typeList = new ArrayList<>();
        List<DictionaryTypeEntity> typeEntityList = dictionaryTypeMapper.getList();
        typeList.add(typeEntity);
        getDictionaryTypeEntitySet(typeEntity, typeList, typeEntityList);
        List<DictionaryTypeEntity> collect = typeList.stream().distinct().collect(Collectors.toList());
        //判断是否有子分类
        if (!collect.isEmpty()) {
            exportModel.setList(collect);
        }
        //获取该类型下的数据
        List<DictionaryDataExportModel> modelList = new ArrayList<>();
        for (DictionaryTypeEntity dictionaryTypeEntity : exportModel.getList()) {
            List<DictionaryDataEntity> entityList = getList(dictionaryTypeEntity.getId());
            for (DictionaryDataEntity dictionaryDataEntity : entityList) {
                DictionaryDataExportModel dataExportModel = JsonUtil.getJsonToBean(dictionaryDataEntity, DictionaryDataExportModel.class);
                modelList.add(dataExportModel);
            }
        }
        exportModel.setModelList(modelList);
        //导出文件
        return fileExport.exportFile(exportModel, FileTypeConstant.TEMPORARY, typeEntity.getFullName(), ModuleTypeEnum.SYSTEM_DICTIONARYDATA.getTableName());
    }

    /**
     * 递归字典分类
     *
     * @param dictionaryTypeEntity 数据字典类型实体
     */
    private void getDictionaryTypeEntitySet(DictionaryTypeEntity dictionaryTypeEntity, List<DictionaryTypeEntity> set, List<DictionaryTypeEntity> typeEntityList) {
        //是否含有子分类
        List<DictionaryTypeEntity> collect = typeEntityList.stream().filter(t -> dictionaryTypeEntity.getId().equals(t.getParentId())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            for (DictionaryTypeEntity typeEntity : collect) {
                set.add(typeEntity);
                getDictionaryTypeEntitySet(typeEntity, set, typeEntityList);
            }
        }
    }

    @Override
    @DSTransactional
    public ActionResult<Object> importData(DictionaryExportModel exportModel, Integer type) throws DataException {
        try {
            StringBuilder message = new StringBuilder();
            StringJoiner exceptionMessage = new StringJoiner("、");
            List<DictionaryTypeEntity> list = JsonUtil.getJsonToList(exportModel.getList(), DictionaryTypeEntity.class);
            List<DictionaryDataEntity> entityList = JsonUtil.getJsonToList(exportModel.getModelList(), DictionaryDataEntity.class);
            //遍历插入分类
            StringJoiner idmessage = new StringJoiner("、");
            StringJoiner fullNameMessage = new StringJoiner("、");
            StringJoiner enCodeMessage = new StringJoiner("、");
            Map<String, String> idFor = new HashMap<>();
            for (DictionaryTypeEntity entity : list) {
                String copyNum = UUID.randomUUID().toString().substring(0, 5);
                if (dictionaryTypeMapper.getInfo(entity.getId()) != null) {
                    idmessage.add(entity.getId());
                }
                if (dictionaryTypeMapper.isExistByFullName(entity.getFullName(), null)) {
                    fullNameMessage.add(entity.getFullName());
                }
                if (dictionaryTypeMapper.isExistByEnCode(entity.getEnCode(), null)) {
                    enCodeMessage.add(entity.getEnCode());
                }
                if ((idmessage.length() > 0 || fullNameMessage.length() > 0 || enCodeMessage.length() > 0)) {
                    if (ObjectUtil.equal(type, 1)) {
                        entity.setFullName(entity.getFullName() + ".副本" + copyNum);
                        entity.setEnCode(entity.getEnCode() + copyNum);
                        String oldId = entity.getId();
                        entity.setId(RandomUtil.uuId());
                        dictionaryTypeMapper.setIgnoreLogicDelete().deleteById(entity);
                        if (Optional.ofNullable(idFor.get(entity.getParentId())).isPresent()) {
                            entity.setParentId(idFor.get(entity.getParentId()));
                        }
                        dictionaryTypeMapper.setIgnoreLogicDelete().insertOrUpdate(entity);
                        idFor.put(oldId, entity.getId());
                    }
                } else {
                    dictionaryTypeMapper.setIgnoreLogicDelete().deleteById(entity);
                    dictionaryTypeMapper.setIgnoreLogicDelete().insertOrUpdate(entity);
                }
            }
            if (idmessage.length() > 0) {
                exceptionMessage.add("ID（" + idmessage.toString() + "）重复");
            }
            if (enCodeMessage.length() > 0) {
                exceptionMessage.add("编码（" + idmessage.toString() + "）重复");
            }
            if (fullNameMessage.length() > 0) {
                exceptionMessage.add("名称（" + idmessage.toString() + "）重复");
            }
            if (exceptionMessage.length() > 0) {
                message.append(exceptionMessage.toString()).append("；");
                exceptionMessage = new StringJoiner("、");
                idmessage = new StringJoiner("、");
                fullNameMessage = new StringJoiner("、");
                enCodeMessage = new StringJoiner("、");
            }
            for (DictionaryDataEntity entity1 : entityList) {
                String copyNum = UUID.randomUUID().toString().substring(0, 5);
                if (this.getInfo(entity1.getId()) != null) {
                    idmessage.add(entity1.getId());
                }
                if (this.isExistByFullName(entity1.getDictionaryTypeId(), entity1.getFullName(), null)) {
                    fullNameMessage.add(entity1.getFullName());
                }
                if (this.isExistByEnCode(entity1.getDictionaryTypeId(), entity1.getEnCode(), null)) {
                    enCodeMessage.add(entity1.getEnCode());
                }
                if (ObjectUtil.equal(type, 1)) {
                    entity1.setId(RandomUtil.uuId());
                    if (Optional.ofNullable(idFor.get(entity1.getDictionaryTypeId())).isPresent()) {
                        entity1.setDictionaryTypeId(idFor.get(entity1.getDictionaryTypeId()));
                    }
                    if (this.isExistByFullName(entity1.getDictionaryTypeId(), entity1.getFullName(), null)
                            || this.isExistByEnCode(entity1.getDictionaryTypeId(), entity1.getEnCode(), null)) {
                        entity1.setFullName(entity1.getFullName() + ".副本" + copyNum);
                        entity1.setEnCode(entity1.getEnCode() + copyNum);
                    }
                    this.setIgnoreLogicDelete().saveOrUpdate(entity1);
                } else if (idmessage.length() == 0 && fullNameMessage.length() == 0 && enCodeMessage.length() == 0) {
                    this.setIgnoreLogicDelete().removeById(entity1);
                    this.setIgnoreLogicDelete().saveOrUpdate(entity1);
                }
            }
            if (idmessage.length() > 0) {
                exceptionMessage.add("ID（" + idmessage.toString() + "）重复");
            }
            if (enCodeMessage.length() > 0) {
                exceptionMessage.add("编码（" + enCodeMessage.toString() + "）重复");
            }
            if (fullNameMessage.length() > 0) {
                exceptionMessage.add("名称（" + fullNameMessage.toString() + "）重复");
            }
            if (exceptionMessage.length() > 0) {
                message.append("modelList：" + exceptionMessage.toString() + "；");
            }
            if (ObjectUtil.equal(type, 0) && message.length() > 0) {
                return ActionResult.fail(message.toString().substring(0, message.lastIndexOf("；")));
            }
            return ActionResult.success(MsgCode.IMP001.get());
        } catch (Exception e) {
            //手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new DataException(e.getMessage());
        } finally {
            this.clearIgnoreLogicDelete();
            dictionaryTypeMapper.clearIgnoreLogicDelete();
            this.clearIgnoreLogicDelete();
        }
    }

    @Override
    public List<DictionaryDataEntity> getListByTypeDataCode(String typeCode) {
        return this.baseMapper.getListByTypeDataCode(typeCode);
    }

    @Override
    public List<DictionaryDataEntity> getByTypeCodeEnable(String typeCode) {
        return this.baseMapper.getByTypeCodeEnable(typeCode);
    }
}
