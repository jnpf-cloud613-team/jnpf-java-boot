package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.ProvinceEntity;
import jnpf.base.model.province.*;
import jnpf.base.service.ProvinceService;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.StringUtil;
import jnpf.util.treeutil.ListToTreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行政区划
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "行政区划", description = "Area")
@RestController
@RequestMapping("/api/system/Area")
@RequiredArgsConstructor
public class AreaController extends SuperController<ProvinceService, ProvinceEntity> {


    private final ProvinceService provinceService;

    /**
     * 列表（异步加载）
     *
     * @param nodeId 节点主键
     * @param page 关键字
     * @return
     */
    @Operation(summary = "列表（异步加载）")
    @Parameter(name = "nodeId", description = "节点主键", required = true)
    @SaCheckPermission("sysData.area")
    @GetMapping("/{nodeId}")
    public ActionResult<ListVO<ProvinceListVO>> list(@PathVariable("nodeId") String nodeId, PaginationProvince page) {
        List<ProvinceEntity> data = provinceService.getList(nodeId, page);
        List<ProvinceEntity> result = JsonUtil.getJsonToList(ListToTreeUtil.treeWhere(data, data), ProvinceEntity.class);
        List<ProvinceListVO> treeList = JsonUtil.getJsonToList(result, ProvinceListVO.class);
        int i = 0;
        for (ProvinceListVO entity : treeList) {
            boolean childNode = provinceService.getList(entity.getId()).isEmpty();
            ProvinceListVO provinceListVO = JsonUtil.getJsonToBean(entity, ProvinceListVO.class);
            provinceListVO.setIsLeaf(childNode);
            provinceListVO.setHasChildren(!childNode);
            treeList.set(i, provinceListVO);
            i++;
        }
        ListVO<ProvinceListVO> vo = new ListVO<>();
        vo.setList(treeList);
        return ActionResult.success(vo);
    }

    /**
     * 获取行政区划下拉框数据
     *
     * @param id 主键
     * @param ids 主键集合
     * @return
     */
    @Operation(summary = "获取行政区划下拉框数据")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "ids", description = "主键集合", required = true)
    @GetMapping("/{id}/Selector/{ids}")
    public ActionResult<ListVO<ProvinceSelectListVO>> selectList(@PathVariable("id") String id, @PathVariable("ids") String ids) {
        List<ProvinceEntity> data = provinceService.getList(id);
        data = data.stream().filter(t -> t.getEnabledMark() == 1).collect(Collectors.toList());
        if (!"0".equals(ids)) {
            //排除子集
            filterData(data, new ArrayList<>(Collections.singletonList(ids)));
        }
        List<ProvinceSelectListVO> treeList = JsonUtil.getJsonToList(data, ProvinceSelectListVO.class);
        int i = 0;
        for (ProvinceSelectListVO entity : treeList) {
            ProvinceSelectListVO provinceListVO = JsonUtil.getJsonToBean(entity, ProvinceSelectListVO.class);
            provinceListVO.setIsLeaf(false);
            treeList.set(i, provinceListVO);
            i++;
        }
        ListVO<ProvinceSelectListVO> vo = new ListVO<>();
        vo.setList(treeList);
        return ActionResult.success(vo);
    }

    /**
     * 递归排除子集
     *
     * @param data 普通列表
     * @param id   ignore
     */
    private void filterData(List<ProvinceEntity> data, List<String> id) {
        List<ProvinceEntity> collect = null;
        //获取子集信息
        for (String ids : id) {
            collect = data.stream().filter(t -> ids.equals(t.getParentId())).collect(Collectors.toList());
            data.removeAll(collect);
        }
        //递归移除子集的子集
        if(collect != null && !collect.isEmpty()) {
                filterData(data, collect.stream().map(ProvinceEntity::getId).collect(Collectors.toList()));
            }

    }

    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "获取行政区划信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("sysData.area")
    @GetMapping("/{id}/Info")
    public ActionResult<ProvinceInfoVO> info(@PathVariable("id") String id) throws DataException {
        ProvinceEntity entity = provinceService.getInfo(id);
        ProvinceInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, ProvinceInfoVO.class);
        if (!"-1".equals(entity.getParentId())) {
            ProvinceEntity parent = provinceService.getInfo(entity.getParentId());
            vo.setParentName(parent.getFullName());
        }
        return ActionResult.success(vo);
    }

    /**
     * 新建
     *
     * @param provinceCrForm 实体对象
     * @return ignore
     */
    @Operation(summary = "添加行政区划")
    @Parameter(name = "provinceCrForm", description = "实体对象", required = true)
    @SaCheckPermission("sysData.area")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid ProvinceCrForm provinceCrForm) {
        ProvinceEntity entity = JsonUtil.getJsonToBean(provinceCrForm, ProvinceEntity.class);
        if (provinceService.isExistByEnCode(provinceCrForm.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.SYS001.get());
        }
        if (StringUtil.isEmpty(provinceCrForm.getParentId())) {
            entity.setParentId("-1");
        }
        if (entity.getParentId().equals("-1")) {
            entity.setType("1");
        } else {
            ProvinceEntity info = provinceService.getInfo(entity.getParentId());
            int type = info!=null? Integer.parseInt(info.getType()) + 1 : 1;
            entity.setType(String.valueOf(type));
        }
        provinceService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param id             主键值
     * @param provinceUpForm ignore
     * @return ignore
     */
    @Operation(summary = "修改行政区划")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "provinceUpForm", description = "实体对象", required = true)
    @SaCheckPermission("sysData.area")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid ProvinceUpForm provinceUpForm) {
        ProvinceEntity entity = JsonUtil.getJsonToBean(provinceUpForm, ProvinceEntity.class);
        if (provinceService.isExistByEnCode(provinceUpForm.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.SYS001.get());
        }
        boolean flag = provinceService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("sysData.area")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        if (provinceService.getList(id).isEmpty()) {
            ProvinceEntity entity = provinceService.getInfo(id);
            if (entity != null) {
                provinceService.delete(entity);
                return ActionResult.success(MsgCode.SU003.get());
            }
            return ActionResult.fail(MsgCode.FA003.get());
        } else {
            return ActionResult.fail(MsgCode.SYS002.get());
        }
    }

    /**
     * 更新行政区划状态
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "更新行政区划状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("sysData.area")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object> upState(@PathVariable("id") String id) {
        ProvinceEntity entity = provinceService.getInfo(id);
        if (entity.getEnabledMark() == null || "1".equals(String.valueOf(entity.getEnabledMark()))) {
            entity.setEnabledMark(0);
        } else {
            entity.setEnabledMark(1);
        }
        boolean flag = provinceService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 行政区划id转名称
     *
     * @param model 二维数组
     * @return ignore
     */
    @Operation(summary = "行政区划id转名称")
    @Parameter(name = "model", description = "二维数组", required = true)
    @PostMapping("/GetAreaByIds")
    public ActionResult<Object> getAreaByIds(@RequestBody AreaModel model) {
        // 返回给前端的list
        List<List<String>> list = new LinkedList<>();
        for (List<String> idList : model.getIdsList()) {
            List<ProvinceEntity> proList = provinceService.getProList(idList);
            List<String> collect = proList.stream().map(ProvinceEntity::getFullName).collect(Collectors.toList());
            list.add(collect);
        }
        return ActionResult.success(list);
    }

}
