package com.demo.dronebackend.service;

import com.demo.dronebackend.model.GeoEntry;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface GeoCacheService {
    /**
     * 异步更新/触发反查。实现应保证：
     * - 幂等/去重（相同坐标短时间内不重复请求第三方）
     * - 在内部线程池或异步机制里执行网络调用（不阻塞调用线程）
     */
    void updateLocation(String deviceKey, double lon, double lat);

    /**
     * 读取缓存条目（尽量返回快速，Optional 里可能包含地址或只有坐标等元数据）
     */
    Optional<GeoEntry> getEntry(String deviceKey);
}
