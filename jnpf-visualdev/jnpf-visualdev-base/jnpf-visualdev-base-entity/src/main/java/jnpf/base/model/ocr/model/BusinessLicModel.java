package jnpf.base.model.ocr.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrModel;
import jnpf.util.DateUtil;
import jnpf.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.regex.Pattern;

/**
 * ocr解析营业执照模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析营业执照模型")
@NoArgsConstructor
public class BusinessLicModel extends OcrModel {
    @Schema(description = TYSHXYDM)
    private String code;
    @Schema(description = "企业名称")
    private String name;
    @Schema(description = "企业类型")
    private String businessType;
    @Schema(description = "法定代表人")
    private String userName;
    @Schema(description = "经营范围")
    private String businessScope;
    @Schema(description = "注册资本")
    private String capital;
    @Schema(description = "成立日期")
    private Long createDate;
    @Schema(description = "营业期限")
    private String timeLimit;
    @Schema(description = "地址")
    private String address;

    private static final String TYSHXYDM = "统一社会信用代码";

    @Override
    public void extract(String url, JSONObject body) {
        super.extract(url, body);
        if (StringUtil.isEmpty(this.getContent()) || !this.getContent().contains("营业执照")) {
            return;
        }
        this.setOther();
        validateCode();
        businessScope();
        this.setContent(null);
        this.setStrList(null);
    }

    /**
     * 统一社会信用代码识别补充
     */
    public void validateCode() {
        Pattern pattern = Pattern.compile("^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$");
        if (StringUtil.isEmpty(code) || code.trim().length() != 18 || pattern.matcher(code).matches()) {
            int index = this.getContent().indexOf(TYSHXYDM) + 8;
            String str = this.getContent().substring(index, index + 18);
            // 先进行正则表达式验证
            if (pattern.matcher(str).matches()) {
                this.code = str;
            } else {
                for (String item : this.getStrList()) {
                    if (pattern.matcher(item).matches()) {
                        this.code = item;
                        return;
                    }
                }
            }
        }
    }

    /**
     * 经营范围和住所识别补充
     */
    public void businessScope() {
        int codeIndex = this.getContent().indexOf("经营范围");
        int addressIndex = this.getContent().indexOf("住所");
        if (codeIndex > 0 && addressIndex > 0 && codeIndex < addressIndex) {
            int start = 0;
            int end = 0;
            int size = this.getStrList().size();
            int addressStart = 0;
            int addressEnd = 0;
            for (int i = 0; i < size; i++) {
                if (this.getStrList().get(i).startsWith("经营范围")) {
                    start = i;
                }
                if (this.getStrList().get(i).startsWith("住")) {
                    addressStart = i;
                }
                if (this.getStrList().get(i).startsWith("所")) {
                    addressEnd = i;
                }
                if (this.getStrList().get(i).startsWith("登记机关")) {
                    end = i;
                    break;
                }
            }
            if (addressStart > 0 && start > 0 && end > start && addressEnd > addressStart) {
                List<String> addList = this.getStrList().subList(addressStart, addressEnd + 1);
                String addressAll = String.join("", addList);
                if (addressAll.length() < 3) return;
                this.address = addressAll.substring(2);
                List<String> scopeList = this.getStrList().subList(start, end);
                String scopeAll = String.join("", scopeList);
                this.businessScope = scopeAll.replace(addressAll, "").trim();
            }
        }
    }

    /**
     * 根据关键词切割补全内容
     */
    public void setOther() {
        List<String> properties = Arrays.asList(TYSHXYDM, "名称", "类型", "法定代表人", "经营范围", "注册资本", "成立日期", "营业期限", "住所", "登记机关");
        Map<String, String> result = extractProperties(this.getContent(), properties);
        for (Map.Entry<String, String> entry : result.entrySet()) {
            switch (entry.getKey()) {
                case TYSHXYDM:
                    this.code = entry.getValue();
                    break;
                case "名称":
                    this.name = entry.getValue();
                    break;
                case "类型":
                    this.businessType = entry.getValue();
                    break;
                case "法定代表人":
                    this.userName = entry.getValue();
                    break;
                case "经营范围":
                    this.businessScope = entry.getValue();
                    break;
                case "注册资本":
                    this.capital = entry.getValue();
                    break;
                case "成立日期":
                    this.createDate = DateUtil.checkDate(entry.getValue(), "yyyy年MM月dd日").getTime();
                    break;
                case "营业期限":
                    this.timeLimit = entry.getValue();
                    break;
                case "住所":
                    this.address = entry.getValue();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 整体切割
     *
     * @param input
     * @param propertyNames
     * @return
     */
    public static Map<String, String> extractProperties(String input, List<String> propertyNames) {
        // 存储属性和它们的起始位置
        Map<String, Integer> propertyPositions = new HashMap<>();
        Map<String, String> result = new LinkedHashMap<>();

        // 查找每个属性的位置
        for (String property : propertyNames) {
            int index = input.indexOf(property);
            if (index != -1) {
                propertyPositions.put(property, index);
            }
        }

        // 如果没有找到任何属性，返回空map
        if (propertyPositions.isEmpty()) {
            return result;
        }

        // 按位置排序属性
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(propertyPositions.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue());

        // 提取每个属性的内容
        for (int i = 0; i < sortedEntries.size(); i++) {
            String currentProperty = sortedEntries.get(i).getKey();
            int startPos = sortedEntries.get(i).getValue() + currentProperty.length();

            // 确定结束位置（下一个属性的开始位置或字符串末尾）
            int endPos = (i < sortedEntries.size() - 1)
                    ? sortedEntries.get(i + 1).getValue()
                    : input.length();

            // 提取内容并去除可能的冒号、空格等
            String content = input.substring(startPos, endPos).trim();
            content = content.replaceAll("^[:：\\s]+", "").trim();

            result.put(currentProperty, content);
        }

        return result;
    }
}
