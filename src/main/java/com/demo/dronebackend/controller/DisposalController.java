package com.demo.dronebackend.controller;


import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.dronebackend.dto.disposal.BatchDeleteRequest;
import com.demo.dronebackend.dto.disposal.DisposalRecordQuery;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.DisposalRecord;
import com.demo.dronebackend.service.DisposalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("admin/disposals")
public class DisposalController {

    private final DisposalRecordService disposalService;

    /**
     * 1. 分页展示干扰记录
     */
    @GetMapping
    public Result<?> list(
            @Valid @ModelAttribute DisposalRecordQuery query) {
        return disposalService.DisposalList(query);
    }

    /**
     * 2. 删除单条干扰记录
     */
    @DeleteMapping("/{disposal_id}")
    public Result<?> deleteSingle(
            @PathVariable("disposal_id") Long id) {
        return  disposalService.delete(id);
    }

    /**
     * 3. 批量删除干扰记录
     */
    @PostMapping("/batch_delete")
    public Result<?> deleteBatch(
            @Valid @RequestBody BatchDeleteRequest<Long> req) {

        return disposalService.deleteBatch(req.getIds());
    }
}
