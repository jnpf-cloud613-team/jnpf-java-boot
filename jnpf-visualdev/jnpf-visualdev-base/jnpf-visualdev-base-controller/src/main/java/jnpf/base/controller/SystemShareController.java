package jnpf.base.controller;

import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.SystemShareEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.model.VisualdevTreeVO;
import jnpf.base.model.share.SystemShareForm;
import jnpf.base.model.share.SystemShareVo;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SystemService;
import jnpf.base.service.SystemShareService;
import jnpf.base.service.VisualdevReleaseService;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.util.context.RequestContext;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 跨应用数据
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/30 16:22:57
 */
@Tag(name = "跨应用数据", description = "systemShare")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/share")
public class SystemShareController extends SuperController<SystemShareService, SystemShareEntity> {

    private final SystemShareService systemShareService;
    private final SystemService systemService;
    private final VisualdevReleaseService visualdevReleaseService;
    private final DictionaryDataService dictionaryDataApi;

    @Operation(summary = "跨应用数据")
    @GetMapping
    public ActionResult<Object> list() {
        List<SystemShareEntity> list = systemShareService.getList();
        List<String> collect = list.stream().map(SystemShareEntity::getObjectId).collect(Collectors.toList());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("ids", collect);
        return ActionResult.success(resultMap);
    }

    @Operation(summary = "跨应用数据回显")
    @Parameter(name = "ids", description = "主键", required = true)
    @PostMapping("/SelectedList")
    public ActionResult<List<SystemShareVo>> selectedList(@RequestBody SystemShareForm form) {
        List<SystemShareVo> list = systemShareService.selectedList(form.getIds());
        return ActionResult.success(list);
    }

    @Operation(summary = "保存")
    @Parameter(name = "form", description = "新建模型", required = true)
    @PostMapping("/save")
    public ActionResult<Object> save(@RequestBody SystemShareForm form) {
        systemShareService.save(form.getIds());
        return ActionResult.success(MsgCode.SU002.get());
    }

    @Operation(summary = "跨应用功能下拉框")
    @GetMapping("/Selector")
    public ActionResult<ListVO<VisualdevTreeVO>> selector() {
        List<VisualdevTreeVO> voList = new ArrayList<>();
        VisualdevTreeVO thisVo = new VisualdevTreeVO("category1", "当前应用");
        VisualdevTreeVO otherVo = new VisualdevTreeVO("category2", "跨应用");

        //当前应用
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        String systemId = Objects.equals(infoByEnCode.getIsMain(), 1) ? "" : infoByEnCode.getId();
        List<VisualdevReleaseEntity> vsList = visualdevReleaseService.selectorList(systemId);
        //主系统，套件关联表单用应用分类
        if (Objects.equals(infoByEnCode.getIsMain(), 1)) {
            List<String> sysIds = vsList.stream().map(VisualdevReleaseEntity::getSystemId).collect(Collectors.toList());
            List<SystemEntity> listByIds = systemService.getListByIds(sysIds, null);
            for (SystemEntity se : listByIds) {
                VisualdevTreeVO vo = JsonUtil.getJsonToBean(se, VisualdevTreeVO.class);
                vo.setHasChildren(true);
                List<VisualdevTreeVO> childList = new ArrayList<>();
                for (VisualdevReleaseEntity entity : vsList) {
                    if (vo.getId().equals(entity.getSystemId())) {
                        VisualdevTreeVO model = JsonUtil.getJsonToBean(entity, VisualdevTreeVO.class);
                        childList.add(model);
                    }
                }
                if (CollUtil.isNotEmpty(childList)) {
                    vo.setChildren(childList);
                    voList.add(vo);
                }
            }
            ListVO<VisualdevTreeVO> listVO = new ListVO<>();
            listVO.setList(voList);
            return ActionResult.success(listVO);
        }
        //是否过滤webType。只要在线开发带列表的
        vsList = vsList.stream().filter(t -> Objects.equals(t.getType(), 1) && Objects.equals(t.getWebType(), 2)).collect(Collectors.toList());
        List<String> vsIds = vsList.stream().map(VisualdevReleaseEntity::getCategory).collect(Collectors.toList());
        Map<String, List<VisualdevReleaseEntity>> vsGroup = vsList.stream().collect(Collectors.groupingBy(VisualdevReleaseEntity::getCategory));
        List<DictionaryDataEntity> dictionList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        List<VisualdevTreeVO> thisChild = new ArrayList<>();
        for (DictionaryDataEntity item : dictionList) {
            if (vsIds.contains(item.getId())) {
                VisualdevTreeVO visualdevTreeVO = new VisualdevTreeVO();
                visualdevTreeVO.setId(item.getId());
                visualdevTreeVO.setFullName(item.getFullName());
                visualdevTreeVO.setHasChildren(true);
                List<VisualdevTreeVO> children = new ArrayList<>();
                for (VisualdevReleaseEntity vsEnt : vsGroup.get(item.getId())) {
                    VisualdevTreeVO child = new VisualdevTreeVO();
                    child.setId(vsEnt.getId());
                    child.setFullName(vsEnt.getFullName());
                    children.add(child);
                }
                visualdevTreeVO.setChildren(children);
                thisChild.add(visualdevTreeVO);
            }
        }

        //跨应用
        List<SystemShareVo> listShare = systemShareService.selector();
        List<SystemEntity> sysList = systemService.getList();
        List<String> sysIds = listShare.stream().map(SystemShareVo::getSourceId).collect(Collectors.toList());
        Map<String, List<SystemShareVo>> group = listShare.stream().collect(Collectors.groupingBy(SystemShareVo::getSourceId));
        //应用分类
        List<VisualdevTreeVO> otherChild = new ArrayList<>();
        for (SystemEntity item : sysList) {
            if (sysIds.contains(item.getId())) {
                VisualdevTreeVO visualdevTreeVO = new VisualdevTreeVO();
                visualdevTreeVO.setId(item.getId());
                visualdevTreeVO.setFullName(item.getFullName());
                visualdevTreeVO.setHasChildren(true);
                List<VisualdevTreeVO> children = new ArrayList<>();
                for (SystemShareVo systemShareVo : group.get(item.getId())) {
                    VisualdevTreeVO child = new VisualdevTreeVO();
                    child.setId(systemShareVo.getObjectId());
                    child.setFullName(systemShareVo.getObjectName());
                    children.add(child);
                }
                visualdevTreeVO.setChildren(children);
                otherChild.add(visualdevTreeVO);
            }
        }
        thisVo.setChildren(thisChild);
        otherVo.setChildren(otherChild);
        voList.add(thisVo);
        voList.add(otherVo);
        ListVO<VisualdevTreeVO> listVO = new ListVO<>();
        listVO.setList(voList);
        return ActionResult.success(listVO);
    }
}
