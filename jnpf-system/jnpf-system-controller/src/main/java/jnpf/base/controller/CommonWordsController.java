package jnpf.base.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.CommonWordsEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.commonword.ComWordsPagination;
import jnpf.base.model.commonword.CommonWordsForm;
import jnpf.base.model.commonword.CommonWordsVO;
import jnpf.base.service.CommonWordsService;
import jnpf.base.service.SystemService;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 常用语控制类
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-06
 */
@Tag(name = "审批常用语", description = "commonWords")
@RestController
@RequestMapping("/api/system/CommonWords")
@RequiredArgsConstructor
public class CommonWordsController extends SuperController<CommonWordsService, CommonWordsEntity> {


    private final CommonWordsService commonWordsService;

    private final SystemService systemService;


    /**
     * 列表
     *
     * @param comWordsPagination 页面参数对象
     * @return 列表结果集
     */
    @Operation(summary = "当前系统应用列表")
    @GetMapping()
    public ActionResult<PageListVO<CommonWordsVO>> getList(@Valid ComWordsPagination comWordsPagination) {
        List<CommonWordsEntity> entityList = commonWordsService.getSysList(comWordsPagination, false);
        List<CommonWordsVO> voList = JsonUtil.getJsonToList(entityList, CommonWordsVO.class);
        formatSystemNames(voList);
        return ActionResult.page(voList, JsonUtil.getJsonToBean(comWordsPagination, PaginationVO.class));
    }

    /**
     * 获取信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取信息")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/{id}")
    public ActionResult<CommonWordsVO> getInfo(@PathVariable String id) {
        CommonWordsEntity entity = commonWordsService.getById(id);
        if (StringUtil.isNotEmpty(entity.getSystemIds())) {
            String[] sysIds = entity.getSystemIds().split(",");
            List<String> ids = new ArrayList<>();
            for (String sysId : sysIds) {
                if (!StringUtil.isEmpty(sysId)) {
                    SystemEntity systemEntity = systemService.getInfo(sysId);
                    if (systemEntity != null && systemEntity.getEnabledMark() == 1) {
                        ids.add(sysId);
                    }
                }
            }
            if (!ids.isEmpty()) {
                entity.setSystemIds(StringUtils.join(ids, ","));
            } else {
                entity.setSystemIds(null);
            }
        }
        CommonWordsVO vo = JsonUtil.getJsonToBean(entity, CommonWordsVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 下拉列表
     *
     * @return
     */
    @Operation(summary = "下拉列表")
    @GetMapping("/Selector")
    public ActionResult<ListVO<CommonWordsVO>> getSelect(String type) {
        List<CommonWordsVO> voList = JsonUtil.getJsonToList(commonWordsService.getListModel(type), CommonWordsVO.class);
        formatSystemNames(voList);
        return ActionResult.success(new ListVO<>(voList));
    }

    /**
     * 新建
     *
     * @param commonWordsForm 实体模型
     * @return
     */
    @Operation(summary = "新建")
    @Parameter(name = "commonWordsForm", description = "实体模型", required = true)
    @PostMapping("")
    public ActionResult<CommonWordsForm> create(@RequestBody CommonWordsForm commonWordsForm) {
        if (Boolean.TRUE.equals(commonWordsService.existCommonWord(null, commonWordsForm.getCommonWordsText(), commonWordsForm.getCommonWordsType()))) {
            return ActionResult.fail(MsgCode.SYS105.get());
        }
        CommonWordsEntity entity = JsonUtil.getJsonToBean(commonWordsForm, CommonWordsEntity.class);
        entity.setId(RandomUtil.uuId());
        entity.setUsesNum(0L);
        commonWordsService.save(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @param commonWordsForm 实体模型
     * @return
     */
    @Operation(summary = "修改")
    @Parameter(name = "commonWordsForm", description = "实体模型", required = true)
    @PutMapping("/{id}")
    public ActionResult<CommonWordsForm> update(@RequestBody CommonWordsForm commonWordsForm,@PathVariable String id) {
        if (Boolean.TRUE.equals(commonWordsService.existCommonWord(commonWordsForm.getId(), commonWordsForm.getCommonWordsText(), commonWordsForm.getCommonWordsType()))) {
            return ActionResult.fail(MsgCode.SYS105.get());
        }
        CommonWordsEntity entity = JsonUtil.getJsonToBean(commonWordsForm, CommonWordsEntity.class);
        entity.setId(commonWordsForm.getId());
        commonWordsService.updateById(entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    @DeleteMapping("/{id}")
    public ActionResult<CommonWordsForm> delete(@PathVariable String id) {
        //对象存在判断
        if (commonWordsService.getById(id) != null) {
            commonWordsService.removeById(id);
            return ActionResult.success(MsgCode.SU003.get());
        } else {
            return ActionResult.fail(MsgCode.FA003.get());
        }
    }

    @Operation(summary = "常用语使用")
    @Parameter(name = "commonWordsForm", description = "实体模型")
    @PostMapping("/UsesNum")
    public ActionResult<CommonWordsForm> addCommonWordsNum(@RequestBody CommonWordsForm commonWordsForm) {
        commonWordsService.addCommonWordsNum(commonWordsForm.getCommonWordsText());
        return ActionResult.success(MsgCode.SU000.get());
    }

    private void formatSystemNames(List<CommonWordsVO> voList) {
        voList.forEach(vo -> {
            if (StringUtil.isNotEmpty(vo.getSystemIds())) {
                List<String> sysNameList = systemService.getListByIds(vo.getSystemIds(), null).stream()
                        .map(SystemEntity::getFullName).collect(Collectors.toList());
                vo.setSystemNames(StringUtils.join(sysNameList, ","));
            }
        });
    }

}
