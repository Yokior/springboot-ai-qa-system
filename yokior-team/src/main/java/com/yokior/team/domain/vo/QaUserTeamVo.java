package com.yokior.team.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Yokior
 * @description 团队成员视图对象
 * @date 2025/5/6 16:18
 */
@Data
@Builder
public class QaUserTeamVo
{
    // 团队基本信息
    private QaTeamVo teamInfo;

    // 团队成员信息
    private List<TeamMemberVo> teamMembers;
}
