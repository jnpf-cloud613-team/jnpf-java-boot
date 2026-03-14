package jnpf.base.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.entity.*;
import jnpf.base.mapper.PrintDevMapper;
import jnpf.base.model.dataset.DataSetForm;
import jnpf.base.model.dataset.DataSetInfo;
import jnpf.base.model.dataset.DataSetPagination;
import jnpf.base.model.dataset.TableTreeModel;
import jnpf.base.model.export.PrintExportVo;
import jnpf.base.model.print.*;
import jnpf.base.model.vo.PrintDevVO;
import jnpf.base.service.*;
import jnpf.base.util.dataset.DataSetSwapUtil;
import jnpf.constant.AuthorizeConst;
import jnpf.constant.CodeConst;
import jnpf.constant.MsgCode;
import jnpf.emnus.DataSetTypeEnum;
import jnpf.exception.DataException;
import jnpf.flowable.entity.RecordEntity;
import jnpf.permission.entity.AuthorizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.user.UserRelationIds;
import jnpf.permission.service.AuthorizeService;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import jnpf.util.enums.DictionaryDataEnum;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 打印模板-服务实现类
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Service
@RequiredArgsConstructor
public class PrintDevServiceImpl extends SuperServiceImpl<PrintDevMapper, PrintDevEntity> implements PrintDevService {

    
    private final CodeNumService codeNumService;
    
    private final  DictionaryDataService dictionaryDataService;
    
    private final  DictionaryTypeService dictionaryTypeService;

    
    private final  PrintVersionService printVersionService;
    
    private final  UserService userService;
    
    private final  DataSetService dataSetService;
    
    private final  WorkFlowApi workFlowApi;
    
    private final  DataSetSwapUtil dataSetSwapUtil;
    
    private final  AuthorizeService authorizeService;
    
    private final  DataInterfaceService dataInterfaceService;

    
    private final  SystemService systemService;

    @Override
    public List<PrintDevEntity> getList(PaginationPrint paginationPrint) {
        return this.baseMapper.getList(paginationPrint);
    }

    @Override
    public List<PrintDevEntity> getListByIds(List<String> idList) {
        return this.baseMapper.getListByIds(idList);
    }

    @Override
    public List<PrintDevEntity> getListByCreUser(String creUser) {
        return this.baseMapper.getListByCreUser(creUser);
    }

    @Override
    public void create(PrintDevFormDTO dto) {
        PrintDevEntity entity = JsonUtil.getJsonToBean(dto, PrintDevEntity.class);
        // 校验
        this.baseMapper.creUpdateCheck(entity, true, true);
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.DY), code -> this.isExistByEnCode(code, null)));
        }
        this.baseMapper.create(entity);
        dto.setId(entity.getId());
        List<PrintVersionEntity> list = printVersionService.getList(entity.getId());
        if (CollectionUtils.isEmpty(list)) {
            printVersionService.create(dto);
        }
    }

    @Override
    public PrintDevInfoVO getVersionInfo(String versionId) {
        PrintVersionEntity versionEntity = printVersionService.getById(versionId);
        PrintDevEntity entity = this.getById(versionEntity.getTemplateId());
        PrintDevInfoVO vo = JsonUtil.getJsonToBean(entity, PrintDevInfoVO.class);
        vo.setVersionId(versionId);
        vo.setPrintTemplate(versionEntity.getPrintTemplate());
        vo.setConvertConfig(versionEntity.getConvertConfig());
        vo.setGlobalConfig(versionEntity.getGlobalConfig());
        List<DataSetEntity> list = dataSetService.getList(new DataSetPagination(DataSetTypeEnum.PRINT_VER.getCode(), versionId));
        List<DataSetInfo> dataSetInfoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (DataSetEntity item : list) {
                DataSetInfo bean = JsonUtil.getJsonToBean(item, DataSetInfo.class);
                try {
                    SumTree<TableTreeModel> printTableFields = dataSetService.getTabFieldStruct(item);
                    bean.setChildren(printTableFields.getChildren());
                    dataSetInfoList.add(bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (Objects.equals(item.getType(), 3)) {
                    DataInterfaceEntity info = dataInterfaceService.getInfo(item.getInterfaceId());
                    if (info != null) {
                        bean.setTreePropsName(info.getFullName());
                    }
                }
            }
        }
        vo.setDataSetList(dataSetInfoList);
        return vo;
    }

    @Override
    @DSTransactional
    public void saveOrRelease(PrintDevUpForm form) {
        PrintDevEntity entity = this.getById(form.getId());
        PrintVersionEntity versionNew = printVersionService.getById(form.getVersionId());
        //已归档
        boolean isDossier = false;
        if (Objects.equals(versionNew.getState(), 2) || Objects.equals(versionNew.getState(), 1)) {
            isDossier = !isDossier;
        }
        //发布
        if (Objects.equals(form.getType(), 1) && StringUtil.isNotEmpty(form.getVersionId())) {
                PrintVersionEntity info = printVersionService.getList(form.getId()).stream().filter(t -> Objects.equals(t.getState(), 1)).findFirst().orElse(null);
                if (info != null) {
                    // 变更归档状态，排序码
                    info.setSortCode(0L);
                    info.setState(2);
                    printVersionService.updateById(info);
                }
                versionNew.setState(1);
                versionNew.setSortCode(1L);
                //已发布
                if (Objects.equals(entity.getState(), 0)) {
                    entity.setState(1);
                }
            }

        //已归档不修改设计内容和数据集内容
        if (!isDossier) {
            versionNew.setPrintTemplate(form.getPrintTemplate());
            versionNew.setConvertConfig(form.getConvertConfig());
            versionNew.setGlobalConfig(form.getGlobalConfig());
            //数据集创建
            String versionId = versionNew.getId();
            List<DataSetForm> dataSetList = form.getDataSetList() != null ? form.getDataSetList() : new ArrayList<>();
            dataSetService.create(dataSetList, DataSetTypeEnum.PRINT_VER.getCode(), versionId);
        }
        printVersionService.updateById(versionNew);

        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        this.updateById(entity);
    }


    @Override
    public List<PrintDevVO> getTreeModel(String category) {
        SystemEntity systemEntity = systemService.getInfoByEnCode(RequestContext.getAppCode());
        QueryWrapper<PrintDevEntity> query = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(category)) {
            query.lambda().eq(PrintDevEntity::getCategory, category);
        }
        query.lambda().eq(PrintDevEntity::getSystemId, systemEntity.getId());
        query.lambda().eq(PrintDevEntity::getState, 1);
        query.lambda().orderByAsc(PrintDevEntity::getSortCode).orderByDesc(PrintDevEntity::getCreatorTime);
        List<PrintDevEntity> printEntityList = this.list(query);
        return setTreeModel(printEntityList);
    }

    private List<PrintDevVO> setTreeModel(List<PrintDevEntity> printEntityList) {
        //数据字典缺失
        DictionaryTypeEntity infoByEnCode = dictionaryTypeService.getInfoByEnCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        if (infoByEnCode == null) {
            throw new DataException(MsgCode.PRI002.get());
        }
        List<DictionaryDataEntity> dicDataList = dictionaryDataService.
                getList(infoByEnCode.getId());
        List<PrintDevTreeModel> modelAll = new LinkedList<>();
        //设置树形主节点（不显示没有子集的）
        for (DictionaryDataEntity dicEntity : dicDataList) {
            PrintDevTreeModel model = new PrintDevTreeModel();
            model.setFullName(dicEntity.getFullName());
            model.setId(dicEntity.getId());
            Long num = printEntityList.stream().filter(t -> t.getCategory().equals(dicEntity.getId())).count();
            //编码底下存在的子节点总数
            if (num > 0) {
                model.setNum(Integer.parseInt(num.toString()));
                modelAll.add(model);
            }
        }
        List<String> userId = new ArrayList<>();
        printEntityList.forEach(t -> {
            userId.add(t.getCreatorUserId());
            if (StringUtil.isNotEmpty(t.getLastModifyUserId())) {
                userId.add(t.getLastModifyUserId());
            }
        });
        List<UserEntity> userList = userService.getUserName(userId);
        //设置子节点分支
        for (PrintDevEntity printEntity : printEntityList) {
            DictionaryDataEntity dicDataEntity = dicDataList.stream()
                    .filter(t -> t.getId().equals(printEntity.getCategory())).findFirst().orElse(null);
            //如果字典存在则装入容器
            PrintDevTreeModel model = JsonUtil.getJsonToBean(printEntity, PrintDevTreeModel.class);
            if (dicDataEntity != null) {
                //创建者
                UserEntity creatorUser = userList.stream().filter(t -> t.getId().equals(model.getCreatorUser())).findFirst().orElse(null);
                model.setCreatorUser(creatorUser != null ? creatorUser.getRealName() + "/" + creatorUser.getAccount() : "");
                //修改人
                UserEntity lastmodifyuser = userList.stream().filter(t -> t.getId().equals(model.getLastModifyUser())).findFirst().orElse(null);
                model.setLastModifyUser(lastmodifyuser != null ? lastmodifyuser.getRealName() + "/" + lastmodifyuser.getAccount() : "");

                model.setParentId(dicDataEntity.getId());
                modelAll.add(model);
            }
        }
        List<SumTree<PrintDevTreeModel>> trees = TreeDotUtils.convertListToTreeDot(modelAll);
        return JsonUtil.getJsonToList(trees, PrintDevVO.class);
    }

    @Override
    public void creUpdateCheck(PrintDevEntity printDevEntity, Boolean fullNameCheck, Boolean encodeCheck) {
        this.baseMapper.creUpdateCheck(printDevEntity, fullNameCheck, encodeCheck);
    }

    @Override
    public Boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public List<PrintOption> getPrintTemplateOptions(List<String> ids) {
        return this.baseMapper.getPrintTemplateOptions(ids);
    }


    @Override
    @DSTransactional
    public String importData(PrintExportVo infoVO, Integer type) {
        PrintDevEntity entity = JsonUtil.getJsonToBean(infoVO, PrintDevEntity.class);
        StringJoiner stringJoiner = new StringJoiner("、");
        //id为空切名称不存在时
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintDevEntity::getId, entity.getId());
        if (this.getById(infoVO.getId()) != null) {
            if (Objects.equals(type, 0)) {
                stringJoiner.add("ID");
            } else {
                entity.setId(RandomUtil.uuId());
            }
        }
        queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintDevEntity::getEnCode, entity.getEnCode());
        if (this.count(queryWrapper) > 0) {
            stringJoiner.add(MsgCode.IMP009.get());
        }
        queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintDevEntity::getFullName, entity.getFullName());
        queryWrapper.lambda().eq(PrintDevEntity::getSystemId, entity.getSystemId());
        if (this.count(queryWrapper) > 0) {
            stringJoiner.add(MsgCode.IMP008.get());
        }
        if (stringJoiner.length() > 0 && ObjectUtil.equal(type, 1)) {
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            entity.setEnCode(entity.getEnCode() + copyNum);
        } else if (ObjectUtil.equal(type, 0) && stringJoiner.length() > 0) {
            return stringJoiner.toString() + MsgCode.IMP007.get();
        }
        entity.setState(0);
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getLoginUserId());
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        this.setIgnoreLogicDelete().removeById(entity);
        this.setIgnoreLogicDelete().saveOrUpdate(entity);
        this.clearIgnoreLogicDelete();
        //版本添加
        createVerAndSet(infoVO, entity.getId());
        return "";
    }


    @Override
    public List<OperatorRecordEntity> getFlowTaskOperatorRecordList(String taskId) {
        List<OperatorRecordEntity> operatorRecordList = new ArrayList<>();
        if (StringUtil.isEmpty(taskId)) {
            return operatorRecordList;
        }
        try {
           List<RecordEntity> recordList = workFlowApi.getRecordList(taskId);
            operatorRecordList = JsonUtil.getJsonToList(recordList, OperatorRecordEntity.class);
            //已办人员
            operatorRecordList.forEach(or -> {
                UserEntity userEntity = userService.getInfo(or.getHandleId());
                or.setUserName(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
                or.setHandleStatus(or.getHandleType());
            });
        } catch (Exception e) {
            e.getMessage();
        }
        return operatorRecordList;
    }

    @Override
    public Map<String, Object> getDataMap(String templateId, String formId, String flwoTaskId, Map<String, Object> params) {
        List<PrintVersionEntity> versionList = printVersionService.getList(templateId);
        if (CollectionUtils.isEmpty(versionList)) {
            throw new DataException(MsgCode.PRI001.get());
        }
        //启用中的版本
        PrintVersionEntity startVersion = versionList.get(0);
        Map<String, Object> dataMap = new HashMap<>(16);
        dataMap.put("printTemplate", startVersion.getPrintTemplate());
        String convertConfig = startVersion.getConvertConfig();
        dataMap.put("convertConfig", startVersion.getConvertConfig());

        List<OperatorRecordEntity> operatorRecordList = this.getFlowTaskOperatorRecordList(flwoTaskId);
        dataMap.put("operatorRecordList", operatorRecordList);

        Map<String, Object> printData = new HashMap<>(16);
        List<DataSetEntity> list = dataSetService.getList(new DataSetPagination(DataSetTypeEnum.PRINT_VER.getCode(), startVersion.getId()));
        for (DataSetEntity item : list) {
            Map<String, Object> dataMapOrList = dataSetService.getDataMapOrList(item, params, formId, false);
            dataSetSwapUtil.swapData(templateId, convertConfig, dataMapOrList);
            printData.putAll(dataMapOrList);
        }
        //打印全局配置
        if (StringUtil.isNotEmpty(startVersion.getGlobalConfig())) {
            dataMap.put("globalConfig", startVersion.getGlobalConfig());
        }
        dataMap.put("printData", printData);
        return dataMap;
    }

    @Override
    public void copyPrintdev(String templateId) {
        PrintDevEntity entity = this.getById(templateId);
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        String fullName = entity.getFullName() + ".副本" + copyNum;
        if (fullName.length() > 50) {
            throw new DataException(MsgCode.PRI006.get());
        }
        PrintDevFormDTO form = JsonUtil.getJsonToBean(entity, PrintDevFormDTO.class);
        form.setId(RandomUtil.uuId());
        form.setFullName(fullName);
        form.setEnCode(entity.getEnCode() + copyNum);

        List<PrintVersionEntity> list = printVersionService.getList(templateId);
        if (CollectionUtils.isNotEmpty(list)) {
            PrintVersionEntity versionEntity = list.get(0);
            form.setPrintTemplate(versionEntity.getPrintTemplate());
            form.setConvertConfig(versionEntity.getConvertConfig());
            form.setGlobalConfig(versionEntity.getGlobalConfig());
            List<DataSetEntity> dst = dataSetService.getList(new DataSetPagination(DataSetTypeEnum.PRINT_VER.getCode(), versionEntity.getId()));
            List<DataSetForm> dataSetFormList = new ArrayList<>();
            for (DataSetEntity item : dst) {
                DataSetForm dataSetForm = JsonUtil.getJsonToBean(item, DataSetForm.class);
                dataSetForm.setId(null);
                dataSetFormList.add(dataSetForm);
            }
            form.setDataSetList(dataSetFormList);
        }
        this.create(form);
    }

    @Override
    public List<PrintDevEntity> getWorkSelector(PaginationPrint pagination) {
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(PrintDevEntity::getFullName, pagination.getKeyword())
                            .or().like(PrintDevEntity::getEnCode, pagination.getKeyword())
            );
        }
        if (StringUtil.isNotEmpty(pagination.getCategory())) {
            queryWrapper.lambda().eq(PrintDevEntity::getCategory, pagination.getCategory());
        }

        //是发布
        queryWrapper.lambda().eq(PrintDevEntity::getState, 1);
        //是通用
        queryWrapper.lambda().eq(PrintDevEntity::getCommonUse, 1);

        if (Objects.nonNull(pagination.getVisibleType())) {
            queryWrapper.lambda().eq(PrintDevEntity::getVisibleType, pagination.getVisibleType());
        } else {
            //权限判断
            String userId;
            if (StringUtil.isNotEmpty(pagination.getUserId())) {
                userId = pagination.getUserId();
            } else {
                userId = UserProvider.getUser().getUserId();
            }
            UserEntity info = userService.getInfo(userId);

            if (!Objects.equals(info.getIsAdministrator(), 1)) {
                List<String> objectIds = new ArrayList<>();
                UserRelationIds userObjectIdList = userService.getUserObjectIdList(userId);
                objectIds.addAll(userObjectIdList.getPosition());
                objectIds.addAll(userObjectIdList.getRole());
                List<String> authList = authorizeService.getListByRoleIdsAndItemType(objectIds, AuthorizeConst.PRINT).stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());
                queryWrapper.lambda().and(
                        t -> t.eq(PrintDevEntity::getVisibleType, 1).or().in(!authList.isEmpty(), PrintDevEntity::getId, authList)
                );
            }
        }
        if (StringUtil.isNotEmpty(pagination.getSystemId())) {
            queryWrapper.lambda().eq(PrintDevEntity::getSystemId, pagination.getSystemId());
        }

        queryWrapper.lambda().orderByAsc(PrintDevEntity::getSortCode).orderByDesc(PrintDevEntity::getCreatorTime);
        if (Objects.equals(pagination.getDataType(), 1)) {
            return this.list(queryWrapper);
        }
        Page<PrintDevEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<PrintDevEntity> iPage = this.page(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    @Override
    public List<PrintDevEntity> getWorkSelector(List<String> id) {
        return this.baseMapper.getWorkSelector(id);
    }

    @Override
    public List<PrintExportVo> getExportList(String systemId) {
        List<PrintDevEntity> listBySystemId = this.baseMapper.getListBySystemId(systemId);
        List<PrintExportVo> voList = new ArrayList<>();
        for (PrintDevEntity item : listBySystemId) {
            List<PrintVersionEntity> list = printVersionService.getList(item.getId());
            if (list.isEmpty()) {
                continue;
            }
            PrintVersionEntity vInfo = list.stream().filter(t -> Objects.equals(t.getState(), 1)).findFirst().orElse(list.get(0));
            PrintDevInfoVO info = this.getVersionInfo(vInfo.getId());
            PrintExportVo vo = JsonUtil.getJsonToBean(info, PrintExportVo.class);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public boolean importCopy(List<PrintExportVo> list, String systemId) {
        try {
            if (CollectionUtils.isNotEmpty(list)) {
                for (PrintExportVo item : list) {
                    PrintDevEntity entity = JsonUtil.getJsonToBean(item, PrintDevEntity.class);
                    String id = RandomUtil.uuId();
                    entity.setId(id);
                    entity.setState(0);
                    entity.setCreatorTime(new Date());
                    entity.setCreatorUserId(UserProvider.getLoginUserId());
                    entity.setLastModifyTime(null);
                    entity.setLastModifyUserId(null);
                    entity.setSystemId(systemId);
                    if (StringUtil.isEmpty(entity.getEnCode())) {
                        entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.DY), code -> this.isExistByEnCode(code, null)));
                    }
                    this.save(entity);
                    //版本添加
                    createVerAndSet(item, id);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 添加版本和数据集
     *
     * @param item
     * @param id
     */
    private void createVerAndSet(PrintExportVo item, String id) {
        PrintVersionEntity versionEntity = new PrintVersionEntity();
        String versionId = RandomUtil.uuId();
        versionEntity.setId(versionId);
        versionEntity.setTemplateId(id);
        versionEntity.setCreatorUserId(UserProvider.getUser().getUserId());
        versionEntity.setCreatorTime(new Date());
        versionEntity.setPrintTemplate(item.getPrintTemplate());
        versionEntity.setConvertConfig(item.getConvertConfig());
        versionEntity.setGlobalConfig(item.getGlobalConfig());
        versionEntity.setVersion(1);
        versionEntity.setState(0);
        versionEntity.setSortCode(0l);
        printVersionService.save(versionEntity);
        //数据集创建
        List<DataSetForm> dataSetList = item.getDataSetList() != null ? JsonUtil.getJsonToList(item.getDataSetList(), DataSetForm.class) : new ArrayList<>();
        dataSetList.stream().forEach(t -> t.setId(null));
        dataSetService.create(dataSetList, DataSetTypeEnum.PRINT_VER.getCode(), versionId);
    }

    @Override
    public void deleteBySystemId(String systemId) {
        List<PrintDevEntity> listBySystemId = this.baseMapper.getListBySystemId(systemId);
        for (PrintDevEntity entity : listBySystemId) {
            printVersionService.removeByTemplateId(entity.getId());
            this.baseMapper.deleteById(entity);
        }
    }
}
