package com.demo.dronebackend.service;

import com.demo.dronebackend.model.GeoEntry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class GeoCacheServiceImpl implements GeoCacheService {

    private final ConcurrentHashMap<String, GeoEntry> cache = new ConcurrentHashMap<>();
    private final ExecutorService geocodeExecutor = Executors.newFixedThreadPool(8);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 配置参数（可从配置文件注入）
    private final double DISTANCE_THRESHOLD_METERS = 20.0;   // 位置变化阈值：20米
    private final Duration MIN_TRIGGER_INTERVAL = Duration.ofSeconds(30); // 同一设备最小触发间隔
    private final Duration ADDRESS_TTL = Duration.ofHours(24); // 地址过期时间

    private final TiandituService tiandituService; // 注入你的反查服务

    public GeoCacheServiceImpl(TiandituService tiandituService) {
        this.tiandituService = tiandituService;
    }

    @Override
    public void updateLocation(String deviceKey, double lon, double lat) {
        Instant now = Instant.now();

        cache.compute(deviceKey, (key, existing) -> {
            if (existing == null) {
                // 新条目：保存坐标并立即触发解析
                GeoEntry e = new GeoEntry();
                e.setLon(lon);
                e.setLat(lat);
                e.setLastLocationUpdate(now);
                e.setLastResolveAttempt(null);
                e.setLastResolvedTime(null);
                e.setAddress(null);
                e.setStatus(GeoEntry.Status.PENDING);
                triggerResolveAsync(key, e);
                return e;
            } else {
                // 更新坐标时间戳
                double oldLon = existing.getLon();
                double oldLat = existing.getLat();
                existing.setLon(lon);
                existing.setLat(lat);
                existing.setLastLocationUpdate(now);

                // 计算距离（米）
                double distMeters = haversineMeters(oldLat, oldLon, lat, lon);

                // 是否过期（地址）
                boolean addressExpired = existing.getLastResolvedTime() == null ||
                        Duration.between(existing.getLastResolvedTime(), now).compareTo(ADDRESS_TTL) > 0;

                // 最近一次触发解析时间（避免重复触发）
                Instant lastAttempt = existing.getLastResolveAttempt();
                boolean canTriggerByTime = (lastAttempt == null) ||
                        Duration.between(lastAttempt, now).compareTo(MIN_TRIGGER_INTERVAL) > 0;

                // 决策：距离超过阈值 或 地址过期 && 满足最小触发间隔 才触发解析
                if ((distMeters >= DISTANCE_THRESHOLD_METERS || addressExpired) && canTriggerByTime) {
                    existing.setStatus(GeoEntry.Status.PENDING);
                    existing.setLastResolveAttempt(now); // 记录尝试时间，防止短时间内重复触发
                    triggerResolveAsync(key, existing);
                } else {
                    // 否则不触发解析（去重/采样成功）
                }
                return existing;
            }
        });
    }

    @Override
    public Optional<GeoEntry> getEntry(String deviceKey) {
        return Optional.ofNullable(cache.get(deviceKey));
    }

    private void triggerResolveAsync(String deviceKey, GeoEntry entry) {
        // 在线程池中进行网络调用（不会阻塞调用线程）
        geocodeExecutor.submit(() -> {
            try {
                String addr = tiandituService.reverseGeocode(entry.getLon(), entry.getLat());
                entry.setAddress(addr);
                entry.setLastResolvedTime(Instant.now());
                entry.setStatus(GeoEntry.Status.OK);
                entry.setRetryCount(0);
            } catch (Exception ex) {
                entry.setStatus(GeoEntry.Status.ERROR);
                int nextRetry = entry.getRetryCount() + 1;
                entry.setRetryCount(nextRetry);
                // 可实现重试策略：简单指数退避
                if (nextRetry <= 3) {
                    long delaySec = (long) Math.pow(2, nextRetry);
                    scheduler.schedule(() -> {
                        // 再次触发解析（按布尔条件再检查或直接触发）
                        triggerResolveAsync(deviceKey, entry);
                    }, delaySec, TimeUnit.SECONDS);
                } else {
                    // 达到最大重试：记录日志，并交由定时任务或人工处理
                    // log.warn(...)
                }
            }
        });
    }

    // Haversine 公式：返回两点之间的大圆距离（米）
    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // 地球半径（米）
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // 关闭线程池（在 @PreDestroy 调用）
    public void shutdown() {
        geocodeExecutor.shutdown();
        scheduler.shutdown();
    }
}
