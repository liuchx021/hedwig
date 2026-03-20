package com.blueship581.hedwig.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一业务错误码枚举。
 * <p>
 * 编码规则：
 * <ul>
 *   <li>10xxx — 通用 / 系统级</li>
 *   <li>20xxx — 认证 & 授权</li>
 *   <li>30xxx — 厂商集成</li>
 *   <li>40xxx — 业务数据（血糖、监测对象等）</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    // ==================== 10xxx 通用 ====================
    SUCCESS(0, HttpStatus.OK, "操作成功"),
    INTERNAL_ERROR(10000, HttpStatus.INTERNAL_SERVER_ERROR, "系统出现异常，请稍后重试"),
    BAD_REQUEST(10001, HttpStatus.BAD_REQUEST, "请求参数错误"),
    PARAM_MISSING(10002, HttpStatus.BAD_REQUEST, "缺少必要参数"),
    PARAM_INVALID(10003, HttpStatus.BAD_REQUEST, "参数格式不正确"),
    RESOURCE_NOT_FOUND(10004, HttpStatus.NOT_FOUND, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(10005, HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方式"),
    UNSUPPORTED_MEDIA_TYPE(10006, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的媒体类型"),
    TOO_MANY_REQUESTS(10007, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试"),

    // ==================== 20xxx 认证 & 授权 ====================
    UNAUTHORIZED(20000, HttpStatus.UNAUTHORIZED, "未登录或登录已过期，请重新登录"),
    BAD_CREDENTIALS(20001, HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    ACCESS_DENIED(20002, HttpStatus.FORBIDDEN, "没有权限执行此操作"),
    TOKEN_EXPIRED(20003, HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"),
    TOKEN_INVALID(20004, HttpStatus.UNAUTHORIZED, "无效的访问令牌"),
    USER_ALREADY_EXISTS(20005, HttpStatus.BAD_REQUEST, "用户名已存在"),
    USER_NOT_FOUND(20006, HttpStatus.NOT_FOUND, "用户不存在"),

    // ==================== 30xxx 厂商集成 ====================
    VENDOR_ERROR(30000, HttpStatus.BAD_GATEWAY, "上游厂商服务异常，请稍后重试"),
    VENDOR_TOKEN_INVALID(30001, HttpStatus.BAD_REQUEST, "访问令牌无效或已过期，请重新获取后再连接"),
    VENDOR_LOGIN_FAILED(30002, HttpStatus.BAD_REQUEST, "厂商账号登录失败"),
    VENDOR_API_EMPTY(30003, HttpStatus.BAD_GATEWAY, "厂商接口返回为空"),
    VENDOR_API_ERROR(30004, HttpStatus.BAD_GATEWAY, "厂商接口返回异常"),
    VENDOR_UNSUPPORTED(30005, HttpStatus.BAD_REQUEST, "不支持的厂商类型"),
    VENDOR_CONNECTION_NOT_FOUND(30006, HttpStatus.NOT_FOUND, "未找到对应的厂商连接"),

    // ==================== 40xxx 业务数据 ====================
    GLUCOSE_NO_DATA(40000, HttpStatus.NOT_FOUND, "暂无血糖数据"),
    SUBJECT_NOT_FOUND(40001, HttpStatus.NOT_FOUND, "未找到对应的监测对象"),
    NIGHTSCOUT_SYNC_DISABLED(40002, HttpStatus.BAD_REQUEST, "Nightscout 同步未启用"),
    NIGHTSCOUT_TARGET_NOT_FOUND(40003, HttpStatus.NOT_FOUND, "Nightscout 推送目标不存在"),
    NIGHTSCOUT_NAME_DUPLICATE(40004, HttpStatus.BAD_REQUEST, "Nightscout 目标名称已存在"),
    NIGHTSCOUT_PUSH_FAILED(40005, HttpStatus.BAD_GATEWAY, "Nightscout 推送失败"),
    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
