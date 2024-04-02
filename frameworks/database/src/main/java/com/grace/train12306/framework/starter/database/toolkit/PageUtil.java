package com.grace.train12306.framework.starter.database.toolkit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grace.train12306.framework.starter.common.toolkit.BeanUtil;
import com.grace.train12306.framework.starter.convention.page.PageRequest;
import com.grace.train12306.framework.starter.convention.page.PageResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类
 */
public class PageUtil {

    public static Page convert(PageRequest pageRequest) {
        return convert(pageRequest.getCurrent(), pageRequest.getSize());
    }

    public static Page convert(long current, long size) {
        return new Page(current, size);
    }

    public static PageResponse convert(IPage iPage) {
        return buildConventionPage(iPage);
    }

    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage, Class<TARGET> targetClass) {
        iPage.convert(each -> BeanUtil.convert(each, targetClass));
        return buildConventionPage(iPage);
    }

    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage, Function<? super ORIGINAL, ? extends TARGET> mapper) {
        List<TARGET> targetDataList = iPage.getRecords().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return PageResponse.<TARGET>builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .records(targetDataList)
                .total(iPage.getTotal())
                .build();
    }

    private static PageResponse buildConventionPage(IPage iPage) {
        return PageResponse.builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .records(iPage.getRecords())
                .total(iPage.getTotal())
                .build();
    }
}
