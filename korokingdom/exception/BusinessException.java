package com.game.korokingdom.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    private Integer code;

    //默认错误码400
    public BusinessException(String message) {
        super(message);     //存入父类的detailmessage
        this.code = 400;
    }

    //自定义错误码
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}