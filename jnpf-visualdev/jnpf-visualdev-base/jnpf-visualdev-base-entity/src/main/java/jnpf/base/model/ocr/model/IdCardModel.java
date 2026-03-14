package jnpf.base.model.ocr.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrConstant;
import jnpf.base.model.ocr.OcrModel;
import jnpf.util.DateUtil;
import jnpf.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ocr解析身份证模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析身份证模型")
@NoArgsConstructor
@Slf4j
public class IdCardModel extends OcrModel {
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "性别")
    private String sex;
    @Schema(description = "民族")
    private String nation;
    @Schema(description = "出生日期")
    private Long birth;
    @Schema(description = "地址")
    private String address;
    @Schema(description = "身份证号")
    private String idNum;
    //国徽面
    @Schema(description = "发证机关")
    private String authority;
    @Schema(description = "证件有效期")
    private String validDate;
    @Schema(description = "证件有效期起始日期")
    private Long startDate;
    @Schema(description = "证件有效期结束日期")
    private Long endDate;

    @Override
    public void extract(String url, JSONObject body) {
        super.extract(url, body);
        if (Objects.equals(this.getType(), OcrConstant.IDCARD_FRONT)) {
            for (String str : this.getStrList()) {
                predictName(str);
                national(str);
                cardNumber(str);
            }
            address();
            if (StringUtil.isNotEmpty(idNum)) {
                sex();
                birthday();
            }
            //以下是补充识别
            fullName();
            national2();
        } else {
            for (String str : this.getStrList()) {
                qianFaJiGuan(str);
                youXiaoQiXian(str);
            }
            if (StringUtil.isNotEmpty(validDate)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
                try {
                    // 拆分字符串
                    String[] dates = validDate.split("-");
                    startDate = sdf.parse(dates[0]) != null ? sdf.parse(dates[0]).getTime() : null;
                    // 解析结束日期
                    endDate = sdf.parse(dates[1]) != null ? sdf.parse(dates[1]).getTime() : null;
                } catch (ParseException e) {
                    log.error("日期解析失败: " + e.getMessage());
                }
            }
        }
        this.setContent(null);
        this.setStrList(null);
    }

    /**
     * 获取身份证姓名
     */
    private void predictName(String str) {
        if (str.contains("姓名") || str.contains("名")) {
            String pattern = ".*名[一-龥]{1,4}";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(str);
            if (m.matches()) {
                name = str.substring(str.indexOf("名") + 1);
            }
        }
    }

    /**
     * 为了防止第一次得到的名字为空，以后是遇到什么情况就解决什么情况就行了
     */
    private void fullName() {
        if (StringUtil.isEmpty(name)) {
            String result = this.getContent();
            if (result.contains("性") || result.contains("性别")) {
                String str = result.substring(0, result.lastIndexOf("性"));
                String pattern = ".*名[一-龥]{1,4}";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(str);
                if (m.matches()) {
                    name = str.substring(str.indexOf("名") + 1);
                }
            }
        }
    }

    /**
     * 获取民族
     */
    private void national(String str) {
        String pattern = ".*民族[一-龥]{1,4}";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        if (m.matches()) {
            nation = str.substring(str.indexOf("族") + 1);
        }
    }

    private void national2() {
        if (StringUtil.isEmpty(nation) && this.getContent().contains("出生")) {
            String front = this.getContent().split("出生")[0];
            nation = front.substring(front.indexOf("民") + 2);
        }
    }

    /**
     * 获取身份证地址
     */
    private void address() {
        StringBuilder addressJoin = new StringBuilder();
        for (String str : this.getStrList()) {
            if (str.contains("住址") || str.contains("址") || str.contains("省") || str.contains("市")
                    || str.contains("县") || str.contains("街") || str.contains("乡") || str.contains("村")
                    || str.contains("镇") || str.contains("区") || str.contains("城") || str.contains("组")
                    || str.contains("号") || str.contains("幢") || str.contains("室")
            ) {
                addressJoin.append(str);
            }
        }
        String s = addressJoin.toString();
        if (s.contains("省") || s.contains("县") || s.contains("住址") || s.contains("址") || s.contains("公民身份")) {
            address = s.substring(s.indexOf("址") + 1, s.indexOf("公民身份"));
        } else {
            address = s;
        }
    }

    /**
     * 获取身份证号
     */
    private void cardNumber(String str) {
        // 18位身份证正则表达式
        Pattern r = Pattern.compile("[1-9]\\d{13,16}[a-zA-Z0-9]");
        Matcher m = r.matcher(str);
        if (m.find()) {
            idNum = m.group();
        }
    }

    /**
     * 二代身份证18位
     */
    private void sex() {
        // 取倒身份证倒数第二位的数字的奇偶性判断性别，二代身份证18位
        String substring = idNum.substring(idNum.length() - 2, idNum.length() - 1);
        int parseInt = Integer.parseInt(substring);
        if (parseInt % 2 == 0) {
            sex = "女";
        } else {
            sex = "男";
        }
    }

    /**
     * 从身份证中获取出生信息
     */
    private void birthday() {
        String dateStr = idNum.substring(6, 14);
        String year = dateStr.substring(0, 4);
        String month = dateStr.substring(4, 6);
        String day = dateStr.substring(6, 8);
        String bd = year + "-" + month + "-" + day;
        birth = DateUtil.stringToDates(bd).getTime();
    }

    /**
     * 获取身份证反面信息的签发机关
     */
    private void qianFaJiGuan(String str) {
        if (str.contains("公安局")) {
            if (str.contains("签发机关")) {
                str = str.replace("签发机关", "");
            }
            String pattern = ".*局";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(str);
            if (m.matches()) {
                authority = str;
            }
        }
    }

    /**
     * 身份证反面有效期识别
     */
    private void youXiaoQiXian(String str) {
        if (str.contains("有效期限")) {
            // String为引用类型
            str = str.replace("有效期限", "");
        }
        String pattern = "\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}-\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        if (m.matches()) {
            validDate = str;
        }
    }
}
