package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.disposal.DisposalRecordQuery;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.mapper.UserMapper;
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

import java.util.List;

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
        if (q.getDeviceId() != null) {
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
        return Result.success(null);
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
        return Result.success(null);
    }

    @Override
    public Result<?> getDisposalCount() {
        long disposedCount = query().count();

        return null;
    }
}




