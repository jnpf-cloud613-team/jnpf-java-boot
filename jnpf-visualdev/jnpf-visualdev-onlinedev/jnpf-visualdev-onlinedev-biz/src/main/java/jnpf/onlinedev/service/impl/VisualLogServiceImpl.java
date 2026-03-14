package jnpf.onlinedev.service.impl;

import cn.hutool.core.bean.BeanUtil;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.model.VisualLogModel;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualdevReleaseService;
import jnpf.constant.JnpfConst;
import jnpf.event.ProjectEventListener;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableFields;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.module.ProjectEventBuilder;
import jnpf.module.ProjectEventInstance;
import jnpf.onlinedev.entity.VisualLogEntity;
import jnpf.onlinedev.mapper.VisualLogMapper;
import jnpf.onlinedev.model.enums.OnlineDataTypeEnum;
import jnpf.onlinedev.model.log.VisualLogForm;
import jnpf.onlinedev.model.log.VisualLogPage;
import jnpf.onlinedev.service.VisualLogService;
import jnpf.onlinedev.util.OnlinePublicUtils;
import jnpf.onlinedev.util.OnlineSwapDataUtils;
import jnpf.util.JsonUtil;
import jnpf.util.PublishEventUtil;
import jnpf.util.TableFeildsEnum;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据日志实现
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/27 18:24:42
 */
@Service
@RequiredArgsConstructor
public class VisualLogServiceImpl extends SuperServiceImpl<VisualLogMapper, VisualLogEntity> implements VisualLogService {

    private final VisualdevReleaseService visualdevReleaseService;
    private final OnlineSwapDataUtils onlineSwapDataUtils;

    /**
     * 创建日志事件
     *
     * @param form 数据id
     */
    @Override
    public void createEventLog(VisualLogForm form) {
        PublishEventUtil.publishLocalEvent(new ProjectEventBuilder(JnpfConst.VSLOG_EVENT_KEY, form));
    }

    @Override
    public List<VisualLogEntity> getList(VisualLogPage pagination) {
        return this.baseMapper.getList(pagination);
    }

    /**
     * 监听日志插入
     *
     * @param redisEvent
     */
    @ProjectEventListener(channelRegex = JnpfConst.VSLOG_EVENT_KEY + ".*")
    public void createLogByEvent(ProjectEventInstance redisEvent) {
        VisualLogForm form = (VisualLogForm) redisEvent.getSource();
        VisualLogEntity visualLogEntity = new VisualLogEntity();
        visualLogEntity.setType(form.getType());
        visualLogEntity.setModelId(form.getModelId());
        visualLogEntity.setDataId(form.getDataId());

        //修改数据
        if (Objects.equals(form.getType(), 1)) {
            List<VisualLogModel> listLog = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(form.getListLog())) {
                listLog = form.getListLog();
            } else {
                addLog(form, listLog);
            }
            visualLogEntity.setDataLog(JsonUtil.getObjectToString(listLog));
            if (CollectionUtils.isNotEmpty(listLog)) {
                this.save(visualLogEntity);
            }
        } else {
            //新增数据
            this.save(visualLogEntity);
        }
    }

    public void addLog(VisualLogForm formSource, List<VisualLogModel> listLog) {
        VisualLogForm form = BeanUtil.copyProperties(formSource, VisualLogForm.class);
        VisualdevReleaseEntity visualdevEntity = visualdevReleaseService.getById(form.getModelId());
        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);

        List<FieLdsModel> fields = new ArrayList<>();
        OnlinePublicUtils.recursionFields(fields, fieLdsModels);

        List<Map<String, Object>> swapInfoOld = onlineSwapDataUtils.getSwapInfo(Arrays.asList(form.getOldData()), fields, visualdevEntity.getId(), false, null);
        Map<String, Object> oldData = swapInfoOld.get(0);
        List<Map<String, Object>> swapInfoNew = onlineSwapDataUtils.getSwapInfo(Arrays.asList(form.getNewData()), fields, visualdevEntity.getId(), false, null);
        Map<String, Object> newData = swapInfoNew.get(0);

        for (FieLdsModel item : fields) {
            //移除系统字段
            if (isSkipFields(item)) continue;
            if (JnpfKeyConsts.CHILD_TABLE.equals(item.getConfig().getJnpfKey())) {
                //子表字段
                addChildTableLog(item, tableModels, listLog, oldData, newData);
            } else {
                addMainMastLog(item, listLog, oldData, newData);
            }
        }
    }

    /**
     * 添加主附表字段日志
     *
     * @param item
     * @param listLog
     * @param oldData
     * @param newData
     */
    private void addMainMastLog(FieLdsModel item, List<VisualLogModel> listLog, Map<String, Object> oldData, Map<String, Object> newData) {
        String vModel = item.getVModel();
        ConfigModel config = item.getConfig();
        String jnpfKey = config.getJnpfKey();
        String label = config.getLabel();
        String dataType = config.getDataType();
        if (!Objects.equals(oldData.get(vModel), newData.get(vModel))) {
            Integer actionType = 1;//0-新增，1-修改
            if (oldData.get(vModel) == null || oldData.get(vModel).toString().trim().isEmpty()) {
                actionType = 0;
            }
            boolean nameModified = false;
            if (JnpfKeyConsts.getNameModified().contains(jnpfKey)
                    && (JnpfKeyConsts.getNameModifiedNotDynamic().contains(jnpfKey) || OnlineDataTypeEnum.DYNAMIC.getType().equals(dataType))) {
                nameModified = true;
            }

            String newValue = getValueByType(newData, vModel, jnpfKey);
            String oldValue = getValueByType(oldData, vModel, jnpfKey);
            VisualLogModel vlogModel = new VisualLogModel();
            vlogModel.setField(vModel);
            vlogModel.setFieldName(label);
            vlogModel.setJnpfKey(jnpfKey);
            vlogModel.setNewData(newValue);
            vlogModel.setOldData(oldValue);
            vlogModel.setType(actionType);
            vlogModel.setNameModified(nameModified);
            listLog.add(vlogModel);
        }
    }

    /**
     * 添加子表字段日志
     *
     * @param item
     * @param tableModels
     * @param listLog
     * @param oldData
     * @param newData
     */
    private void addChildTableLog(FieLdsModel item, List<TableModel> tableModels, List<VisualLogModel> listLog, Map<String, Object> oldData, Map<String, Object> newData) {
        ConfigModel config = item.getConfig();
        String jnpfKey = config.getJnpfKey();
        String label = config.getLabel();
        TableModel tableModel = tableModels.stream().filter(t -> t.getTable().equals(config.getTableName())).findFirst().orElse(null);
        TableFields mainField = tableModel.getFields().stream().filter(t ->
                Objects.equals(t.getPrimaryKey(), 1) && !t.getField().equalsIgnoreCase(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
        String mainKey = mainField.getField();

        List<FieLdsModel> children = item.getConfig().getChildren();
        String vModel = item.getVModel();
        //子表数据
        List<Map<String, Object>> childData = new ArrayList<>();
        //子表表头
        List<Map<String, Object>> childField = new ArrayList<>();
        for (FieLdsModel childItem : children) {
            String jnpfKeyChild = childItem.getConfig().getJnpfKey();
            if (isSkipFields(childItem))
                continue;
            Map<String, Object> childItemMap = new HashMap<>();
            childItemMap.put("prop", childItem.getVModel());
            childItemMap.put("label", childItem.getConfig().getLabel());
            childItemMap.put("jnpfKey", jnpfKeyChild);
            boolean nameModified = false;
            if (JnpfKeyConsts.getNameModified().contains(jnpfKeyChild)
                    && (JnpfKeyConsts.getNameModifiedNotDynamic().contains(jnpfKeyChild) || OnlineDataTypeEnum.DYNAMIC.getType().equals(childItem.getConfig().getDataType()))) {
                nameModified = true;
            }
            childItemMap.put("nameModified", nameModified);
            childField.add(childItemMap);
        }

        List<Map<String, Object>> childOld = oldData.get(vModel) == null ? new ArrayList<>() : (List) oldData.get(vModel);
        List<Map<String, Object>> childNew = newData.get(vModel) == null ? new ArrayList<>() : (List) newData.get(vModel);
        List<Object> newIds = childNew.stream().map(t -> t.get(mainKey)).collect(Collectors.toList());
        List<Map<String, Object>> deleteMap = childOld.stream().filter(t -> !newIds.contains(t.get(mainKey))).collect(Collectors.toList());
        for (Map<String, Object> chilMap : deleteMap) {
            doDelData(chilMap, children, childData);
        }
        for (Map<String, Object> chilMap : childNew) {
            doNewData(chilMap, mainKey, childOld, children, childData);
        }
        if (CollectionUtils.isNotEmpty(childData)) {
            VisualLogModel vlogModel = new VisualLogModel();
            vlogModel.setField(vModel);
            vlogModel.setFieldName(label);
            vlogModel.setJnpfKey(jnpfKey);
            vlogModel.setChidData(childData);
            vlogModel.setChidField(childField);
            vlogModel.setType(1);
            listLog.add(vlogModel);
        }
    }

    private void doDelData(Map<String, Object> chilMap, List<FieLdsModel> children, List<Map<String, Object>> childData) {
        Map<String, Object> childDataMap = new HashMap<>();
        for (FieLdsModel childItem : children) {
            String childJnpfKey = childItem.getConfig().getJnpfKey();
            //移除系统字段
            if (isSkipFields(childItem)) continue;
            String childVmodel = childItem.getVModel();
            String childVmodelOld = "jnpf_old_" + childItem.getVModel();
            String oldValue = getValueByType(chilMap, childVmodel, childJnpfKey);
            childDataMap.put(childVmodel, null);
            childDataMap.put(childVmodelOld, oldValue);
            childDataMap.put("jnpf_type", 2);
        }
        childData.add(childDataMap);
    }

    private void doNewData(Map<String, Object> chilMap, String mainKey, List<Map<String, Object>> childOld, List<FieLdsModel> children, List<Map<String, Object>> childData) {
        Object mainId = chilMap.get(mainKey);
        Map<String, Object> oldMap = childOld.stream().filter(t -> t.get(mainKey).equals(mainId)).findFirst().orElse(null);
        Integer jnpfType = 1;
        if (oldMap == null) {
            jnpfType = 0;
        }
        Map<String, Object> childDataMap = new HashMap<>();
        boolean hasChanged = false;
        for (FieLdsModel childItem : children) {
            String childJnpfKey = childItem.getConfig().getJnpfKey();
            //移除系统字段
            if (isSkipFields(childItem)) continue;
            String childVmodel = childItem.getVModel();
            String childVmodelOld = "jnpf_old_" + childItem.getVModel();
            String newValue = getValueByType(chilMap, childVmodel, childJnpfKey);
            String oldValue = getValueByType(oldMap, childVmodel, childJnpfKey);
            if (!Objects.equals(newValue, oldValue)) {
                hasChanged = true;
            }
            childDataMap.put(childVmodel, newValue);
            childDataMap.put(childVmodelOld, oldValue);
            childDataMap.put("jnpf_type", jnpfType);
        }
        if (hasChanged) {
            childData.add(childDataMap);
        }
    }

    /**
     * 跳过字段不处理
     *
     * @param childItem
     * @return
     */
    private boolean isSkipFields(FieLdsModel childItem) {
        //字段不处理直接跳过 ：（JnpfKeyConsts.CALCULATE 计算公式仅展示也跳过）
        List<String> skipFields = new ArrayList<>();
        skipFields.addAll(JnpfKeyConsts.getSystemKey());
        skipFields.add(JnpfKeyConsts.POPUPSELECT_ATTR);
        skipFields.add(JnpfKeyConsts.RELATIONFORM_ATTR);
        String jnpfKey = childItem.getConfig().getJnpfKey();
        return skipFields.contains(jnpfKey) || (JnpfKeyConsts.CALCULATE.equals(jnpfKey) && Objects.equals(childItem.getIsStorage(), 0));
    }

    private String getValueByType(Map<String, Object> map, String vModel, String jnpfKey) {
        if (map != null) {
            Object o = map.get(vModel);
            String value = o == null ? "" : o.toString();
            switch (jnpfKey) {
                case JnpfKeyConsts.UPLOADFZ:
                    List<Map<String, String>> listM = (List) o;
                    if (listM != null) {
                        StringJoiner sj = new StringJoiner(",");
                        listM.stream().forEach(t -> sj.add(t.get("name")));
                        value = sj.toString();
                    }
                    break;
                case JnpfKeyConsts.LOCATION:
                    Map<String, Object> lcationMap = JsonUtil.stringToMap(value);
                    if (MapUtils.isNotEmpty(lcationMap)) {
                        value = lcationMap.get("fullAddress") != null ? lcationMap.get("fullAddress").toString() : "";
                    }
                    break;
                default:
                    break;
            }
            return value;
        }
        return null;
    }
}
