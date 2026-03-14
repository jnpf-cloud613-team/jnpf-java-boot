package jnpf.base.model.ocr;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.model.*;
import jnpf.exception.DataException;
import jnpf.util.wxutil.HttpUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * ocr解析模型父类
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@Data
@Schema(description = "ocr解析模型父类")
@NoArgsConstructor
public class OcrModel {
    @JsonIgnore
    private String type;
    @JsonIgnore
    private List<String> strList;
    private String content;

    public static OcrModel get(String type) {
        OcrModel ocrModel = new OcrModel();
        if (OcrConstant.TEXT_OCR.equals(type)) {
            return ocrModel;
        }
        if (OcrConstant.BANK_CARD.equals(type)) {
            ocrModel = new BankCardModel();
        }
        if (OcrConstant.IDCARD_FRONT.equals(type) || OcrConstant.IDCARD_BACK.equals(type)) {
            ocrModel = new IdCardModel();
            ocrModel.setType(type);
            return ocrModel;
        }
        if (OcrConstant.BUSINESS_LICENSE.equals(type)) {
            ocrModel = new BusinessLicModel();
        }
        if (OcrConstant.INVOICE.equals(type)) {
            ocrModel = new InvoiceLicModel();
        }
        if (OcrConstant.DRIVING_LICENSE.equals(type)) {
            ocrModel = new DrivingLicModel();
        }
        if (OcrConstant.VEHICLE_LICENSE.equals(type)) {
            ocrModel = new VehicleLicModel();
        }
        if (OcrConstant.TRAIN_TICKET.equals(type)) {
            ocrModel = new TrainTicketModel();
        }
        return ocrModel;
    }

    public void getStrList(JSONObject obj) {
        List<String> list = new ArrayList<>();
        if (obj != null && obj.containsKey("status")) {
            JSONArray results = obj.getJSONArray("results");
            for (Object result : results) {
                JSONArray m = (JSONArray) result;
                for (Object n : m) {
                    JSONObject o = (JSONObject) n;
                    list.add(o.getString("text"));
                }
            }
        }
        strList = list;
    }

    public void extract(String url, JSONObject body) {
        JSONObject systemJson = this.getSystemJson(url, body);
        getStrList(systemJson);
        this.content = String.join("", strList);
    }

    public JSONObject getSystemJson(String url, JSONObject body) {
        JSONObject jsonObject = HttpUtil.httpRequest(url + "/predict/ocr_system", "POST", body.toJSONString());
        if (jsonObject == null) {
            throw new DataException("OCR服务器连接失败！");
        }
        return jsonObject;
    }

    public JSONObject getTableJson(String url, JSONObject body) {
        //structure_layout
        return HttpUtil.httpRequest(url + "/predict/structure_table", "POST", body.toJSONString());
    }
}
