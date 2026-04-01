package com.itheima.dormrepairsystemspringboot.common;


import lombok.Data;

//前端给了登录信息、注册信息后，正确与否后端就返回这个result，不同的报错情况对应不同的状态码
//Result这个类其实只是一个告知信息的作用，只返回状态码，其实不做判断是否报错、token是否正确的操作
@Data
public class Result {

    private Integer code;   //响应码
    private String msg;
    private Object data;  //object - 万能变量啥都能存

    //无参构造（必须）,调用时new
    public Result() {}

    //成功响应① 查询列表、查询详情、登录成功（返回用户信息和 token）的时候只需要返回数据，把（）参数返回继续操作
    public static Result success(Object data) {
        Result r = new Result();         //要在每一个方法里面都new一个，避免对同个对象同时多次请求的时候新的请求覆盖旧的请求
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    //成功响应②   注册成功、修改成功、删除成功、绑定宿舍成功，不带数据，只返回提示
    public static Result success(String msg) {
        Result r = new Result();
        r.setCode(200);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }

    //错误响应
    public static Result error(String msg) {
        Result r = new Result();
        r.setCode(500);
        r.setMsg(msg);
        r.setData(null);
        return r;
    }

}