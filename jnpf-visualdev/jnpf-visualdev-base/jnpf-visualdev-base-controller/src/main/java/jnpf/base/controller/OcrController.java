package jnpf.base.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.model.ocr.OcrForm;
import jnpf.base.model.ocr.OcrModel;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.util.FileUploadUtils;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ocr解析
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@Slf4j
@Tag(name = "ocr解析", description = "ocr")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/ocr")
public class OcrController {

    private final ConfigValueUtil configValueUtil;

    @Operation(summary = "ocr解析")
    @PostMapping
    public ActionResult<OcrModel> form(@RequestBody OcrForm form) {
        String url = form.getUrl();
        String substring = url.substring(url.lastIndexOf("/") + 1);
        File file = FileUploadUtils.downloadFileToLocal(new FileParameter(FileTypeConstant.ANNEXPIC, substring));
        try (FileInputStream ins = new FileInputStream(file)) {
            byte[] byteArray = IOUtils.toByteArray(ins);
            String str = Base64.getEncoder().encodeToString(byteArray);
            JSONObject body = new JSONObject();
            List<String> list = new ArrayList<>();
            list.add(str);
            body.put("images", list);
            String ocrUrl = configValueUtil.getOcrUrl();
            if (StringUtil.isEmpty(ocrUrl)) {
                return ActionResult.fail(MsgCode.SYS183.get());
            }
            if (ocrUrl.endsWith("/")) {
                ocrUrl = ocrUrl.substring(0, ocrUrl.length() - 1);
            }
            OcrModel ocrModel = OcrModel.get(form.getType());
            ocrModel.extract(ocrUrl, body);
            return ActionResult.success(ocrModel);
        } catch (DataException e) {
            log.error(e.getMessage(), e);
            return ActionResult.fail(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ActionResult.fail(MsgCode.FA104.get());
    }
}
