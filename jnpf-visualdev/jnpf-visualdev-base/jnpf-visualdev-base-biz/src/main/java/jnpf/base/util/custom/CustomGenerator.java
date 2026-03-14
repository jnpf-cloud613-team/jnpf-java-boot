package jnpf.base.util.custom;

import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.engine.AbstractTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@Slf4j
public class CustomGenerator {

    private ConfigBuilder config;
    private InjectionConfig injectionConfig;
    private DataSourceConfig dataSource;
    private StrategyConfig strategy;
    private PackageConfig packageInfo;
    private GlobalConfig globalConfig;
    private AbstractTemplateEngine templateEngine;

    private Map<String, Object> customParams;

    public CustomGenerator(Map<String, Object> customParams) {
        this.customParams = customParams;
    }

    public void execute(String path) {
        if (null == this.config) {
            this.config = new ConfigBuilder(packageInfo, dataSource, strategy, null, globalConfig, injectionConfig);
        }
        if (null == this.templateEngine) {
            if (customParams != null) {
                this.templateEngine = new CustomTemplateEngine(customParams, path);
            } else {
                this.templateEngine = new CustomTemplateEngine(path);
            }
        }
        this.templateEngine.init(this.config).batchOutput().open();
    }

    public DataSourceConfig getDataSource() {
        return this.dataSource;
    }

    public CustomGenerator setDataSource(DataSourceConfig dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public StrategyConfig getStrategy() {
        return this.strategy;
    }

    public CustomGenerator setStrategy(StrategyConfig strategy) {
        this.strategy = strategy;
        return this;
    }

    public PackageConfig getPackageInfo() {
        return this.packageInfo;
    }

    public CustomGenerator setPackageInfo(PackageConfig packageInfo) {
        this.packageInfo = packageInfo;
        return this;
    }

    public ConfigBuilder getConfig() {
        return this.config;
    }

    public CustomGenerator setConfig(ConfigBuilder config) {
        this.config = config;
        return this;
    }

    public GlobalConfig getGlobalConfig() {
        return this.globalConfig;
    }

    public CustomGenerator setGlobalConfig(GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
        return this;
    }

    public InjectionConfig getCfg() {
        return this.injectionConfig;
    }

    public CustomGenerator setCfg(InjectionConfig injectionConfig) {
        this.injectionConfig = injectionConfig;
        return this;
    }

    public AbstractTemplateEngine getTemplateEngine() {
        return this.templateEngine;
    }

    public CustomGenerator setTemplateEngine(AbstractTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        return this;
    }
}
