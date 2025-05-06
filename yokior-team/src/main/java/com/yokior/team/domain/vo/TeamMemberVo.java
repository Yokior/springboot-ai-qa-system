package com.yokior.team.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yokior
 * @description 团队成员信息
 * @date 2025/5/6 16:20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamMemberVo
{
    // 用户id
    private Long userId;

    // 头像
    private String avatar;

    // 昵称
    private String nickName;

    // 角色
    private String role;

    // 加入时间
    private String joinTime;
}
