package com.bodyplan.module.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.training.dto.ExerciseResponse;
import com.bodyplan.module.training.dto.ExerciseTagResponse;
import com.bodyplan.module.training.entity.Exercise;
import com.bodyplan.module.training.entity.ExerciseTag;
import com.bodyplan.module.training.mapper.ExerciseMapper;
import com.bodyplan.module.training.mapper.ExerciseTagMapper;
import com.bodyplan.module.training.support.TagValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户端动作库查询（只暴露上架动作）与标签字典。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseQueryService {

    private static final int STATUS_ENABLED = 1;
    private static final long DEFAULT_PAGE_SIZE = 10;
    private static final long MAX_PAGE_SIZE = 100;

    private final ExerciseMapper exerciseMapper;
    private final ExerciseTagMapper exerciseTagMapper;

    public PageResult<ExerciseResponse> list(long page,
                                             long size,
                                             String part,
                                             String sportType,
                                             String scene,
                                             String equipment,
                                             Integer difficulty,
                                             String keyword) {
        long normalizedPage = Math.max(page, 1);
        long normalizedSize = Math.min(Math.max(size <= 0 ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, STATUS_ENABLED)
                .eq(StringUtils.hasText(part), Exercise::getPart, part)
                .eq(StringUtils.hasText(sportType), Exercise::getSportType, sportType)
                .eq(StringUtils.hasText(scene), Exercise::getScene, scene)
                .eq(StringUtils.hasText(equipment), Exercise::getEquipment, equipment)
                .eq(difficulty != null, Exercise::getDifficulty, difficulty)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(Exercise::getName, keyword)
                        .or().like(Exercise::getSuitableFor, keyword)
                        .or().like(Exercise::getKeyPoints, keyword))
                .orderByAsc(Exercise::getSortNo)
                .orderByAsc(Exercise::getId);
        Page<Exercise> result = exerciseMapper.selectPage(new Page<>(normalizedPage, normalizedSize), wrapper);
        log.info("exercise list queried, scenario=exercise-list, page={}, size={}, part={}, sportType={}, scene={}, total={}",
                normalizedPage, normalizedSize, part, sportType, scene, result.getTotal());
        return PageResult.of(result.getRecords().stream().map(this::toResponse).toList(),
                normalizedPage, normalizedSize, result.getTotal());
    }

    public ExerciseResponse detail(Long id) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null || exercise.getStatus() == null || exercise.getStatus() != STATUS_ENABLED) {
            log.warn("exercise not available, scenario=exercise-detail, exerciseId={}", id);
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND, "动作不存在或已下架");
        }
        return toResponse(exercise);
    }

    public List<ExerciseTagResponse> tags(String tagType) {
        List<ExerciseTag> tags = exerciseTagMapper.selectList(new LambdaQueryWrapper<ExerciseTag>()
                .eq(ExerciseTag::getStatus, STATUS_ENABLED)
                .eq(StringUtils.hasText(tagType), ExerciseTag::getTagType, tagType)
                .orderByAsc(ExerciseTag::getTagType)
                .orderByAsc(ExerciseTag::getSortNo)
                .orderByAsc(ExerciseTag::getId));
        return tags.stream()
                .map(tag -> new ExerciseTagResponse(tag.getId(), tag.getTagType(), tag.getTagCode(), tag.getTagName(), tag.getSortNo()))
                .toList();
    }

    public ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getPart(),
                exercise.getSportType(),
                exercise.getDifficulty(),
                exercise.getScene(),
                exercise.getEquipment(),
                TagValues.split(exercise.getContraFlags()),
                exercise.getVideoUrl(),
                exercise.getCoverUrl(),
                exercise.getKeyPoints(),
                exercise.getCautions(),
                exercise.getSuitableFor(),
                exercise.getStatus(),
                exercise.getSortNo()
        );
    }
}
