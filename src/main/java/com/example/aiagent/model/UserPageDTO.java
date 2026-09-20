package com.example.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户管理分页结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPageDTO {

    /** 命中总数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页条数 */
    private int size;

    /** 总页数 */
    private int totalPages;

    /** 当前页数据 */
    private List<UserDTO> items;
}
