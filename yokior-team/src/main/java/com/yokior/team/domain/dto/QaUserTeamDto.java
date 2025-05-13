package com.yokior.team.domain.dto;

import com.yokior.team.domain.QaUserTeam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yokior
 * @description 团队信息DTO
 * @date 2025/5/6 15:59
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaUserTeamDto extends QaUserTeam
{
    // 团队名称
    private String name;

    // 转让人id
    private Long targetUserId;

    // 密码
    private String password;
}
