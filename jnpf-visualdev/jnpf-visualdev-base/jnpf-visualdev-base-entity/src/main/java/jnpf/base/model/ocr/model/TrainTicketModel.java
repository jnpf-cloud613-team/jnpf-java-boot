package jnpf.base.model.ocr.model;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.ocr.OcrModel;
import jnpf.util.DateUtil;
import jnpf.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ocr解析火车票模型
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "ocr解析火车票模型")
@NoArgsConstructor
public class TrainTicketModel extends OcrModel {
    @Schema(description = "票号")
    private String ticketNum;
    @Schema(description = "出发地")
    private String startingStation;
    @Schema(description = "目的地")
    private String destinationStation;
    @Schema(description = "车次")
    private String trainNum;

    @Schema(description = "发车日期")
    private Long dateTime;
    @Schema(description = "票价")
    private String ticketRates;
    @Schema(description = "售票地点")
    private String ticketLocation;
    @Schema(description = "序列号")
    private String serialNum;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "身份证号")
    private String idNum;
    @Schema(description = "座位号")
    private String seat;
    @Schema(description = "座位类型")
    private String seatCategory;
    @Schema(description = "检票口")
    private String ticketGate;

    @Override
    public void extract(String url, JSONObject body) {
        super.extract(url, body);
        if (StringUtil.isEmpty(this.getContent()) || !this.getContent().contains("车")) {
            return;
        }
        this.setInfo();
        this.setContent(null);
        this.setStrList(null);
    }

    private void setInfo() {
        List<String> list = new ArrayList<>(this.getStrList());
        ticketNum = list.get(0).matches("(?:[A-Za-z]*\\d+[A-Za-z]*\\d*|[A-Za-z]+\\d+)") ? list.get(0) : list.get(1);
        //起始终止站
        startingStation();
        //序列号
        serialNum();
        //身份证
        idNum();
        int index = 0;
        for (String str : list) {
            dateTime(str);
            ticketRates(str);
            ticketLocation(str);
            seat(str);
            seatCategory(str);
            ticketGate(str);
            //用户名称
            if (StringUtil.isNotEmpty(idNum) && str.contains(idNum)) {
                if (!str.equals(idNum)) {
                    if (isAllChinese(str.substring(idNum.length()))) {
                        name = str.substring(idNum.length());
                    }
                } else if (index + 1 < list.size()) {
                    if (isAllChinese(list.get(index + 1))) {
                        name = list.get(index + 1);
                    } else if (isAllChinese(list.get(index - 1))) {
                        name = list.get(index - 1);
                    }
                }
            }
            index++;
        }

    }

    private void startingStation() {
        if (StringUtil.isEmpty(startingStation)) {
            int index = 0;
            List<String> strList = this.getStrList();
            int size = strList.size();

            for (String str : strList) {
                if (isAllChinese(str) && index + 2 < size && isTrainNum(strList.get(index + 1))
                        && (isAllChinese(strList.get(index + 2)) || isAllCaracter(strList.get(index + 2)))) {
                    startingStation = str.equals("站") ? strList.get(index - 1) + str : str;
                    trainNum = strList.get(index + 1);
                    destinationStation = strList.get(index + 2);
                    if (isAllCaracter(strList.get(index + 2))) {
                        String[] split = strList.get(index + 1).split("次");
                        trainNum = split.length > 0 ? split[0] : null;
                        destinationStation = split.length > 1 ? split[1] : null;
                    }
                    break;
                }

                index++;
            }
            //当顺序不正常，做一下补充填写
            int firstPY = 0;
            for (int i = 0; i < strList.size(); i++) {
                if (isAllCaracter(strList.get(i))) {
                    firstPY = i;
                    break;
                }
            }
            if (StringUtil.isEmpty(destinationStation) && firstPY > 0) {
                for (String s : strList.subList(firstPY - 3, firstPY)) {
                    if (containsNumber(s)) {
                        trainNum = s;
                    } else {
                        // 第一个非数字字符串作为出发站，第二个作为目的站
                        if (StringUtil.isEmpty(startingStation)) {
                            startingStation = s;
                        } else {
                            destinationStation = s;
                        }
                    }
                }
            }
        }
    }

    private void dateTime(String str) {
        if (dateTime == null && str.matches("\\d{4}年\\d{1,2}月\\d{1,2}日\\d{1,2}[:：]\\d{1,2}")) {
            try {
                String[] split = str.split("年");
                String year = split[0];
                String[] split2 = split[1].split("月");
                String month = String.format("%02d", Integer.parseInt(split2[0]));
                String[] split3 = split2[1].split("日");
                String day = String.format("%02d", Integer.parseInt(split3[0]));
                String[] split4 = split3[1].contains(":") ? split3[1].split(":") : split3[1].split("：");
                String hh = String.format("%02d", Integer.parseInt(split4[0]));
                String mmStr = split4[1].length() > 2 || split4[1].contains("开") ? split4[1].split("开")[0] : split4[1];
                String mm = String.format("%02d", Integer.parseInt(mmStr));
                String bd = year + "-" + month + "-" + day + " " + hh + ":" + mm;
                dateTime = DateUtil.checkDate(bd, "yyyy-MM-dd HH:mm").getTime();
            } catch (Exception e) {
                log.error("火车票解析错误：" + e.getMessage());
            }
        }
    }

    private void ticketRates(String str) {
        if (StringUtil.isEmpty(ticketRates) && StringUtil.isNotEmpty(str) && str.matches("[￥¥#]?\\d+(?:\\.\\d+)?")) {
            ticketRates = str.replaceAll("[￥¥#元]", "");
        }
    }

    private void ticketLocation(String str) {
        if (StringUtil.isEmpty(ticketLocation) && str.endsWith("售")) {
            ticketLocation = str.substring(0, str.length() - 1);
        }
    }

    private void serialNum() {
        if (StringUtil.isEmpty(serialNum)) {
            List<String> strList = this.getStrList();
            int size = strList.size();
            for (int i = size - 1; i >= 0; i--) {
                if (isNumAndCarater(strList.get(i)) && strList.get(i).length() > 4) {
                    serialNum = strList.get(i);
                    return;
                }
            }
        }
    }

    private void seat(String str) {
        if (StringUtil.isEmpty(seat) && str.matches("\\d{1,2}车(?:\\d{1,3}[A-Z]?号?|无座)(?:下铺|上铺|中铺|硬座|硬卧|软卧)?")) {
            seat = str;
        }
    }

    private void seatCategory(String str) {
        String regex = "(?:新空调)?(?:软卧|硬卧|软座|硬座|商务座|一等座|二等座|特等座|动卧|一等卧|二等卧|高级动卧|无座|站票)(?:普快)?";
        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (StringUtil.isEmpty(seatCategory) || matcher.matches()) {
            seatCategory = str;
        }
    }

    private void idNum() {
        if (StringUtil.isEmpty(idNum)) {
            String regex = "[1-9]\\d{5}(?:\\d{4}|\\*{4})(?:\\d{4}|\\*{4})\\d{3}[0-9Xx]";
            Matcher matcher = Pattern.compile(regex).matcher(this.getContent());
            if (matcher.find()) {
                idNum = matcher.group();
            }

        }
    }

    private void ticketGate(String str) {
        if (StringUtil.isEmpty(ticketGate) && (str.contains("检票") || str.contains("候车"))) {
            ticketGate = str;
        }
    }

    private boolean isAllChinese(String str) {
        return str.matches("[一-龥]+");
    }

    private boolean isAllCaracter(String str) {
        return str.matches("[a-zA-Z]+");
    }

    private boolean isNumAndCarater(String str) {
        return str.matches("[0-9a-zA-Z]+");
    }

    private boolean isTrainNum(String str) {
        return str.matches("([GCDZTK]?\\d{1,4})(次)?(?:\\s*(\\S+))?");
    }

    public static boolean containsNumber(String str) {
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }
}
