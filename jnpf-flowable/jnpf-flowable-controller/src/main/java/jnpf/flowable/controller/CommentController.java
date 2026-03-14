package jnpf.flowable.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.CommentEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.model.comment.CommentCrForm;
import jnpf.flowable.model.comment.CommentListVO;
import jnpf.flowable.model.comment.CommentPagination;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.service.CommentService;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.service.TemplateNodeService;
import jnpf.flowable.util.ServiceUtil;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UploaderUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 流程评论
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Tag(name = "流程评论", description = "Comment")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workflow/comment")
public class CommentController extends SuperController<CommentService, CommentEntity> {

    private final ServiceUtil serviceUtil;
    private final CommentService commentService;
    private final TaskService taskService;
    private final TemplateNodeService templateNodeService;

    /**
     * 获取流程评论列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "获取流程评论列表")
    @GetMapping
    public ActionResult<PageListVO<CommentListVO>> list(CommentPagination pagination) {
        List<CommentEntity> list = commentService.getlist(pagination);
        List<String> replyId = list.stream().map(CommentEntity::getReplyId).filter(StringUtil::isNotEmpty).collect(Collectors.toList());
        List<CommentEntity> replyList = commentService.getlist(replyId);
        List<CommentListVO> listVO = JsonUtil.getJsonToList(list, CommentListVO.class);
        List<String> userId = list.stream().map(CommentEntity::getCreatorUserId).collect(Collectors.toList());
        userId.addAll(replyList.stream().map(CommentEntity::getCreatorUserId).collect(Collectors.toList()));
        UserInfo userInfo = UserProvider.getUser();
        List<UserEntity> userName = serviceUtil.getUserName(userId);
        for (CommentListVO commentModel : listVO) {
            UserEntity userEntity = userName.stream().filter(t -> t.getId().equals(commentModel.getCreatorUserId())).findFirst().orElse(null);
            if (commentModel.getCreatorUserId().equals(userInfo.getUserId()) && !"1".equals(String.valueOf(commentModel.getDeleteShow()))) {
                commentModel.setIsDel(1); //1-删除按钮显示
            } else if ("1".equals(String.valueOf(commentModel.getDeleteShow()))) {
                commentModel.setIsDel(2); //1-删除按钮显示
                commentModel.setText("该评论已被删除");
            } else {
                commentModel.setIsDel(0);
            }
            commentModel.setCreatorUser(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
            commentModel.setCreatorUserHeadIcon(userEntity != null ? UploaderUtil.uploaderImg(userEntity.getHeadIcon()) : commentModel.getCreatorUserHeadIcon());
            List<CommentEntity> collect = replyList.stream().filter(t -> t.getId().equals(commentModel.getReplyId())).collect(Collectors.toList());
            StringJoiner name = new StringJoiner(",");
            StringJoiner text = new StringJoiner(",");
            for (CommentEntity replyEntity : collect) {
                if (null != replyEntity) {
                    UserEntity user = userName.stream().filter(t -> t.getId().equals(replyEntity.getCreatorUserId())).findFirst().orElse(null);
                    if (user != null) {
                        name.add(user.getRealName() + "/" + user.getAccount());
                    }
                    String resText = ("1".equals(String.valueOf(replyEntity.getDeleteShow())) || "1".equals(String.valueOf(replyEntity.getDeleteMark()))) ? "该评论已被删除" : replyEntity.getText();
                    text.add(resText);
                }
            }
            commentModel.setReplyText(text.toString());
            commentModel.setReplyUser(name.toString());
            CommentEntity entity = list.stream().filter(e -> ObjectUtil.equals(e.getId(), commentModel.getId())).findFirst().orElse(null);
            if (null != entity) {
                commentModel.setFile(entity.getFiles());
            }
        }
        PaginationVO vo = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, vo);
    }

    /**
     * 新建流程评论
     *
     * @param commentForm 流程评论模型
     * @return
     */
    @Operation(summary = "新建流程评论")
    @PostMapping
    @Parameter(name = "commentForm", description = "流程评论模型", required = true)
    public ActionResult<Object> create(@RequestBody @Valid CommentCrForm commentForm) throws WorkFlowException {
        CommentEntity entity = JsonUtil.getJsonToBean(commentForm, CommentEntity.class);
        entity.setFiles(commentForm.getFile());
        commentService.create(entity);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 删除流程评论
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除流程评论")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        CommentEntity entity = commentService.getInfo(id);
        if (entity.getCreatorUserId().equals(UserProvider.getUser().getUserId())) {
            boolean delComment = false;
            TaskEntity task = taskService.getInfoSubmit(entity.getTaskId(), TaskEntity::getId, TaskEntity::getFlowId);
            if (task != null) {
                TemplateNodeEntity nodeEntity = templateNodeService.getList(task.getFlowId()).stream().filter(e -> StringUtils.equals(NodeEnum.GLOBAL.getType(), e.getNodeType())).findFirst().orElse(null);
                if (nodeEntity != null) {
                    NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                    delComment = nodeModel.getHasCommentDeletedTips();
                }
            }
            commentService.delete(entity, delComment);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.success(MsgCode.FA003.get());
    }

}
