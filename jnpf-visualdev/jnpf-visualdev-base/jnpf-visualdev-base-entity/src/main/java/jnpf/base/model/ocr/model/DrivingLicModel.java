package jnpf.base.model.ocr.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrModel;
import jnpf.util.DateUtil;
import jnpf.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ocr解析驾驶证模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析驾驶证模型")
@NoArgsConstructor
public class DrivingLicModel extends OcrModel {
    @Schema(description = "证号")
    private String idNum;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "性别")
    private String sex;
    @Schema(description = "国籍")
    private String nationality;
    @Schema(description = "住址")
    private String address;
    @Schema(description = "出生日期")
    private Long birthDate;
    @Schema(description = CCLZRQ)
    private Long issueDate;
    @Schema(description = "准驾车型")
    private String classType;
    @Schema(description = "有效期限")
    private String validPeriod;
    @Schema(description = "证件有效期起始日期")
    private Long startDate;
    @Schema(description = "证件有效期结束日期")
    private Long endDate;

    private static final String CCLZRQ = "初次领证日期";
    private static final String DATE_DD = "yyyy-MM-dd";

    @Override
    public void extract(String url, JSONObject body) {
        super.extract(url, body);
        if (StringUtil.isEmpty(this.getContent()) || !this.getContent().contains("驾驶证")) {
            return;
        }
        this.setInfo();
        this.setContent(null);
        this.setStrList(null);
    }

    private void setInfo() {
        // 使用正则表达式提取关键信息
        String ocrText = this.getContent();
        this.cardNumber(ocrText);
        this.setThisName();
        this.sex();
        this.validPeriod();
        //for循环取单行数据，避免整体取到其他行合并信息
        List<String> strList = this.getStrList();
        for (int i = 0; i < strList.size(); i++) {
            if (StringUtil.isEmpty(this.nationality) && strList.get(i).contains("国籍")) {
                this.nationality = strList.get(i).endsWith("国籍") ? this.getStrList().get(i + 1) :
                        strList.get(i).substring(strList.get(i).indexOf("国籍") + 2);
            }
            if (StringUtil.isEmpty(this.address) && strList.get(i).contains("住址")) {
                this.address = strList.get(i).endsWith("住址") ? this.getStrList().get(i + 1) :
                        strList.get(i).substring(strList.get(i).indexOf("住址") + 2);
            }
            if (StringUtil.isEmpty(this.classType) && strList.get(i).contains("准驾车型")) {
                this.classType = this.getStrList().get(i + 1);
            }

            if (this.birthDate == null && strList.get(i).contains("出生日期")) {
                String s = strList.get(i).endsWith("出生日期") ? this.getStrList().get(i + 1) :
                        strList.get(i).substring(strList.get(i).indexOf("出生日期") + 6);
                if (StringUtil.isNotEmpty(s)) {
                    Date date = DateUtil.checkDate(s, DATE_DD);
                    this.birthDate = date != null ? date.getTime() : null;
                }
            }
            if (this.issueDate == null && strList.get(i).contains(CCLZRQ)) {
                String s = strList.get(i).endsWith(CCLZRQ) ? this.getStrList().get(i + 1) :
                        strList.get(i).substring(strList.get(i).indexOf(CCLZRQ) + 6);
                if (StringUtil.isNotEmpty(s)) {
                    Date date = DateUtil.checkDate(s, DATE_DD);
                    this.issueDate = date != null ? date.getTime() : null;
                }
            }
        }

        //补充
        this.birthDate();
        this.address();
        this.nationality();
    }

    private List<String> getKeyword() {
        return new ArrayList<>(Arrays.asList("中华人民", "Driving", "证号", "姓名", "性别", "国籍", "Name", "Na", "住址", "Add", "Address",
                "Address", "初次", "领证日期", "准驾车型", "有效期限", "Valid", "Period", "出生日期", "of", "Birth", "Data", "First", "Issue", "Class"));
    }

    private boolean containKeyword(String str) {
        for (String s : getKeyword()) {
            if (str.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取身份证号
     */
    private void cardNumber(String str) {
        // 18位身份证正则表达式
        Pattern r = Pattern.compile("[1-9]\\d{13,16}[a-zA-Z0-9]");
        Matcher m = r.matcher(str);
        if (m.find()) {
            this.idNum = m.group();
        }
    }

    private void sex() {
        // 取倒身份证倒数第二位的数字的奇偶性判断性别，二代身份证18位
        if (StringUtil.isNotEmpty(idNum)) {
            String substring = idNum.substring(idNum.length() - 2, idNum.length() - 1);
            int parseInt = Integer.parseInt(substring);
            if (parseInt % 2 == 0) {
                sex = "女";
            } else {
                sex = "男";
            }
        }
    }

    private void setThisName() {
        List<String> nameList = new ArrayList<>();
        int i = this.getStrList().indexOf("住址");
        if (i < 3) {
            i = 6;
        }
        for (String str : this.getStrList().subList(0, i)) {
            if (StringUtil.isNotEmpty(str)) {
                boolean flag = false;
                for (String n : this.getKeyword()) {
                    if (str.contains(n)) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    nameList.add(str);
                }
            }
        }
        if (!nameList.isEmpty()) {
            for (String str : nameList) {
                String pattern = "[一-龥]{1,4}";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(str);
                if (m.matches()) {
                    this.name = str;
                    return;
                }
            }
        }
    }

    private void birthDate() {
        if (birthDate == null && StringUtil.isNotEmpty(idNum)) {
            String dateStr = idNum.substring(6, 14);
            String year = dateStr.substring(0, 4);
            String month = dateStr.substring(4, 6);
            String day = dateStr.substring(6, 8);
            String bd = year + "-" + month + "-" + day;
            birthDate = DateUtil.stringToDates(bd).getTime();
        }
    }

    private void validPeriod() {
        for (String s : this.getStrList()) {
            if (s.contains("-") && s.contains("至") && s.length() > 14) {
                this.validPeriod = s;
            }
        }
        if (StringUtil.isNotEmpty(validPeriod)) {
            // 拆分字符串
            String[] dates = validPeriod.replace(" ", "").split("至");
            // 解析开始日期
            Date sdate = DateUtil.checkDate(dates[0], DATE_DD);
            startDate = sdate != null ? sdate.getTime() : null;
            // 解析结束日期
            Date edate = DateUtil.checkDate(dates[1], DATE_DD);
            endDate = edate != null ? edate.getTime() : null;
        }
    }

    private void address() {
        if (StringUtil.isEmpty(address) || containKeyword(address)) {
            for (String s : this.getStrList()) {
                if ((s.contains("省") || s.contains("市") || s.contains("区")) && s.length() > 6) {
                    if (s.startsWith("住址")) s = s.substring(2);
                    this.address = s;
                    return;
                }
            }
        }
    }

    private void nationality() {
        if (StringUtil.isEmpty(nationality) || containKeyword(nationality)) {
            for (String s : this.getStrList()) {
                if (s.contains("CHN") || s.contains("中国")) {
                    this.nationality = s;
                    return;
                }
            }
        }
    }
}
