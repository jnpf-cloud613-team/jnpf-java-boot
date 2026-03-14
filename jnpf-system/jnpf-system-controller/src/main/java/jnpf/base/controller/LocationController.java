package jnpf.base.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.model.province.MapParams;
import jnpf.constant.MsgCode;
import jnpf.util.NoDataSourceBind;
import jnpf.util.ServletUtil;
import jnpf.util.wxutil.HttpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Tag(name = "定位转发接口", description = "location")
@RestController
@RequestMapping("/api/system/Location")
public class LocationController {

    @Operation(summary = "查询附近数据")
    @GetMapping("/around")
    @NoDataSourceBind
    public ActionResult<JSONObject> getAroundList(MapParams params) {
        JSONObject rstObj;
        String url = "https://restapi.amap.com/v3/place/around?key=" + params.getKey() + "&location=" + params.getLocation()
                + "&radius=" + params.getRadius() + "&offset=" + params.getOffset() + "&page=" + params.getPage();
        try {
            rstObj = HttpUtil.httpRequest(url, "GET", null);
        } catch (Exception e) {
            return ActionResult.fail(MsgCode.SYS023.get());
        }
        if (rstObj == null) {
            return ActionResult.fail(MsgCode.SYS024.get());
        }
        return ActionResult.success(rstObj);
    }

    @Operation(summary = "根据关键字查询附近数据")
    @GetMapping("/text")
    @NoDataSourceBind
    public ActionResult<JSONObject> getTextList(MapParams params) {
        JSONObject rstObj;
        String url = "https://restapi.amap.com/v3/place/text?key=" + params.getKey() + "&keywords=" + params.getKeywords()
                + "&radius=" + params.getRadius() + "&offset=" + params.getOffset() + "&page=" + params.getPage();
        try {
            rstObj = HttpUtil.httpRequest(url, "GET", null);
        } catch (Exception e) {
            return ActionResult.fail(MsgCode.SYS023.get());
        }
        if (rstObj == null) {
            return ActionResult.fail(MsgCode.SYS024.get());
        }
        return ActionResult.success(rstObj);
    }

    @Operation(summary = "输入提示")
    @GetMapping("/inputtips")
    @NoDataSourceBind
    public ActionResult<JSONObject> getInputTips(MapParams params) {
        JSONObject rstObj;
        String url = "https://restapi.amap.com/v3/assistant/inputtips?key=" + params.getKey() + "&keywords=" + params.getKeywords();
        try {
            rstObj = HttpUtil.httpRequest(url, "GET", null);
        } catch (Exception e) {
            return ActionResult.fail(MsgCode.SYS023.get());
        }
        if (rstObj == null) {
            return ActionResult.fail(MsgCode.SYS024.get());
        }
        return ActionResult.success(rstObj);
    }

    @Operation(summary = "定位图片")
    @GetMapping("/staticmap")
    @NoDataSourceBind
    public void staticmap(MapParams params) {
        String requestUrl = "https://restapi.amap.com/v3/staticmap?location=" + params.getLocation() + "&zoom=" + params.getZoom() + "&size="
                + params.getSize() + "&key=" + params.getKey();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(50000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Content-Type", "image/png");
            HttpServletResponse response = ServletUtil.getResponse();
            InputStream ins = conn.getInputStream();
            OutputStream os = response.getOutputStream();

            try {
                BufferedImage image = ImageIO.read(ins);
                assert os !=null;
                if (image != null) {
                    ImageIO.write(image, "png", os);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                conn.disconnect();
                os.flush();
                os.close();
                ins.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Operation(summary = "经纬度转地址")
    @GetMapping("/regeo")
    @NoDataSourceBind
    public ActionResult<JSONObject> regeo(MapParams params) {
        JSONObject rstObj;
        String url = "https://restapi.amap.com/v3/geocode/regeo?key=" + params.getKey() + "&&location=" + params.getLocation();
        try {
            rstObj = HttpUtil.httpRequest(url, "GET", null);
        } catch (Exception e) {
            return ActionResult.fail(MsgCode.SYS023.get());
        }
        if (rstObj == null) {
            return ActionResult.fail(MsgCode.SYS024.get());
        }
        return ActionResult.success(rstObj);
    }
}
