package com.bodyplan.module.nutrition;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import static com.bodyplan.module.nutrition.NutritionModels.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionService {
    private final JdbcTemplate jdbc;

    public PageResult<Food> foods(String keyword, int page, boolean admin) {
        int safePage = Math.max(1, Math.min(page, 10000));
        String term = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String where = " WHERE name LIKE ? " + (admin ? "" : " AND enabled=1 ");
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nutrition_food" + where, Long.class, term);
        List<Food> rows = jdbc.query("SELECT * FROM nutrition_food" + where + " ORDER BY name,id LIMIT 30 OFFSET ?",
                this::food, term, (safePage - 1) * 30);
        return new PageResult<>(rows, safePage, 30, total == null ? 0 : total);
    }

    @Transactional
    public String saveFood(Long id, FoodSave request) {
        if (id == null) {
            id = IdWorker.getId();
            jdbc.update("""
                    INSERT INTO nutrition_food (id,name,category,kcal,protein,fat,carbohydrate,fiber,source,enabled)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, id, request.name().trim(), request.category().trim(), request.kcal(), request.protein(), request.fat(),
                    request.carbohydrate(), request.fiber(), request.source().trim(), request.enabled());
        } else {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM nutrition_food WHERE id=?", Integer.class, id) == 0)
                throw new BusinessException(ErrorCode.NOT_FOUND, "食物不存在");
            jdbc.update("""
                    UPDATE nutrition_food SET name=?,category=?,kcal=?,protein=?,fat=?,carbohydrate=?,fiber=?,source=?,enabled=? WHERE id=?
                    """, request.name().trim(), request.category().trim(), request.kcal(), request.protein(), request.fat(),
                    request.carbohydrate(), request.fiber(), request.source().trim(), request.enabled(), id);
        }
        log.info("food saved, scenario=food-admin-save, operatorId={}, foodId={}", CurrentUserProvider.require().getId(), id);
        return id.toString();
    }

    public Day day(LocalDate date) {
        long userId = CurrentUserProvider.require().getId();
        List<Entry> entries = jdbc.query("SELECT * FROM diet_entry WHERE user_id=? AND entry_date=? ORDER BY created_at,id",
                this::entry, userId, date);
        List<Target> targets = jdbc.query("SELECT * FROM nutrition_target WHERE user_id=? AND target_date=?",
                (rs, row) -> new Target(date, rs.getString("goal"), rs.getInt("kcal"), rs.getInt("protein"),
                        rs.getInt("fat"), rs.getInt("carbohydrate"), rs.getInt("fiber")), userId, date);
        return new Day(date, entries, totals(entries), targets.isEmpty() ? null : targets.get(0));
    }

    @Transactional
    public String saveEntry(Long id, EntrySave request) {
        long userId = CurrentUserProvider.require().getId();
        if (!Set.of("BREAKFAST", "LUNCH", "DINNER", "SNACK").contains(request.meal())
                || request.entryDate().isAfter(LocalDate.now()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "餐次或记录日期不正确");
        if (id != null) requireEntry(id, userId);
        var foods = jdbc.query("SELECT * FROM nutrition_food WHERE id=? AND enabled=1", this::food, request.foodId());
        if (foods.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "该食物已停用，请重新选择");
        Food food = foods.get(0);
        BigDecimal factor = request.grams().divide(BigDecimal.valueOf(100));
        BigDecimal kcal = amount(food.kcal(), factor), protein = amount(food.protein(), factor);
        BigDecimal fat = amount(food.fat(), factor), carbs = amount(food.carbohydrate(), factor), fiber = amount(food.fiber(), factor);
        if (id == null) {
            id = IdWorker.getId();
            jdbc.update("""
                    INSERT INTO diet_entry
                    (id,user_id,entry_date,meal,food_id,food_name,grams,kcal,protein,fat,carbohydrate,fiber,source)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, id, userId, request.entryDate(), request.meal(), food.id(), food.name(), request.grams(),
                    kcal, protein, fat, carbs, fiber, food.source());
        } else {
            jdbc.update("""
                    UPDATE diet_entry SET entry_date=?,meal=?,food_id=?,food_name=?,grams=?,kcal=?,protein=?,fat=?,carbohydrate=?,fiber=?,source=?
                    WHERE id=? AND user_id=?
                    """, request.entryDate(), request.meal(), food.id(), food.name(), request.grams(), kcal, protein, fat,
                    carbs, fiber, food.source(), id, userId);
        }
        log.info("diet saved, scenario=diet-save, userId={}, entryId={}, foodId={}, date={}", userId, id, food.id(), request.entryDate());
        return id.toString();
    }

    @Transactional
    public void deleteEntry(long id) {
        long userId = CurrentUserProvider.require().getId();
        requireEntry(id, userId);
        jdbc.update("DELETE FROM diet_entry WHERE id=? AND user_id=?", id, userId);
        log.info("diet deleted, scenario=diet-delete, userId={}, entryId={}", userId, id);
    }

    public void saveTarget(Target request) {
        long userId = CurrentUserProvider.require().getId();
        jdbc.update("""
                INSERT INTO nutrition_target (user_id,target_date,goal,kcal,protein,fat,carbohydrate,fiber)
                VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE
                goal=VALUES(goal),kcal=VALUES(kcal),protein=VALUES(protein),fat=VALUES(fat),carbohydrate=VALUES(carbohydrate),fiber=VALUES(fiber)
                """, userId, request.targetDate(), request.goal(), request.kcal(), request.protein(), request.fat(), request.carbohydrate(), request.fiber());
        log.info("target saved, scenario=nutrition-target-save, userId={}, date={}, goal={}", userId, request.targetDate(), request.goal());
    }

    private void requireEntry(long id, long userId) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM diet_entry WHERE id=? AND user_id=?", Integer.class, id, userId) == 0) {
            log.warn("diet unavailable, scenario=diet-owner-check, userId={}, entryId={}", userId, id);
            throw new BusinessException(ErrorCode.NOT_FOUND, "饮食记录不存在");
        }
    }

    static BigDecimal amount(BigDecimal value, BigDecimal factor) {
        return value == null ? null : value.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    public static Totals totals(List<Entry> entries) {
        return new Totals(sum(entries, Entry::kcal), sum(entries, Entry::protein), sum(entries, Entry::fat),
                sum(entries, Entry::carbohydrate), sum(entries, Entry::fiber), entries.stream().allMatch(e -> e.fiber() != null));
    }

    private static BigDecimal sum(List<Entry> entries, Function<Entry, BigDecimal> getter) {
        return entries.stream().map(getter).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Food food(ResultSet rs, int row) throws SQLException {
        return new Food(rs.getString("id"), rs.getString("name"), rs.getString("category"), rs.getBigDecimal("kcal"),
                rs.getBigDecimal("protein"), rs.getBigDecimal("fat"), rs.getBigDecimal("carbohydrate"),
                rs.getBigDecimal("fiber"), rs.getString("source"), rs.getBoolean("enabled"));
    }

    private Entry entry(ResultSet rs, int row) throws SQLException {
        return new Entry(rs.getString("id"), rs.getDate("entry_date").toLocalDate(), rs.getString("meal"),
                rs.getString("food_id"), rs.getString("food_name"), rs.getBigDecimal("grams"), rs.getBigDecimal("kcal"),
                rs.getBigDecimal("protein"), rs.getBigDecimal("fat"), rs.getBigDecimal("carbohydrate"),
                rs.getBigDecimal("fiber"), rs.getString("source"));
    }
}
