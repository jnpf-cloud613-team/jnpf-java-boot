package jnpf.permission.constant;

import jnpf.constant.KeyConst;
import jnpf.model.ExcelColumnAttr;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.*;

public class OrgColumnMap {

    String excelName = "组织信息";
    /**
     * 全部字段
     */
    private static final Map<String, String> ALL_KEY_MAP = new LinkedHashMap<>();
    /**
     * 组织map
     */
    private static final Map<String, String> ORG_MAP = new LinkedHashMap<>();
    /**
     * 部门map
     */
    private static final Map<String, String> DEP_MAP = new LinkedHashMap<>();

    static {
        ALL_KEY_MAP.put(KeyConst.PARENT_ID, "上级组织");
        ALL_KEY_MAP.put(KeyConst.CATEGORY, "组织类型");
        ALL_KEY_MAP.put(KeyConst.FULL_NAME, "组织名称");
        ALL_KEY_MAP.put(KeyConst.EN_CODE, "组织编码");
        ALL_KEY_MAP.put(KeyConst.SORT_CODE, "排序");
        ALL_KEY_MAP.put(KeyConst.DESCRIPTION, "说明");

        ORG_MAP.put(KeyConst.CATEGORY, "类型");
        ORG_MAP.put(KeyConst.PARENT_ID, "上级公司");
        ORG_MAP.put(KeyConst.FULL_NAME, "公司名称");
        ORG_MAP.put(KeyConst.EN_CODE, "公司编码");
        ORG_MAP.put(KeyConst.SHORT_NAME, "公司简称");
        ORG_MAP.put(KeyConst.ENTERPRISE_NATURE, "公司性质");
        ORG_MAP.put(KeyConst.INDUSTRY, "所属行业");
        ORG_MAP.put(KeyConst.FOUNDED_TIME, "成立时间");
        ORG_MAP.put(KeyConst.TELE_PHONE, "公司电话");
        ORG_MAP.put(KeyConst.FAX, "公司传真");
        ORG_MAP.put(KeyConst.WEB_SITE, "公司主页");
        ORG_MAP.put(KeyConst.ADDRESS, "公司地址");
        ORG_MAP.put(KeyConst.MANAGER_NAME, "公司法人");
        ORG_MAP.put(KeyConst.MANAGER_TELE_PHONE, "联系电话");
        ORG_MAP.put(KeyConst.MANAGER_MOBILE_PHONE, "联系手机");
        ORG_MAP.put(KeyConst.MANAGE_EMAIL, "联系邮箱");
        ORG_MAP.put(KeyConst.BANK_NAME, "开户银行");
        ORG_MAP.put(KeyConst.BANK_ACCOUNT, "银行账户");
        ORG_MAP.put(KeyConst.BUSINESSSCOPE, "经营范围");
        ORG_MAP.put(KeyConst.MANAGER_ID, "部门主管");

        DEP_MAP.put(KeyConst.CATEGORY, "类型");
        DEP_MAP.put(KeyConst.PARENT_ID, "所属组织");
        DEP_MAP.put(KeyConst.FULL_NAME, "部门名称");
        DEP_MAP.put(KeyConst.EN_CODE, "部门编码");
        DEP_MAP.put(KeyConst.MANAGER_ID, "部门主管");
        DEP_MAP.put(KeyConst.SORT_CODE, "排序");
        DEP_MAP.put(KeyConst.DESCRIPTION, "说明");
    }

    /**
     * 根据类型获取excel表头字段
     *
     * @param type
     * @return
     */
    public Map<String, String> getColumnByType(Integer type) {
        Map<String, String> map = new LinkedHashMap<>();
        switch (type) {
            case 2:
                map = new LinkedHashMap<>(DEP_MAP);
                break;
            case 1:
                map = new LinkedHashMap<>(ORG_MAP);
                map.remove(KeyConst.MANAGER_ID);
                break;
            default:
                map.putAll(ALL_KEY_MAP);
                break;
        }
        return map;
    }

    public String getExcelName() {
        return excelName;
    }


    public List<ExcelColumnAttr> getFieldsModel(boolean isError, Integer type) {
        List<ExcelColumnAttr> models = new ArrayList<>();
        //异常原因
        if (isError) {
            ExcelColumnAttr attr = ExcelColumnAttr.builder()
                    .key("errorsInfo")
                    .name("异常原因")
                    .build();
            models.add(attr);
        }
        List<String> requirelist = Arrays.asList(KeyConst.CATEGORY, KeyConst.FULL_NAME);
        // 遍历添加属性
        Map<String, String> keyMap = getColumnByType(type);

        for (Map.Entry<String, String> keyItem : keyMap.entrySet()) {
            String key = keyItem.getKey();
            ExcelColumnAttr attr = ExcelColumnAttr.builder()
                    .key(key)
                    .name(keyItem.getValue())
                    .build();
            if (requirelist.contains(key)) {
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
        Map<String, Object> depMapDemo = new HashMap<>();
        depMapDemo.put(KeyConst.FULL_NAME, "组织名称");
        depMapDemo.put(KeyConst.PARENT_ID, "上级组织名称/编码（为空时则该组织添加为一级组织）");
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(depMapDemo);
        return list;
    }
}
