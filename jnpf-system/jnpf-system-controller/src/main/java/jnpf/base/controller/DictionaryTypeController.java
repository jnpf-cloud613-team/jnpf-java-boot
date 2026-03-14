package jnpf.base.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.JsonUtil;
import jnpf.base.ActionResult;
import jnpf.base.vo.ListVO;
import jnpf.base.model.dictionarytype.*;
import jnpf.base.entity.DictionaryTypeEntity;
import jnpf.base.service.DictionaryTypeService;
import jnpf.util.StringUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 字典分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "数据字典分类", description = "DictionaryType")
@RestController
@RequestMapping("/api/system/DictionaryType")
@RequiredArgsConstructor
public class DictionaryTypeController extends SuperController<DictionaryTypeService, DictionaryTypeEntity> {


    private final DictionaryTypeService dictionaryTypeService;

    /**
     * 获取字典分类
     *
     * @return
     */
    @Operation(summary = "获取字典分类")
    @GetMapping
    public ActionResult<ListVO<DictionaryTypeListVO>> list() {
        List<DictionaryTypeEntity> data = dictionaryTypeService.getList();
        List<DictionaryTypeModel> voListVO = JsonUtil.getJsonToList(data, DictionaryTypeModel.class);
        voListVO.forEach(vo -> {
            if (StringUtil.isNotEmpty(vo.getCategory()) && "1".equals(vo.getCategory()) && "-1".equals(vo.getParentId())) {
                vo.setCategory("系统");
                vo.setParentId("1");
            } else if (StringUtil.isNotEmpty(vo.getCategory()) && "0".equals(vo.getCategory()) && "-1".equals(vo.getParentId())) {
                vo.setCategory("业务");
                vo.setParentId("0");
            }
        });
        List<SumTree<DictionaryTypeModel>> sumTrees = TreeDotUtils.convertListToTreeDot(voListVO);
        List<DictionaryTypeListVO> list = JsonUtil.getJsonToList(sumTrees, DictionaryTypeListVO.class);

        DictionaryTypeListVO parentVO = new DictionaryTypeListVO();
        parentVO.setFullName("系统字典");
        parentVO.setChildren(new ArrayList<>());
        parentVO.setId("1");
        DictionaryTypeListVO parentVO1 = new DictionaryTypeListVO();
        parentVO1.setFullName("业务字典");
        parentVO1.setChildren(new ArrayList<>());
        parentVO1.setId("0");

        list.forEach(vo -> {
            if ("系统".equals(vo.getCategory())) {
                List<DictionaryTypeListVO> children = parentVO.getChildren();
                children.add(vo);
                parentVO.setHasChildren(true);
            }else {
                List<DictionaryTypeListVO> children = parentVO1.getChildren();
                children.add(vo);
                parentVO1.setHasChildren(true);
            }
        });
        List<DictionaryTypeListVO> listVo = new ArrayList<>();
        listVo.add(parentVO1);
        listVo.add(parentVO);

        ListVO<DictionaryTypeListVO> vo = new ListVO<>();
        vo.setList(listVo);
        return ActionResult.success(vo);
    }


    /**
     * 获取字典分类
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取所有字典分类下拉框列表")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/Selector/{id}")
    public ActionResult<ListVO<DictionaryTypeListVO>> selectorTreeView(@PathVariable("id") String id) {
        List<DictionaryTypeEntity> data = dictionaryTypeService.getList();
        if (!"0".equals(id)) {
            data.remove(dictionaryTypeService.getInfo(id));
        }
        List<DictionaryTypeModel> voListVO = JsonUtil.getJsonToList(data, DictionaryTypeModel.class);
        voListVO.forEach(vo -> {
            if (StringUtil.isNotEmpty(vo.getCategory()) && "1".equals(vo.getCategory()) && "-1".equals(vo.getParentId())) {
                vo.setCategory("系统");
                vo.setParentId("1");
            } else if (StringUtil.isNotEmpty(vo.getCategory()) && "0".equals(vo.getCategory()) && "-1".equals(vo.getParentId())) {
                vo.setCategory("业务");
                vo.setParentId("0");
            }
        });
        List<SumTree<DictionaryTypeModel>> sumTrees = TreeDotUtils.convertListToTreeDot(voListVO);
        List<DictionaryTypeListVO> list = JsonUtil.getJsonToList(sumTrees, DictionaryTypeListVO.class);

        DictionaryTypeListVO parentVO = new DictionaryTypeListVO();
        parentVO.setFullName("系统字典");
        parentVO.setChildren(new ArrayList<>());
        parentVO.setId("1");
        DictionaryTypeListVO parentVO1 = new DictionaryTypeListVO();
        parentVO1.setFullName("业务字典");
        parentVO1.setChildren(new ArrayList<>());
        parentVO1.setId("0");

        list.forEach(vo -> {
            if ("系统".equals(vo.getCategory())) {
                List<DictionaryTypeListVO> children = parentVO.getChildren();
                children.add(vo);
                parentVO.setHasChildren(true);
            }else {
                List<DictionaryTypeListVO> children = parentVO1.getChildren();
                children.add(vo);
                parentVO1.setHasChildren(true);
            }
        });
        List<DictionaryTypeListVO> listVo = new ArrayList<>();
        listVo.add(parentVO1);
        listVo.add(parentVO);

        ListVO<DictionaryTypeListVO> vo = new ListVO<>();
        vo.setList(listVo);
        return ActionResult.success(vo);
    }

    /**
     * 获取字典分类信息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取字典分类信息")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/{id}")
    public ActionResult<DictionaryTypeInfoVO> info(@PathVariable("id") String id) throws DataException {
        DictionaryTypeEntity entity = dictionaryTypeService.getInfo(id);
        if ("-1".equals(entity.getParentId())) {
            entity.setParentId(String.valueOf(entity.getCategory()));
        }
        DictionaryTypeInfoVO vo = JsonUtil.getJsonToBeanEx(entity, DictionaryTypeInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 添加字典分类
     *
     * @param dictionaryTypeCrForm 实体对象
     * @return
     */
    @Operation(summary = "添加字典分类")
    @Parameter(name = "dictionaryTypeCrForm", description = "实体对象", required = true)
    @SaCheckPermission("sysData.dictionary")
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid DictionaryTypeCrForm dictionaryTypeCrForm) {
        DictionaryTypeEntity entity = JsonUtil.getJsonToBean(dictionaryTypeCrForm, DictionaryTypeEntity.class);
        if ("0".equals(entity.getParentId()) || "1".equals(entity.getParentId())) {
            entity.setCategory(Integer.parseInt(entity.getParentId()));
            entity.setParentId("-1");
        } else {
            DictionaryTypeEntity entity1 = dictionaryTypeService.getInfo(dictionaryTypeCrForm.getParentId());
            entity.setCategory(entity1.getCategory());
        }
        if (dictionaryTypeService.isExistByFullName(entity.getFullName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (dictionaryTypeService.isExistByEnCode(entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        dictionaryTypeService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改字典分类
     *
     * @param dictionaryTypeUpForm 实体对象
     * @param id                   主键值
     * @return
     */
    @Operation(summary = "修改字典分类")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "dictionaryTypeUpForm", description = "实体对象", required = true)
    @SaCheckPermission("sysData.dictionary")
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid DictionaryTypeUpForm dictionaryTypeUpForm) {
        DictionaryTypeEntity entity = JsonUtil.getJsonToBean(dictionaryTypeUpForm, DictionaryTypeEntity.class);
        if ("0".equals(entity.getParentId()) || "1".equals(entity.getParentId())) {
            entity.setCategory(Integer.parseInt(entity.getParentId()));
            entity.setParentId("-1");
        } else {
            DictionaryTypeEntity entity1 = dictionaryTypeService.getInfo(dictionaryTypeUpForm.getParentId());
            entity.setCategory(entity1.getCategory());
        }
        if (dictionaryTypeService.isExistByFullName(entity.getFullName(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (dictionaryTypeService.isExistByEnCode(entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = dictionaryTypeService.update(id, entity);
        if (!flag) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除字典分类
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "删除字典分类")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("sysData.dictionary")
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        DictionaryTypeEntity entity = dictionaryTypeService.getInfo(id);
        if (entity != null) {
            boolean isOk = dictionaryTypeService.delete(entity);
            if (isOk) {
                return ActionResult.success(MsgCode.SU003.get());
            } else {
                return ActionResult.fail(MsgCode.SYS014.get());
            }
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

}
