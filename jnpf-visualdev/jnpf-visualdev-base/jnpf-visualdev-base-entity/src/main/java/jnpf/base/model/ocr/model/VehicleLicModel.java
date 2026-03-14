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
 * ocr解析行驶证模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析行驶证模型")
@NoArgsConstructor
public class VehicleLicModel extends OcrModel {
    @Schema(description = "号牌号码")
    private String plateNo;
    @Schema(description = "车辆类型")
    private String vehicleType;
    @Schema(description = "所有人")
    private String owner;
    @Schema(description = "住址")
    private String address;
    @Schema(description = "使用性质")
    private String useCharacter;
    @Schema(description = "品牌型号")
    private String model;
    @Schema(description = CLSBDH)
    private String vin;
    @Schema(description = FDJHM)
    private String engineNo;
    @Schema(description = "注册日期")
    private Long registerDate;
    @Schema(description = "发证日期")
    private Long issueDate;

    private static final String CLSBDH = "车辆识别代号";
    private static final String FDJHM = "发动机号码";
    private static final String DATE_DD = "yyyyMMdd";
    private static final String DATE_DD2 = "yyyy-MM-dd";

    @Override
    public void extract(String url, JSONObject body) {
        super.extract(url, body);
        if (StringUtil.isEmpty(this.getContent()) || !this.getContent().contains("行驶证")) {
            return;
        }
        this.setInfo();
        this.setContent(null);
        this.setStrList(null);
    }

    private void setInfo() {
        // 使用正则表达式提取关键信息
        String ocrText = this.getContent();
        int size = this.getStrList().size();
        //for循环取单行数据，避免整体取到其他行合并信息
        for (int i = 0; i < size; i++) {
            if (StringUtil.isEmpty(this.plateNo) && this.getStrList().get(i).contains("号牌号码")) {
                this.plateNo = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.vehicleType) && this.getStrList().get(i).contains("车辆类型")) {
                this.vehicleType = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.owner) && this.getStrList().get(i).contains("所有人")) {
                this.owner = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.address) && this.getStrList().get(i).contains("住址")) {
                this.address = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.useCharacter) && this.getStrList().get(i).contains("使用性质")) {
                this.useCharacter = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.model) && this.getStrList().get(i).contains("品牌型号")) {
                this.model = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.vin) && this.getStrList().get(i).contains(CLSBDH)) {
                this.vin = this.getStrList().get(i + 1);
            }
            if (StringUtil.isEmpty(this.engineNo) && this.getStrList().get(i).contains(FDJHM)) {
                this.engineNo = this.getStrList().get(i + 1);
            }
            if (this.registerDate == null && this.getStrList().get(i).contains("注册日期")) {
                Date date = DateUtil.checkDate(this.getStrList().get(i + 1), DATE_DD2);
                if (date == null) {
                    date = DateUtil.checkDate(this.getStrList().get(i + 1), DATE_DD);
                }
                this.registerDate = date != null ? date.getTime() : null;
            }
            if (this.issueDate == null && this.getStrList().get(i).contains("发证日期")) {
                Date date = DateUtil.checkDate(this.getStrList().get(i + 1), DATE_DD2);
                if (date == null) {
                    date = DateUtil.checkDate(this.getStrList().get(i + 1), DATE_DD);
                }
                this.issueDate = date != null ? date.getTime() : null;
            }
        }
        //车牌号识别
        extractPlateNo();

        otherProp(ocrText);
    }

    private void otherProp(String ocrText) {
        String vehicleTypeA = betweenText(ocrText, "车辆类型", "所有");
        if (StringUtil.isEmpty(this.vehicleType) || (StringUtil.isNotEmpty(vehicleTypeA) && !vehicleTypeA.startsWith(this.vehicleType))) {
            this.vehicleType = vehicleTypeA;
        }
        String ownerA = betweenText(ocrText, "所有", "住址");
        if (StringUtil.isEmpty(this.owner) || (StringUtil.isNotEmpty(ownerA) && !ownerA.startsWith(this.owner))) {
            this.owner = ownerA;
        }
        String addressA = betweenText(ocrText, "住址", "使用性质");
        if (StringUtil.isEmpty(this.address) || (StringUtil.isNotEmpty(addressA) && !addressA.startsWith(this.address))) {
            this.address = addressA;
        }
        String useCharacterA = betweenText(ocrText, "使用性质", "品牌型号");
        if (StringUtil.isEmpty(this.useCharacter) || (StringUtil.isNotEmpty(useCharacterA) && !useCharacterA.startsWith(this.useCharacter))) {
            this.useCharacter = useCharacterA;
        }
        String modelA = betweenText(ocrText, "品牌型号", CLSBDH);
        if (StringUtil.isEmpty(this.model) || (StringUtil.isNotEmpty(modelA) && !modelA.startsWith(this.model))) {
            this.model = modelA;
        }
        String vinA = betweenText(ocrText, CLSBDH, FDJHM);
        if (StringUtil.isEmpty(this.vin) || (StringUtil.isNotEmpty(vinA) && !vinA.startsWith(this.vin))) {
            this.vin = vinA;
        }
        String engineNoA = betweenText(ocrText, FDJHM, "注册日期");
        if (StringUtil.isEmpty(this.engineNo) || (StringUtil.isNotEmpty(engineNoA) && !engineNoA.startsWith(this.engineNo))) {
            this.engineNo = engineNoA;
        }
        Long registerDateA = extractDateField("注册日期[:：]\\s*(\\d{4}-\\d{1,2}-\\d{1,2})", ocrText);
        if (this.registerDate != null && registerDateA != null) {
            this.registerDate = registerDateA;
        }
        Long issueDateA = extractDateField("发证日期[:：]\\s*(\\d{4}-\\d{1,2}-\\d{1,2})", ocrText);
        if (this.issueDate != null && issueDateA != null) {
            this.issueDate = issueDateA;
        }
    }

    private Long extractDateField(String regex, String text) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            Date date = DateUtil.checkDate(dateStr, DATE_DD2);
            if (date == null) {
                date = DateUtil.checkDate(dateStr, DATE_DD);
            }
            return date != null ? date.getTime() : null;
        }
        return null;
    }

    private String betweenText(String content, String text1, String text2) {
        List<String> list = new ArrayList<>(Arrays.asList("号牌号码", "PlateNo", "Plate", "No", "车辆类型", "vehicleType", "Vehicle", "Type", "所有人", "Owner", "住址",
                "Address", "使用性质", "UseCharacter", "Use", "Character", "品牌型号", "Model", CLSBDH, "Vin", FDJHM, "EngineNo", "Engine",
                "注册日期", "RegisterDate", "Register", "Data", "发证日期", "IssueDate", "Issue"));
        if (StringUtil.isNotEmpty(this.plateNo)) {
            list.add(this.plateNo);
        }
        int m = content.indexOf(text1);
        int n = content.indexOf(text2);
        if (m > 0 && n > m) {
            String substring = content.substring(m + text1.length(), n);
            if (StringUtil.isNotEmpty(substring)) {
                for (String s : list) {
                    if (substring.contains(s)) {
                        substring = substring.replace(s, "");
                    }
                }
            }
            return substring;
        }
        return "";
    }

    private void extractPlateNo() {
        String regex = "[京津冀晋蒙辽吉黑沪苏浙皖闽赣鲁豫鄂湘粤桂琼渝川贵云藏陕甘青宁新台港澳][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[警学领]?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this.getContent());
        if ((StringUtil.isEmpty(this.plateNo) || !pattern.matcher(this.plateNo).matches()) && matcher.find()) {
            this.plateNo = matcher.group();
        }
    }
}
