package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.model.cachemanage.CacheManageInfoVO;
import jnpf.base.model.cachemanage.CacheManageListVO;
import jnpf.base.model.cachemanage.PaginationCacheManage;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存管理
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Slf4j
@Tag(name = "缓存管理", description = "CacheManage")
@RestController
@RequestMapping("/api/system/CacheManage")
@RequiredArgsConstructor
public class CacheManageController {


    private final RedisUtil redisUtil;


    /**
     * 获取缓存列表
     *
     * @param page 分页参数
     * @return ignore
     */
    @Operation(summary = "获取缓存列表")
    @SaCheckPermission("sysService.cache")
    @GetMapping
    public ActionResult<PageListVO<CacheManageListVO>> getList(PaginationCacheManage page) {
        String tenantId = UserProvider.getUser().getTenantId();
        List<CacheManageListVO> list = new ArrayList<>();
        String findKey = StringUtil.isEmpty(tenantId) ? "*" : "*" + tenantId + "*";
        Set<String> data = redisUtil.findKeysContaining(findKey);
        for (String key : data) {
            try {
                if (!StringUtil.isEmpty(tenantId) && key.contains(tenantId)) {
                    CacheManageListVO model = new CacheManageListVO();
                    model.setName(key);
                    model.setCacheSize(String.valueOf(redisUtil.getString(key)).getBytes().length);
                    model.setOverdueTime(new Date((DateUtil.getTime(new Date()) + redisUtil.getLiveTime(key)) * 1000).getTime());
                    list.add(model);
                } else if (StringUtil.isEmpty(tenantId)) {
                    CacheManageListVO model = new CacheManageListVO();
                    model.setName(key);
                    model.setCacheSize(String.valueOf(redisUtil.getString(key)).getBytes().length);
                    model.setOverdueTime(new Date((DateUtil.getTime(new Date()) + redisUtil.getLiveTime(key)) * 1000).getTime());
                    list.add(model);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        list = list.stream().sorted(Comparator.comparing(CacheManageListVO::getOverdueTime)).collect(Collectors.toList());
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            list = list.stream().filter(t -> t.getName().contains(page.getKeyword())).collect(Collectors.toList());
        }
        if (ObjectUtil.isNotNull(page.getOverdueStartTime()) && ObjectUtil.isNotNull(page.getOverdueEndTime())) {
            list = list.stream().filter(t -> t.getOverdueTime() >= page.getOverdueStartTime() && t.getOverdueTime() <= page.getOverdueEndTime()).collect(Collectors.toList());
        }

        PaginationVO paginationVO = JsonUtil.getJsonToBean(page, PaginationVO.class);
        paginationVO.setTotal(list.size());
        //假分页
        if (CollectionUtils.isNotEmpty(list)) {
            List<List<CacheManageListVO>> partition = Lists.partition(list, (int) page.getPageSize());
            int i = (int) page.getCurrentPage() - 1;
            list = partition.size() > i ? partition.get(i) : Collections.emptyList();
        }
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 获取缓存信息
     *
     * @param name 主键值
     * @return ignore
     */
    @Operation(summary = "获取缓存信息")
    @Parameter(name = "name", description = "主键值", required = true)
    @SaCheckPermission("sysService.cache")
    @GetMapping("/{name}")
    public ActionResult<CacheManageInfoVO> info(@PathVariable("name") String name) {
        name = XSSEscape.escape(name);
        String json = String.valueOf(redisUtil.getString(name));
        CacheManageInfoVO vo = new CacheManageInfoVO();
        vo.setName(name);
        vo.setValue(json);
        return ActionResult.success(vo);
    }

    /**
     * 清空所有缓存
     *
     * @return ignore
     */
    @Operation(summary = "清空所有缓存")
    @SaCheckPermission("sysService.cache")
    @PostMapping("/Actions/ClearAll")
    public ActionResult<Object> clearAll() {
        String tenantId = UserProvider.getUser().getTenantId();
        String findKey = StringUtil.isEmpty(tenantId) ? "*" : "*" + tenantId + "*";
        Set<String> data = redisUtil.findKeysContaining(findKey);
        redisUtil.remove(data);
        return ActionResult.success(MsgCode.SYS004.get());
    }

    /**
     * 清空单个缓存
     *
     * @param name 主键值
     * @return ignore
     */
    @Operation(summary = "清空单个缓存")
    @Parameter(name = "name", description = "主键值", required = true)
    @SaCheckPermission("sysService.cache")
    @DeleteMapping("/{name}")
    public ActionResult<Object> clear(@PathVariable("name") String name) {
        redisUtil.remove(name);
        return ActionResult.success(MsgCode.SYS004.get());
    }
}
