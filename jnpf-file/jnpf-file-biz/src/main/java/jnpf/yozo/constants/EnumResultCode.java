package jnpf.yozo.constants;

public enum EnumResultCode {
    E_SUCCESS(0, "操作成功"),
    E_GENERATE_SIGN_FAIL(1000, "获取签名失败");

    private Integer value;
    private String info;

    private EnumResultCode(Integer value, String info) {
        this.value = value;
        this.info = info;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getInfo() {
        return this.info;
    }

    public static EnumResultCode getEnum(Integer value) {
        EnumResultCode[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            EnumResultCode code = var1[var3];
            if (code.getValue().equals(value)) {
                return code;
            }
        }

        return null;
    }
}
