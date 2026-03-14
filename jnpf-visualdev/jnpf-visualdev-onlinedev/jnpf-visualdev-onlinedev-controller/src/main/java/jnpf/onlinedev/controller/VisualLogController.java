package jnpf.onlinedev.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.onlinedev.entity.VisualLogEntity;
import jnpf.onlinedev.model.log.VisualLogPage;
import jnpf.onlinedev.model.log.VisualLogVo;
import jnpf.onlinedev.service.VisualLogService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.UploaderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "在线开发数据日志", description = "OnlineLog")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/OnlineLog")
public class VisualLogController {

    private final VisualLogService visualLogService;
    private final UserService userService;

    @Operation(summary = "列表")
    @GetMapping
    public ActionResult<PageListVO<VisualLogVo>> list(VisualLogPage page) {
        List<VisualLogEntity> list = visualLogService.getList(page);
        List<String> userId = list.stream().map(t -> t.getCreatorUserId()).collect(Collectors.toList());
        List<UserEntity> userList = userService.getUserName(userId);
        List<VisualLogVo> listVo = new ArrayList<>();
        for (VisualLogEntity entity : list) {
            VisualLogVo vo = JsonUtil.getJsonToBean(entity, VisualLogVo.class);
            UserEntity userEntity = userList.stream().filter(t -> vo.getCreatorUserId().equals(t.getId())).findFirst().orElse(null);
            if (userEntity != null) {
                vo.setCreatorUserName(userEntity.getRealName());
                vo.setHeadIcon(UploaderUtil.uploaderImg(userEntity.getHeadIcon()));
            }
            vo.setCreatorTime(DateUtil.dateToString(entity.getCreatorTime(), "yyyy-MM-dd HH:mm"));
            listVo.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(page, PaginationVO.class);
        return ActionResult.page(listVo, paginationVO);
    }
}
