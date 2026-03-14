package jnpf.permission.constant;

import jnpf.constant.KeyConst;
import jnpf.model.ExcelColumnAttr;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.*;

public class PosColumnMap {

    String excelName = "岗位信息";
    //岗位约束类型
    private static final Map<Integer, String> CONSTRAINT_TYPE_MAP = new LinkedHashMap<>();
    private static final Map<String, String> KEY_MAP = new LinkedHashMap<>();

    static {
        CONSTRAINT_TYPE_MAP.put(0, "互斥约束");
        CONSTRAINT_TYPE_MAP.put(1, "基数约束");
        CONSTRAINT_TYPE_MAP.put(2, "先决约束");

        KEY_MAP.put(KeyConst.ORGANIZE_ID, "所属组织");
        KEY_MAP.put(KeyConst.PARENT_ID, "上级岗位");
        KEY_MAP.put(KeyConst.FULL_NAME, "岗位名称");
        KEY_MAP.put(KeyConst.EN_CODE, "岗位编码");
        KEY_MAP.put(KeyConst.IS_CONDITION, "岗位约束");
        KEY_MAP.put(KeyConst.CERTIFICATES_TYPE, "约束类型");
        KEY_MAP.put(KeyConst.MUTUAL_EXCLUSION, "互斥岗位");
        KEY_MAP.put(KeyConst.USER_NUM, "用户基数");
        KEY_MAP.put(KeyConst.PERMISSION_NUM, "权限基数");
        KEY_MAP.put(KeyConst.PREREQUISITE, "先决岗位");
        KEY_MAP.put(KeyConst.SORT_CODE, "排序");
        KEY_MAP.put(KeyConst.DESCRIPTION, "说明");
    }

    public static final Map<Integer, String> CONSTRAINT_TYPE = Collections.unmodifiableMap(CONSTRAINT_TYPE_MAP);

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
        List<String> requireFields = Arrays.asList(KeyConst.ORGANIZE_ID, KeyConst.FULL_NAME);
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
        //所属组织
        map.put(KeyConst.ORGANIZE_ID, "组织名称/编码");
        map.put(KeyConst.PARENT_ID, "岗位名称/编码");
        //约束类型
        Map<Integer, String> constraintTypeMap = PosColumnMap.CONSTRAINT_TYPE;
        String constraintType = String.join(",", constraintTypeMap.values());
        map.put(KeyConst.CONSTRAINT_TYPE, constraintType + "：多选组合");
        map.put(KeyConst.MUTUAL_EXCLUSION, "岗位名称1/编码1,岗位名称2/编码2");
        map.put(KeyConst.PREREQUISITE, "岗位名称1/编码1,岗位名称2/编码2");
        list.add(map);
        return list;
    }

}
