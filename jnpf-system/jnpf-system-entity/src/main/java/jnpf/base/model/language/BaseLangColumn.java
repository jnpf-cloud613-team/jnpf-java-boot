package jnpf.base.model.language;

import jnpf.model.ExcelColumnAttr;
import lombok.Getter;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.*;

/**
 * 多语言导入模型
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/6/25 15:31:23
 */
public class BaseLangColumn {

    /**
     * -- GETTER --
     *  表格名称
     *
     * @return
     */
    @Getter
    String excelName = "翻译管理";

    private static final Map<String, String> KEY_MAP = new HashMap<>();

    static {
        KEY_MAP.put("enCode", "翻译标记");
        KEY_MAP.put("type", "翻译分类");
    }

    public BaseLangColumn(Map<String, String> keyMap) {
        KEY_MAP.putAll(keyMap);
    }

    /**
     * 根据类型获取excel表头字段
     *

     * @return
     */
    public Map<String, String> getColumnByType() {
        return KEY_MAP;
    }

    /**
     * 获取字段列表
     *
     * @param isError
     * @return
     */
    public List<ExcelColumnAttr> getFieldsModel(boolean isError) {
        List<ExcelColumnAttr> models = new ArrayList<>();
        //异常原因
        if (isError) {
            ExcelColumnAttr attr = ExcelColumnAttr.builder().key("errorsInfo").name("异常原因").build();
            models.add(attr);
        }
        List<String> requireFields = Collections.singletonList("enCode");
        for (Map.Entry<String, String> entry : KEY_MAP.entrySet()) {
            String key = entry.getKey();
            ExcelColumnAttr attr = ExcelColumnAttr.builder().key(key).name(KEY_MAP.get(key)).build();
            if (requireFields.contains(key)) {
                attr.setRequire(true);
                attr.setFontColor(IndexedColors.RED.getIndex());
            }
            models.add(attr);
        }
        return models;
    }

    /**
     * 获取默认值
     */
    public List<Map<String, Object>> getDefaultList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();

        list.add(map);
        return list;
    }
}
