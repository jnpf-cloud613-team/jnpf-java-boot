package jnpf.base.model.ocr.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrModel;
import jnpf.base.model.ocr.OcrResModel;
import jnpf.util.DateUtil;
import jnpf.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ocr解析发票模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析发票模型")
@NoArgsConstructor
public class InvoiceLicModel extends OcrModel {
    @Schema(description = "发票名称")
    private String title;
    @Schema(description = "发票代码")
    private String code;
    @Schema(description = "发票号码")
    private String number;
    @Schema(description = "开票日期")
    private Long date;
    @Schema(description = "校验码")
    private String checkCode;
    @Schema(description = "密码区")
    private String password;
    @Schema(description = "购买方名称")
    private String buyerName;
    @Schema(description = "购买方纳税人识别号")
    private String buyerTaxId;
    @Schema(description = "购买方地址/电话")
    private String buyerContact;
    @Schema(description = "购买方开户行及账号")
    private String buyerBankAccount;
    @Schema(description = "合计金额")
    private String totalAmount;
    @Schema(description = "合计税额")
    private String totalTaxAmount;
    @Schema(description = "价税合计（大写）")
    private String totalAmountUpper;
    @Schema(description = "价税合计（小写）")
    private String totalAmountLower;
    @Schema(description = "销售方名称")
    private String sellerName;
    @Schema(description = "销售方纳税人识别号")
    private String sellerTaxId;
    @Schema(description = "销售方地址/电话")
    private String sellerContact;
    @Schema(description = "销售方开户行及账号")
    private String sellerBankAccount;
    @Schema(description = "备注")
    private String remarks;
    @Schema(description = "收款人")
    private String payee;
    @Schema(description = "复核人")
    private String reviewer;
    @Schema(description = "开票人")
    private String issuer;

    @Schema(description = "项目详情")
    private List<InvoiceDetail> invoiceDetail = new ArrayList<>();

    @Schema(description = "最大宽度")
    @JsonIgnore
    private Integer widthMax = 0;
    @Schema(description = "最大高度")
    @JsonIgnore
    private Integer heightMax = 0;

    @Override
    public void extract(String url, JSONObject body) {
        JSONObject systemJson = this.getSystemJson(url, body);
        List<OcrResModel> param = getParam(systemJson);
        if (StringUtil.isEmpty(this.getContent()) || !this.getContent().contains("发票")) {
            return;
        }
        //两种发票--带和不带密码区
        if (this.getStrList().contains("密") || this.getStrList().contains("码") || this.getStrList().contains("区") || this.getContent().contains("校验码")) {
            this.setInfoOne(param);
        } else {
            this.setInfoTwo(param);
        }
        this.setContent(null);
        this.setStrList(null);
    }

    //数据转换
    private List<OcrResModel> getParam(JSONObject obj) {
        //识别
        List<String> list = new ArrayList<>();
        List<OcrResModel> listJosn = new ArrayList<>();
        if (obj != null && obj.containsKey("status") && Objects.equals("000", String.valueOf(obj.get("status")))) {
            JSONArray results = obj.getJSONArray("results");
            for (Object result : results) {
                JSONArray m = (JSONArray) result;
                for (Object n : m) {
                    JSONObject o = (JSONObject) n;
                    list.add(o.getString("text"));

                    if (StringUtil.isEmpty(o.getString("text"))) continue;
                    OcrResModel resModel = new OcrResModel();
                    resModel.setText(o.getString("text").replace(" ", ""));
                    resModel.setConfidence(o.getBigDecimal("confidence"));
                    JSONArray textRegion = o.getJSONArray("text_region");
                    if (textRegion.size() == 4) {
                        JSONArray o1 = (JSONArray) textRegion.get(0);
                        JSONArray o2 = (JSONArray) textRegion.get(2);
                        resModel.setX1(o1.getInteger(0));
                        resModel.setY1(o1.getInteger(1));
                        resModel.setX2(o2.getInteger(0));
                        resModel.setY2(o2.getInteger(1));
                        if (resModel.getX1() > this.widthMax) {
                            this.widthMax = resModel.getX1();
                        }
                        if (resModel.getX2() > this.widthMax) {
                            this.widthMax = resModel.getX2();
                        }
                        if (resModel.getY1() > this.heightMax) {
                            this.heightMax = resModel.getX1();
                        }
                        if (resModel.getY2() > this.heightMax) {
                            this.heightMax = resModel.getX1();
                        }
                        listJosn.add(resModel);
                    }
                }
            }
        }
        this.setStrList(list);
        this.setContent(String.join("", list));
        return listJosn;
    }

    //第一种发票类型
    private void setInfoOne(List<OcrResModel> param) {
        //购买方底部线
        Integer h1 = param.stream().filter(t -> "单位".equals(t.getText()) || "数量".equals(t.getText()) || "单价".equals(t.getText()))
                .collect(Collectors.groupingBy(OcrResModel::getY1, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
        //货物服务清单以上部分
        List<OcrResModel> collect1 = param.stream().filter(t -> t.getY1() < h1).collect(Collectors.toList());
        //头部信息
        topInfo(collect1);
        //购买方信息
        buyerInfo(collect1);
        //货物服务清单以下部分
        List<OcrResModel> collect2 = param.stream().filter(t -> t.getY1() >= h1 - 10).collect(Collectors.toList());
        //统计和备注
        countAndRemark(collect2);
        //销售方信息
        sellerDown(collect2);
    }

    //字段解析
    private String getStr(String key, OcrResModel item, List<OcrResModel> list, Integer index) {
        String value = "";
        String text = item.getText();
        if (text.contains(key)) {
            if (text.endsWith("：")) {
                if ((index + 1) < list.size() && list.get(index + 1) != null && list.get(index + 1).getY1() < item.getY2()) {
                    value = list.get(index + 1).getText();
                }
            } else {
                value = text.substring(text.indexOf("：") + 1);
            }
        }
        return value;
    }

    //上部分
    private void topInfo(List<OcrResModel> collect1) {
        int xMid = widthMax / 2;
        int[] hSet = collect1.stream().filter(t -> "名".equals(t.getText()) || t.getText().contains("称：") || t.getText().startsWith("名称"))
                .map(OcrResModel::getY1).collect(Collectors.toSet()).stream().mapToInt(Integer::intValue).toArray();
        if (hSet.length > 0) {
            //购买方名称以上部分
            List<OcrResModel> collect3 = collect1.stream().filter(t -> t.getY2() < hSet[0]).collect(Collectors.toList());
            OcrResModel ocrResModel1 = collect3.stream().filter(t -> t.getX1() < xMid && t.getX2() > xMid).findFirst().orElse(null);
            title = ocrResModel1 != null ? ocrResModel1.getText() : null;
            int i = 0;
            for (OcrResModel item : collect3) {
                if (StringUtil.isEmpty(code)) code = getStr("代码：", item, collect3, i);
                if (StringUtil.isEmpty(number)) number = getStr("号码：", item, collect3, i);
                if (StringUtil.isEmpty(number) && item.getText().startsWith("No")) {
                    number = item.getText().substring(2);
                }
                if (date == null) {
                    String dateStr = getStr("开票", item, collect3, i);
                    if (StringUtil.isNotEmpty(dateStr)) {
                        dateStr = dateStr.length() > 11 ? dateStr.substring(dateStr.length() - 11) : dateStr;
                        Date date1 = DateUtil.checkDate(dateStr, "yyyy年MM月dd日");
                        date = date1 != null ? date1.getTime() : null;
                    }
                }
                if (StringUtil.isEmpty(checkCode)) checkCode = getStr("校验码", item, collect3, i);
                i++;
            }

            //补充，如果识别不到代码和号码
            if (StringUtil.isEmpty(code)) {
                for (OcrResModel item : collect3) {
                    if (Pattern.compile("\\d{10,12}").matcher(item.getText()).matches()) {
                        code = item.getText();
                        return;
                    }
                }
            }
        }
    }

    //购买方
    private void buyerInfo(List<OcrResModel> collect1) {
        int xMid = widthMax / 2;
        OcrResModel namModel = collect1.stream().filter(t -> "名".equals(t.getText()) || t.getText().contains("称：")
                || "名称：".equals(t.getText())).findFirst().orElse(null);
        //购买方部分
        List<OcrResModel> collect4 = namModel != null ? collect1.stream().filter(t -> t.getY2() > namModel.getY1() - 5).collect(Collectors.toList()) : collect1;
        //购买方左边
        List<OcrResModel> collect5 = collect4.stream().filter(t -> t.getX1() < xMid).collect(Collectors.toList());
        int j = 0;
        for (OcrResModel item : collect5) {
            if (StringUtil.isEmpty(buyerName)) buyerName = getStr("称：", item, collect5, j);
            if (StringUtil.isEmpty(buyerTaxId)) buyerTaxId = getStr("识别号：", item, collect5, j);
            if (StringUtil.isEmpty(buyerContact)) buyerContact = getStr("电话：", item, collect5, j);
            if (StringUtil.isEmpty(buyerBankAccount)) buyerBankAccount = getStr("标识：", item, collect5, j);
            if (StringUtil.isEmpty(buyerBankAccount)) buyerBankAccount = getStr("账号：", item, collect5, j);
            j++;
        }
        //购买方右边
        List<OcrResModel> collect6 = collect4.stream().filter(t -> t.getX1() > xMid).collect(Collectors.toList());
        OcrResModel ocrResModel = collect6.stream().filter(t ->
                "密".equals(t.getText()) || "码".equals(t.getText()) || "区".equals(t.getText())).findFirst().orElse(null);
        if (ocrResModel != null) {
            password = collect6.stream().filter(t -> t.getX1() > ocrResModel.getX2()).map(OcrResModel::getText).collect(Collectors.joining());
        } else {
            password = collect6.stream().map(OcrResModel::getText).collect(Collectors.joining());
        }
    }

    //下部分
    private void countAndRemark(List<OcrResModel> collect2) {
        OcrResModel countModel = collect2.stream().filter(t -> "合".equals(t.getText()) || "计".equals(t.getText())).findFirst().orElse(null);
        if (countModel != null) {
            int indexCount = collect2.indexOf(countModel);
            if (collect2.get(indexCount + 1).getX1() < widthMax / 2) {
                indexCount = indexCount + 1;
            }
            totalAmount = collect2.get(indexCount + 1).getText();
            totalTaxAmount = collect2.get(indexCount + 2).getText();
            if (StringUtil.isNotEmpty(totalAmount) && !Character.isDigit(totalAmount.charAt(0))) {
                totalAmount = totalAmount.substring(1);
            }
            if (StringUtil.isNotEmpty(totalTaxAmount) && !Character.isDigit(totalTaxAmount.charAt(0))) {
                totalTaxAmount = totalTaxAmount.substring(1);
            }
            //中间部分数组
            List<OcrResModel> itemCollect = collect2.stream().filter(t -> t.getY2() < countModel.getY1()).collect(Collectors.toList());
            itemInfo(itemCollect);
        }

        OcrResModel countUpper = collect2.stream().filter(t -> "价税合计（大写）".equals(t.getText()) || t.getText().contains("税合")).findFirst().orElse(null);
        if (countUpper != null) {
            int indexCountUpper = collect2.indexOf(countUpper);
            totalAmountUpper = collect2.get(indexCountUpper + 1).getText();
            String tal = collect2.get(indexCountUpper + 2).getText();
            if (tal.endsWith("写）")) {
                totalAmountLower = collect2.get(indexCountUpper + 3).getText();
            } else {
                totalAmountLower = tal.contains("）") && tal.split("）").length > 1 ? tal.split("）")[1] : tal;
            }
            if (StringUtil.isNotEmpty(totalAmountLower) && !Character.isDigit(totalAmountLower.charAt(0))) {
                totalAmountLower = totalAmountLower.substring(1);
            }
        }

        int downIndex = countUpper != null ? countUpper.getY2() : heightMax / 2;
        //下面部分数组
        List<OcrResModel> downCollect = collect2.stream().filter(t -> t.getY2() > downIndex).collect(Collectors.toList());
        int i = 0;
        for (OcrResModel item : downCollect) {
            if (StringUtil.isEmpty(payee)) {
                String str = getStr("收款人：", item, downCollect, i);
                if (StringUtil.isNotEmpty(str) && !str.contains("复核")) {
                    payee = str;
                }
            }
            if (StringUtil.isEmpty(reviewer)) {
                String str = getStr("复核：", item, downCollect, i);
                if (StringUtil.isNotEmpty(str) && !str.contains("开票人")) {
                    reviewer = str;
                }
            }
            if (StringUtil.isEmpty(issuer)) issuer = getStr("开票人：", item, downCollect, i);
            i++;
        }
        //备注
        OcrResModel remarkM = collect2.stream().filter(t -> "备".equals(t.getText()) || "注".equals(t.getText())).findFirst().orElse(null);
        if (remarkM != null) {
            OcrResModel remarkLimit = collect2.stream().filter(t -> t.getText().contains("开票人")).findFirst().orElse(null);
            List<OcrResModel> collect = downCollect.stream().filter(t -> {
                if (remarkLimit != null) {
                    return t.getX1() > remarkM.getX2() && t.getY1() > downIndex && t.getY1() < remarkLimit.getY1();
                }
                return t.getX1() > remarkM.getX2() && t.getY1() > downIndex;
            }).collect(Collectors.toList());
            remarks = collect.stream().map(OcrResModel::getText).collect(Collectors.joining());
        }
    }

    //销售方
    private void sellerDown(List<OcrResModel> collect) {
        OcrResModel countUpper = collect.stream().filter(t -> "价税合计（大写）".equals(t.getText())
                || "税合".equals(t.getText())).findFirst().orElse(null);
        int downIndex = countUpper != null ? countUpper.getY2() : heightMax / 2;
        //下面部分数组
        List<OcrResModel> downCollect = collect.stream().filter(t -> t.getY2() > downIndex).collect(Collectors.toList());
        int i = 0;
        for (OcrResModel item : downCollect) {
            if (StringUtil.isEmpty(sellerName)) sellerName = getStr("称：", item, downCollect, i);
            if (StringUtil.isEmpty(sellerTaxId)) sellerTaxId = getStr("识别号：", item, downCollect, i);
            if (StringUtil.isEmpty(sellerContact)) sellerContact = getStr("电话：", item, downCollect, i);
            if (StringUtil.isEmpty(sellerBankAccount)) sellerBankAccount = getStr("标识：", item, downCollect, i);
            if (StringUtil.isEmpty(sellerBankAccount)) sellerBankAccount = getStr("账号：", item, downCollect, i);
            i++;
        }
    }

    //中间物资信息
    private void itemInfo(List<OcrResModel> itemCollect) {
        OcrResModel itemTitle = itemCollect.stream().filter(t ->
                "单位".equals(t.getText()) || "数量".equals(t.getText()) || "税率".equals(t.getText())).findFirst().orElse(null);

        OcrResModel nameT = itemCollect.stream().filter(t -> t.getText().contains("服务名称") || t.getText().contains("货物或")
                || t.getText().contains("项目名称")).findFirst().orElse(null);
        OcrResModel typeT = itemCollect.stream().filter(t -> t.getText().equals("规格型号") || t.getText().contains("型号")).findFirst().orElse(null);
        OcrResModel unitT = itemCollect.stream().filter(t -> t.getText().equals("单位")).findFirst().orElse(null);
        OcrResModel numT = itemCollect.stream().filter(t -> t.getText().equals("数量")).findFirst().orElse(null);
        if (numT == null) {
            numT = itemCollect.stream().filter(t -> t.getText().equals("数")).findFirst().orElse(null);
        }
        OcrResModel priceT = itemCollect.stream().filter(t -> t.getText().equals("单价")).findFirst().orElse(null);
        OcrResModel amountT = itemCollect.stream().filter(t -> t.getText().equals("金额")).findFirst().orElse(null);
        if (amountT == null) {
            amountT = itemCollect.stream().filter(t -> t.getText().equals("金")).findFirst().orElse(null);
        }
        OcrResModel rateT = itemCollect.stream().filter(t -> t.getText().contains("税率") || t.getText().equals("税单")).findFirst().orElse(null);
        OcrResModel taxAmountT = itemCollect.stream().filter(t -> t.getText().equals("税额")).findFirst().orElse(null);
        if (taxAmountT == null) {
            taxAmountT = itemCollect.stream().filter(t -> t.getText().equals("税")).findFirst().orElse(null);
        }
        if (nameT == null || typeT == null || unitT == null || numT == null || priceT == null
                || amountT == null || rateT == null || taxAmountT == null) {
            return;
        }
        List<OcrResModel> eachModel = new ArrayList<>();
        eachModel.add(0, nameT);
        eachModel.add(1, typeT);
        eachModel.add(2, unitT);
        eachModel.add(3, numT);
        eachModel.add(4, priceT);
        eachModel.add(5, amountT);
        eachModel.add(6, rateT);
        eachModel.add(7, taxAmountT);
        eachModel.add(8, itemTitle);
        itemInfoParse(itemCollect, eachModel);
    }

    private void itemInfoParse(List<OcrResModel> itemCollect, List<OcrResModel> eachModel) {
        List<OcrResModel> nameList = new ArrayList<>();//品名
        List<OcrResModel> typeList = new ArrayList<>();//类型
        List<OcrResModel> unitList = new ArrayList<>();//单位
        List<OcrResModel> numList = new ArrayList<>();//数量
        List<OcrResModel> priceList = new ArrayList<>();//单价
        List<OcrResModel> amountList = new ArrayList<>();//金额
        List<OcrResModel> rateList = new ArrayList<>();//税率
        List<OcrResModel> taxAmountList = new ArrayList<>();//税额
        List<OcrResModel> collect = itemCollect.stream().filter(t -> t.getY1() > eachModel.get(8).getY2() - 8).collect(Collectors.toList());//误差给8
        for (OcrResModel item : collect) {
            if (item.getX2() > eachModel.get(0).getX1() && item.getX1() < (eachModel.get(1).getX1() + eachModel.get(0).getX2()) / 2) {
                nameList.add(item);
            }
            if (item.getX1() > (eachModel.get(1).getX1() + eachModel.get(0).getX2()) / 2 && item.getX1() < (eachModel.get(2).getX1() + eachModel.get(1).getX2()) / 2) {
                typeList.add(item);
            }
            if (item.getX1() > (eachModel.get(2).getX1() + eachModel.get(1).getX2()) / 2 && item.getX1() < (eachModel.get(3).getX1() + eachModel.get(2).getX2()) / 2) {
                unitList.add(item);
            }
            if (item.getX1() > (eachModel.get(3).getX1() + eachModel.get(2).getX2()) / 2 && item.getX1() < (eachModel.get(4).getX1() + eachModel.get(3).getX2()) / 2) {
                numList.add(item);
            }
            if (item.getX1() > (eachModel.get(4).getX1() + eachModel.get(3).getX2()) / 2 && item.getX1() < (eachModel.get(5).getX1() + eachModel.get(4).getX2()) / 2) {
                priceList.add(item);
            }
            if (item.getX1() > (eachModel.get(5).getX1() + eachModel.get(4).getX2()) / 2 && item.getX1() < (eachModel.get(6).getX1() + eachModel.get(6).getX1()) / 2) {
                amountList.add(item);
            }
            if (item.getX1() > (eachModel.get(6).getX1() + eachModel.get(6).getX1()) / 2 && item.getX1() < eachModel.get(7).getX1()) {
                if (StringUtil.isNotEmpty(item.getText()) && !Character.isDigit(item.getText().length() - 1)) {
                    item.setText(item.getText().substring(0, item.getText().length() - 1));
                }
                rateList.add(item);
            }
            if (item.getX1() > eachModel.get(7).getX1()) {
                taxAmountList.add(item);
            }
        }
        Map<String, List<OcrResModel>> map = new HashMap<>();
        map.put("nameList", nameList);
        map.put("typeList", typeList);
        map.put("unitList", unitList);
        map.put("numList", numList);
        map.put("priceList", priceList);
        map.put("amountList", amountList);
        map.put("rateList", rateList);
        map.put("taxAmountList", taxAmountList);
        invoiceDetail = InvoiceDetail.getList(map);
    }

    //+++++++++++++++++++++++++++++++++++++++++第二种发票类型+++++++++++++++++++++++++++++++++++++++
    //第二种发票类型
    private void setInfoTwo(List<OcrResModel> param) {
        //购买方底部线
        Integer h1 = param.stream().filter(t -> "单位".equals(t.getText()) || "数量".equals(t.getText()) || "单价".equals(t.getText()))
                .collect(Collectors.groupingBy(OcrResModel::getY1, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
        //货物服务清单以上部分
        List<OcrResModel> collect1 = param.stream().filter(t -> t.getY2() < h1).collect(Collectors.toList());
        //头部信息
        topInfo(collect1);
        //购买方信息 和销售方信息
        buyerAndSeller(collect1);
        //货物服务清单以下部分
        List<OcrResModel> collect2 = param.stream().filter(t -> t.getY1() >= h1 - 10).collect(Collectors.toList());
        //统计和备注
        countAndRemark(collect2);
    }

    //第二种发票购买方和销售方
    private void buyerAndSeller(List<OcrResModel> upList) {
        int xMid = widthMax / 2;
        OcrResModel namModel = upList.stream().filter(t -> t.getText().startsWith("名称：")).findFirst().orElse(null);
        if (namModel == null) return;
        //购买方部分
        List<OcrResModel> collect4 = upList.stream().filter(t -> t.getY2() > namModel.getY1() - 10).collect(Collectors.toList());
        //购买方左边
        List<OcrResModel> collect5 = collect4.stream().filter(t -> t.getX1() < xMid).collect(Collectors.toList());
        int i = 0;
        for (OcrResModel item : collect5) {
            if (StringUtil.isEmpty(buyerName)) buyerName = getStr("名称：", item, collect5, i);
            if (StringUtil.isEmpty(buyerTaxId)) buyerTaxId = getStr("识别号：", item, collect5, i);
            i++;
        }
        //购买方右边
        List<OcrResModel> collect6 = collect4.stream().filter(t -> t.getX1() > xMid).collect(Collectors.toList());
        int j = 0;
        for (OcrResModel item : collect6) {
            if (StringUtil.isEmpty(sellerName)) sellerName = getStr("名称：", item, collect5, j);
            if (StringUtil.isEmpty(sellerTaxId)) sellerTaxId = getStr("识别号：", item, collect5, j);
            j++;
        }
    }
}
