package jnpf.base.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import jnpf.base.ActionResult;
import jnpf.base.entity.*;
import jnpf.base.mapper.*;
import jnpf.base.model.module.*;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.ModuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.vo.DownloadVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.CodeConst;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.database.util.DbTypeUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.model.tenant.TenantAuthorizeModel;
import jnpf.permission.service.CodeNumService;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统功能
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class ModuleServiceImpl extends SuperServiceImpl<ModuleMapper, ModuleEntity> implements ModuleService {
    
    private final FileExport fileExport;
    
    private final  ConfigValueUtil configValueUtil;
    
    private  final CodeNumService codeNumService;
    
    private final  DbLinkService dbLinkService;

    
    private final  ModuleDataAuthorizeMapper moduleDataAuthorizeMapper;
    
    private final  ModuleButtonMapper buttonMapper;
    
    private final  ModuleColumnMapper columnMapper;
    
    private final  ModuleFormMapper formMapper;
    
    private final  ModuleDataAuthorizeSchemeMapper schemeMapper;
    
    private final  ModuleDataAuthorizeMapper authorizeMapper;
    
    private final  SystemMapper systemMapper;

    private final ModuleDataMapper moduleDataMapper;

    @Override
    public List<ModuleEntity> getList(MenuListModel param) {
        String appCode = param.getAppCode();
        SystemEntity systemEntity = systemMapper.getInfoByEnCode(param.getAppCode());
        String systemId = systemEntity.getId();
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<ModuleEntity> queryWrapper = new QueryWrapper<>();
        if (JnpfConst.MAIN_SYSTEM_CODE.equals(appCode) && param.isRelease()) {
            queryWrapper.lambda().eq(ModuleEntity::getCategory, JnpfConst.WEB);
            queryWrapper.lambda().eq(ModuleEntity::getSystemId, systemId);
            queryWrapper.lambda().select(ModuleEntity::getId);
            List<String> workModuleIds = this.list(queryWrapper).stream().map(ModuleEntity::getId).collect(Collectors.toList());
            // 重新定义一个查询对象
            queryWrapper = new QueryWrapper<>();
            if (!workModuleIds.isEmpty()) {
                queryWrapper.lambda().notIn(ModuleEntity::getId, workModuleIds);
            }
        }

        List<String> collect;
        // 根据系统id获取功能
        if (!"0".equals(systemId)) {
            collect = param.getModuleList().stream().filter(t -> t.getSystemId().equals(systemId)).map(ModuleModel::getId).collect(Collectors.toList());
        } else {
            collect = param.getModuleList().stream().map(ModuleModel::getId).distinct().collect(Collectors.toList());
        }
        collect.add("");
        List<List<String>> lists = Lists.partition(collect, 1000);
        queryWrapper.lambda().and(t -> {
            for (List<String> list : lists) {
                t.in(ModuleEntity::getId, list).or();
            }
        });
        if (!StringUtil.isEmpty(param.getCategory())) {
            queryWrapper.lambda().eq(ModuleEntity::getCategory, param.getCategory());
        }
        if (!StringUtil.isEmpty(param.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(t -> t.like(ModuleEntity::getFullName, param.getKeyword())
                    .or().like(ModuleEntity::getEnCode, param.getKeyword())
                    .or().like(ModuleEntity::getUrlAddress, param.getKeyword())
            );
        }
        if (param.getType() != null) {
            flag = true;
            queryWrapper.lambda().eq(ModuleEntity::getType, param.getType());
        }
        if (param.getEnabledMark() != null) {
            flag = true;
            queryWrapper.lambda().eq(ModuleEntity::getEnabledMark, param.getEnabledMark());
        }
        if (StringUtil.isNotEmpty(param.getParentId())) {
            queryWrapper.lambda().eq(ModuleEntity::getParentId, param.getParentId());
        }
        queryWrapper.lambda().orderByAsc(ModuleEntity::getSortCode)
                .orderByDesc(ModuleEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(ModuleEntity::getLastModifyTime);
        }
        // 移除工作流程菜单
        List<String> moduleCode = new ArrayList<>();
        moduleCode.addAll(JnpfConst.MODULE_CODE);
        moduleCode.addAll(JnpfConst.TEAMWORK_MODULE);
        queryWrapper.lambda().notIn(ModuleEntity::getEnCode, moduleCode);
        return this.list(queryWrapper);
    }

    @DSTransactional
    @Override
    public void create(ModuleEntity entity) {
        this.setAutoEnCode(entity);
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ModuleEntity entity) {
        entity.setId(id);
        this.setAutoEnCode(entity);
        entity.setLastModifyTime(DateUtil.getNowDate());
        return this.updateById(entity);
    }

    @DSTransactional
    @Override
    public void delete(ModuleEntity entity) {
        this.removeById(entity.getId());
        buttonMapper.deleteByModuleId(entity.getId());
        QueryWrapper<ModuleColumnEntity> columnWrapper = new QueryWrapper<>();
        columnWrapper.lambda().eq(ModuleColumnEntity::getModuleId, entity.getId());
        columnMapper.deleteByModuleId(entity.getId());
        QueryWrapper<ModuleDataAuthorizeEntity> dataWrapper = new QueryWrapper<>();
        dataWrapper.lambda().eq(ModuleDataAuthorizeEntity::getModuleId, entity.getId());
        moduleDataAuthorizeMapper.deleteByModuleId(entity.getId());
        QueryWrapper<ModuleFormEntity> formWrapper = new QueryWrapper<>();
        formWrapper.lambda().eq(ModuleFormEntity::getModuleId, entity.getId());
        formMapper.deleteByModuleId(entity.getId());
        QueryWrapper<ModuleDataAuthorizeSchemeEntity> schemeWrapper = new QueryWrapper<>();
        schemeWrapper.lambda().eq(ModuleDataAuthorizeSchemeEntity::getModuleId, entity.getId());
        schemeMapper.deleteByModuleId(entity.getId());
        QueryWrapper<ModuleDataEntity> moduleDataWrapper = new QueryWrapper<>();
        moduleDataWrapper.lambda().eq(ModuleDataEntity::getModuleId, entity.getId());
        moduleDataMapper.deleteByModuleId(entity.getId());
    }

    @Override
    public ModuleEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public List<ModuleEntity> getList(boolean filterFlowWork, List<String> moduleAuthorize, List<String> moduleUrlAddressAuthorize) {
        return this.baseMapper.getList(filterFlowWork, moduleAuthorize, moduleUrlAddressAuthorize);
    }

    @Override
    public List<ModuleEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<ModuleEntity> getListTenant() {
        String urlAddress = null;
        try {
            if (Boolean.FALSE.equals(DbTypeUtil.checkOracle(dbLinkService.getResource("0")))) {
                urlAddress = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.baseMapper.getListTenant(urlAddress);
    }

    @Override
    public List<ModuleEntity> getListByParentId(String id) {
        return this.baseMapper.getListByParentId(id);
    }

    @Override
    public ModuleEntity getInfo(String id, String systemId) {
        return this.baseMapper.getInfo(id, systemId);
    }

    @Override
    public ModuleEntity getInfo(String id, String systemId, String parentId) {
        return this.baseMapper.getInfo(id, systemId, parentId);
    }

    @Override
    public boolean isExistByFullName(ModuleEntity entity, String category, String systemId) {
        return this.baseMapper.isExistByFullName(entity, category, systemId);
    }

    @Override
    public boolean isExistByEnCode(ModuleEntity entity, String category, String systemId) {
        return this.baseMapper.isExistByEnCode(entity, category, systemId);
    }

    @Override
    public boolean isExistByAddress(ModuleEntity entity, String category, String systemId) {
        return this.baseMapper.isExistByAddress(entity, category, systemId);
    }

    @Override
    public void deleteBySystemId(String systemId) {
        this.baseMapper.deleteBySystemId(systemId);
    }

    @Override
    public DownloadVO exportData(String id) {
        //获取信息转model
        ModuleEntity moduleEntity = getInfo(id);
        List<ModuleButtonEntity> buttonServiceList = buttonMapper.getListByModuleIds(id);
        List<ModuleColumnEntity> columnServiceList = columnMapper.getList(id);
        List<ModuleDataAuthorizeSchemeEntity> schemeServiceList = schemeMapper.getList(id);
        List<ModuleDataAuthorizeEntity> authorizeServiceList = authorizeMapper.getList(id);
        List<ModuleFormEntity> formList = formMapper.getList(id);
        ModuleExportModel exportModel = new ModuleExportModel();
        exportModel.setModuleEntity(moduleEntity);
        exportModel.setButtonEntityList(buttonServiceList);
        exportModel.setColumnEntityList(columnServiceList);
        exportModel.setFormEntityList(formList);
        exportModel.setSchemeEntityList(schemeServiceList);
        exportModel.setAuthorizeEntityList(authorizeServiceList);
        //导出文件
        return fileExport.exportFile(exportModel, FileTypeConstant.TEMPORARY, moduleEntity.getFullName(), ModuleTypeEnum.SYSTEM_MODULE.getTableName());
    }

    @Override
    @DSTransactional
    public ActionResult<Object> importData(ModuleExportModel exportModel, Integer type) throws DataException {
        try {
            boolean isAdd = ObjectUtil.equal(type, 1);
            StringBuilder message = new StringBuilder();
            ModuleEntity moduleEntity = exportModel.getModuleEntity();
            StringJoiner stringJoiner = new StringJoiner("、");
            if (getInfo(moduleEntity.getId()) != null) {
                stringJoiner.add("ID");
            }
            String id = moduleEntity.getId();
            moduleEntity.setId(null);
            if (isExistByEnCode(moduleEntity, moduleEntity.getCategory(), moduleEntity.getSystemId())) {
                stringJoiner.add("编码");
            }
            if (isExistByFullName(moduleEntity, moduleEntity.getCategory(), moduleEntity.getSystemId())) {
                stringJoiner.add("名称");
            }
            if (isExistByAddress(moduleEntity, moduleEntity.getCategory(), moduleEntity.getSystemId())) {
                stringJoiner.add("路由地址");
            }
            moduleEntity.setId(id);
            if (stringJoiner.length() > 0) {
                if (isAdd) {
                    String copyNum = UUID.randomUUID().toString().substring(0, 5);
                    moduleEntity.setFullName(moduleEntity.getFullName() + ".副本" + copyNum);
                    moduleEntity.setEnCode(moduleEntity.getEnCode() + copyNum);
                    moduleEntity.setId(RandomUtil.uuId());
                    this.setIgnoreLogicDelete().removeById(moduleEntity);
                    this.setIgnoreLogicDelete().saveOrUpdate(moduleEntity);
                }
            } else {
                this.setIgnoreLogicDelete().removeById(moduleEntity);
                this.setIgnoreLogicDelete().saveOrUpdate(moduleEntity);
            }
            if (stringJoiner.length() > 0) {
                message.append(stringJoiner.toString()).append("重复；");
            }
            StringJoiner exceptionMessage = new StringJoiner("、");
            StringJoiner idmessage = new StringJoiner("、");
            StringJoiner fullNameMessage = new StringJoiner("、");
            StringJoiner enCodeMessage = new StringJoiner("、");
            //按钮
            List<ModuleButtonEntity> buttonEntityList = JsonUtil.getJsonToList(exportModel.getButtonEntityList(), ModuleButtonEntity.class);
            //新ID映射
            Map<String, String> idConvert = new HashMap<>(buttonEntityList.size(), 1);
            if (isAdd) {
                buttonEntityList.forEach(button -> idConvert.put(button.getId(), RandomUtil.uuId()));
            }
            for (ModuleButtonEntity buttonEntity : buttonEntityList) {
                if (buttonMapper.getInfo(buttonEntity.getId()) != null) {
                    idmessage.add(buttonEntity.getId());
                }
                if (buttonMapper.isExistByFullName(moduleEntity.getId(), buttonEntity.getFullName(), null)) {
                    fullNameMessage.add(buttonEntity.getFullName());
                }
                if (buttonMapper.isExistByEnCode(moduleEntity.getId(), buttonEntity.getEnCode(), null)) {
                    enCodeMessage.add(buttonEntity.getEnCode());
                }
                if (isAdd) {
                    buttonEntity.setId(idConvert.get(buttonEntity.getId()));
                    buttonEntity.setModuleId(moduleEntity.getId());
                    if (idConvert.containsKey(buttonEntity.getParentId())) {
                        buttonEntity.setParentId(idConvert.get(buttonEntity.getParentId()));
                    }
                    if (fullNameMessage.length() > 0 || enCodeMessage.length() > 0) {
                        String copyNum = UUID.randomUUID().toString().substring(0, 5);
                        buttonEntity.setFullName(buttonEntity.getFullName() + ".副本" + copyNum);
                        buttonEntity.setEnCode(buttonEntity.getEnCode() + copyNum);
                    }
                    buttonMapper.setIgnoreLogicDelete().insertOrUpdate(buttonEntity);
                } else if (idmessage.length() == 0 && fullNameMessage.length() == 0 && enCodeMessage.length() == 0) {
                    buttonMapper.setIgnoreLogicDelete().deleteById(buttonEntity);
                    buttonEntity.setModuleId(moduleEntity.getId());
                    buttonMapper.setIgnoreLogicDelete().insertOrUpdate(buttonEntity);
                }
            }
            tmpMessage("buttonEntityList：", message, exceptionMessage, idmessage, fullNameMessage, enCodeMessage);
            //列表
            List<ModuleColumnEntity> columnEntityList = JsonUtil.getJsonToList(exportModel.getColumnEntityList(), ModuleColumnEntity.class);
            for (ModuleColumnEntity columnEntity : columnEntityList) {
                if (columnMapper.getInfo(columnEntity.getId()) != null) {
                    idmessage.add(columnEntity.getId());
                }
                if (columnMapper.isExistByFullName(moduleEntity.getId(), columnEntity.getFullName(), null)) {
                    fullNameMessage.add(columnEntity.getFullName());
                }
                if (columnMapper.isExistByEnCode(moduleEntity.getId(), columnEntity.getEnCode(), null)) {
                    enCodeMessage.add(columnEntity.getEnCode());
                }
                if (isAdd) {
                    columnEntity.setId(RandomUtil.uuId());
                    columnEntity.setModuleId(moduleEntity.getId());
                    if (fullNameMessage.length() > 0 || enCodeMessage.length() > 0) {
                        String copyNum = UUID.randomUUID().toString().substring(0, 5);
                        columnEntity.setFullName(columnEntity.getFullName() + ".副本" + copyNum);
                        columnEntity.setEnCode(columnEntity.getEnCode() + copyNum);
                    }
                    columnMapper.setIgnoreLogicDelete().insertOrUpdate(columnEntity);
                } else if (idmessage.length() == 0 && fullNameMessage.length() == 0 && enCodeMessage.length() == 0) {
                    columnMapper.setIgnoreLogicDelete().deleteById(columnEntity);
                    columnEntity.setModuleId(moduleEntity.getId());
                    columnMapper.setIgnoreLogicDelete().insertOrUpdate(columnEntity);
                }
            }
            tmpMessage("columnEntityList：", message, exceptionMessage, idmessage, fullNameMessage, enCodeMessage);
            //表单
            List<ModuleFormEntity> formEntityList = JsonUtil.getJsonToList(exportModel.getFormEntityList(), ModuleFormEntity.class);
            for (ModuleFormEntity formEntity : formEntityList) {
                if (formMapper.getInfo(formEntity.getId()) != null) {
                    idmessage.add(formEntity.getId());
                }
                if (formMapper.isExistByFullName(moduleEntity.getId(), formEntity.getFullName(), null)) {
                    fullNameMessage.add(formEntity.getFullName());
                }
                if (formMapper.isExistByEnCode(moduleEntity.getId(), formEntity.getEnCode(), null)) {
                    enCodeMessage.add(formEntity.getEnCode());
                }
                if (isAdd) {
                    formEntity.setId(RandomUtil.uuId());
                    formEntity.setModuleId(moduleEntity.getId());
                    if (fullNameMessage.length() > 0 || enCodeMessage.length() > 0) {
                        String copyNum = UUID.randomUUID().toString().substring(0, 5);
                        formEntity.setFullName(formEntity.getFullName() + ".副本" + copyNum);
                        formEntity.setEnCode(formEntity.getEnCode() + copyNum);
                    }
                    formMapper.setIgnoreLogicDelete().insertOrUpdate(formEntity);
                } else if (idmessage.length() == 0 && fullNameMessage.length() == 0 && enCodeMessage.length() == 0) {
                    formMapper.setIgnoreLogicDelete().deleteById(formEntity);
                    formEntity.setModuleId(moduleEntity.getId());
                    formMapper.setIgnoreLogicDelete().insertOrUpdate(formEntity);
                }
            }
            tmpMessage("formEntityList：", message, exceptionMessage, idmessage, fullNameMessage, enCodeMessage);
            //数据权限
            Map<String, String> authorizeId = new HashMap<>(16);
            List<ModuleDataAuthorizeEntity> authorizeEntityList = JsonUtil.getJsonToList(exportModel.getAuthorizeEntityList(), ModuleDataAuthorizeEntity.class);
            for (ModuleDataAuthorizeEntity authorizeEntity : authorizeEntityList) {
                if (authorizeMapper.getInfo(authorizeEntity.getId()) != null) {
                    idmessage.add(authorizeEntity.getId());
                }
                if (authorizeMapper.isExistByFullName(moduleEntity.getId(), authorizeEntity.getFullName(), null)) {
                    fullNameMessage.add(authorizeEntity.getFullName());
                }
                if (authorizeMapper.isExistByEnCode(moduleEntity.getId(), authorizeEntity.getEnCode(), null)) {
                    enCodeMessage.add(authorizeEntity.getEnCode());
                }
                if (isAdd) {
                    authorizeEntity.setId(RandomUtil.uuId());
                    authorizeEntity.setModuleId(moduleEntity.getId());
                    if (fullNameMessage.length() > 0 || enCodeMessage.length() > 0) {
                        String copyNum = UUID.randomUUID().toString().substring(0, 5);
                        authorizeEntity.setFullName(authorizeEntity.getFullName() + ".副本" + copyNum);
                        authorizeEntity.setEnCode(authorizeEntity.getEnCode() + copyNum);
                    }
                    authorizeMapper.setIgnoreLogicDelete().insertOrUpdate(authorizeEntity);
                    authorizeId.put(authorizeEntity.getId(), authorizeEntity.getId());
                } else if (idmessage.length() == 0 && fullNameMessage.length() == 0 && enCodeMessage.length() == 0) {
                    authorizeMapper.setIgnoreLogicDelete().deleteById(authorizeEntity);
                    authorizeEntity.setModuleId(moduleEntity.getId());
                    authorizeMapper.setIgnoreLogicDelete().insertOrUpdate(authorizeEntity);
                }
            }
            tmpMessage("authorizeEntityList：", message, exceptionMessage, idmessage, fullNameMessage, enCodeMessage);
            //数据权限方案
            List<ModuleDataAuthorizeSchemeEntity> schemeEntityList = JsonUtil.getJsonToList(exportModel.getSchemeEntityList(), ModuleDataAuthorizeSchemeEntity.class);
            for (ModuleDataAuthorizeSchemeEntity schemeEntity : schemeEntityList) {
                if (schemeMapper.getInfo(schemeEntity.getId()) != null) {
                    idmessage.add(schemeEntity.getId());
                }
                if (Boolean.TRUE.equals(schemeMapper.isExistByFullName(null, schemeEntity.getFullName(), moduleEntity.getId()))) {
                    fullNameMessage.add(schemeEntity.getFullName());
                }
                if (Boolean.TRUE.equals(schemeMapper.isExistByEnCode(null, schemeEntity.getEnCode(), moduleEntity.getId()))) {
                    enCodeMessage.add(schemeEntity.getEnCode());
                }
                if (isAdd) {
                    schemeEntity.setId(RandomUtil.uuId());
                    schemeEntity.setModuleId(moduleEntity.getId());
                    String conditionJson = schemeEntity.getConditionJson();
                    if (StringUtil.isNotEmpty(conditionJson)) {
                        for (Map.Entry<String, String> entry : authorizeId.entrySet()) {
                            String oldId = entry.getKey();
                            conditionJson = conditionJson.replaceAll(oldId, authorizeId.get(oldId));
                        }
                    }
                    if (fullNameMessage.length() > 0 || enCodeMessage.length() > 0) {
                        String copyNum = UUID.randomUUID().toString().substring(0, 5);
                        schemeEntity.setFullName(schemeEntity.getFullName() + ".副本" + copyNum);
                        schemeEntity.setEnCode(schemeEntity.getEnCode() + copyNum);
                    }
                    schemeMapper.setIgnoreLogicDelete().insertOrUpdate(schemeEntity);
                } else if (idmessage.length() == 0 && fullNameMessage.length() == 0 && enCodeMessage.length() == 0) {
                    schemeMapper.setIgnoreLogicDelete().deleteById(schemeEntity);
                    schemeEntity.setModuleId(moduleEntity.getId());
                    schemeMapper.setIgnoreLogicDelete().insertOrUpdate(schemeEntity);
                }
            }
            tmpMessage("schemeEntityList：", message, exceptionMessage, idmessage, fullNameMessage, enCodeMessage);
            if (ObjectUtil.equal(type, 0) && message.length() > 0) {
                return ActionResult.fail(message.toString().substring(0, message.lastIndexOf("；")));
            }
            return ActionResult.success(MsgCode.IMP001.get());
        } catch (Exception e) {
            e.printStackTrace();
            //手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new DataException(e.getMessage());
        } finally {
            this.clearIgnoreLogicDelete();
            buttonMapper.clearIgnoreLogicDelete();
            columnMapper.clearIgnoreLogicDelete();
            formMapper.clearIgnoreLogicDelete();
            authorizeMapper.clearIgnoreLogicDelete();
            schemeMapper.clearIgnoreLogicDelete();
        }
    }

    private void tmpMessage(String moduleType, StringBuilder message, StringJoiner exceptionMessage, StringJoiner idmessage, StringJoiner fullNameMessage, StringJoiner enCodeMessage) {
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
            message.append(moduleType).append(exceptionMessage.toString()).append("；");
        }
    }

    @Override
    @DSTransactional
    public List<ModuleEntity> getModuleList(String visualId) {
        return this.baseMapper.getModuleList(visualId);
    }

    @Override
    public List<ModuleEntity> getModuleBySystemIds(List<String> ids, List<String> moduleAuthorize, List<String> moduleUrlAddressAuthorize, Integer type) {
        return this.baseMapper.getModuleBySystemIds(ids, moduleAuthorize, moduleUrlAddressAuthorize, type);
    }

    @Override
    public List<ModuleEntity> getModuleByIds(List<String> ids) {
        return this.baseMapper.getModuleByIds(ids);
    }

    @Override
    public List<ModuleEntity> getListByEnCode(List<String> enCodeList) {
        return this.baseMapper.getListByEnCode(enCodeList);
    }

    @Override
    public List<ModuleEntity> findModuleAdmin(int mark, String id, List<String> moduleAuthorize, List<String> moduleUrlAddressAuthorize) {
        return this.baseMapper.findModuleAdmin(mark, id, moduleAuthorize, moduleUrlAddressAuthorize);
    }

    @Override
    public void getParentModule(List<ModuleEntity> data, Map<String, ModuleEntity> moduleEntityMap) {
        data.forEach(t -> {
            ModuleEntity moduleEntity = t;
            while (moduleEntity != null) {
                if (!moduleEntityMap.containsKey(moduleEntity.getId())) {
                    moduleEntityMap.put(moduleEntity.getId(), moduleEntity);
                }
                moduleEntity = this.getInfo(moduleEntity.getParentId());
            }
        });
    }

    @Override
    public List<ModuleEntity> getListByUrlAddress(List<String> ids, List<String> urlAddressList) {
        return this.baseMapper.getListByUrlAddress(ids, urlAddressList);
    }

    @Override
    public ModuleNameVO getModuleNameList(String visualId) {
        Map<String, SystemEntity> sysMap = systemMapper.getList().stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        ModuleNameVO moduleNameVO = new ModuleNameVO();
        List<ModuleEntity> moduleList = this.getModuleList(visualId);
        QueryWrapper<ModuleEntity> moduleWrapper = new QueryWrapper<>();

        List<ModuleEntity> listAll = this.list(moduleWrapper);
        if (!moduleList.isEmpty()) {
            List<ModuleEntity> pcList = moduleList.stream().filter(module -> "web".equalsIgnoreCase(module.getCategory())).collect(Collectors.toList());
            List<ModuleEntity> appList = moduleList.stream().filter(module -> "app".equalsIgnoreCase(module.getCategory())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(pcList)) {
                moduleNameVO.setPcIds(pcList.stream().map(ModuleEntity::getId).collect(Collectors.toList()));
                StringJoiner joiner = new StringJoiner("；");
                for (ModuleEntity moduleEntity : pcList) {
                    List<String> aa = new ArrayList<>();
                    getName(moduleEntity.getId(), listAll, aa, sysMap);
                    Collections.reverse(aa);
                    joiner.add(aa.stream().collect(Collectors.joining("/")));
                    moduleNameVO.setPcNames(joiner.toString());
                }
            }
            if (CollectionUtils.isNotEmpty(appList)) {
                moduleNameVO.setAppIds(appList.stream().map(ModuleEntity::getId).collect(Collectors.toList()));
                StringJoiner joiner = new StringJoiner("；");
                for (ModuleEntity moduleEntity : appList) {
                    List<String> aa = new ArrayList<>();
                    getName(moduleEntity.getId(), listAll, aa, sysMap);
                    Collections.reverse(aa);
                    joiner.add(aa.stream().collect(Collectors.joining("/")));
                    moduleNameVO.setAppNames(joiner.toString());
                }
            }
        }
        return moduleNameVO;
    }

    private void getName(String id, List<ModuleEntity> listAll, List<String> str, Map<String, SystemEntity> sysMap) {
        for (ModuleEntity item : listAll) {
            if (item.getId().equals(id)) {
                str.add(item.getFullName());
                if (StringUtil.isNotEmpty(item.getParentId())) {
                    if (Objects.equals("-1", item.getParentId())) {
                        SystemEntity info = sysMap.get(item.getSystemId());
                        if (info != null) {
                            str.add(info.getFullName());
                        }
                    } else {
                        getName(item.getParentId(), listAll, str, sysMap);
                    }
                }
            }
        }
    }

    @Override
    public List<ModuleSelectorVo> getFormMenuList(ModulePagination pagination) {
        return this.baseMapper.getFormMenuList(pagination);
    }

    /**
     * 获取应用菜单列表
     *
     * @return
     */
    @Override
    public List<MenuSelectAllVO> getSystemMenu(Integer type, List<Integer> webType, List<String> categorys) {
        List<String> moduleAuthorize = new ArrayList<>();
        List<String> moduleUrlAddressAuthorize = new ArrayList<>();
        if (configValueUtil.isMultiTenancy()) {
            TenantAuthorizeModel tenantAuthorizeModel = TenantDataSourceUtil.getCacheModuleAuthorize(UserProvider.getUser().getTenantId());
            moduleAuthorize = tenantAuthorizeModel.getModuleIdList();
            moduleUrlAddressAuthorize = tenantAuthorizeModel.getUrlAddressList();
        }
        SystemEntity infoByEnCode = systemMapper.getInfoByEnCode(RequestContext.getAppCode());
        List<ModuleEntity> menuList = this.getList(false, moduleAuthorize, moduleUrlAddressAuthorize)
                .stream().filter(t -> Objects.equals(infoByEnCode.getId(), t.getSystemId())).collect(Collectors.toList());

        List<MenuSelectAllVO> menuvo = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(menuList)) {
            for (ModuleEntity item : menuList) {
                String propertyJson = item.getPropertyJson();
                PropertyJsonModel pjm = JsonUtil.getJsonToBean(propertyJson, PropertyJsonModel.class);
                if (Objects.equals(type, item.getType()) && webType.contains(pjm.getWebType())
                        && categorys.contains(item.getCategory().toLowerCase())) {
                    menuvo.add(JsonUtil.getJsonToBean(item, MenuSelectAllVO.class));
                }
            }
        }
        return menuvo;
    }

    @Override
    public List<ModuleSelectorVo> getPageList(ModulePagination pagination) {
        return this.baseMapper.getPageList(pagination);
    }

    @Override
    public void setAutoEnCode(ModuleEntity entity) {
        String codePre = "";
        SystemEntity info = systemMapper.getInfo(entity.getSystemId());
        if (info != null && JnpfConst.MAIN_SYSTEM_CODE.equals(info.getEnCode())) {
            codePre = CodeConst.XTCD;
        } else {
            codePre = CodeConst.YYCD;
        }
        // 自动生成编码
        if (StringUtil.isEmpty(entity.getEnCode())) {
            final String codePreF = codePre;
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(codePreF), code -> {
                entity.setEnCode(code);
                return this.isExistByEnCode(entity, entity.getCategory(), entity.getSystemId());
            }));
        }
    }
}
