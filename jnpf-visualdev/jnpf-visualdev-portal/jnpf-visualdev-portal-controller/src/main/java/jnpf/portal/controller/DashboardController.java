package jnpf.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.vo.ListVO;
import jnpf.message.model.NoticeModel;
import jnpf.message.model.message.NoticeVO;
import jnpf.message.service.MessageService;
import jnpf.portal.model.EmailVO;
import jnpf.service.EmailReceiveService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主页控制器
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "主页控制器", description = "Home")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/visualdev/Dashboard")
public class DashboardController {

    private final EmailReceiveService emailReceiveService;
    private final MessageService messageService;


    /**
     * 获取通知公告
     *
     * @return
     */
    @Operation(summary = "获取通知公告")
    @PostMapping("/Notice")
    public ActionResult<ListVO<NoticeVO>> getNotice(@RequestBody NoticeModel noticeModel) {
        List<NoticeVO> list = JsonUtil.getJsonToList(messageService.getNoticeList(noticeModel.getTypeList()), NoticeVO.class);
        ListVO<NoticeVO> voList = new ListVO<>();
        voList.setList(list);
        return ActionResult.success(voList);
    }

    /**
     * 获取未读邮件
     *
     * @return
     */
    @Operation(summary = "获取未读邮件")
    @GetMapping("/Email")
    public ActionResult<ListVO<EmailVO>> getEmail() {
        List<EmailVO> list = JsonUtil.getJsonToList(emailReceiveService.getDashboardReceiveList(), EmailVO.class);
        ListVO<EmailVO> voList = new ListVO<>();
        voList.setList(list);
        return ActionResult.success(voList);
    }

}
