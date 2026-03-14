package jnpf.flowable.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.DelegateInfoEntity;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.delegate.*;
import jnpf.flowable.service.DelegateInfoService;
import jnpf.flowable.service.DelegateService;
import jnpf.flowable.util.ServiceUtil;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/13 17:27
 */
@Tag(name = "流程委托", description = "DelegateController")
@RestController
@RequestMapping("/api/workflow/delegate")
@RequiredArgsConstructor
public class DelegateController extends SuperController<DelegateService, DelegateEntity> {


    private final ServiceUtil serviceUtil;


    private final DelegateService delegateService;

    private final DelegateInfoService delegateInfoService;

    /**
     * 获取流程委托列表
     *
     * @param pagination 分页参数
     */
    @Operation(summary = "获取流程委托列表")
    @GetMapping
    public ActionResult<PageListVO<DelegateListVO>> list(DelegatePagination pagination) {
        List<DelegateListVO> voList;
        if (ObjectUtil.equals(pagination.getType(), 2) || ObjectUtil.equals(pagination.getType(), 4)) {
            voList = delegateInfoService.getList(pagination);
        } else {
            voList = delegateService.getList(pagination);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 委托信息列表
     *
     * @param delegateId 委托主键
     */
    @Operation(summary = "委托信息列表")
    @GetMapping("/Info/{delegateId}")
    public ActionResult<Object> getDelegateInfo(@PathVariable("delegateId") String delegateId) {
        List<DelegateInfoEntity> list = delegateInfoService.getList(delegateId);
        return ActionResult.success(JsonUtil.getJsonToList(list, DelegateListVO.class));
    }

    /**
     * 获取流程委托信息
     *
     * @param id 主键
     */
    @Operation(summary = "获取流程委托信息")
    @GetMapping("/{id}")
    public ActionResult<Object> info(@PathVariable("id") String id) throws WorkFlowException {
        DelegateEntity entity = delegateService.getInfo(id);
        if (null == entity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        DelegateInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, DelegateInfoVO.class);
        List<DelegateInfoEntity> infoList = delegateInfoService.getList(entity.getId());
        if (CollUtil.isNotEmpty(infoList)) {
            List<String> toUserNameList = infoList.stream().map(DelegateInfoEntity::getToUserName).collect(Collectors.toList());
            vo.setToUserName(String.join(",", toUserNameList));
            List<String> toUserIdList = infoList.stream().map(DelegateInfoEntity::getToUserId).collect(Collectors.toList());
            vo.setToUserId(toUserIdList);
        }
        return ActionResult.success(vo);
    }

    /**
     * 新建流程委托
     *
     * @param fo 参数
     */
    @Operation(summary = "新建流程委托")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid DelegateCrForm fo) {
        // 超管和管理员不能新增委托/代理（即：只有普通用户身份才能新建委托/代理，提示：“管理员不能新建委托/代理”）
        if (!serviceUtil.isCommonUser(UserProvider.getLoginUserId())) {
            return ActionResult.fail(MsgCode.WF130.get());
        }
        if (StringUtils.isBlank(fo.getUserId())) {
            UserInfo userInfo = UserProvider.getUser();
            fo.setUserId(userInfo.getUserId());
            fo.setUserName(userInfo.getUserName() + "/" + userInfo.getUserAccount());
        }
        boolean isDelegate = ObjectUtil.equals(fo.getType(), "0");
        if (fo.getToUserId().contains(fo.getUserId())) {
            return ActionResult.fail(isDelegate ? MsgCode.WF017.get() : MsgCode.WF137.get());
        }
        // 受托人/代理人不能选择admin和本人
        List<String> toUserList = fo.getToUserId();
        String admin = serviceUtil.getAdmin();
        for (String toUser : toUserList) {
            if (ObjectUtil.equals(toUser, admin)) {
                return ActionResult.fail(MsgCode.WF131.get());
            }
        }
        if (this.alreadyDelegate(fo, null)) {
            return ActionResult.fail(isDelegate ? MsgCode.WF018.get() : MsgCode.WF144.get());
        }
        DelegateCrForm reverse = new DelegateCrForm();
        BeanUtils.copyProperties(fo, reverse);
        reverse.setUserIdList(fo.getToUserId());
        reverse.setToUserId(ImmutableList.of(fo.getUserId()));
        if (this.alreadyDelegate(reverse, null)) {
            return ActionResult.fail(isDelegate ? MsgCode.WF019.get() : MsgCode.WF145.get());
        }
        delegateService.create(fo);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新流程委托
     *
     * @param id 主键
     * @param fo 参数
     */
    @Operation(summary = "更新流程委托")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid DelegateUpForm fo) throws WorkFlowException {
        DelegateEntity entity = delegateService.getInfo(id);
        if (null == entity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        fo.setUserId(entity.getUserId());
        boolean isDelegate = ObjectUtil.equals(fo.getType(), "0");
        if (fo.getToUserId().contains(fo.getUserId())) {
            return ActionResult.fail(isDelegate ? MsgCode.WF017.get() : MsgCode.WF137.get());
        }
        if (this.alreadyDelegate(fo, id)) {
            return ActionResult.fail(isDelegate ? MsgCode.WF018.get() : MsgCode.WF144.get());
        }
        // 判断是否有人接受
        List<DelegateInfoEntity> infoList = delegateInfoService.getList(id);
        if (CollUtil.isNotEmpty(infoList)) {
            DelegateInfoEntity delegateInfoEntity = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 1)).findFirst().orElse(null);
            if (null != delegateInfoEntity) {
                return ActionResult.fail(MsgCode.WF132.get());
            }
        }
        DelegateCrForm reverse = new DelegateCrForm();
        BeanUtils.copyProperties(fo, reverse);
        reverse.setUserIdList(fo.getToUserId());
        reverse.setToUserId(ImmutableList.of(fo.getUserId()));
        if (this.alreadyDelegate(reverse, id)) {
            return ActionResult.fail(isDelegate ? MsgCode.WF019.get() : MsgCode.WF145.get());
        }
        if (delegateService.update(entity, fo)) {
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.success(MsgCode.FA002.get());
    }

    // 判断是否已有委托
    private boolean alreadyDelegate(DelegateCrForm form, String id) {
        List<DelegateEntity> delegateEntities = new ArrayList<>();
        if (CollUtil.isNotEmpty(form.getUserIdList())) {
            for (String userId : form.getUserIdList()) {
                form.setUserId(userId);
                List<DelegateEntity> list = delegateService.selectSameParamAboutDelaget(form);
                delegateEntities.addAll(list);
            }
        } else {
            delegateEntities = delegateService.selectSameParamAboutDelaget(form);
        }
        for (DelegateEntity delegate : delegateEntities) {
            if (delegate.getId().equals(id)) {
                continue;
            }
            //时间交叉
            if ((form.getStartTime() <= delegate.getStartTime().getTime() && form.getEndTime() >= delegate.getStartTime().getTime()) ||
                    (form.getStartTime() >= delegate.getStartTime().getTime() && form.getStartTime() <= delegate.getEndTime().getTime())) {
                if (StringUtil.isEmpty(form.getFlowId())) {
                    return true;
                } else {
                    if (StringUtil.isEmpty(delegate.getFlowId())) {
                        return true;
                    } else {
                        List<String> split = Arrays.asList(delegate.getFlowId().split(","));
                        List<String> split1 = Arrays.asList(form.getFlowId().split(","));
                        for (String srt : split) {
                            if (split1.contains(srt)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 结束委托
     *
     * @param id 主键
     */
    @Operation(summary = "结束委托")
    @PutMapping("/Stop/{id}")
    public ActionResult<Object> stop(@PathVariable("id") String id) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, -1);
        Date date = calendar.getTime();
        DelegateEntity entity = delegateService.getInfo(id);
        if (null != entity) {
            entity.setStartTime(date);
            entity.setEndTime(date);
            delegateService.updateStop(id, entity);
            return ActionResult.success(MsgCode.SU008.get());
        }
        return ActionResult.fail(MsgCode.FA002.get());
    }

    /**
     * 删除流程委托
     *
     * @param id 主键
     */
    @Operation(summary = "删除流程委托")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        DelegateEntity entity = delegateService.getInfo(id);
        if (null != entity) {
            delegateService.delete(entity);
            delegateInfoService.delete(entity.getId());
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }


    /**
     * 获取委托人
     * 根据被委托人查询可发起的流程列表
     */
    @Operation(summary = "获取委托人")
    @GetMapping("/UserList")
    public ActionResult<Object> getUserListByFlowId(@RequestParam("templateId") String templateId) throws WorkFlowException {
        ListVO<CandidateUserVo> userList = delegateService.getUserList(templateId);
        return ActionResult.success(userList);
    }

    /**
     * 确认
     *
     * @param id   委托信息主键
     * @param type 类型，1.接受  2.拒绝
     */
    @Operation(summary = "确认")
    @PostMapping("/Notarize/{id}")
    public ActionResult<Object> accept(@PathVariable("id") String id, @RequestParam("type") Integer type) throws WorkFlowException {
        DelegateInfoEntity delegateInfo = delegateInfoService.getById(id == null ? "" : id);
        if (null == delegateInfo) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        delegateInfo.setStatus(type);
        delegateService.notarize(delegateInfo);
        return ActionResult.success(MsgCode.SU004.get());
    }

}
