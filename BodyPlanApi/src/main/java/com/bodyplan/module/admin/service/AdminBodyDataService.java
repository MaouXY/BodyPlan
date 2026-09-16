package com.bodyplan.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.admin.dto.bodydata.AdminBodyDataItemResponse;
import com.bodyplan.module.profile.entity.BodyData;
import com.bodyplan.module.profile.mapper.BodyDataMapper;
import com.bodyplan.module.user.entity.UserAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 管理端身体数据查看服务：只读，按记录时间倒序分页。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBodyDataService {

    private final BodyDataMapper bodyDataMapper;
    private final AdminUserService adminUserService;

    /**
     * 分页查询身体数据记录。
     *
     * @param page   页码，从 1 开始
     * @param size   每页条数
     * @param userId 用户ID过滤，可空
     */
    public PageResult<AdminBodyDataItemResponse> listBodyData(Long page, Long size, Long userId) {
        long current = AdminPageSupport.normalizePage(page);
        long pageSize = AdminPageSupport.normalizeSize(size);

        Page<BodyData> bodyDataPage = bodyDataMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<BodyData>()
                        .eq(userId != null, BodyData::getUserId, userId)
                        .orderByDesc(BodyData::getRecordedAt)
                        .orderByDesc(BodyData::getId));

        List<BodyData> records = bodyDataPage.getRecords();
        List<Long> userIds = records.stream().map(BodyData::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserAccount> accountMap = adminUserService.selectAccounts(userIds);

        List<AdminBodyDataItemResponse> items = records.stream()
                .map(record -> {
                    UserAccount account = accountMap.get(record.getUserId());
                    return new AdminBodyDataItemResponse(
                            record.getId(),
                            record.getUserId(),
                            account == null ? null : account.getUsername(),
                            account == null ? null : account.getNickname(),
                            record.getHeightCm(),
                            record.getWeightKg(),
                            record.getBmi(),
                            record.getBodyType(),
                            record.getScore(),
                            record.getRemark(),
                            record.getRecordedAt()
                    );
                })
                .toList();

        log.info("admin body data list queried, scenario=admin-body-data-list, page={}, size={}, userId={}, total={}",
                current, pageSize, userId, bodyDataPage.getTotal());
        return PageResult.of(items, current, pageSize, bodyDataPage.getTotal());
    }
}
