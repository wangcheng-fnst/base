package com.wangc.base.es.exception;

/**
 *  异常信息常量<br>
 *
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public interface ExceptionMessage {

    /**
     *  not found es sql.<br>
     *
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    String ES_SQL_NOT_FOUND = "not found es sql.";

    /**
     *  total size must < 10000<br>
     *
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    String ES_TOO_DEEP_PAGE = "total size must < 10000";

    /**
     *  es properties not set!<br>
     *
     * @see [相关类/方法]（可选）
     * @since [产品/模块版本] （可选）
     */
    String ES_NOT_INIT = "es properties not set!";

    /**
     *  rsf接口调用返回null
     */
    String RSF_RETURN_NULL = "rsf return is null";
}
