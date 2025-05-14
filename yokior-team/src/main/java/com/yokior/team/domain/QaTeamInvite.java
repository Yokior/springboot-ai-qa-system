package com.yokior.team.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class QaTeamInvite implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 邀请ID */
    private Long inviteId;

    /** 团队ID */
    private Long teamId;

    /** 邀请码 */
    private String inviteCode;

    /** 邀请人ID */
    private Long inviterId;

    /** 过期时间 */
    private Date expireTime;

    /** 状态（0正常 1已使用 2已取消） */
    private String status;

    /** 创建时间 */
    private Date createTime;

    /** 备注 */
    private String remark;
}