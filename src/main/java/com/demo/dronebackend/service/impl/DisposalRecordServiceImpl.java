package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.disposal.DisposalRecordQuery;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.DisposalRecord;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.DisposalRecordService;
import com.demo.dronebackend.mapper.DisposalRecordMapper;
import com.demo.dronebackend.util.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
* @author 28611
* @description 针对表【disposal_record(处置记录表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
@RequiredArgsConstructor
public class DisposalRecordServiceImpl extends ServiceImpl<DisposalRecordMapper, DisposalRecord>
    implements DisposalRecordService{

    private final DisposalRecordMapper mapper;
    private final AlarmMapper alarmMapper;
    @Override
    public Result<?> DisposalList(DisposalRecordQuery q) {
        Page<DisposalRecord> page = new Page<>(q.getPage(), q.getSize());

        LambdaQueryWrapper<DisposalRecord> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q.getDeviceId())) {
            qw.eq(DisposalRecord::getDeviceId, q.getDeviceId());
        }
        if (q.getCounterStart() != null) {
            qw.ge(DisposalRecord::getTime, q.getCounterStart());
        }
        if (q.getCounterEnd() != null) {
            qw.le(DisposalRecord::getTime, q.getCounterEnd());
        }

        User user = CurrentUserContext.get();
        if(!PermissionType.admin.getDesc().equals( user.getPermission())){
            Long userId = user.getId();
            qw.eq(DisposalRecord::getDeviceId, q.getDeviceId());
            qw.inSql(DisposalRecord::getDeviceId,
                    "SELECT id FROM device WHERE device_user_id" + userId);
        }
        Page<DisposalRecord> disposalRecordPage = mapper.selectPage(page, qw);

        return Result.success(new MyPage<DisposalRecord>(disposalRecordPage));
    }

    @Override
    public Result<?> delete(Long id) {
        int r = mapper.deleteById(id);
        if (r == 0) {
            throw new BusinessException("干扰记录不存在或已删除，ID=" + id);
        }
        return Result.success("删除成功");
    }

    @Override
    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除失败：ID 列表为空");
        }
        int r = mapper.deleteBatchIds(ids);
        if (r == 0) {
            throw new BusinessException("未删除任何记录，请检查 ID 是否正确");
        }
        return Result.success("批量删除成功");
    }

    @Override
    public Result<?> getDisposalCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        // 今日处置总数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        Long disposedCnt = alarmMapper.countDisposedAlarms(
                userId,
                Timestamp.valueOf(todayStart),
                Timestamp.valueOf(todayEnd)
        );

        // 今日未处置总数
        Long unDisposedCnt = alarmMapper.countUndisposedAlarms(userId);

        Map<String, Long> disposalCount = new LinkedHashMap<>();
        disposalCount.put("disposed_count",disposedCnt);
        disposalCount.put("undisposed_count",unDisposedCnt);
        return Result.success(disposalCount);
    }
}




