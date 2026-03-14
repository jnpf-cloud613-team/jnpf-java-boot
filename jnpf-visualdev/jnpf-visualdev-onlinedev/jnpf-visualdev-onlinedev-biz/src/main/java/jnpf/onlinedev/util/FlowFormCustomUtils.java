package jnpf.onlinedev.util;

import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.CheckFormModel;
import jnpf.base.model.VisualLogModel;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.util.FlowFormDataUtil;
import jnpf.base.util.FormCheckUtils;
import jnpf.base.util.ServiceBaseUtil;
import jnpf.constant.MsgCode;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormCloumnUtil;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.RecursionForm;
import jnpf.onlinedev.model.OnlineInfoModel;
import jnpf.onlinedev.model.log.VisualLogForm;
import jnpf.onlinedev.service.VisualDevInfoService;
import jnpf.onlinedev.service.VisualLogService;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.TableFeildsEnum;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义流程表单处理
 *
 * @author JNPF开发平台组
 * @version V3.4.5
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/10/21
 */
@Component
@RequiredArgsConstructor
public class FlowFormCustomUtils {

    private final FlowFormDataUtil flowDataUtil;
    private final FormCheckUtils formCheckUtils;
    private final ServiceBaseUtil serviceUtil;
    private final VisualLogService visualLogService;
    private final VisualDevInfoService visualDevInfoService;

    public void create(VisualdevEntity visualdevEntity, FlowFormDataModel flowFormDataModel) throws WorkFlowException {
        String id = flowFormDataModel.getId();
        Map<String, Object> map = flowFormDataModel.getMap();
        UserEntity delegateUser = flowFormDataModel.getDelegateUser();
        List<Map<String, Object>> listFlowOperate = flowFormDataModel.getFormOperates();
        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        DbLinkEntity linkEntity = serviceUtil.getDbLink(visualdevEntity.getDbLinkId());

        //递归遍历模板
        RecursionForm recursionForm = new RecursionForm(list, tableModels);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //是否开启并发锁
        boolean concurrency = false;
        Integer primaryKeyPolicy = formData.getPrimaryKeyPolicy();
        if (Boolean.TRUE.equals(formData.getConcurrencyLock())) {
            //初始化version值
            map.put(TableFeildsEnum.VERSION.getField(), 0);
            concurrency = true;
        }
        //单行唯一校验
        CheckFormModel checkFormModel = CheckFormModel.builder().formFieldList(list).dataMap(map).linkEntity(linkEntity).tableModelList(tableModels)
                .visualdevEntity(visualdevEntity).id(null).isTransfer(flowFormDataModel.getIsTransfer()).build();
        String b = formCheckUtils.checkForm(checkFormModel);
        if (StringUtil.isNotEmpty(b)) {
            throw new WorkFlowException(b);
        }
        String mainId = id;
        UserEntity userEntity;
        UserInfo userInfo = UserProvider.getUser();
        if (delegateUser != null) {
            delegateUser.setId(userInfo.getUserId());
            userEntity = delegateUser;
        } else {
            userEntity = serviceUtil.getUserInfo(userInfo.getUserId());
        }
        DataModel dataModel = DataModel.builder().visualId(visualdevEntity.getId())
                .dataNewMap(map).fieLdsModelList(list).tableModelList(tableModels).formAllModel(formAllModel).mainId(mainId).link(linkEntity)
                .userEntity(userEntity).concurrencyLock(concurrency).primaryKeyPolicy(primaryKeyPolicy).flowFormOperates(listFlowOperate).build();
        flowDataUtil.create(dataModel);

        //数据日志
        if (formData.isDataLog()) {
            visualLogService.createEventLog(VisualLogForm.builder().modelId(visualdevEntity.getId()).dataId(dataModel.getMainId()).newData(map).type(0).build());
        }
    }

    public DataModel update(VisualdevEntity visualdevEntity, FlowFormDataModel flowFormDataModel) throws WorkFlowException, DataException {
        Map<String, Object> map = flowFormDataModel.getMap();
        String id = flowFormDataModel.getId();
        List<Map<String, Object>> listFlowOperate = flowFormDataModel.getFormOperates();
        FormDataModel formData = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        TableModel mainT = tableModels.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(new TableModel());
        DbLinkEntity linkEntity = serviceUtil.getDbLink(visualdevEntity.getDbLinkId());
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);

        //递归遍历模板
        RecursionForm recursionForm = new RecursionForm(list, tableModels);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //是否开启并发锁
        boolean isConcurrencyLock = false;
        Integer primaryKeyPolicy = formData.getPrimaryKeyPolicy();
        if (Boolean.TRUE.equals(formData.getConcurrencyLock())) {
            if (map.get(TableFeildsEnum.VERSION.getField()) == null) {
                map.put(TableFeildsEnum.VERSION.getField(), 0);
            } else {
                Object realId = id;
                if (Objects.equals(primaryKeyPolicy, 2)) {
                    realId = Long.parseLong(id);
                }
                boolean version = flowDataUtil.getVersion(mainT.getTable(), linkEntity, map, realId);
                if (!version) {
                    throw new WorkFlowException(MsgCode.VS405.get());
                } else {
                    Integer vs = Integer.valueOf(String.valueOf(map.get(TableFeildsEnum.VERSION.getField())));
                    map.put(TableFeildsEnum.VERSION.getField(), vs + 1);
                }
            }
            isConcurrencyLock = true;
        }

        CheckFormModel checkFormModel = CheckFormModel.builder().formFieldList(list).dataMap(map).linkEntity(linkEntity).tableModelList(tableModels)
                .visualdevEntity(visualdevEntity).id(id).isTransfer(flowFormDataModel.getIsTransfer()).build();
        String b = formCheckUtils.checkForm(checkFormModel);
        if (StringUtil.isNotEmpty(b)) {
            throw new WorkFlowException(b);
        }

        //数据日志
        VisualdevModelDataInfoVO resOld = visualDevInfoService.getDetailsDataInfo(id, visualdevEntity,
                OnlineInfoModel.builder().needRlationFiled(true).needSwap(false).formAllModel(formAllModel).build());
        Map<String, Object> oldData = JsonUtil.stringToMap(resOld.getData());

        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = serviceUtil.getUserInfo(userInfo.getUserId());
        DataModel dataModel = DataModel.builder().visualId(visualdevEntity.getId())
                .dataNewMap(map).fieLdsModelList(list).formAllModel(formAllModel).tableModelList(tableModels).mainId(id).link(linkEntity)
                .userEntity(userEntity).concurrencyLock(isConcurrencyLock).primaryKeyPolicy(primaryKeyPolicy).flowFormOperates(listFlowOperate)
                .logicalDelete(formData.getLogicalDelete()).build();
        flowDataUtil.update(dataModel);

        VisualdevModelDataInfoVO res = visualDevInfoService.getDetailsDataInfo(id, visualdevEntity,
                OnlineInfoModel.builder().needRlationFiled(true).needSwap(false).formAllModel(formAllModel).build());
        Map<String, Object> newData = JsonUtil.stringToMap(res.getData());
        VisualLogForm form = VisualLogForm.builder().modelId(visualdevEntity.getId()).dataId(id).oldData(oldData).newData(newData).type(1).build();

        //处理变更字段信息-任务流程用
        List<VisualLogModel> listLog = new ArrayList<>();
        visualLogService.addLog(form, listLog);
        dataModel.setListLog(listLog);
        //数据日志
        if (formData.isDataLog()) {
            form.setListLog(listLog);
            visualLogService.createEventLog(form);
        }
        return dataModel;
    }

    public DataModel saveOrUpdate(VisualdevEntity visualdevEntity, FlowFormDataModel flowFormDataModel) throws WorkFlowException, DataException {
        List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        TableModel mainT = tableModels.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(new TableModel());
        DbLinkEntity linkEntity = serviceUtil.getDbLink(visualdevEntity.getDbLinkId());
        FormDataModel formDataModel = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
        boolean autoIncrement = Objects.equals(formDataModel.getPrimaryKeyPolicy(), 2);
        String id = flowFormDataModel.getId();
        Object mainId = id;
        if (autoIncrement) {
            mainId = Long.parseLong(id);
        }
        SqlTable sqlTable = SqlTable.of(mainT.getTable());
        String realId = formCheckUtils.getCount(mainId, sqlTable, mainT, linkEntity);

        if (StringUtil.isNotEmpty(realId)) {
            flowFormDataModel.setId(realId);
            return this.update(visualdevEntity, flowFormDataModel);
        } else {
            this.create(visualdevEntity, flowFormDataModel);
        }
        return null;
    }

    public Map<String, Object> info(VisualdevEntity visualdevEntity, String id) {
        return flowDataUtil.getEditDataInfo(visualdevEntity, id, OnlineInfoModel.builder().build());
    }
}
