package jnpf.base.model.ocr.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrResModel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Schema(description = "ocr解析发票模型（项目详情）")
@NoArgsConstructor
public class InvoiceDetail {
    @Schema(description = "发票明细-货物或应税劳务/服务名称")
    private String itemName;
    @Schema(description = "发票明细-规格型号")
    private String itemSpec;
    @Schema(description = "发票明细-单位")
    private String itemUnit;
    @Schema(description = "发票明细-数量")
    private String itemQuantity;
    @Schema(description = "发票明细-单价")
    private String itemUnitPrice;
    @Schema(description = "发票明细-金额")
    private String itemAmount;
    @Schema(description = "发票明细-税率")
    private String itemTaxRate;
    @Schema(description = "发票明细-税额")
    private String itemTaxAmount;

    public static List<InvoiceDetail> getList(Map<String, List<OcrResModel>> map) {
        List<OcrResModel> nameList = map.get("nameList") != null ? map.get("nameList") : new ArrayList<>();
        List<OcrResModel> typeList = map.get("typeList") != null ? map.get("typeList") : new ArrayList<>();
        List<OcrResModel> unitList = map.get("unitList") != null ? map.get("unitList") : new ArrayList<>();
        List<OcrResModel> numList = map.get("numList") != null ? map.get("numList") : new ArrayList<>();
        List<OcrResModel> priceList = map.get("priceList") != null ? map.get("priceList") : new ArrayList<>();
        List<OcrResModel> amountList = map.get("amountList") != null ? map.get("amountList") : new ArrayList<>();
        List<OcrResModel> rateList = map.get("rateList") != null ? map.get("rateList") : new ArrayList<>();
        List<OcrResModel> taxAmountList = map.get("taxAmountList") != null ? map.get("taxAmountList") : new ArrayList<>();
        List<InvoiceDetail> list = new ArrayList<>();
        if (priceList.size() == 1) {
            InvoiceDetail invoiceDetail = new InvoiceDetail();
            invoiceDetail.setItemName(String.join("", nameList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemSpec(String.join("", typeList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemUnit(String.join("", unitList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemQuantity(String.join("", numList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemUnitPrice(String.join("", priceList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemAmount(String.join("", amountList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemTaxRate(String.join("", rateList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            invoiceDetail.setItemTaxAmount(String.join("", taxAmountList.stream().map(OcrResModel::getText).collect(Collectors.toList())));
            list.add(invoiceDetail);
        } else if (priceList.size() > 1) {
            for (int i = 0; i < priceList.size(); i++) {
                OcrResModel ocrResModel = priceList.get(i);
                Integer y1 = ocrResModel.getY1() - 8;
                Integer y2;
                InvoiceDetail invoiceDetail = new InvoiceDetail();
                if (i + 1 < priceList.size()) {
                    OcrResModel nextModel = priceList.get(i + 1);
                    y2 = nextModel.getY1() - 8;
                } else {
                    y2 = 0;
                }
                invoiceDetail.setItemName(String.join("", nameList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemSpec(String.join("", typeList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemUnit(String.join("", unitList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemQuantity(String.join("", numList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemUnitPrice(String.join("", priceList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemAmount(String.join("", amountList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemTaxRate(String.join("", rateList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                invoiceDetail.setItemTaxAmount(String.join("", taxAmountList.stream()
                        .filter(t -> y2 == 0 ? t.getY1() > y1 : (t.getY1() > y1 && t.getY1() < y2)).map(OcrResModel::getText).collect(Collectors.toList())));
                list.add(invoiceDetail);
            }
        }
        return list;
    }
}

