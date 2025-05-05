package com.yokior.team.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yokior
 * @description
 * @date 2025/5/5 19:52
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaTeamVo
{
    /** 团队ID */
    private Long teamId;

    /** 团队名称 */
    private String name;

    /** 团队描述 */
    private String description;

    /** 团队头像地址 */
    private String avatar;

    /** 团队创建者ID */
    private String ownerUserName;

    /** 用户在团队中的角色（creator, admin, member） */
    private String role;

    /** 加入时间 */
    private String joinTime;
}
