package com.demo.dronebackend.util;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AlarmKeyGenerator {

    private final JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME_ALARM = "alarm";
    private static final long TABLE_CAPACITY = 100000L;

    /**
     * 获取下一个可用的主键，实现循环覆盖逻辑。
     * 使用 Propagation.REQUIRES_NEW 确保ID生成在独立的短事务中完成，
     * 这会尽快提交并释放 table_meta 上的行锁，最大限度地减少对其他线程的阻塞。
     *
     * @return 计算出的下一个可用主键 (ID范围: 1 到 100000)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long generateNextKey() {
        String selectSql = "SELECT id, incr_pos_id, loop_count FROM table_meta WHERE table_name = ? FOR UPDATE";
        Map<String, Object> currentMeta;
        try {
            currentMeta = jdbcTemplate.queryForMap(selectSql, TABLE_NAME_ALARM);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("严重错误：元信息表 table_meta 中未找到 '" + TABLE_NAME_ALARM + "' 的记录！", e);
        }

        // 确定本次要返回的ID，并计算下一次要使用的ID
        long metaRowId = (long) currentMeta.get("id");
        long currentKeyToUse = (long) currentMeta.get("incr_pos_id");
        long currentLoopCount = (long) currentMeta.get("loop_count");
        long nextKey;
        long nextLoopCount = currentLoopCount;
        if (currentKeyToUse >= TABLE_CAPACITY) {
            // 如果当前ID达到或超过容量上限，则重置为1，并增加循环次数
            nextKey = 1L;
            nextLoopCount++;
        } else {
            // 否则，正常加一
            nextKey = currentKeyToUse + 1;
        }
        // 更新回数据库
        String updateSql = "UPDATE table_meta SET incr_pos_id = ?, loop_count = ? WHERE id = ?";
        jdbcTemplate.update(updateSql, nextKey, nextLoopCount, metaRowId);

        // 返回id
        return currentKeyToUse;
    }
}