package com.bodyplan.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseTagResponse;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseTagSaveRequest;
import com.bodyplan.module.training.entity.ExerciseTag;
import com.bodyplan.module.training.mapper.ExerciseTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端动作标签管理服务：列表、新增、更新、删除（字典项无引用，物理删除）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExerciseTagService {

    /** 标签编码最大长度（与 exercise_tag.tag_code VARCHAR(40) 对齐） */
    private static final int CODE_MAX_LENGTH = 40;

    /** 标签名称最大长度（与 exercise_tag.tag_name VARCHAR(50) 对齐） */
    private static final int NAME_MAX_LENGTH = 50;

    /** 状态：启用 */
    private static final int STATUS_ON = 1;

    private final ExerciseTagMapper exerciseTagMapper;

    /**
     * 标签列表，按 tagType、sortNo 升序。
     *
     * @param tagType 标签类型，可空（PART/SPORT_TYPE/SCENE/EQUIPMENT/DIFFICULTY）
     */
    public List<AdminExerciseTagResponse> listTags(String tagType) {
        AdminEnumValidator.validateTagType(tagType);
        String trimmedType = StringUtils.hasText(tagType) ? tagType.trim() : null;

        List<ExerciseTag> tags = exerciseTagMapper.selectList(new LambdaQueryWrapper<ExerciseTag>()
                .eq(trimmedType != null, ExerciseTag::getTagType, trimmedType)
                .orderByAsc(ExerciseTag::getTagType)
                .orderByAsc(ExerciseTag::getSortNo)
                .orderByAsc(ExerciseTag::getId));

        List<AdminExerciseTagResponse> items = tags.stream().map(this::toResponse).toList();
        log.info("admin exercise tag list queried, scenario=admin-exercise-tag-list, tagType={}, total={}",
                trimmedType, items.size());
        return items;
    }

    /**
     * 新增标签，(tagType, tagCode) 重复时抛 CONFLICT。
     */
    public AdminExerciseTagResponse createTag(AdminExerciseTagSaveRequest request) {
        AdminEnumValidator.validateTagType(request.tagType());
        if (!StringUtils.hasText(request.tagType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签类型不能为空");
        }
        String tagType = request.tagType().trim();
        String tagCode = normalizeCode(request.tagCode());
        String tagName = normalizeName(request.tagName());

        if (existsSameCode(tagType, tagCode, null)) {
            log.warn("admin exercise tag create conflict, scenario=admin-exercise-tag-create, tagType={}, tagCode={}",
                    tagType, tagCode);
            throw new BusinessException(ErrorCode.CONFLICT, "同类型下已存在相同编码的标签：" + tagCode);
        }

        ExerciseTag tag = new ExerciseTag();
        tag.setTagType(tagType);
        tag.setTagCode(tagCode);
        tag.setTagName(tagName);
        tag.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        tag.setStatus(request.status() == null ? STATUS_ON : request.status());

        exerciseTagMapper.insert(tag);
        AdminExerciseTagResponse response = toResponse(exerciseTagMapper.selectById(tag.getId()));
        log.info("admin exercise tag created, scenario=admin-exercise-tag-create, tagId={}, tagType={}, tagCode={}",
                response.id(), response.tagType(), response.tagCode());
        return response;
    }

    /**
     * 更新标签，标签不存在抛 EXERCISE_TAG_NOT_FOUND。
     */
    public AdminExerciseTagResponse updateTag(Long tagId, AdminExerciseTagSaveRequest request) {
        ExerciseTag existing = exerciseTagMapper.selectById(tagId);
        if (existing == null) {
            log.warn("admin exercise tag update failed, scenario=admin-exercise-tag-update, tagId={}, reason=tag-not-found", tagId);
            throw new BusinessException(ErrorCode.EXERCISE_TAG_NOT_FOUND, "动作标签不存在");
        }

        if (request.tagType() != null && StringUtils.hasText(request.tagType())) {
            AdminEnumValidator.validateTagType(request.tagType());
        }
        String tagType = StringUtils.hasText(request.tagType()) ? request.tagType().trim() : existing.getTagType();
        String tagCode = normalizeCode(request.tagCode());
        String tagName = normalizeName(request.tagName());

        if (existsSameCode(tagType, tagCode, tagId)) {
            log.warn("admin exercise tag update conflict, scenario=admin-exercise-tag-update, tagId={}, tagType={}, tagCode={}",
                    tagId, tagType, tagCode);
            throw new BusinessException(ErrorCode.CONFLICT, "同类型下已存在相同编码的标签：" + tagCode);
        }

        ExerciseTag tag = new ExerciseTag();
        tag.setId(tagId);
        tag.setTagType(tagType);
        tag.setTagCode(tagCode);
        tag.setTagName(tagName);
        tag.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        tag.setStatus(request.status() == null ? STATUS_ON : request.status());

        exerciseTagMapper.updateById(tag);
        AdminExerciseTagResponse response = toResponse(exerciseTagMapper.selectById(tagId));
        log.info("admin exercise tag updated, scenario=admin-exercise-tag-update, tagId={}, tagType={}, tagCode={}",
                tagId, response.tagType(), response.tagCode());
        return response;
    }

    /**
     * 删除标签（字典项，无引用关系，直接物理删除）。
     */
    public void deleteTag(Long tagId) {
        ExerciseTag existing = exerciseTagMapper.selectById(tagId);
        if (existing == null) {
            log.warn("admin exercise tag delete failed, scenario=admin-exercise-tag-delete, tagId={}, reason=tag-not-found", tagId);
            throw new BusinessException(ErrorCode.EXERCISE_TAG_NOT_FOUND, "动作标签不存在");
        }
        exerciseTagMapper.deleteById(tagId);
        log.info("admin exercise tag deleted, scenario=admin-exercise-tag-delete, tagId={}, tagType={}, tagCode={}",
                tagId, existing.getTagType(), existing.getTagCode());
    }

    private boolean existsSameCode(String tagType, String tagCode, Long excludeId) {
        return exerciseTagMapper.selectCount(new LambdaQueryWrapper<ExerciseTag>()
                .eq(ExerciseTag::getTagType, tagType)
                .eq(ExerciseTag::getTagCode, tagCode)
                .ne(excludeId != null, ExerciseTag::getId, excludeId)) > 0;
    }

    private String normalizeCode(String tagCode) {
        String trimmed = tagCode == null ? null : tagCode.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签编码不能为空");
        }
        if (trimmed.length() > CODE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签编码长度不能超过 " + CODE_MAX_LENGTH + " 个字符");
        }
        return trimmed;
    }

    private String normalizeName(String tagName) {
        String trimmed = tagName == null ? null : tagName.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称不能为空");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名称长度不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        return trimmed;
    }

    private AdminExerciseTagResponse toResponse(ExerciseTag tag) {
        return new AdminExerciseTagResponse(
                tag.getId(),
                tag.getTagType(),
                tag.getTagCode(),
                tag.getTagName(),
                tag.getSortNo(),
                tag.getStatus()
        );
    }
}
