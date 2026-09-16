package com.bodyplan.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseResponse;
import com.bodyplan.module.admin.dto.exercise.AdminExerciseSaveRequest;
import com.bodyplan.module.training.entity.Exercise;
import com.bodyplan.module.training.mapper.ExerciseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理端动作管理服务：分页查询、新增、更新、下架（逻辑下架，不物理删除）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExerciseService {

    /** 动作名称最大长度（与 exercise.name VARCHAR(64) 对齐） */
    private static final int NAME_MAX_LENGTH = 64;

    /** 状态：上架 */
    private static final int STATUS_ON = 1;

    /** 状态：下架 */
    private static final int STATUS_OFF = 0;

    private final ExerciseMapper exerciseMapper;

    /**
     * 分页查询动作列表，按 sortNo 升序、id 升序。
     *
     * @param page       页码，从 1 开始
     * @param size       每页条数
     * @param part       目标部位，可空
     * @param sportType  运动类型，可空
     * @param scene      场景，可空
     * @param equipment  器械，可空
     * @param difficulty 难度（1-3），可空
     * @param keyword    动作名称模糊匹配，可空
     * @param status     状态，可空（1 上架、0 下架）
     */
    public PageResult<AdminExerciseResponse> listExercises(Long page, Long size, String part, String sportType,
                                                          String scene, String equipment, String difficulty,
                                                          String keyword, Integer status) {
        long current = AdminPageSupport.normalizePage(page);
        long pageSize = AdminPageSupport.normalizeSize(size);

        AdminEnumValidator.validatePart(part);
        AdminEnumValidator.validateSportType(sportType);
        AdminEnumValidator.validateScene(scene);
        AdminEnumValidator.validateEquipment(equipment);
        AdminEnumValidator.validateDifficulty(difficulty);
        if (status != null) {
            AdminEnumValidator.validateStatus(status, "动作状态");
        }
        Integer difficultyValue = StringUtils.hasText(difficulty) ? Integer.valueOf(difficulty.trim()) : null;
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<Exercise> exercisePage = exerciseMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<Exercise>()
                        .eq(StringUtils.hasText(part), Exercise::getPart, trimToNull(part))
                        .eq(StringUtils.hasText(sportType), Exercise::getSportType, trimToNull(sportType))
                        .eq(StringUtils.hasText(scene), Exercise::getScene, trimToNull(scene))
                        .eq(StringUtils.hasText(equipment), Exercise::getEquipment, trimToNull(equipment))
                        .eq(difficultyValue != null, Exercise::getDifficulty, difficultyValue)
                        .eq(status != null, Exercise::getStatus, status)
                        .like(trimmedKeyword != null, Exercise::getName, trimmedKeyword)
                        .orderByAsc(Exercise::getSortNo)
                        .orderByAsc(Exercise::getId));

        List<AdminExerciseResponse> items = exercisePage.getRecords().stream().map(this::toResponse).toList();
        log.info("admin exercise list queried, scenario=admin-exercise-list, page={}, size={}, total={}, part={}, sportType={}, status={}",
                current, pageSize, exercisePage.getTotal(), part, sportType, status);
        return PageResult.of(items, current, pageSize, exercisePage.getTotal());
    }

    /**
     * 新增动作。同 part 下动作名称重复时抛 CONFLICT。
     */
    public AdminExerciseResponse createExercise(AdminExerciseSaveRequest request) {
        AdminEnumValidator.validatePart(request.part());
        AdminEnumValidator.validateSportType(request.sportType());
        AdminEnumValidator.validateDifficulty(request.difficulty());
        AdminEnumValidator.validateScene(request.scene());
        AdminEnumValidator.validateEquipment(request.equipment());
        AdminEnumValidator.validateContraFlags(request.contraFlags());

        String name = normalizeName(request.name());
        String part = trimToNull(request.part());
        if (existsSameName(part, name, null)) {
            log.warn("admin exercise create conflict, scenario=admin-exercise-create, name={}, part={}", name, part);
            throw new BusinessException(ErrorCode.CONFLICT, "同一部位下已存在同名动作：" + name);
        }

        Exercise exercise = new Exercise();
        exercise.setName(name);
        applyRequest(exercise, request);

        exerciseMapper.insert(exercise);
        AdminExerciseResponse response = toResponse(exerciseMapper.selectById(exercise.getId()));
        log.info("admin exercise created, scenario=admin-exercise-create, exerciseId={}, name={}, part={}",
                response.id(), response.name(), response.part());
        return response;
    }

    /**
     * 更新动作，动作不存在抛 EXERCISE_NOT_FOUND。
     */
    public AdminExerciseResponse updateExercise(Long exerciseId, AdminExerciseSaveRequest request) {
        Exercise existing = exerciseMapper.selectById(exerciseId);
        if (existing == null) {
            log.warn("admin exercise update failed, scenario=admin-exercise-update, exerciseId={}, reason=exercise-not-found", exerciseId);
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND, "动作不存在");
        }

        AdminEnumValidator.validatePart(request.part());
        AdminEnumValidator.validateSportType(request.sportType());
        AdminEnumValidator.validateDifficulty(request.difficulty());
        AdminEnumValidator.validateScene(request.scene());
        AdminEnumValidator.validateEquipment(request.equipment());
        AdminEnumValidator.validateContraFlags(request.contraFlags());

        String name = normalizeName(request.name());
        String part = trimToNull(request.part());
        if (existsSameName(part, name, exerciseId)) {
            log.warn("admin exercise update conflict, scenario=admin-exercise-update, exerciseId={}, name={}, part={}",
                    exerciseId, name, part);
            throw new BusinessException(ErrorCode.CONFLICT, "同一部位下已存在同名动作：" + name);
        }

        Exercise exercise = new Exercise();
        exercise.setId(exerciseId);
        exercise.setName(name);
        applyRequest(exercise, request);

        exerciseMapper.updateById(exercise);
        AdminExerciseResponse response = toResponse(exerciseMapper.selectById(exerciseId));
        log.info("admin exercise updated, scenario=admin-exercise-update, exerciseId={}, name={}", exerciseId, response.name());
        return response;
    }

    /**
     * 下架动作：仅把 status 置 0，保留数据行（训练计划明细通过 exercise_id 引用动作快照）。
     */
    public void disableExercise(Long exerciseId) {
        Exercise existing = exerciseMapper.selectById(exerciseId);
        if (existing == null) {
            log.warn("admin exercise disable failed, scenario=admin-exercise-disable, exerciseId={}, reason=exercise-not-found", exerciseId);
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND, "动作不存在");
        }

        Exercise updating = new Exercise();
        updating.setId(exerciseId);
        updating.setStatus(STATUS_OFF);
        exerciseMapper.updateById(updating);
        log.info("admin exercise disabled, scenario=admin-exercise-disable, exerciseId={}, previousStatus={}",
                exerciseId, existing.getStatus());
    }

    /**
     * 把请求参数写入实体（不含 name 与 id，name 由调用方单独处理）。
     */
    private void applyRequest(Exercise exercise, AdminExerciseSaveRequest request) {
        exercise.setPart(trimToNull(request.part()));
        exercise.setSportType(trimToNull(request.sportType()));
        exercise.setDifficulty(request.difficulty());
        exercise.setScene(trimToNull(request.scene()));
        exercise.setEquipment(trimToNull(request.equipment()));
        exercise.setContraFlags(AdminCheckinSupport.joinToList(request.contraFlags()));
        exercise.setVideoUrl(trimToNull(request.videoUrl()));
        exercise.setCoverUrl(trimToNull(request.coverUrl()));
        exercise.setKeyPoints(trimToNull(request.keyPoints()));
        exercise.setCautions(trimToNull(request.cautions()));
        exercise.setSuitableFor(trimToNull(request.suitableFor()));
        exercise.setStatus(request.status() == null ? STATUS_ON : request.status());
        exercise.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
    }

    private boolean existsSameName(String part, String name, Long excludeId) {
        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getName, name)
                .ne(excludeId != null, Exercise::getId, excludeId);
        if (part == null) {
            wrapper.isNull(Exercise::getPart);
        } else {
            wrapper.eq(Exercise::getPart, part);
        }
        return exerciseMapper.selectCount(wrapper) > 0;
    }

    private String normalizeName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动作名称不能为空");
        }
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动作名称长度不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private AdminExerciseResponse toResponse(Exercise exercise) {
        return new AdminExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getPart(),
                exercise.getSportType(),
                exercise.getDifficulty(),
                exercise.getScene(),
                exercise.getEquipment(),
                AdminCheckinSupport.splitToArray(exercise.getContraFlags()),
                exercise.getVideoUrl(),
                exercise.getCoverUrl(),
                exercise.getKeyPoints(),
                exercise.getCautions(),
                exercise.getSuitableFor(),
                exercise.getStatus(),
                exercise.getSortNo(),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt()
        );
    }
}
