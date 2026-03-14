package jnpf.base.util.custom;


import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.config.ConstVal;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.engine.AbstractTemplateEngine;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;
import java.util.Properties;

public class CustomTemplateEngine extends AbstractTemplateEngine {

    private static final String DOT_VM = ".vm";
    private VelocityEngine velocityEngine;

    private Map<String, Object> customParams;

    private String path;

    public CustomTemplateEngine(String path) {
        this.path = path;
    }

    public CustomTemplateEngine(Map<String, Object> customParams, String path) {
        this.customParams = customParams;
        this.path = path;
    }

    @Override
    public CustomTemplateEngine init(ConfigBuilder configBuilder) {
        super.setConfigBuilder(configBuilder);
        if (null == this.velocityEngine) {
            Properties p = new Properties();
            p.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, path);
            p.setProperty("ISO-8859-1", StringPool.UTF_8);
            p.setProperty("output.encoding", StringPool.UTF_8);
            this.velocityEngine = new VelocityEngine(p);
        }

        return this;
    }

    @Override
    public String writer(@NotNull Map<String, Object> objectMap, @NotNull String templateName, @NotNull String templateString) throws Exception {
        StringWriter writer = new StringWriter();
        this.velocityEngine.evaluate(new VelocityContext(objectMap), writer, templateName, templateString);
        return writer.toString();
    }

    @Override
    public void writer(Map<String, Object> objectMap, String templatePath, File outputFile) throws Exception {
        if (templatePath == null || templatePath.trim().isEmpty()) {
            return;
        }

        Template template = this.velocityEngine.getTemplate(templatePath, ConstVal.UTF8);
        // 使用 try-with-resources 自动关闭资源
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, ConstVal.UTF8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            if (customParams != null) {
                objectMap.putAll(customParams);
            }

            template.merge(new VelocityContext(objectMap), writer);
        }
    }

    @Override
    public String templateFilePath(String filePath) {
        if (filePath == null) {
            return "";
        }
        if (!filePath.contains(DOT_VM)) {
            StringBuilder fp = new StringBuilder();
            fp.append(filePath).append(DOT_VM);
            return fp.toString();
        } else {
            return filePath;
        }
    }
}
