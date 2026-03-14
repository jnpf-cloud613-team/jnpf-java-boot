package jnpf.base.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.base.Joiner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.ProvinceAtlasEntity;
import jnpf.base.model.province.AtlasJsonModel;
import jnpf.base.model.province.ProvinceListTreeVO;
import jnpf.base.service.ProvinceAtlasService;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.wxutil.HttpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 行政区划-地图
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2023/1/29 10:41:25
 */
@Tag(name = "行政区划-地图", description = "atlas")
@RestController
@RequestMapping("/api/system/atlas")
@RequiredArgsConstructor
public class ProvinceAtlasController extends SuperController<ProvinceAtlasService, ProvinceAtlasEntity> {


    private final ProvinceAtlasService provinceAtlasService;
    public static final String ATLAS_URL = "https://geo.datav.aliyun.com/areas_v3/bound/geojson?code=";

    //树形递归
    private static boolean addChild(ProvinceListTreeVO node, List<ProvinceListTreeVO> list) {
        for (int i = 0; i < list.size(); i++) {
            ProvinceListTreeVO n = list.get(i);
            if (n.getId().equals(node.getParentId())) {
                if (n.getChildren() == null) {
                    n.setChildren(new ArrayList<>());
                }
                List<ProvinceListTreeVO> children = n.getChildren();
                children.add(node);
                n.setChildren(children);
                return true;
            }
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                List<ProvinceListTreeVO> children = n.getChildren();
                if (addChild(node, children)) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * 获取所有列表
     *
     * @return ignore
     */
    @Operation(summary = "获取所有列表")
    @GetMapping
    public ActionResult<List<ProvinceListTreeVO>> list() {
        List<ProvinceAtlasEntity> list = provinceAtlasService.getList();
        List<ProvinceListTreeVO> listVOS = JsonUtil.getJsonToList(list, ProvinceListTreeVO.class);
        listVOS.forEach(item -> {
            if( StringUtil.isNotEmpty(item.getAtlasCenter())){
                String[] split = item.getAtlasCenter().split(",");
                item.setCenterLong(new BigDecimal(split[0]));
                item.setCenterLat(new BigDecimal(split[1]));
            }
        });
        for (int i = 0; i < listVOS.size(); i++) {
            ProvinceListTreeVO item = listVOS.get(i);
            if (!StringUtil.isEmpty(item.getParentId()) && addChild(item, listVOS) && !listVOS.isEmpty()) {
                    listVOS.remove(item);
                    i--;
                }

        }
        return ActionResult.success(listVOS);
    }

    /**
     * 获取列表
     *
     * @param pid 主键
     * @return
     */
    @Operation(summary = "获取列表")
    @Parameter(name = "pid", description = "主键", required = true)
    @GetMapping("/list/{pid}")
    public ActionResult<List<ProvinceListTreeVO>> getListByPid(@PathVariable("pid") String pid) {
        List<ProvinceAtlasEntity> list = provinceAtlasService.getListByPid(pid);
        List<ProvinceListTreeVO> listVOS = JsonUtil.getJsonToList(list, ProvinceListTreeVO.class);
        return ActionResult.success(listVOS);
    }

    /**
     * 获取地图json
     *
     * @param code 编码
     * @return
     */
    @Operation(summary = "获取地图json")
    @Parameter(name = "code", description = "编码", required = true)
    @GetMapping("/geojson")
    public ActionResult<JSONObject> geojson(@RequestParam("code") String code) {
        ProvinceAtlasEntity oneByCode = provinceAtlasService.findOneByCode(code);
        if(oneByCode == null){
            return ActionResult.fail(MsgCode.SYS022.get());
        }
        List<ProvinceAtlasEntity> listByPid = provinceAtlasService.getListByPid(oneByCode.getId());
        String url = ATLAS_URL + code;
        if (CollectionUtils.isNotEmpty(listByPid)) {
            url += "_full";
        }
        JSONObject rstObj;
        try {
            rstObj = HttpUtil.httpRequest(url, "GET", null);
        } catch (Exception e) {
            return ActionResult.fail(MsgCode.SYS023.get());
        }
        if (rstObj == null) {
            return ActionResult.fail(MsgCode.SYS024.get());
        }
        return ActionResult.success(rstObj);
    }

    /**
     * 同步行政区划信息
     *
     * @return
     */
    @Operation(summary = "同步行政区划信息")
    @GetMapping("/crePy")
    public ActionResult<Object> crePy() {
        List<ProvinceAtlasEntity> list = provinceAtlasService.list();
        for (ProvinceAtlasEntity p : list) {

            String url = ATLAS_URL + p.getId();
            JSONObject rstObj;
            try {
                rstObj = HttpUtil.httpRequest(url, "GET", null);
                if (rstObj == null) {
                    provinceAtlasService.removeById(p);
                } else {
                    //将获取到的信息写入表
                    AtlasJsonModel jsonToBean = JsonUtil.getJsonToBean(rstObj, AtlasJsonModel.class);
                    List<BigDecimal> center = jsonToBean.getFeatures().get(0).getProperties().getCenter();
                    p.setAtlasCenter(Joiner.on(",").join(center));
                    provinceAtlasService.updateById(p);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return ActionResult.fail(MsgCode.SYS023.get());
            }
        }
        return ActionResult.success();
    }

}
