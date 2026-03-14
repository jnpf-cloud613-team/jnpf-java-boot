package jnpf.permission.constant;

import jnpf.constant.KeyConst;
import jnpf.model.ExcelColumnAttr;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.*;

public class UserColumnMap {

    String excelName = "用户信息";

    private static final Map<String, String> KEY_MAP = new LinkedHashMap<>();

    static {
        KEY_MAP.put(KeyConst.POSITION_ID, "岗位");
        KEY_MAP.put(KeyConst.ROLE_ID, "角色");
        KEY_MAP.put(KeyConst.ACCOUNT, "账户");
        KEY_MAP.put(KeyConst.REAL_NAME, "姓名");
        KEY_MAP.put(KeyConst.GENDER, "性别");
        KEY_MAP.put(KeyConst.ENABLED_MARK, "状态");
        KEY_MAP.put(KeyConst.EMAIL, "电子邮箱");
        KEY_MAP.put(KeyConst.RANKS, "职级");
        KEY_MAP.put(KeyConst.NATION, "民族");
        KEY_MAP.put(KeyConst.NATIVE_PLACE, "籍贯");
        KEY_MAP.put(KeyConst.ENTRY_DATE, "入职时间");
        KEY_MAP.put(KeyConst.CERTIFICATES_TYPE, "证件类型");
        KEY_MAP.put(KeyConst.CERTIFICATES_NUMBER, "证件号码");
        KEY_MAP.put(KeyConst.EDUCATION, "文化程度");
        KEY_MAP.put(KeyConst.BIRTHDAY, "出生年月");
        KEY_MAP.put(KeyConst.TELE_PHONE, "办公电话");
        KEY_MAP.put(KeyConst.LANDLINE, "办公座机");
        KEY_MAP.put(KeyConst.MOBILE_PHONE, "手机号码");
        KEY_MAP.put(KeyConst.URGENT_CONTACTS, "紧急联系");
        KEY_MAP.put(KeyConst.URGENT_TELE_PHONE, "紧急电话");
        KEY_MAP.put(KeyConst.POSTAL_ADDRESS, "通讯地址");
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
        List<String> requireFields = Arrays.asList(KeyConst.ACCOUNT, KeyConst.REAL_NAME, KeyConst.GENDER, KeyConst.ENABLED_MARK);
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
        map.put(KeyConst.POSITION_ID, "岗位名称/岗位编码,岗位名称1/岗位编码1");
        map.put(KeyConst.ROLE_ID, "角色名称,角色名称1");
        map.put(KeyConst.ENTRY_DATE, "yyyy-MM-dd");
        map.put(KeyConst.BIRTHDAY, "yyyy-MM-dd");
        list.add(map);
        return list;
    }
}
