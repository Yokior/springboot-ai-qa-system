package com.yokior.team.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeamInviteDto implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 团队ID */
    private Long teamId;

    /** 有效期小时 */
    private Integer expireTimeHour;

    /** 有效期 */
    private Date expireTime;

    /** 邀请码 */
    private String inviteCode;
}
