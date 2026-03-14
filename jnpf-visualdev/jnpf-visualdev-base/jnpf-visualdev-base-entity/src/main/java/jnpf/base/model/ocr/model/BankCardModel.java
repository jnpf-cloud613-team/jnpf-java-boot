package jnpf.base.model.ocr.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrModel;
import jnpf.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ocr解析银行卡模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析银行卡模型")
@NoArgsConstructor
public class BankCardModel extends OcrModel {
    @Schema(description = "银行卡号")
    private String code;
    @Schema(description = "银行名称")
    private String name;
    @Schema(description = "银行卡类型")
    private String cardType;
    @Schema(description = "有效期")
    private String validDate;

    @Override
    public void extract(String url, JSONObject body) {
        super.extract(url, body);
        if (StringUtil.isEmpty(this.getContent()) || !this.getContent().contains("银行")) {
            return;
        }
        this.setOther();
        this.setCardCode();
        this.setContent(null);
        this.setStrList(null);
    }

    private void setOther() {
        for (String str : this.getStrList()) {
            if (StringUtil.isNotEmpty(str)) {
                if (StringUtil.isEmpty(this.name) && str.endsWith("银行")) {
                    this.name = str;
                }
                if (StringUtil.isEmpty(this.cardType) && str.contains("卡")) {
                    this.cardType = str;
                }

                if (StringUtil.isEmpty(this.code) && str.matches("^(?=(?:\\D*\\d){10}).+$")) {
                    this.code = StringUtils.getDigits(str);
                }

                if (StringUtil.isEmpty(this.validDate) && str.length() == 5 && str.matches("^(0[1-9]|1[0-2]).\\d{2}$")) {
                    String substring = str.substring(2, 3);
                    this.validDate = str.replace(substring, "/");
                }
            }
        }

    }

    private void setCardCode() {
        Pattern pattern = Pattern.compile("(\\d{16,19})");
        String str = this.getContent().replaceAll("\\s+", "");
        Matcher matcher = pattern.matcher(str);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        if (!list.isEmpty()) {
            for (String item : list) {
                if (isValidCardNumber(item)) { // 使用验证方法
                    this.code = item;
                    return;
                }
            }
            if (StringUtil.isEmpty(this.code)) {
                this.code = list.get(0);
            }
        }
    }

    /**
     * 验证银行卡号有效性
     *
     * @param cardNumber
     * @return
     */
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || (cardNumber.length() != 16 && cardNumber.length() != 19)) {
            return false;
        }

        // Luhn算法验证
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
