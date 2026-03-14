package jnpf.onlinedev.util;

import cn.hutool.core.collection.CollUtil;
import jnpf.base.model.ColumnDataModel;
import jnpf.constant.KeyConst;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/7/28
 */
@Slf4j
public class OnlineDevListUtils {
    OnlineDevListUtils() {
    }

    /**
     * 分组页面
     *
     * @param realList
     * @param columnDataModel
     * @return
     */
    public static List<Map<String, Object>> groupData(List<Map<String, Object>> realList, ColumnDataModel columnDataModel) {
        List<Map<String, Object>> columnList = JsonUtil.getJsonToListMap(columnDataModel.getColumnList());
        String firstField;
        String groupField = columnDataModel.getGroupField();
        List<Map<String, Object>> collect = columnList.stream().filter(t -> "left".equals(t.get("fixed"))
                && !String.valueOf(t.get("prop")).equals(columnDataModel.getGroupField())).collect(Collectors.toList());
        Map<String, Object> map = null;
        if (CollUtil.isNotEmpty(collect)) {
            map = collect.stream().filter(t -> !String.valueOf(t.get("prop")).equals(columnDataModel.getGroupField())).findFirst().orElse(null);
        } else {
            map = columnList.stream().filter(t -> !String.valueOf(t.get("prop")).equals(columnDataModel.getGroupField())).findFirst().orElse(null);
        }
        if (map == null) {
            map = columnList.stream().filter(t -> String.valueOf(t.get("prop")).equals(columnDataModel.getGroupField())).findFirst().orElse(null);
        }
        firstField = String.valueOf(map.get("prop"));

        Map<String, List<Map<String, Object>>> twoMap = new LinkedHashMap<>(16);

        for (Map<String, Object> realMap : realList) {
            String value = String.valueOf(realMap.get(groupField));
            if (realMap.get(groupField) instanceof Double) {
                value = realMap.get(groupField).toString().replaceAll(".0+$", "").replaceAll("[.]$", "");
            }
            boolean isKey = twoMap.get(value) != null;
            if (isKey) {
                List<Map<String, Object>> maps = twoMap.get(value);
                maps.add(realMap);
                twoMap.put(value, maps);
            } else {
                List<Map<String, Object>> childrenList = new ArrayList<>();
                childrenList.add(realMap);
                twoMap.put(value, childrenList);
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> keyItem : twoMap.entrySet()) {
            Map<String, Object> thirdMap = new HashMap<>(16);
            thirdMap.put(firstField, !keyItem.getKey().equals("null") ? keyItem.getKey() : "");
            thirdMap.put("top", true);
            thirdMap.put("id", RandomUtil.uuId());
            thirdMap.put(KeyConst.CHILDREN, keyItem.getValue());
            resultList.add(thirdMap);
        }
        return resultList;
    }


    /**
     * 树形列表页面
     *
     * @param realList
     * @param columnDataModel
     * @return
     */
    public static List<Map<String, Object>> treeListData(List<Map<String, Object>> realList, ColumnDataModel columnDataModel) {
        String parentField = columnDataModel.getParentField() + "_id";
        String childField = columnDataModel.getSubField();
        return realList.stream()
                .filter(item -> {
                    if (!shouldProcessItem(item, parentField)) {
                        return true; // 保留该项
                    }
                    // 如果需要处理且addChild返回true，则过滤掉该项
                    return !addChild(item, realList, parentField, childField);
                })
                .collect(Collectors.toList());
    }

    private static boolean shouldProcessItem(Map<String, Object> item, String parentField) {
        Object parentValue = item.get(parentField);
        if (parentValue == null) {
            return false;
        }
        String parentStr = parentValue.toString();
        return StringUtil.isNotEmpty(parentStr) && !"[]".equals(parentStr);
    }

    //递归
    private static boolean addChild(Map<String, Object> node, List<Map<String, Object>> list, String parentField, String childField) {

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> ele = list.get(i);
            if (ele.get(childField).equals(node.get(parentField))) {
                ele.computeIfAbsent(KeyConst.CHILDREN, k -> new ArrayList<>());
                List<Map<String, Object>> children = (List<Map<String, Object>>) ele.get(KeyConst.CHILDREN);
                children.add(node);
                ele.put(KeyConst.CHILDREN, children);
                return true;
            }
            if (ele.get(KeyConst.CHILDREN) != null) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) ele.get(KeyConst.CHILDREN);
                if (addChild(node, children, parentField, childField)) {
                    return true;
                }
            }
        }
        return false;
    }

}
