package com.demo.dronebackend.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyPage<T> {
    private long current;
    private long pages; //一共多少页
    private long size; //每页显示数
    private long total;
    private List<T> records;

    public MyPage(IPage page) {
        this.current = page.getCurrent();
        this.pages = page.getPages();
        this.size = page.getSize();
        this.total = page.getTotal();
        this.records = page.getRecords();
    }

}
