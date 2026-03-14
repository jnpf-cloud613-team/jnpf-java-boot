package jnpf.base.model.province;

import lombok.Data;

@Data
public class AtlasFeaturesModel {
    private String type;
    private AtlasPropModel properties;
    private Object geometry;
}
