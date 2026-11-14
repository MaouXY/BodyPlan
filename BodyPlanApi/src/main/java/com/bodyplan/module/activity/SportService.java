package com.bodyplan.module.activity;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUserProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.geographiclib.Geodesic;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.bodyplan.module.activity.SportModels.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private static final Set<String> SPORTS = Set.of("RUN", "WALK", "RIDE", "STRENGTH", "OTHER");
    private static final Set<String> MODES = Set.of("AEROBIC", "INTERVAL", "STRENGTH", "FREE");
    private static final String COLUMNS = """
            id,client_record_id,sport_type,training_mode,target_distance_m,target_pace_sec,
            started_at,ended_at,duration_sec,distance_m,pace_sec_per_km,average_speed_kmh,point_count,remark
            """;

    public List<Summary> history() {
        return jdbc.query("SELECT " + COLUMNS + " FROM sport_session WHERE user_id=? ORDER BY started_at DESC,id DESC LIMIT 100",
                this::summary, CurrentUserProvider.require().getId());
    }

    public Detail detail(long id) {
        long userId = CurrentUserProvider.require().getId();
        var records = jdbc.query("SELECT " + COLUMNS + ",route_json FROM sport_session WHERE id=? AND user_id=?",
                (rs, row) -> new Detail(summary(rs, row), decode(rs.getString("route_json"), id, userId)), id, userId);
        if (records.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "运动记录不存在");
        return records.get(0);
    }

    @Transactional
    public Summary save(Save request) {
        long userId = CurrentUserProvider.require().getId();
        validate(request, userId);
        String hash = hash(encode(request, userId));
        var existing = findByClient(userId, request.clientRecordId(), hash);
        if (existing != null) return existing;
        double distance = distance(request.points(), request.sportType());
        BigDecimal meters = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        Integer pace = distance >= 10 ? (int) Math.round(request.durationSec() * 1000.0 / distance) : null;
        BigDecimal speed = BigDecimal.valueOf(distance * 3.6 / request.durationSec()).setScale(2, RoundingMode.HALF_UP);
        long id = IdWorker.getId();
        try {
            jdbc.update("""
                    INSERT INTO sport_session
                    (id,user_id,client_record_id,request_hash,sport_type,training_mode,target_distance_m,target_pace_sec,
                    started_at,ended_at,duration_sec,distance_m,pace_sec_per_km,average_speed_kmh,route_json,point_count,remark)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, id, userId, request.clientRecordId(), hash, request.sportType(), request.trainingMode(),
                    request.targetDistanceM(), request.targetPaceSec(), request.startedAt(), request.endedAt(),
                    request.durationSec(), meters, pace, speed, encode(request.points(), userId), request.points().size(), request.remark());
        } catch (DuplicateKeyException exception) {
            log.warn("sport save duplicate, scenario=sport-save, userId={}, clientRecordId={}", userId, request.clientRecordId(), exception);
            Summary duplicate = findByClient(userId, request.clientRecordId(), hash);
            if (duplicate != null) return duplicate;
            throw exception;
        }
        log.info("sport saved, scenario=sport-save, userId={}, sessionId={}, clientRecordId={}, distanceM={}, pointCount={}",
                userId, id, request.clientRecordId(), meters, request.points().size());
        return new Summary(id, request.clientRecordId(), request.sportType(), request.trainingMode(), request.targetDistanceM(),
                request.targetPaceSec(), request.startedAt(), request.endedAt(), request.durationSec(), meters, pace, speed,
                request.points().size(), request.targetDistanceM() != null && distance >= request.targetDistanceM(), request.remark());
    }

    private Summary findByClient(long userId, String clientId, String hash) {
        var rows = jdbc.query("SELECT " + COLUMNS + ",request_hash FROM sport_session WHERE user_id=? AND client_record_id=?",
                (rs, row) -> {
                    if (!hash.equals(rs.getString("request_hash"))) {
                        log.warn("sport idempotency conflict, scenario=sport-save, userId={}, clientRecordId={}", userId, clientId);
                        throw new BusinessException(ErrorCode.CONFLICT, "该运动已保存，不能用相同编号覆盖不同内容");
                    }
                    return summary(rs, row);
                }, userId, clientId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    static void validate(Save request, long userId) {
        if (!SPORTS.contains(request.sportType()) || !MODES.contains(request.trainingMode())
                || request.endedAt() <= request.startedAt()
                || request.endedAt() > Instant.now().toEpochMilli() + 60000
                || request.endedAt() - request.startedAt() > 7 * 86400000L
                || request.durationSec() * 1000L > request.endedAt() - request.startedAt() + 1000) {
            log.warn("sport invalid, scenario=sport-save, userId={}, clientRecordId={}, reason=type-or-time", userId, request.clientRecordId());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "运动项目或运动时间不正确");
        }
        if (Set.of("STRENGTH", "OTHER").contains(request.sportType()) && !request.points().isEmpty())
            throw new BusinessException(ErrorCode.BAD_REQUEST, "室内训练不记录 GPS 路线");
        long previousTime = 0;
        int previousSegment = 0;
        for (Point point : request.points()) {
            if (!Double.isFinite(point.latitude()) || !Double.isFinite(point.longitude()) || !Double.isFinite(point.accuracy())
                    || point.latitude() < -90 || point.latitude() > 90 || point.longitude() < -180 || point.longitude() > 180
                    || point.accuracy() < 0 || point.accuracy() > 50 || point.segment() < previousSegment
                    || point.timestampMs() <= previousTime || point.timestampMs() < request.startedAt() || point.timestampMs() > request.endedAt()) {
                log.warn("sport invalid, scenario=sport-save, userId={}, clientRecordId={}, pointTime={}, reason=route-order-or-coordinate",
                        userId, request.clientRecordId(), point.timestampMs());
                throw new BusinessException(ErrorCode.BAD_REQUEST, "轨迹时间顺序或定位数据不正确");
            }
            previousTime = point.timestampMs();
            previousSegment = point.segment();
        }
    }

    // 与客户端共用相同的距离过滤约定，暂停和失联段不能连线累计。
    public static double distance(List<Point> points, String sport) {
        double distance = 0;
        double maxSpeed = sport.equals("RIDE") ? 25 : sport.equals("WALK") ? 5 : 12;
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1), b = points.get(i);
            double seconds = (b.timestampMs() - a.timestampMs()) / 1000.0;
            if (a.segment() != b.segment() || seconds <= 0 || seconds > 30) continue;
            double delta = Geodesic.WGS84.Inverse(a.latitude(), a.longitude(), b.latitude(), b.longitude()).s12;
            if (delta >= 3 && delta / seconds <= maxSpeed) distance += delta;
        }
        return distance;
    }

    private Summary summary(ResultSet rs, int row) throws SQLException {
        BigDecimal meters = rs.getBigDecimal("distance_m");
        Integer target = rs.getObject("target_distance_m", Integer.class);
        return new Summary(rs.getLong("id"), rs.getString("client_record_id"), rs.getString("sport_type"),
                rs.getString("training_mode"), target, rs.getObject("target_pace_sec", Integer.class),
                rs.getLong("started_at"), rs.getLong("ended_at"), rs.getInt("duration_sec"), meters,
                rs.getObject("pace_sec_per_km", Integer.class), rs.getBigDecimal("average_speed_kmh"),
                rs.getInt("point_count"), target != null && meters.doubleValue() >= target, rs.getString("remark"));
    }

    private String encode(Object value, long userId) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error("sport JSON failed, scenario=sport-serialize, userId={}", userId, exception);
            throw new IllegalStateException("运动记录编码失败", exception);
        }
    }

    private List<Point> decode(String route, long id, long userId) {
        try {
            return json.readValue(route, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            log.error("sport JSON invalid, scenario=sport-detail, userId={}, sessionId={}", userId, id, exception);
            throw new IllegalStateException("运动轨迹数据异常", exception);
        }
    }

    private String hash(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            log.error("hash unavailable, scenario=sport-save, userId={}", CurrentUserProvider.require().getId(), exception);
            throw new IllegalStateException(exception);
        }
    }
}
