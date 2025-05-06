package com.yokior.team.domain.dto;

import lombok.Data;

/**
 * @author Yokior
 * @description 团队成员数据传输对象
 * @date 2025/5/6 22:05
 */
@Data
public class TeamMemberDto
{
    // 团队ID
    private Long teamId;

    // 团队成员名称
    private String name;

    // 团队成员角色
    private String role;
}
