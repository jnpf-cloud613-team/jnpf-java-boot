package jnpf.flowable.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.service.SuperServiceImpl;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.enums.TemplateJsonStatueEnum;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.job.QuartzJobUtil;
import jnpf.flowable.job.TimeTriggerJob;
import jnpf.flowable.job.TriggerJobUtil;
import jnpf.flowable.mapper.TemplateJsonMapper;
import jnpf.flowable.mapper.TemplateMapper;
import jnpf.flowable.mapper.TemplateNodeMapper;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.template.FlowConfigModel;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.trigger.TimeTriggerModel;
import jnpf.flowable.service.TemplateJsonService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.ServiceUtil;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateJsonServiceImpl extends SuperServiceImpl<TemplateJsonMapper, TemplateJsonEntity> implements TemplateJsonService {


    private final FlowAbleUrl flowAbleUrl;

    private final ServiceUtil serviceUtil;

    private final RedisUtil redisUtil;

    private final FlowUtil flowUtil;


    private final TemplateMapper templateMapper;

    private final TemplateNodeMapper templateNodeMapper;

    @Override
    public List<TemplateJsonEntity> getListByTemplateIds(List<String> id) {
        return this.baseMapper.getListByTemplateIds(id);
    }

    @Override
    public List<TemplateJsonEntity> getList(String templateId) {
        return this.baseMapper.getList(templateId);
    }

    @Override
    public List<TemplateJsonEntity> getListOfEnable() {
        return this.baseMapper.getListOfEnable();
    }

    @Override
    public TemplateJsonEntity getInfo(String id) throws WorkFlowException {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean update(String id, TemplateJsonEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @DSTransactional
    @Override
    public void save(TemplateNodeUpFrom from) throws WorkFlowException {
        String templateJsonId = from.getFlowId();
        String id = from.getId();
        String flowXml = from.getFlowXml();
        Map<String, Map<String, Object>> flowNodes = from.getFlowNodes();
        TemplateJsonEntity jsonEntity = getInfo(templateJsonId);
        jsonEntity.setFlowXml(flowXml);
        TemplateEntity entity = templateMapper.getInfo(id);
        String flowConfig = from.getFlowConfig();
        entity.setFlowConfig(flowConfig);
        FlowConfigModel config = JsonUtil.getJsonToBean(flowConfig, FlowConfigModel.class);
        config = config == null ? new FlowConfigModel() : config;
        entity.setVisibleType(config.getVisibleType());
        //发布流程
        if (Objects.equals(from.getType(), 1)) {
            if (StringUtil.isEmpty(jsonEntity.getFlowableId())) {
                String deploymentId = flowAbleUrl.deployFlowAble(flowXml, templateJsonId);
                jsonEntity.setFlowableId(deploymentId);
            }
            //改流程版本
            if (StringUtil.isNotEmpty(id)) {
                TemplateJsonEntity info = getList(id).stream().filter(t -> Objects.equals(t.getState(), TemplateJsonStatueEnum.START.getCode())).findFirst().orElse(null);
                int state = jsonEntity.getState();
                if (info != null) {
                    // 变更归档状态，排序码
                    info.setState(TemplateJsonStatueEnum.HISTORY.getCode());
                    this.update(info.getId(), info);
                }
                jsonEntity.setState(TemplateJsonStatueEnum.START.getCode());
                entity.setFlowId(templateJsonId);
                entity.setFlowableId(jsonEntity.getFlowableId());
                entity.setVersion(jsonEntity.getVersion());
                entity.setEnabledMark(1);
                entity.setStatus(TemplateStatueEnum.UP.getCode());
                // 归档状态的仅启用
                if (Objects.equals(2, state)) {
                    if (StringUtil.isEmpty(entity.getEnCode())) {
                        entity.setEnCode(flowUtil.getEnCode(entity));
                    }
                    templateMapper.update(entity.getId(), entity);
                    this.update(jsonEntity.getId(), jsonEntity);
                    return;
                }
            }
        }
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(flowUtil.getEnCode(entity));
        }
        templateMapper.update(entity.getId(), entity);

        TemplateNodeEntity timeTriggerNode = null;
        if (ObjectUtil.isNotEmpty(flowNodes)) {
            List<TemplateNodeEntity> list = templateNodeMapper.getList(jsonEntity.getId());
            List<String> deleteList = list.stream().map(TemplateNodeEntity::getId).collect(Collectors.toList());
            templateNodeMapper.delete(deleteList);
            // 开始节点的表单
            String startFormId = null;
            for (Map.Entry<String, Map<String, Object>> stringMapEntry : flowNodes.entrySet()) {
                String key = stringMapEntry.getKey();
                NodeModel startNode = JsonUtil.getJsonToBean(flowNodes.get(key), NodeModel.class);
                if (ObjectUtil.equals(startNode.getType(), NodeEnum.START.getType())) {
                    startFormId = startNode.getFormId();
                    break;
                }
            }
            for (Map.Entry<String, Map<String, Object>> stringMapEntry : flowNodes.entrySet()) {
                String key = stringMapEntry.getKey();
                NodeModel nodeModel = JsonUtil.getJsonToBean(flowNodes.get(key), NodeModel.class);
                TemplateNodeEntity nodeEntity = list.stream().filter(t -> ObjectUtil.equals(t.getNodeCode(), key)).findFirst().orElse(new TemplateNodeEntity());
                nodeEntity.setId(RandomUtil.uuId());
                nodeEntity.setFlowId(jsonEntity.getId());
                nodeEntity.setNodeCode(key);
                nodeEntity.setNodeJson(JsonUtil.getObjectToString(flowNodes.get(key)));
                nodeEntity.setNodeType(nodeModel.getType());
                if (StringUtil.isNotEmpty(nodeModel.getFormId())) {
                    nodeEntity.setFormId(nodeModel.getFormId());
                } else {
                    nodeEntity.setFormId(startFormId);
                }
                // 触发节点：定时、webhook 将flowId设置到formId字段   通知将msgId设置到formId
                if (ObjectUtil.equals(nodeModel.getType(), NodeEnum.TIME_TRIGGER.getType())) {
                    nodeEntity.setFormId(nodeEntity.getFlowId());
                    if (Objects.equals(from.getType(), 1)) {
                        timeTriggerNode = nodeEntity;
                    }
                } else if (ObjectUtil.equals(nodeModel.getType(), NodeEnum.WEBHOOK_TRIGGER.getType())) {
                    nodeEntity.setFormId(nodeEntity.getFlowId());
                } else if (ObjectUtil.equals(nodeModel.getType(), NodeEnum.NOTICE_TRIGGER.getType())) {
                    nodeEntity.setFormId(nodeModel.getNoticeId());
                }
                templateNodeMapper.insertOrUpdate(nodeEntity);
            }

        }
        this.update(jsonEntity.getId(), jsonEntity);
        if (null != timeTriggerNode) {
            this.timeTrigger(timeTriggerNode);
        }
    }

    public void timeTrigger(TemplateNodeEntity nodeEntity) {
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        String id = RandomUtil.uuId();
        TimeTriggerModel model = new TimeTriggerModel();
        model.setId(id);
        model.setFlowId(nodeEntity.getFlowId());
        model.setUserInfo(UserProvider.getUser());
        model.setCron(nodeModel.getCron());
        model.setEndLimit(nodeModel.getEndLimit());
        model.setEndTimeType(nodeModel.getEndTimeType());

        String start = nodeModel.getStartTime();
        String end = nodeModel.getEndTime();
        Date startTime = ObjectUtil.isNotEmpty(start) ? new Date(Long.parseLong(start)) : new Date();
        Date endTime = ObjectUtil.equals(nodeModel.getEndTimeType(), 2) ? new Date(Long.parseLong(end)) : null;
        model.setStartTime(startTime.getTime());
        model.setEndTime(endTime != null ? endTime.getTime() : null);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAll(JsonUtil.entityToMap(model));

        redisUtil.removeHash(TriggerJobUtil.TRIGGER_MODEL, model.getId());
        boolean isAdd = endTime == null || endTime.getTime() > System.currentTimeMillis();
        if (isAdd) {
            QuartzJobUtil.addJob(id, nodeModel.getCron(), TimeTriggerJob.class, jobDataMap, startTime, endTime);
        }
    }

    @DSTransactional
    @Override
    public void create(TemplateNodeUpFrom from) {
        flowUtil.create(from);
    }

    @Override
    public void delete(List<String> idList) {
        this.baseMapper.delete(idList);
        templateNodeMapper.delete(idList);
    }

    @Override
    public void copy(TemplateJsonEntity entity, String flowId) {
        List<TemplateNodeEntity> list = templateNodeMapper.getList(entity.getId());
        Map<String, Map<String, Object>> flowNodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : list) {
            Map<String, Object> map = JsonUtil.stringToMap(nodeEntity.getNodeJson());
            if (ObjectUtil.equals(nodeEntity.getNodeType(), NodeEnum.WEBHOOK_TRIGGER.getType())) {
                map.put("webhookUrl", "");
            }
            flowNodes.put(nodeEntity.getNodeCode(), map);
        }
        TemplateNodeUpFrom from = new TemplateNodeUpFrom();
        from.setId(entity.getTemplateId());
        from.setFlowXml(entity.getFlowXml());
        from.setFlowNodes(flowNodes);
        from.setFlowId(flowId);
        create(from);
    }

    @Override
    public TemplateJsonInfoVO getInfoVo(String id) throws WorkFlowException {
        TemplateJsonEntity jsonEntity = this.getInfo(id);
        TemplateEntity entity = templateMapper.getInfo(jsonEntity.getTemplateId());
        TemplateJsonInfoVO vo = JsonUtil.getJsonToBean(entity, TemplateJsonInfoVO.class);
        vo.setFlowXml(jsonEntity.getFlowXml());
        List<TemplateNodeEntity> templateNodeList = templateNodeMapper.getList(jsonEntity.getId());
        Map<String, Map<String, Object>> flowNodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : templateNodeList) {
            flowNodes.put(nodeEntity.getNodeCode(), JsonUtil.stringToMap(nodeEntity.getNodeJson()));
        }
        vo.setFlowableId(jsonEntity.getFlowableId());
        vo.setFlowNodes(flowNodes);
        vo.setFlowId(jsonEntity.getId());
        return vo;
    }

    @Override
    public VisualdevEntity getFormInfo(String id) throws WorkFlowException {
        TemplateEntity template = templateMapper.getInfo(id);
        String flowId = template.getFlowId();
        TemplateJsonEntity jsonEntity = this.getInfo(flowId);
        List<TemplateNodeEntity> templateNodeList = templateNodeMapper.getList(jsonEntity.getId());
        TemplateNodeEntity global = templateNodeList.stream().filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.GLOBAL.getType())).findFirst().orElse(null);
        if (null != global) {
            return serviceUtil.getFormInfo(global.getFormId());
        }
        return null;
    }
}
