package jnpf.permission.constant;

import jnpf.constant.KeyConst;
import jnpf.model.ExcelColumnAttr;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.*;

public class RoleColumnMap {

    String excelName = "角色信息";

    private static final Map<String, String> KEY_MAP = new LinkedHashMap<>();

    static {
        KEY_MAP.put(KeyConst.FULL_NAME, "角色名称");
        KEY_MAP.put(KeyConst.EN_CODE, "角色编码");
        KEY_MAP.put(KeyConst.GLOBAL_MARK, "角色类型");
        KEY_MAP.put(KeyConst.ORGANIZE_ID, "所属组织");
        KEY_MAP.put(KeyConst.ENABLED_MARK, "状态");
        KEY_MAP.put(KeyConst.SORT_CODE, "排序");
        KEY_MAP.put(KeyConst.DESCRIPTION, "说明");
    }

    /**
     * 表格名称
     *
     * @return
     */
    public String getExcelName() {
        return excelName;
    }

    /**
     * 根据类型获取excel表头字段
     *
     * @param type
     * @return
     */
    public Map<String, String> getColumnByType(Integer type) {
        return type == null || type == 0 ? KEY_MAP : new LinkedHashMap<>();
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
        List<String> requireFields = Arrays.asList(KeyConst.FULL_NAME, KeyConst.EN_CODE, KeyConst.GLOBAL_MARK, KeyConst.ENABLED_MARK);
        for (Map.Entry<String, String> keyItem : KEY_MAP.entrySet()) {
            String key = keyItem.getKey();
            ExcelColumnAttr attr = ExcelColumnAttr.builder().key(key).name(keyItem.getValue()).build();
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
        map.put(KeyConst.ORGANIZE_ID, "公司名称/公司名称1/部门名称");
        list.add(map);
        return list;
    }

}
