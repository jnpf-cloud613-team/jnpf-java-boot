package jnpf.flowable.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableList;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.DelegateInfoEntity;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.mapper.DelegateInfoMapper;
import jnpf.flowable.mapper.DelegateMapper;
import jnpf.flowable.mapper.TemplateJsonMapper;
import jnpf.flowable.mapper.TemplateMapper;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.delegate.DelegateCrForm;
import jnpf.flowable.model.delegate.DelegateListVO;
import jnpf.flowable.model.delegate.DelegatePagination;
import jnpf.flowable.model.delegate.DelegateUpForm;
import jnpf.flowable.model.message.DelegateModel;
import jnpf.flowable.model.util.FlowConstant;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.DelegateService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.MsgUtil;
import jnpf.flowable.util.OperatorUtil;
import jnpf.flowable.util.ServiceUtil;
import jnpf.message.model.SentMessageForm;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/13 16:56
 */
@Service
@RequiredArgsConstructor
public class DelegateServiceImpl extends SuperServiceImpl<DelegateMapper, DelegateEntity> implements DelegateService {


    private final MsgUtil msgUtil;

    private final OperatorUtil operatorUtil;

    private final ServiceUtil serviceUtil;

    private final FlowUtil flowUtil;


    private final DelegateInfoMapper delegateInfoMapper;

    private final TemplateMapper templateMapper;

    private final TemplateJsonMapper templateJsonMapper;

    @Override
    public List<DelegateListVO> getList(DelegatePagination pagination) {
        String userId = UserProvider.getLoginUserId();
        String keyword = pagination.getKeyword();
        QueryWrapper<DelegateEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DelegateEntity::getUserId, userId);

        QueryWrapper<DelegateInfoEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.lambda().like(DelegateInfoEntity::getToUserName, keyword);
        }
        List<DelegateInfoEntity> delegateInfoList = delegateInfoMapper.selectList(queryWrapper);
        List<String> delegateIds = delegateInfoList.stream().map(DelegateInfoEntity::getDelegateId).distinct().collect(Collectors.toList());
        if (CollUtil.isNotEmpty(delegateIds)) {
            if (StringUtils.isNotBlank(keyword)) {
                wrapper.lambda().and(e -> e.in(DelegateEntity::getId, delegateIds).or().like(DelegateEntity::getFlowName, keyword));
            } else {
                wrapper.lambda().in(DelegateEntity::getId, delegateIds);
            }
        } else {
            if (StringUtils.isNotBlank(keyword)) {
                wrapper.lambda().like(DelegateEntity::getFlowName, keyword);
            } else {
                return new ArrayList<>();
            }
        }
        if (ObjectUtil.equals(pagination.getType(), 1)) {
            wrapper.lambda().eq(DelegateEntity::getType, 0);// 委托
        } else {
            wrapper.lambda().eq(DelegateEntity::getType, 1);// 代理
        }
        wrapper.lambda().orderByAsc(DelegateEntity::getSortCode).orderByDesc(DelegateEntity::getCreatorTime);
        Page<DelegateEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<DelegateEntity> iPage = this.page(page, wrapper);
        List<DelegateEntity> dataList = pagination.setData(iPage.getRecords(), page.getTotal());

        List<DelegateListVO> voList = new ArrayList<>();
        long time = new Date().getTime();
        List<String> ids = dataList.stream().map(DelegateEntity::getId).distinct().collect(Collectors.toList());
        List<DelegateInfoEntity> list = delegateInfoMapper.getList(ids);
        for (DelegateEntity delegate : dataList) {
            DelegateListVO vo = JsonUtil.getJsonToBean(delegate, DelegateListVO.class);
            List<DelegateInfoEntity> infoList = list.stream()
                    .filter(e -> ObjectUtil.equals(e.getDelegateId(), delegate.getId())).collect(Collectors.toList());
            List<String> toUserNameList = infoList.stream().map(DelegateInfoEntity::getToUserName).collect(Collectors.toList());
            vo.setToUserName(String.join(",", toUserNameList));
            long rejectCount = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 2)).count();
            long acceptCount = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 1)).count();
            if (time >= vo.getEndTime() || rejectCount == infoList.size()) {// 已失效，1、所有人都拒绝；2、到达结束时间或终止委托
                vo.setStatus(2);
            } else if (time >= vo.getStartTime() && acceptCount > 0) {// 生效中，对方接受且到达开始时间的状态
                vo.setStatus(1);
            } else {// 未生效，两种场景1：对方已接受但未达到开始时间状态为未生效，2、对方未接受状态为未生效
                vo.setStatus(0);
            }
            if (acceptCount > 0) {
                vo.setIsEdit(false);
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public DelegateEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(DelegateCrForm fo) {
        String userId = UserProvider.getLoginUserId();
        DelegateEntity entity = JsonUtil.getJsonToBean(fo, DelegateEntity.class);
        entity.setId(RandomUtil.uuId());
        entity.setSortCode(RandomUtil.parses());
        entity.setCreatorUserId(userId);

        this.save(entity);
        DelegateModel model = flowUtil.create(fo.getToUserId(), entity);
        msgUtil.delegateMsg(model);
    }

    @Override
    public boolean update(DelegateEntity entity, DelegateUpForm fo) {
        BeanUtil.copyProperties(fo, entity, CopyOptions.create().setIgnoreNullValue(true));
        UserInfo userInfo = UserProvider.getUser();
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(userInfo.getUserId());
        List<String> createList = flowUtil.update(fo.getToUserId(), entity);
        if (!createList.isEmpty()) {
            DelegateModel model = flowUtil.create(createList, entity);
            msgUtil.delegateMsg(model);
        }
        return this.updateById(entity);
    }

    @Override
    public boolean updateStop(String id, DelegateEntity entity) {
        UserInfo userInfo = UserProvider.getUser();
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(userInfo.getUserId());
        DelegateModel delegate = new DelegateModel();
        List<DelegateInfoEntity> infoList = delegateInfoMapper.getList(entity.getId());
        delegate.setToUserIds(infoList.stream().map(DelegateInfoEntity::getToUserId).collect(Collectors.toList()));
        delegate.setType(FlowNature.END_MSG);
        delegate.setUserInfo(userInfo);
        boolean isDelegate = ObjectUtil.equals(entity.getType(), 0);
        delegate.setDelegate(isDelegate);
        msgUtil.delegateMsg(delegate);
        return this.updateById(entity);
    }

    @Override
    public void delete(DelegateEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public List<String> getToUser(String userId, String flowId) {
        return flowUtil.getToUser(userId, flowId);
    }

    @Override
    public List<DelegateEntity> getByToUserId(String toUserId) {
        return flowUtil.getByToUserId(toUserId);
    }

    // 根据 被委托人/代理人id 获取列表
    @Override
    public List<DelegateEntity> getByToUserId(String toUserId, Integer type) {
        return flowUtil.getByToUserId(toUserId, type);
    }

    @Override
    public ListVO<CandidateUserVo> getUserList(String templateId) throws WorkFlowException {
        String userId = UserProvider.getLoginUserId();
        TemplateEntity template = templateMapper.selectById(templateId);
        if (null == template) {
            TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(templateId);
            if (null != jsonEntity) {
                template = templateMapper.selectById(jsonEntity.getTemplateId());
            }
        }
        if (null == template) {
            throw new WorkFlowException(MsgCode.WF122.get());
        }
        if (!ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
            throw new WorkFlowException(MsgCode.WF140.get());
        }
        List<String> userIds = this.getDelegateUser(userId, template);

        Pagination pagination = new Pagination();
        pagination.setPageSize(10000);
        List<CandidateUserVo> jsonToList = operatorUtil.getUserModel(userIds, pagination);
        if (jsonToList.isEmpty()) {
            throw new WorkFlowException(MsgCode.WF125.get());
        }
        ListVO<CandidateUserVo> vo = new ListVO<>();
        vo.setList(jsonToList);
        return vo;
    }

    public List<String> getDelegateUser(String userId, TemplateEntity template) throws WorkFlowException {
        List<String> ids = new ArrayList<>();
        List<DelegateEntity> delegateList = this.getByToUserId(userId, 0);
        if (CollUtil.isNotEmpty(delegateList)) {
            int permissionCount = 0;
            for (DelegateEntity delegateEntity : delegateList) {
                String flowId = delegateEntity.getFlowId();
                if (StringUtils.isNotBlank(flowId)) {
                    if (flowId.contains(template.getId())) {
                        if (ObjectUtil.equals(template.getVisibleType(), FlowNature.ALL)) {
                            ids.add(delegateEntity.getUserId());
                            continue;
                        }
                        List<String> launchPermission = serviceUtil.getPermission(delegateEntity.getUserId());
                        if (launchPermission.contains(template.getId())) {
                            ids.add(delegateEntity.getUserId());
                        } else {
                            permissionCount++;
                        }
                    }
                } else {
                    // 全部流程
                    if (ObjectUtil.equals(template.getVisibleType(), FlowNature.ALL)) {
                        ids.add(delegateEntity.getUserId());
                        continue;
                    }
                    List<String> launchPermission = serviceUtil.getPermission(delegateEntity.getUserId());
                    if (launchPermission.contains(template.getId())) {
                        ids.add(delegateEntity.getUserId());
                    } else {
                        permissionCount++;
                    }
                }
            }
            if (CollUtil.isEmpty(ids) && delegateList.size() == permissionCount) {
                throw new WorkFlowException(MsgCode.WF129.get());
            }
        }
        return ids;
    }


    @Override
    public List<DelegateEntity> selectSameParamAboutDelaget(DelegateCrForm fo) {
        List<DelegateInfoEntity> infoList = delegateInfoMapper.getByToUserIds(fo.getToUserId());
        if (CollUtil.isEmpty(infoList)) {
            return new ArrayList<>();
        }
        List<String> ids = infoList.stream().map(DelegateInfoEntity::getDelegateId).distinct().collect(Collectors.toList());
        QueryWrapper<DelegateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DelegateEntity::getUserId, fo.getUserId()).in(DelegateEntity::getId, ids)
                .eq(DelegateEntity::getType, Integer.valueOf(fo.getType()))
                .gt(DelegateEntity::getEndTime, new Date());
        return this.list(queryWrapper);
    }

    @Override
    public List<DelegateEntity> getList() {
        String userId = UserProvider.getLoginUserId();
        QueryWrapper<DelegateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t ->
                t.eq(DelegateEntity::getCreatorUserId, userId).or().eq(DelegateEntity::getUserId, userId));
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public void notarize(DelegateInfoEntity delegateInfo) {
        delegateInfoMapper.updateById(delegateInfo);
        // 判断全部拒绝，更新时间
        DelegateEntity entity = this.getById(delegateInfo.getDelegateId());
        List<DelegateInfoEntity> infoList = delegateInfoMapper.getList(entity.getId());
        boolean isAllReject = infoList.stream().allMatch(e -> ObjectUtil.equals(e.getStatus(), 2));
        if (isAllReject) {
            Date date = new Date();
            entity.setStartTime(date);
            entity.setEndTime(date);
            this.updateById(entity);
        }
        if (!ObjectUtil.equals(delegateInfo.getStatus(), 0)) {
            UserInfo userInfo = UserProvider.getUser();
            List<String> toUserIds = ImmutableList.of(entity.getUserId());

            SentMessageForm flowMsgModel = new SentMessageForm();
            flowMsgModel.setToUserIds(toUserIds);
            flowMsgModel.setUserInfo(userInfo);
            flowMsgModel.setTemplateId("PZXTLG022");
            Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(FlowConstant.MANDATOR, userInfo.getUserName());
            parameterMap.put(FlowConstant.TITLE, ObjectUtil.equals(entity.getType(), 0) ? "委托" : "代理");
            parameterMap.put(FlowConstant.ACTION, ObjectUtil.equals(delegateInfo.getStatus(), 1) ? "接受" : "拒绝");
            flowMsgModel.setParameterMap(parameterMap);
            int i = ObjectUtil.equals(entity.getType(), 0) ? 1 : 3;
            Integer delegateType = FlowNature.END_MSG.equals(entity.getType()) ? 0 : i;
            Map<String, String> contentMsg = new HashMap<>();
            contentMsg.put("type", delegateType + "");
            flowMsgModel.setContentMsg(contentMsg);
            flowMsgModel.setFlowType(2);
            flowMsgModel.setType(2);

            List<SentMessageForm> messageListAll = new ArrayList<>();
            messageListAll.add(flowMsgModel);
            serviceUtil.sendDelegateMsg(messageListAll);
        }
    }
}
