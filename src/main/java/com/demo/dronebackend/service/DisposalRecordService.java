package com.demo.dronebackend.service;


import com.demo.dronebackend.dto.disposal.DisposalRecordQuery;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.DisposalRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 28611
* @description 针对表【disposal_record(处置记录表)】的数据库操作Service
* @createDate 2025-07-07 09:44:52
*/
public interface DisposalRecordService extends IService<DisposalRecord> {

    Result<?> DisposalList(DisposalRecordQuery query);

    Result<?> delete(Long id);

    Result<?> deleteBatch(List<Long> ids);


    /**
     * 今日告警
     */
    Result<?> getDisposalCount();
}
