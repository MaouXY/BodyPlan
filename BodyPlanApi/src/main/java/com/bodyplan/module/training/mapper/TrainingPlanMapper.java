package com.bodyplan.module.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bodyplan.module.training.entity.TrainingPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingPlanMapper extends BaseMapper<TrainingPlan> {
}
