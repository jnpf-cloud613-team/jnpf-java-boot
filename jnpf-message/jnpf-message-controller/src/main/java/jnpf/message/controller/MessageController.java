package jnpf.message.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
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
import jnpf.message.entity.MessageEntity;
import jnpf.message.entity.MessageReceiveEntity;
import jnpf.message.model.NoticePagination;
import jnpf.message.model.message.*;
import jnpf.message.service.MessageService;
import jnpf.message.util.SendFlowMsgUtil;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统公告
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "系统公告", description = "Message")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/message")
public class MessageController extends SuperController<MessageService, MessageEntity> {

    private final MessageService messageService;
    private final SendFlowMsgUtil sendFlowMsgUtil;
    private final UserService userApi;
    private final DictionaryDataService dictionaryDataApi;

    /**
     * 列表（通知公告）
     *
     * @param pagination
     * @return
     */
    @Operation(summary = "获取系统公告列表（带分页）")
    @SaCheckPermission("msgCenter.notice")
    @PostMapping("/Notice/List")
    public ActionResult<PageListVO<MessageNoticeVO>> noticeList(@RequestBody NoticePagination pagination) {
        messageService.updateEnabledMark();
        List<MessageEntity> list = messageService.getNoticeList(pagination);
        List<UserEntity> userList = userApi.getUserName(list.stream().map(MessageEntity::getCreatorUserId).collect(Collectors.toList()));
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        List<DictionaryDataEntity> noticeType = dictionaryDataApi.getListByTypeDataCode("NoticeType");
        List<MessageNoticeVO> voList = new ArrayList<>();
        // 判断是否过期
        list.forEach(t -> {
            MessageNoticeVO vo = JsonUtil.getJsonToBean(t, MessageNoticeVO.class);
            // 处理是否过期
            if (t.getExpirationTime() != null && t.getEnabledMark() == 1 && t.getExpirationTime().getTime() < System.currentTimeMillis()) {
                vo.setEnabledMark(2);
            }
            DictionaryDataEntity dictionaryDataEntity = noticeType.stream().filter(notice -> notice.getEnCode().equals(t.getCategory())).findFirst().orElse(new DictionaryDataEntity());
            vo.setCategory(dictionaryDataEntity.getFullName());
            // 转换创建人、发布人
            UserEntity user = userList.stream().filter(ul -> ul.getId().equals(t.getCreatorUserId())).findFirst().orElse(null);
            vo.setCreatorUser(user != null ? user.getRealName() + "/" + user.getAccount() : "");
            if (t.getEnabledMark() != null && t.getEnabledMark() != 0) {
                UserEntity entity = userApi.getInfo(t.getLastModifyUserId());
                vo.setLastModifyUserId(entity != null ? entity.getRealName() + "/" + entity.getAccount() : "");
                vo.setReleaseTime(t.getLastModifyTime() != null ? t.getLastModifyTime().getTime() : null);
                vo.setReleaseUser(vo.getLastModifyUserId());
            }
            voList.add(vo);
        });
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 添加系统公告
     *
     * @param noticeCrForm 实体对象
     * @return
     */
    @Operation(summary = "添加系统公告")
    @Parameter(name = "noticeCrForm", description = "新建系统公告模型", required = true)
    @SaCheckPermission("msgCenter.notice")
    @PostMapping("/Notice")
    public ActionResult<Object> create(@RequestBody @Valid NoticeCrForm noticeCrForm) {
        MessageEntity entity = JsonUtil.getJsonToBean(noticeCrForm, MessageEntity.class);
        if (entity != null && StringUtil.isNotEmpty(entity.getBodyText()) && (entity.getBodyText().contains("&lt;") || entity.getBodyText().contains("&amp;lt;"))) {
            return ActionResult.fail(MsgCode.MSERR112.get());
        }
        messageService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改系统公告
     *
     * @param id            主键值
     * @param messageUpForm 实体对象
     * @return
     */
    @Operation(summary = "修改系统公告")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "messageUpForm", description = "修改系统公告模型", required = true)
    @SaCheckPermission("msgCenter.notice")
    @PutMapping("/Notice/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid NoticeUpForm messageUpForm) {
        MessageEntity entity = JsonUtil.getJsonToBean(messageUpForm, MessageEntity.class);
        if (entity != null && StringUtil.isNotEmpty(entity.getBodyText()) && (entity.getBodyText().contains("&lt;") || entity.getBodyText().contains("&amp;lt;"))) {
            return ActionResult.fail(MsgCode.MSERR112.get());
        }
        boolean flag = messageService.update(id, entity);
        return flag ? ActionResult.success(MsgCode.SU004.get()) : ActionResult.fail(MsgCode.FA002.get());
    }

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取/查看系统公告信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.notice")
    @GetMapping("/Notice/{id}")
    public ActionResult<NoticeInfoVO> info(@PathVariable("id") String id) throws DataException {
        MessageEntity entity = messageService.getInfo(id);
        NoticeInfoVO vo = null;
        if (entity != null) {
            UserEntity info = userApi.getInfo(entity.getCreatorUserId());
            entity.setCreatorUserId(info != null ? info.getRealName() + "/" + info.getAccount() : "");
            vo = JsonUtilEx.getJsonToBeanEx(entity, NoticeInfoVO.class);
            vo.setReleaseUser(entity.getCreatorUserId());
            vo.setReleaseTime(entity.getLastModifyTime() != null ? entity.getLastModifyTime().getTime() : null);
            UserEntity userEntity = userApi.getInfo(entity.getLastModifyUserId());
            if (userEntity != null && StringUtil.isNotEmpty(userEntity.getId())) {
                String realName = userEntity.getRealName();
                String account = userEntity.getAccount();
                if (StringUtil.isNotEmpty(realName)) {
                    vo.setReleaseUser(realName + "/" + account);
                }
            }
        }
        return ActionResult.success(vo);
    }

    /**
     * 删除
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "删除系统公告")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.notice")
    @DeleteMapping("/Notice/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        MessageEntity entity = messageService.getInfo(id);
        if (entity != null) {
            messageService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }


    /**
     * 发布公告
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "发布系统公告")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.notice")
    @PutMapping("/Notice/{id}/Actions/Release")
    public ActionResult<Object> release(@PathVariable("id") String id) {
        MessageEntity entity = messageService.getInfo(id);
        if (entity != null) {
            List<String> userIds = null;
            if (StringUtil.isNotEmpty(entity.getToUserIds())) {
                userIds = Arrays.asList(entity.getToUserIds().split(","));
            } else {
                userIds = userApi.getListId();
            }
            List<String> userIdList = userApi.getUserIdList(userIds);
            if (sendFlowMsgUtil.sentNotice(userIdList, entity)) {
                return ActionResult.success(MsgCode.SU011.get());
            }
        }
        return ActionResult.fail(MsgCode.FA011.get());
    }
//=======================================站内消息、消息中心=================================================


    /**
     * 获取消息中心列表
     *
     * @param pagination
     * @return
     */
    @Operation(summary = "列表（通知公告/系统消息/私信消息）")
    @GetMapping
    public ActionResult<PageListVO<MessageInfoVO>> messageList(PaginationMessage pagination) {
        List<MessageInfoVO> listVO = new ArrayList<>();
        List<MessageReceiveEntity> list = messageService.getMessageList3(pagination);
        List<UserEntity> userList = userApi.getUserName(list.stream().map(MessageReceiveEntity::getCreatorUserId).collect(Collectors.toList()));
        list.forEach(t -> {
            MessageInfoVO vo = JsonUtil.getJsonToBean(t, MessageInfoVO.class);
            UserEntity user = userList.stream().filter(ul -> ul.getId().equals(t.getCreatorUserId())).findFirst().orElse(null);
            if (user != null) {
                vo.setReleaseTime(t.getCreatorTime() != null ? t.getCreatorTime().getTime() : null);
                UserEntity entity = userApi.getInfo(t.getCreatorUserId());
                vo.setReleaseUser(entity != null ? entity.getRealName() + "/" + entity.getAccount() : "");
                vo.setCreatorUser(entity != null ? entity.getRealName() + "/" + entity.getAccount() : "");
            }
            listVO.add(vo);
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }


    /**
     * 读取消息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "读取消息")
    @Parameter(name = "id", description = "主键值", required = true)
    @GetMapping("/ReadInfo/{id}")
    public ActionResult<NoticeInfoVO> readInfo(@PathVariable("id") String id) throws DataException {
        MessageReceiveEntity receive = messageService.messageRead(id);
        NoticeInfoVO vo = JsonUtil.getJsonToBean(receive, NoticeInfoVO.class);
        if (receive.getType() != null && Objects.equals(receive.getType(), 1)) {
            UserEntity user = userApi.getInfo(receive.getCreatorUserId());
            receive.setCreatorUserId(user != null ? user.getRealName() + "/" + user.getAccount() : "");
            receive.setBodyText(receive.getBodyText());
            try {
                MessageEntity jsonToBean = JsonUtil.getJsonToBean(receive.getBodyText(), MessageEntity.class);
                if (jsonToBean != null) {
                    vo.setCategory(jsonToBean.getCategory());
                    vo.setCoverImage(jsonToBean.getCoverImage());
                    vo.setExcerpt(jsonToBean.getExcerpt());
                    vo.setExpirationTime(jsonToBean.getExpirationTime() != null ? jsonToBean.getExpirationTime().getTime() : null);
                    vo.setFiles(jsonToBean.getFiles());
                    vo.setBodyText(jsonToBean.getBodyText());
                    if (jsonToBean.getId() != null) {
                        MessageEntity info = messageService.getInfo(jsonToBean.getId());
                        if (info != null) {
                            vo.setCategory(info.getCategory());
                            vo.setCoverImage(info.getCoverImage());
                            vo.setExcerpt(info.getExcerpt());
                            vo.setExpirationTime(info.getExpirationTime() != null ? info.getExpirationTime().getTime() : null);
                            vo.setFiles(info.getFiles());
                            vo.setBodyText(info.getBodyText());
                        }
                    }
                }
            } catch (Exception e) {
                vo.setBodyText(receive.getBodyText());
            }

        }
        vo.setReleaseTime(receive.getCreatorTime() != null ? receive.getCreatorTime().getTime() : null);
        vo.setReleaseUser(receive.getCreatorUserId());
        return ActionResult.success(vo);
    }


    /**
     * 全部已读
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "全部已读")
    @Parameter(name = "pagination", description = "分页模型", required = true)
    @PostMapping("/Actions/ReadAll")
    public ActionResult<Object> allRead(@RequestBody PaginationMessage pagination) {
        List<MessageReceiveEntity> list = messageService.getMessageList3(pagination);
        if (list != null && !list.isEmpty()) {
            List<String> idList = list.stream().map(MessageReceiveEntity::getId).collect(Collectors.toList());
            messageService.messageRead(idList);
            return ActionResult.success(MsgCode.SU005.get());
        } else {
            return ActionResult.fail(MsgCode.MSERR113.get());
        }
    }

    /**
     * app端获取未读数据
     *
     * @return
     */
    @Operation(summary = "app端获取未读数据")
    @GetMapping("/getUnReadMsgNum")
    public ActionResult<Object> getUnReadMsgNum() {
        Map<String, String> map = new HashMap<>();
        UserInfo userInfo = UserProvider.getUser();
        List<Integer> total = new ArrayList<>();
        List<String> msgList = Arrays.asList("unReadNotice", "unReadMsg", "unReadSystemMsg", "unReadSchedule", "unReadForm");
        for (int i = 0; i < msgList.size(); i++) {
            int count = messageService.getUnreadCount(userInfo.getUserId(), i + 1);
            map.put(msgList.get(i), count + "");
            total.add(count);
        }
        int totalNum = total.stream().mapToInt(Integer::intValue).sum();
        map.put("unReadNum", totalNum + "");
        return ActionResult.success(map);
    }

    /**
     * 删除记录
     *
     * @param recordForm 已读模型
     * @return
     */
    @Operation(summary = "删除消息")
    @Parameter(name = "recordForm", description = "已读模型", required = true)
    @DeleteMapping("/Record")
    public ActionResult<Object> deleteRecord(@RequestBody MessageRecordForm recordForm) {
        String[] id = recordForm.getIds().split(",");
        List<String> list = Arrays.asList(id);
        messageService.deleteRecord(list);
        return ActionResult.success(MsgCode.SU003.get());
    }
}
