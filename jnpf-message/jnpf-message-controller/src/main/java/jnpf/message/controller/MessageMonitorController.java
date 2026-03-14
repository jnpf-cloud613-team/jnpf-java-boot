


package jnpf.message.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.message.entity.MessageMonitorEntity;
import jnpf.message.model.messagemonitor.*;
import jnpf.message.service.MessageMonitorService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息监控
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-22
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "消息监控", description = "message")
@RequestMapping("/api/message/MessageMonitor")
public class MessageMonitorController extends SuperController<MessageMonitorService, MessageMonitorEntity> {

    private final MessageMonitorService messageMonitorService;
    private final DictionaryDataService dictionaryDataApi;


    /**
     * 列表
     *
     * @param messageMonitorPagination 消息监控分页模型
     * @return ignore
     */
    @Operation(summary = "列表")
    @SaCheckPermission("monitor.msgMonitor")
    @GetMapping
    public ActionResult<PageListVO<MessageMonitorListVO>> list(MessageMonitorPagination messageMonitorPagination) {
        List<MessageMonitorEntity> list = messageMonitorService.getList(messageMonitorPagination);

        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode("msgSendType");
        List<DictionaryDataEntity> msgSourceTypeList = dictionaryDataApi.getListByTypeDataCode("msgSourceType");

        //处理id字段转名称，若无需转或者为空可删除
        List<MessageMonitorListVO> listVO = JsonUtil.getJsonToList(list, MessageMonitorListVO.class);
        for (MessageMonitorListVO messageMonitorVO : listVO) {
            //消息类型
            if (StringUtil.isNotEmpty(messageMonitorVO.getMessageType())) {
                msgSendTypeList.stream().filter(t -> messageMonitorVO.getMessageType().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> messageMonitorVO.setMessageType(dataTypeEntity.getFullName()));
            }
            //消息来源
            if (StringUtil.isNotEmpty(messageMonitorVO.getMessageSource())) {
                msgSourceTypeList.stream().filter(t -> messageMonitorVO.getMessageSource().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> messageMonitorVO.setMessageSource(dataTypeEntity.getFullName()));
            }
            //子表数据转换
        }

        PageListVO<MessageMonitorListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(messageMonitorPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param messageMonitorForm 消息监控模型
     * @return ignore
     */
    @Operation(summary = ("创建"))
    @PostMapping
    @Parameter(name = "messageMonitorForm", description = "消息监控模型", required = true)
    @SaCheckPermission("monitor.msgMonitor")
    @DSTransactional
    public ActionResult<Object> create(@RequestBody @Valid MessageMonitorForm messageMonitorForm) throws DataException {
        String mainId = RandomUtil.uuId();
        UserInfo userInfo = UserProvider.getUser();
        MessageMonitorEntity entity = JsonUtil.getJsonToBean(messageMonitorForm, MessageMonitorEntity.class);
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setId(mainId);
        messageMonitorService.save(entity);

        return ActionResult.success(MsgCode.SU001.get());
    }


    /**
     * 批量删除
     *
     * @param msgDelForm 消息删除模型
     * @return ignore
     */
    @Operation(summary = ("批量删除"))
    @DeleteMapping("/batchRemove")
    @Parameter(name = "msgDelForm", description = "消息删除模型", required = true)
    @SaCheckPermission("monitor.msgMonitor")
    @DSTransactional
    public ActionResult<Object> batchRemove(@RequestBody MsgDelForm msgDelForm) {
        boolean flag = messageMonitorService.delete(msgDelForm.getIds());
        return flag ? ActionResult.success(MsgCode.SU003.get()) : ActionResult.fail(MsgCode.FA003.get());
    }


    /**
     * 一键清空消息监控记录
     *
     * @return
     */
    @Operation(summary = "一键清空消息监控记录")
    @SaCheckPermission("monitor.msgMonitor")
    @DeleteMapping("/empty")
    public ActionResult<Object> deleteHandelLog() {
        messageMonitorService.emptyMonitor();
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("monitor.msgMonitor")
    @GetMapping("/{id}")
    public ActionResult<MessageMonitorInfoVO> info(@PathVariable("id") String id) {
        MessageMonitorEntity entity = messageMonitorService.getInfo(id);
        MessageMonitorInfoVO vo = JsonUtil.getJsonToBean(entity, MessageMonitorInfoVO.class);

        return ActionResult.success(vo);
    }

    /**
     * 表单信息(详情页)
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "表单信息(详情页)")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("monitor.msgMonitor")
    @GetMapping("/detail/{id}")
    public ActionResult<MessageMonitorInfoVO> detailInfo(@PathVariable("id") String id) {
        MessageMonitorEntity entity = messageMonitorService.getInfo(id);

        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode("msgSendType");
        List<DictionaryDataEntity> msgSourceTypeList = dictionaryDataApi.getListByTypeDataCode("msgSourceType");

        MessageMonitorInfoVO vo = JsonUtil.getJsonToBean(entity, MessageMonitorInfoVO.class);
        if (StringUtil.isNotEmpty(vo.getMessageType())) {
            msgSendTypeList.stream().filter(t -> vo.getMessageType().equals(t.getEnCode())).findFirst()
                    .ifPresent(dataTypeEntity -> vo.setMessageType(dataTypeEntity.getFullName()));
        }
        if (StringUtil.isNotEmpty(vo.getMessageSource())) {
            msgSourceTypeList.stream().filter(t -> vo.getMessageSource().equals(t.getEnCode())).findFirst()
                    .ifPresent(dataTypeEntity -> vo.setMessageSource(dataTypeEntity.getFullName()));
        }
        if (!"webhook".equals(vo.getMessageType())) {
            vo.setReceiveUser(messageMonitorService.userSelectValues(vo.getReceiveUser()));
        }
        return ActionResult.success(vo);
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return ignore
     */
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("monitor.msgMonitor")
    @DSTransactional
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        MessageMonitorEntity entity = messageMonitorService.getInfo(id);
        if (entity != null) {
            messageMonitorService.delete(entity);

        }
        return ActionResult.success(MsgCode.SU003.get());
    }


}
