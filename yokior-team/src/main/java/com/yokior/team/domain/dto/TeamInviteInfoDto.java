package com.yokior.team.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeamInviteInfoDto implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 团队ID */
    private Long teamId;
    
    /** 团队名称 */
    private String teamName;
    
    /** 团队头像 */
    private String teamAvatar;
    
    /** 团队描述 */
    private String teamDescription;
    
    /** 创建者名称 */
    private String ownerName;
    
    /** 邀请人名称 */
    private String inviterName;
    
    /** 邀请人头像 */
    private String inviterAvatar;
    
    /** 过期时间 */
    private Date expireTime;
}