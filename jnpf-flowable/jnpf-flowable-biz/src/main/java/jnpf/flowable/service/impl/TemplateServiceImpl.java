package jnpf.flowable.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jnpf.base.UserInfo;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.export.TemplateExportVo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.AuthorizeConst;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.enums.TemplateJsonStatueEnum;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.template.*;
import jnpf.flowable.model.templatejson.FlowFormModel;
import jnpf.flowable.model.templatejson.TemplateJsonExportModel;
import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.TemplateService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.ServiceUtil;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends SuperServiceImpl<TemplateMapper, TemplateEntity> implements TemplateService {



    private final FlowUtil flowUtil;

    private final  ServiceUtil serviceUtil;


    private final TemplateJsonMapper templateJsonMapper;

    private final  TemplateNodeMapper templateNodeMapper;

    private final  TaskMapper taskMapper;

    private final  CommonMapper commonMapper;

    private final  TriggerTaskMapper triggerTaskMapper;

    private final  TemplateUseNumMapper templateUseNumMapper;

    @Override
    public List<TemplateEntity> getList(TemplatePagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<TemplatePageVo> getSelector(TemplatePagination pagination) {
        // 流程权限
        Set<String> flowId = new HashSet<>();
        String userId = UserProvider.getLoginUserId();
        if (StringUtils.isNotBlank(pagination.getDelegateUser())) {
            userId = pagination.getDelegateUser();
        }

        boolean isDelegate = ObjectUtil.equals(pagination.getIsDelegate(), 1);
        boolean isAuthority = ObjectUtil.equals(pagination.getIsAuthority(), 1);
        boolean isLaunch = ObjectUtil.equals(pagination.getIsLaunch(), 1);
        boolean isSystemFrom = ObjectUtil.equals(pagination.getIsSystemFrom(), 1);
        boolean isFree = ObjectUtil.equals(pagination.getIsFree(), 1);

        //委托流程的数据
        boolean isEntrust = StringUtils.equals("-1", pagination.getSystemId());
        //当前用户
        AuthorizeVO authorizeByUser = serviceUtil.getAuthorizeByUser();
        // 权限
        boolean commonUser = serviceUtil.isCommonUser(userId);
        if (commonUser && (isDelegate || isAuthority)) {
                flowId.addAll(authorizeByUser.getFlowIdList());
            }


        List<String> flowList = new ArrayList<>();
        List<DelegateEntity> delegateEntityList = flowUtil.getByToUserId(userId, 0);
        //获取委托
        for (DelegateEntity delegate : delegateEntityList) {
            List<String> launchPermission = serviceUtil.getPermission(delegate.getUserId());
            if (StringUtil.isNotEmpty(delegate.getFlowId())) {
                QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().in(TemplateEntity::getId, Arrays.asList(delegate.getFlowId().split(",")));
                List<TemplateEntity> list = this.list(queryWrapper);
                //用户拥有权限
                List<String> authoirtyList = list.stream().filter(e -> ObjectUtil.equals(e.getVisibleType(), FlowNature.AUTHORITY)).map(TemplateEntity::getId).collect(Collectors.toList());
                launchPermission.retainAll(authoirtyList);
                flowList.addAll(launchPermission);
                //发起列表显示，委托列表不显示
                if (!isDelegate) {
                    flowId.addAll(launchPermission);
                }
                //公开授权
                List<String> flowIdList = list.stream().map(TemplateEntity::getId).collect(Collectors.toList());
                flowIdList.removeAll(authoirtyList);
                flowList.addAll(flowIdList);
            } else {
                List<String> system = serviceUtil.getPermission(delegate.getUserId(), AuthorizeConst.SYSTEM);
                if (system.isEmpty()) {
                    continue;
                }
                QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().in(TemplateEntity::getSystemId, system)
                        .eq(TemplateEntity::getVisibleType, FlowNature.ALL);
                List<TemplateEntity> list = this.list(queryWrapper);
                List<String> flowIdList = list.stream().map(TemplateEntity::getId).collect(Collectors.toList());
                flowList.addAll(flowIdList);
                //发起列表显示，委托列表不显示
                if (!isDelegate) {
                    flowId.addAll(launchPermission);
                }
            }
        }

        MPJLambdaWrapper<TemplateEntity> wrapper = JoinWrappers.lambda(TemplateEntity.class)
                .selectAll(TemplateEntity.class)
                .eq(TemplateEntity::getEnabledMark, 1).ne(TemplateEntity::getType, FlowNature.QUEST)
                .eq(TemplateEntity::getStatus, TemplateStatueEnum.UP.getCode());

        //关键字（流程名称、流程编码）
        String keyWord = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyWord)) {
            wrapper.and(t -> t.like(TemplateEntity::getEnCode, keyWord).or().like(TemplateEntity::getFullName, keyWord));
        }
        //流程显示类型
        if (isLaunch) {
            List<Integer> typeList = ImmutableList.of(FlowNature.ALL_SHOW_TYPE, FlowNature.FLOW_SHOW_TYPE);
            wrapper.in(TemplateEntity::getShowType, typeList);
        }

        //自由流程
        if (isFree) {
            wrapper.ne(TemplateEntity::getType, FlowNature.FREE);
        }

        List<String> systemIdList = authorizeByUser.getSystemList().stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).map(SystemBaeModel::getId).collect(Collectors.toList());
        if (isEntrust) {
            if (flowList.isEmpty()) {
                return new ArrayList<>();
            }

            List<List<String>> lists = Lists.partition(flowList, 1000);
            wrapper.and(t -> {
                for (List<String> list : lists) {
                    t.in(TemplateEntity::getId, list).or();
                }
            });

            wrapper.notIn(!systemIdList.isEmpty(), TemplateEntity::getSystemId, systemIdList);
        } else {
            //应用主建
            String systemId = pagination.getSystemId();
            if (ObjectUtil.isNotEmpty(systemId)) {
                systemIdList.retainAll(Arrays.asList(systemId.split(",")));
            }

            if (systemIdList.isEmpty()) {
                return new ArrayList<>();
            }

            if (isAuthority && commonUser) {
                    wrapper.and(t -> t.eq(TemplateEntity::getVisibleType, FlowNature.ALL)
                            .or().in(!flowId.isEmpty(), TemplateEntity::getId, flowId)
                    );
                }


            wrapper.in(TemplateEntity::getSystemId, systemIdList);
            //所属分类
            String category = pagination.getCategory();
            if (ObjectUtil.isNotEmpty(category)) {
                if (StringUtils.equals(category, "commonFlow")) {
                    wrapper.leftJoin(CommonEntity.class, CommonEntity::getFlowId, TemplateJsonEntity::getId)
                            .eq(CommonEntity::getCreatorUserId, userId).isNotNull(CommonEntity::getFlowId);
                } else {
                    wrapper.in(TemplateEntity::getCategory, Arrays.asList(category.split(",")));
                }
            }
        }
        wrapper.orderByAsc(TemplateEntity::getSortCode).orderByDesc(TemplateEntity::getCreatorTime);
        Page<TemplatePageVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<TemplatePageVo> userPage = this.selectJoinListPage(page, TemplatePageVo.class, wrapper);

        List<TemplatePageVo> records = userPage.getRecords();
        // 设置常用流程标识
        List<CommonEntity> commonList = commonMapper.getCommonByUserId(userId);
        if (CollUtil.isNotEmpty(commonList)) {
            List<String> flowIds = commonList.stream().map(CommonEntity::getFlowId).distinct().collect(Collectors.toList());
            for (TemplatePageVo templatePageVo : records) {
                if (flowIds.contains(templatePageVo.getId())) {
                    templatePageVo.setIsCommonFlow(true);
                }
            }
        }

        //判断流程的表单类型
        if (isSystemFrom) {
            List<String> flowIdList = records.stream().map(TemplatePageVo::getFlowId).collect(Collectors.toList());
            List<TemplateNodeEntity> list = templateNodeMapper.getList(flowIdList, NodeEnum.START.getType());
            Map<String, String> formMap = new HashMap<>();
            for (TemplateNodeEntity entity : list) {
                formMap.put(entity.getFlowId(), entity.getFormId());
            }
            List<String> formList = serviceUtil.getFormList(new ArrayList<>(formMap.values())).stream().filter(e -> Objects.equals(e.getType(), 2)).map(VisualdevEntity::getId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(formList)) {
                for (TemplatePageVo templatePageVo : records) {
                    String formId = formMap.get(templatePageVo.getFlowId());
                    if (formList.contains(formId)) {
                        templatePageVo.setIsQuote(1);
                    }
                }
            }
        }
        return pagination.setData(records, page.getTotal());
    }

    @Override
    public List<TemplateTreeListVo> getTreeCommon() {
        String userId = UserProvider.getLoginUserId();
        List<TemplateTreeListVo> vos = new ArrayList<>();
        List<CommonEntity> commonList = commonMapper.getCommonByUserId(userId);
        if (CollUtil.isEmpty(commonList)) {
            return vos;
        }
        String systemCodeById = serviceUtil.getSystemCodeById(RequestContext.getAppCode());
        List<String> flowIds = commonList.stream().map(CommonEntity::getFlowId).collect(Collectors.toList());
        List<TemplateEntity> templateList = this.getList(flowIds);
        if (StringUtil.isNotEmpty(systemCodeById)) {
            templateList = templateList.stream().filter(e -> Objects.equals(e.getSystemId(), systemCodeById)).collect(Collectors.toList());
        }
        if (CollUtil.isNotEmpty(templateList)) {
            TemplateTreeListVo allVo = new TemplateTreeListVo();
            allVo.setId(RandomUtil.uuId());
            allVo.setFullName("全部流程");
            List<TemplateTreeListVo> childrenList = new ArrayList<>();
            for (TemplateEntity template : templateList) {
                TemplateTreeListVo vo = JsonUtil.getJsonToBean(template, TemplateTreeListVo.class);
                childrenList.add(vo);
            }
            allVo.setChildren(childrenList);
            vos.add(allVo);

            Map<String, List<TemplateTreeListVo>> map = childrenList.stream().collect(Collectors.groupingBy(TemplateTreeListVo::getCategory));
            List<String> categoryIds = new ArrayList<>(map.keySet());

            List<DictionaryDataEntity> dictionNameList = serviceUtil.getDictionName(categoryIds);
            for (DictionaryDataEntity dict : dictionNameList) {
                TemplateTreeListVo vo = new TemplateTreeListVo();
                vo.setId(dict.getId());
                vo.setFullName(dict.getFullName());
                vo.setChildren(map.get(dict.getId()));
                vos.add(vo);
            }
        }
        return vos;
    }

    @Override
    public List<TemplateTreeListVo> treeList(Integer formType) {
        TemplatePagination pagination = new TemplatePagination();
        pagination.setSystemId(serviceUtil.getSystemCodeById(RequestContext.getAppCode()));
        List<TemplateEntity> list = getListAll(pagination, false);
        List<String> flowIds = list.stream().map(TemplateEntity::getFlowId).collect(Collectors.toList());
        List<TemplateNodeEntity> startNodeList = new ArrayList<>();
        // formType 1.系统表单  2.在线开发表单
        if (null != formType) {
            // 版本主键
            startNodeList.addAll(templateNodeMapper.getList(flowIds, NodeEnum.START.getType()));
        }
        List<String> formIds = startNodeList.stream().map(TemplateNodeEntity::getFormId).collect(Collectors.toList());
        List<VisualdevEntity> formList = serviceUtil.getFormList(formIds);

        List<TemplateTreeListVo> vos = new ArrayList<>();
        List<TemplateTreeListVo> templateTreeListList = new ArrayList<>();
        if (StringUtils.isEmpty(pagination.getSystemId())) {
            List<String> systemId = list.stream().map(TemplateEntity::getSystemId).collect(Collectors.toList());
            templateTreeListList.addAll(JsonUtil.getJsonToList(serviceUtil.getSystemList(systemId), TemplateTreeListVo.class));
        } else {
            templateTreeListList.addAll(JsonUtil.getJsonToList(serviceUtil.getDiList(), TemplateTreeListVo.class));
        }

        for (TemplateTreeListVo dict : templateTreeListList) {
            TemplateTreeListVo vo = new TemplateTreeListVo();
            vo.setFullName(dict.getFullName());
            vo.setId(dict.getId());
            vo.setDisabled(true);
            if (CollUtil.isNotEmpty(list)) {
                List<TemplateEntity> childList = list.stream()
                        .filter(e -> dict.getId().equals(e.getCategory()) || dict.getId().equals(e.getSystemId())).collect(Collectors.toList());

                if (null != formType) {
                    childList = childList.stream().filter(e -> {
                        // 是流程显示类型
                        if (ObjectUtil.equals(e.getShowType(), FlowNature.FLOW_SHOW_TYPE)) {
                            return false;
                        }
                        TemplateNodeEntity node = startNodeList.stream().filter(a -> a.getFlowId().equals(e.getFlowId())).findFirst().orElse(null);
                        if (null != node) {
                            VisualdevEntity form = formList.stream().filter(b -> b.getId().equals(node.getFormId())).findFirst().orElse(null);
                            if (null != form) {
                                return form.getType().equals(1);
                            }
                        }
                        return false;
                    }).collect(Collectors.toList());
                }

                if (CollUtil.isNotEmpty(childList)) {
                    childList = childList.stream()
                            .sorted(Comparator.comparing(TemplateEntity::getSortCode, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TemplateEntity::getCreatorTime).reversed())
                            .collect(Collectors.toList());
                    vo.setNum(childList.size());
                    vo.setChildren(JsonUtil.getJsonToList(childList, TemplateTreeListVo.class));
                    vos.add(vo);
                }
            }
        }
        return vos;
    }

    @Override
    public List<TemplateTreeListVo> treeListWithPower() {
        List<TemplateTreeListVo> vos = new ArrayList<>();

        MPJLambdaWrapper<TemplateEntity> wrapper = JoinWrappers.lambda(TemplateEntity.class)
                .selectAll(TemplateEntity.class)
                .eq(TemplateEntity::getVisibleType, FlowNature.AUTHORITY)
                .isNotNull(TemplateEntity::getFlowId);

        List<TemplateEntity> list = this.selectJoinList(TemplateEntity.class, wrapper);
        List<String> systemIdList = list.stream().map(TemplateEntity::getSystemId).collect(Collectors.toList());
        List<SystemEntity> systemList = serviceUtil.getSystemList(systemIdList);
        for (SystemEntity system : systemList) {
            TemplateTreeListVo vo = new TemplateTreeListVo();
            vo.setFullName(system.getFullName());
            vo.setId(system.getId());
            vo.setDisabled(true);
            if (CollUtil.isNotEmpty(list)) {
                List<TemplateEntity> childList = list.stream()
                        .filter(e -> system.getId().equals(e.getSystemId()))
                        .sorted(Comparator.comparing(TemplateEntity::getSortCode, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TemplateEntity::getCreatorTime, Comparator.reverseOrder()))
                        .collect(Collectors.toList());

                if (CollUtil.isNotEmpty(childList)) {
                    vo.setNum(childList.size());
                    vo.setChildren(JsonUtil.getJsonToList(childList, TemplateTreeListVo.class));
                    vos.add(vo);
                }
            }
        }

        return vos;
    }

    @Override
    public List<TemplateEntity> getListAll(TemplatePagination pagination, boolean isPage) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getEnabledMark, 1).eq(TemplateEntity::getStatus, TemplateStatueEnum.UP.getCode());
        if (ObjectUtil.isNotEmpty(pagination.getType())) {
            flag = true;
            queryWrapper.lambda().eq(TemplateEntity::getType, pagination.getType());
        }
        if (ObjectUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().like(TemplateEntity::getFullName, pagination.getKeyword());
        }
        if (ObjectUtil.isNotEmpty(pagination.getCategory())) {
            flag = true;
            queryWrapper.lambda().eq(TemplateEntity::getCategory, pagination.getCategory());
        }
        if (ObjectUtil.isNotEmpty(pagination.getTemplateIdList())) {
            queryWrapper.lambda().in(TemplateEntity::getId, pagination.getTemplateIdList());
        }
        if (ObjectUtil.isNotEmpty(pagination.getSystemId())) {
            queryWrapper.lambda().eq(TemplateEntity::getSystemId, pagination.getSystemId());
        }
        queryWrapper.lambda().orderByAsc(TemplateEntity::getSortCode).orderByDesc(TemplateEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(TemplateEntity::getLastModifyTime);
        }
        queryWrapper.lambda().select(
                TemplateEntity::getId, TemplateEntity::getEnCode,
                TemplateEntity::getFullName, TemplateEntity::getFlowId,
                TemplateEntity::getStatus, TemplateEntity::getShowType,
                TemplateEntity::getType, TemplateEntity::getIcon,
                TemplateEntity::getCategory, TemplateEntity::getIconBackground,
                TemplateEntity::getCreatorUserId, TemplateEntity::getSortCode,
                TemplateEntity::getEnabledMark, TemplateEntity::getCreatorTime,
                TemplateEntity::getSystemId
        );
        if (isPage) {
            Page<TemplateEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
            IPage<TemplateEntity> userPage = this.page(page, queryWrapper);
            return pagination.setData(userPage.getRecords(), page.getTotal());
        } else {
            return this.list(queryWrapper);
        }
    }

    @Override
    public List<TemplateEntity> getListByFlowIds(List<String> flowIds) {
        return flowUtil.getListByFlowIds(flowIds);
    }

    @Override
    public TemplateEntity getInfo(String id) throws WorkFlowException {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id, String systemId) {
        return this.baseMapper.isExistByFullName(fullName, id, systemId);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id, String systemId) {
        return this.baseMapper.isExistByEnCode(enCode, id, systemId);
    }

    @Override
    public void create(TemplateEntity entity, String flowXml, Map<String, Map<String, Object>> flowNodes) throws WorkFlowException {
        entity.setSystemId(serviceUtil.getSystemCodeById(RequestContext.getAppCode()));
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(getEnCode(entity));
        }
        if (isExistByFullName(entity.getFullName(), entity.getId(), entity.getSystemId())) {
            throw new WorkFlowException(MsgCode.EXIST001.get());
        }
        if (isExistByEnCode(entity.getEnCode(), entity.getId(), null)) {
            throw new WorkFlowException(MsgCode.EXIST002.get());
        }
        UserInfo userInfo = UserProvider.getUser();
        entity.setId(StringUtil.isNotEmpty(entity.getId()) ? entity.getId() : RandomUtil.uuId());
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(new Date());
        entity.setFlowId(null);
        entity.setEnabledMark(0);
        entity.setStatus(TemplateStatueEnum.NONE.getCode());
        entity.setLastModifyUserId(null);
        entity.setLastModifyTime(null);
        this.setIgnoreLogicDelete().removeById(entity.getId());
        this.setIgnoreLogicDelete().saveOrUpdate(entity);
        this.clearIgnoreLogicDelete();
        TemplateNodeUpFrom from = new TemplateNodeUpFrom();
        from.setId(entity.getId());
        from.setFlowXml(flowXml);
        from.setFlowNodes(flowNodes);
        flowUtil.create(from);
    }

    @Override
    public boolean update(String id, TemplateEntity entity) throws WorkFlowException {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(getEnCode(entity));
        }
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(TemplateEntity entity) throws WorkFlowException {
        if (entity != null) {
            if (ObjectUtil.equals(entity.getType(), FlowNature.QUEST)) {
                List<TemplateJsonEntity> list = templateJsonMapper.getList(entity.getId());
                List<String> flowIds = list.stream().map(TemplateJsonEntity::getId).collect(Collectors.toList());
                if (triggerTaskMapper.checkByFlowIds(flowIds)) {
                    throw new WorkFlowException(MsgCode.WF139.get());
                }
            } else {
                List<TaskEntity> taskList = taskMapper.getTaskByTemplate(entity.getId());
                if (CollUtil.isNotEmpty(taskList)) {
                    throw new WorkFlowException(MsgCode.WF124.get());
                }
            }
            commonMapper.deleteFlow(entity.getId());
            templateUseNumMapper.deleteUseNum(entity.getId(), null);
            this.removeById(entity.getId());
            List<String> idList = templateJsonMapper.getList(entity.getId()).stream().map(TemplateJsonEntity::getId).collect(Collectors.toList());
            templateJsonMapper.delete(idList);
            templateNodeMapper.delete(idList);
        }
    }

    @Override
    public void copy(TemplateEntity entity) throws WorkFlowException {
        try {
            TemplateJsonEntity jsonEntity = StringUtil.isNotEmpty(entity.getFlowId()) ? templateJsonMapper.getInfo(entity.getFlowId()) : null;
            String formXml = jsonEntity != null ? jsonEntity.getFlowXml() : null;
            List<TemplateNodeEntity> list = jsonEntity != null ? templateNodeMapper.getList(jsonEntity.getId()) : new ArrayList<>();
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            entity.setEnCode(entity.getEnCode() + copyNum);
            entity.setId(null);
            Map<String, Map<String, Object>> flowNodes = new HashMap<>();
            for (TemplateNodeEntity nodeEntity : list) {
                flowNodes.put(nodeEntity.getNodeCode(), JsonUtil.stringToMap(nodeEntity.getNodeJson()));
            }
            this.create(entity, formXml, flowNodes);
        } catch (Exception e) {
            throw new WorkFlowException(MsgCode.PRI006.get());
        }
    }

    @Override
    public TemplateExportModel export(String id) throws WorkFlowException {
        TemplateEntity entity = getInfo(id);
        TemplateExportModel exportModel = new TemplateExportModel();
        exportModel.setTemplate(entity);
        // 版本
        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(entity.getFlowId());
        TemplateJsonExportModel versionModel = JsonUtil.getJsonToBean(jsonEntity, TemplateJsonExportModel.class);
        // 节点
        List<TemplateNodeEntity> list = templateNodeMapper.getList(entity.getFlowId());
        exportModel.setNodeList(list);
        exportModel.setFlowVersion(versionModel);
        return exportModel;
    }

    @Override
    public void importData(TemplateExportModel model, String type) throws WorkFlowException {
        TemplateEntity entity = model.getTemplate();
        TemplateJsonExportModel versionModel = model.getFlowVersion();
        String systemId = serviceUtil.getSystemCodeById(RequestContext.getAppCode());
        if (null != entity) {
            entity.setFlowId(null);
            entity.setVersion(null);
            entity.setCreatorUserId(UserProvider.getLoginUserId());
            entity.setCreatorTime(new Date());
            entity.setLastModifyTime(null);
            entity.setLastModifyUserId(null);
            entity.setEnabledMark(0);
            if (!Objects.equals(systemId, entity.getSystemId())) {
                entity.setId(RandomUtil.uuId());
                entity.setSystemId(systemId);
                entity.setEnCode(getEnCode(entity));
            }
            List<String> errList = new ArrayList<>();
            // type: 0.当导入数据不存在，作为新数据导入；数据已存在，不做处理  1.当导入数据已存在，增加相同记录新数据，名称和编码自动增加随机码
            TemplateEntity templateEntity = checkImportEntity(entity, type, errList);

            if (!errList.isEmpty()) {
                StringJoiner joiner = new StringJoiner("；");
                joiner.add(String.join("、", errList) + MsgCode.IMP007.get());
                if (StringUtil.isNotEmpty(joiner.toString())) {
                    throw new WorkFlowException(joiner.toString());
                }
            }
            Map<String, Map<String, Object>> flowNodes = new HashMap<>();
            for (TemplateNodeEntity nodeEntity : model.getNodeList()) {
                flowNodes.put(nodeEntity.getNodeCode(), JsonUtil.stringToMap(nodeEntity.getNodeJson()));
            }
            this.create(templateEntity, versionModel.getFlowXml(), flowNodes);
        }
    }

    @Override
    public List<TemplateEntity> getList(List<String> ids) {
        return this.baseMapper.getList(ids);
    }

    @Override
    public List<TemplateEntity> getListByIds(List<String> ids) {
        return this.baseMapper.getListByIds(ids);
    }

    @Override
    public List<TemplateEntity> getListOfHidden(List<String> ids) {
        return this.baseMapper.getListOfHidden(ids);
    }

    // 校验导入的实体
    public TemplateEntity checkImportEntity(TemplateEntity templateEntity, String type, List<String> errList) {
        TemplateEntity entity = JsonUtil.getJsonToBean(templateEntity, TemplateEntity.class);
        boolean skip = Objects.equals("0", type);
        int num = 0;
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getId, entity.getId());
        if (this.count(queryWrapper) > 0) {
            num++;
            if (skip) {
                errList.add("ID");
            }
        }
        if (isExistByEnCode(entity.getEnCode(), null, entity.getSystemId())) {
            num++;
            if (skip) {
                errList.add(MsgCode.IMP009.get());
            }
        }
        if (isExistByFullName(entity.getFullName(), null, entity.getSystemId())) {
            num++;
            if (skip) {
                errList.add(MsgCode.IMP008.get());
            }
        }
        if (num > 0 && !skip) {
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            entity.setEnCode(entity.getEnCode() + copyNum);
        }
        entity.setId(RandomUtil.uuId());
        return entity;
    }

    @Override
    public FlowByFormModel getFlowByFormId(String formId, Boolean start) {
        FlowByFormModel res = new FlowByFormModel();
        List<TemplateByFormModel> resList = new ArrayList<>();

        QueryWrapper<TemplateNodeEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TemplateNodeEntity::getFormId, formId);
        // start 传true 仅发起节点   false 全部节点
        start = null == start || start;
        if (start) {
            wrapper.lambda().eq(TemplateNodeEntity::getNodeType, NodeEnum.START.getType());
        }
        List<TemplateNodeEntity> nodeList = templateNodeMapper.selectList(wrapper);
        if (CollUtil.isEmpty(nodeList)) {
            return res;
        }
        List<String> flowIds = nodeList.stream().map(TemplateNodeEntity::getFlowId).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(flowIds)) {
            return res;
        }
        List<TemplateJsonEntity> versionList = templateJsonMapper.selectByIds(flowIds);
        if (CollUtil.isEmpty(versionList)) {
            return res;
        }
        // 获取启用版本
        versionList = versionList.stream().filter(e -> e.getState().equals(TemplateJsonStatueEnum.START.getCode()))
                .sorted(Comparator.comparing(TemplateJsonEntity::getCreatorTime).reversed()).collect(Collectors.toList());
        // 获取流程
        List<String> templateIds = versionList.stream().map(TemplateJsonEntity::getTemplateId).distinct().collect(Collectors.toList());
        List<TemplateEntity> templateList = CollUtil.isNotEmpty(templateIds) ? this.listByIds(templateIds) : new ArrayList<>();
        // 获取权限流程
        String loginUserId = UserProvider.getLoginUserId();
        //是否普通用户
        boolean commonUser = serviceUtil.isCommonUser(loginUserId);
        List<String> templatePermissionIds = new ArrayList<>();
        if (commonUser) {
            templatePermissionIds.addAll(serviceUtil.getLaunchPermission());
        }
        for (TemplateJsonEntity jsonEntity : versionList) {
            TemplateByFormModel model = new TemplateByFormModel();
            model.setId(jsonEntity.getId());

            TemplateEntity template = templateList.stream()
                    .filter(e -> ObjectUtil.equals(e.getId(), jsonEntity.getTemplateId())).findFirst().orElse(null);
            if (null == template || !ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                continue;
            }
            if (commonUser && ObjectUtil.equals(template.getVisibleType(), FlowNature.AUTHORITY)) {
                // 带权限
                if (templatePermissionIds.contains(jsonEntity.getId())) {
                    model.setFullName(template.getFullName());
                    resList.add(model);
                }
            } else {
                model.setFullName(template.getFullName());
                resList.add(model);
            }
        }
        res.setList(resList);
        res.setIsConfig(true);
        return res;
    }

    @Override
    public List<UserEntity> getSubFlowUserList(String flowId, TemplatePagination pagination) throws WorkFlowException {
        TemplateEntity template = this.getInfo(flowId);
        return serviceUtil.getLaunchUserByTemplateId(template, pagination);
    }

    @Override
    public VisualdevEntity getFormByTemplateId(String templateId) throws WorkFlowException {
        TemplateEntity template = this.getInfo(templateId);
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(template.getFlowId());

        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        return serviceUtil.getFormInfo(nodeEntity.getFormId());
    }

    @Override
    public FlowFormModel getFormIdAndFlowId(List<String> userIdAll, String templateId) throws WorkFlowException {
        return flowUtil.getFormIdAndFlowId(userIdAll, templateId);
    }

    @Override
    public List<String> getFormList() {
        List<String> resList = new ArrayList<>();
        List<TemplateJsonEntity> list = templateJsonMapper.getListOfEnable();
        if (CollUtil.isNotEmpty(list)) {
            List<String> flowIds = list.stream().map(TemplateJsonEntity::getId).distinct().collect(Collectors.toList());
            List<TemplateNodeEntity> startNodeList = templateNodeMapper.getList(flowIds, NodeEnum.START.getType());
            resList = startNodeList.stream().map(TemplateNodeEntity::getFormId).distinct().collect(Collectors.toList());
        }
        return resList;
    }

    @Override
    public Map<String, String> getFlowFormMap() {
        Map<String, String> map = new HashMap<>();
        List<TemplateJsonEntity> listOfEnable = templateJsonMapper.getListOfEnable();
        List<TemplateNodeEntity> listStart = templateNodeMapper.getListStart(listOfEnable);
        List<String> collect = listStart.stream().map(TemplateNodeEntity::getFlowId).collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(collect)) {
            QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(TemplateEntity::getId, TemplateEntity::getFlowId);
            queryWrapper.lambda().in(TemplateEntity::getFlowId, collect);
            Map<String, String> flowTempMap = this.list(queryWrapper).stream().collect(Collectors.toMap(TemplateEntity::getFlowId, TemplateEntity::getId));
            for (TemplateNodeEntity templateNodeEntity : listStart) {
                if (ObjectUtil.isNotEmpty(flowTempMap.get(templateNodeEntity.getFlowId()))) {
                    map.put(flowTempMap.get(templateNodeEntity.getFlowId()), templateNodeEntity.getFormId());
                }
            }
        }
        return map;
    }

    @Override
    public List<TemplatePageVo> getCommonList(TemplatePagination pagination) {
        AuthorizeVO authorize = pagination.getAuthorize();
        if (authorize == null) {
            authorize = serviceUtil.getAuthorizeByUser();
        }
        List<String> systemIdList = authorize.getSystemList().stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).map(SystemBaeModel::getId).collect(Collectors.toList());
        List<String> flowId = authorize.getFlowIdList();
        String userId = UserProvider.getLoginUserId();
        MPJLambdaWrapper<TemplateEntity> wrapper = JoinWrappers.lambda(TemplateEntity.class)
                .selectAll(TemplateEntity.class)
                .leftJoin(CommonEntity.class, CommonEntity::getFlowId, TemplateEntity::getId)
                .eq(TemplateEntity::getEnabledMark, 1).ne(TemplateEntity::getType, FlowNature.QUEST)
                .eq(TemplateEntity::getStatus, TemplateStatueEnum.UP.getCode());
        //应用主建
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            systemIdList.retainAll(Arrays.asList(systemId.split(",")));
        }

        if (systemIdList.isEmpty()) {
            return new ArrayList<>();
        }

        wrapper.and(t -> t.eq(TemplateEntity::getVisibleType, FlowNature.ALL)
                .or().in(!flowId.isEmpty(), TemplateEntity::getId, flowId)
        );

        wrapper.in(TemplateEntity::getSystemId, systemIdList);

        //关键字（流程名称、流程编码）
        String keyWord = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyWord)) {
            wrapper.and(t -> t.like(TemplateEntity::getEnCode, keyWord).or().like(TemplateEntity::getFullName, keyWord));
        }
        wrapper.eq(CommonEntity::getCreatorUserId, userId);
        wrapper.orderByAsc(TemplateEntity::getSortCode).orderByDesc(TemplateEntity::getCreatorTime);
        List<TemplatePageVo> list = this.selectJoinList(TemplatePageVo.class, wrapper);

        //添加应用信息
        List<String> sysIds = list.stream().map(TemplatePageVo::getSystemId).collect(Collectors.toList());
        Map<String, SystemEntity> sysMap = serviceUtil.getSystemList(sysIds).stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        list.stream().forEach(t -> t.setSystemName(sysMap.get(t.getSystemId()) != null ? sysMap.get(t.getSystemId()).getFullName() : ""));
        return list;
    }

    @Override
    public String getEnCode(TemplateEntity entity) {
        String code = serviceUtil.getCode();
        boolean existByEnCode = isExistByEnCode(code, entity.getId(), null);
        if (existByEnCode) {
            return getEnCode(entity);
        }
        return code;
    }

    @Override
    public List<TemplateEntity> getListByCreUser(String creUser) {
        return this.baseMapper.getListByCreUser(creUser);
    }

    @Override
    public List<TemplateExportVo> getExportList(String systemId) {
        List<TemplateEntity> temList = this.baseMapper.getListBySystemId(systemId);
        List<TemplateExportVo> voList = new ArrayList<>();
        for (TemplateEntity item : temList) {
            try {
                TemplateExportVo exportModel = new TemplateExportVo();
                exportModel.setTemplate(item);
                // 版本
                TemplateJsonEntity jsonEntity = templateJsonMapper.getList(item.getId()).get(0);
                TemplateJsonExportModel versionModel = JsonUtil.getJsonToBean(jsonEntity, TemplateJsonExportModel.class);
                // 节点
                List<Object> list = new ArrayList<>(templateNodeMapper.getList(jsonEntity.getId()));
                exportModel.setNodeList(list);
                exportModel.setFlowVersion(versionModel);
                voList.add(exportModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return voList;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        try {
            List<TemplateEntity> temList = this.baseMapper.getListBySystemId(systemId);
            for (TemplateEntity item : temList) {
                TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(item.getFlowId());
                templateJsonMapper.deleteById(jsonEntity);
                List<TemplateNodeEntity> nodeList = templateNodeMapper.getList(item.getFlowId());
                templateNodeMapper.deleteByIds(nodeList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
