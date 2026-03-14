package jnpf.base.util.custom;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;

import java.util.Properties;

public enum VelocityEnum {
    INIT, OTHER;

    public void initVelocity(String path) {
        Properties p = new Properties();
        p.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, path);
        p.setProperty("ISO-8859-1", StringPool.UTF_8);
        p.setProperty("output.encoding", StringPool.UTF_8);
        Velocity.init(p);
    }

}
