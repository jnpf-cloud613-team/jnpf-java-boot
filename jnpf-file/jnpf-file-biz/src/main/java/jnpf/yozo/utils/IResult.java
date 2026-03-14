package jnpf.yozo.utils;

public interface IResult<T> {
    boolean isSuccessCode();

    String getMessage();

    T getData();

    void setData(T var1);
}
