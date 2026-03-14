package jnpf.onlinedev.util;

import jnpf.onlinedev.model.online.VisualColumnSearchVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 关联表单工具类
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/1/7
 */
public class RelationFormUtils {
    RelationFormUtils() {
    }

    /**
     * 转后后查询
     *
     * @param dataVOList
     * @param searchVOList
     * @return
     */
    public static List<Map<String, Object>> getRelationListByKeyword(List<Map<String, Object>> dataVOList, List<VisualColumnSearchVO> searchVOList) {
        List<Map<String, Object>> passDataList = new ArrayList<>();
        for (Map<String, Object> dataMap : dataVOList) {
            int i = 0;
            for (VisualColumnSearchVO searchVO : searchVOList) {
                String s = String.valueOf(dataMap.get(searchVO.getVModel()));
                if (s.contains(String.valueOf(searchVO.getValue()))) {
                    i++;
                }
            }
            if (i > 0) {
                passDataList.add(dataMap);
            }
        }
        return passDataList;
    }
}
