package jnpf.base.util.common;

import jnpf.emnus.ModuleTypeEnum;
import jnpf.util.XSSEscape;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

@Slf4j
public class SuperQueryUtil {
    SuperQueryUtil() {
    }

    public static void createJsFile(String data, String path, String jsFileType) {
        path = XSSEscape.escapePath(path);
        String content = "const " + jsFileType + " = " + data;
        File jsFile = new File(path);
        try (Writer writer = new FileWriter(jsFile);) {
            writer.write(content);
            writer.write(System.getProperty("line.separator"));
            writer.write("export default " + jsFileType);
        } catch (IOException e) {
            log.error("代码生成创建js文件失败：" + e.getMessage(), e);
        }
    }

    public static void createFlowFormJsonFile(String data, String path) {
        try {
            File file = new File(XSSEscape.escapePath(path + File.separator + "returnForm." + ModuleTypeEnum.VISUAL_DEV.getTableName()));
            boolean b = file.createNewFile();
            if (b) {
                @Cleanup Writer out = new FileWriter(file);
                out.write(data);
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
