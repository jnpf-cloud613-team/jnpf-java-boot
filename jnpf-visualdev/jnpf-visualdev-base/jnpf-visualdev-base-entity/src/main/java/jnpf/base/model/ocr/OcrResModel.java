package jnpf.base.model.ocr;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OcrResModel {
    private String text;
    private BigDecimal confidence;
    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;
}
