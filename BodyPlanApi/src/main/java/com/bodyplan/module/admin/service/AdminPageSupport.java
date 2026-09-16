package com.bodyplan.module.admin.service;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;

/**
 * 管理端分页参数保护，避免非法 page/size 打到数据库。
 */
public final class AdminPageSupport {

    /** 列表默认每页条数 */
    public static final long DEFAULT_SIZE = 10L;

    /** 列表每页最大条数 */
    public static final long MAX_SIZE = 100L;

    private AdminPageSupport() {
    }

    /** 规范化页码：空值取 1，小于 1 抛 BAD_REQUEST */
    public static long normalizePage(Long page) {
        if (page == null) {
            return 1L;
        }
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "页码不能小于 1");
        }
        return page;
    }

    /** 规范化每页条数：空值取默认值，超出上限抛 BAD_REQUEST */
    public static long normalizeSize(Long size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每页条数取值范围：1-" + MAX_SIZE);
        }
        return size;
    }
}
