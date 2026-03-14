package jnpf.base.util;

import com.alibaba.fastjson.JSONObject;
import jnpf.config.ConfigValueUtil;
import jnpf.util.UserProvider;
import jnpf.util.wxutil.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportExportUtil {

    private final ConfigValueUtil configValueUtil;

    private String getFlowAbleUrl(boolean isOld) {
        return isOld ? configValueUtil.getOldReportDomain() : configValueUtil.getReportDomain();
    }

    public List<Object> getExportList(String systemId) {
        List<Object> list = new ArrayList<>();
        try {
            String url = getFlowAbleUrl(false) + "/api/Report/getExportList?systemId=" + systemId;
            JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
            if (jsonObject == null || jsonObject.getInteger("code") == 200) {
                list = (List<Object>) jsonObject.get("data");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return list;
    }

    public boolean importCopy(List<Object> list, String systemId) {
        try {
            String url = getFlowAbleUrl(false) + "/api/Report/importCopy";
            JSONObject body = new JSONObject();
            body.put("list", list);
            body.put("systemId", systemId);
            JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", body.toJSONString(), UserProvider.getToken());
            if (jsonObject == null || jsonObject.getInteger("code") == 200) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public void deleteBySystemId(String systemId) {
        String url = getFlowAbleUrl(false) + "/api/Report/deleteBySystemId?systemId=" + systemId;
        HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
    }

    //++++++++++++++++++++++++++++++++旧报表接口++++++++++++++++++++++++++++++++

    public List<Object> getExportListOld(String systemId) {
        List<Object> list = new ArrayList<>();
        try {
            String url = getFlowAbleUrl(true) + "/Data/getExportList?systemId=" + systemId;
            JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
            if (jsonObject == null || jsonObject.getInteger("code") == 200) {
                list = (List<Object>) jsonObject.get("data");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return list;
    }

    public boolean importCopyOld(List<Object> list, String systemId) {
        try {
            String url = getFlowAbleUrl(true) + "/Data/importCopy";
            JSONObject body = new JSONObject();
            body.put("list", list);
            body.put("systemId", systemId);
            JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", body.toJSONString(), UserProvider.getToken());
            if (jsonObject == null || jsonObject.getInteger("code") == 200) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public void deleteBySystemIdOld(String systemId) {
        String url = getFlowAbleUrl(true) + "/Data/deleteBySystemId?systemId=" + systemId;
        HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
    }
}
