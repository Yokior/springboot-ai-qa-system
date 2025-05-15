package com.yokior.team.service;

import com.yokior.team.domain.dto.TeamInviteDto;
import com.yokior.team.domain.dto.TeamInviteInfoDto;

public interface IQaTeamInviteService
{
    /**
     * 创建团队邀请链接
     * 
     * @param teamInviteDto 邀请信息
     * @return 邀请详情
     */
    TeamInviteDto createTeamInvite(TeamInviteDto teamInviteDto);

    /**
     * 获取邀请信息
     * 
     * @param inviteCode 邀请码
     * @return 邀请详情
     */
    TeamInviteInfoDto getTeamInviteInfo(String inviteCode);

    /**
     * 接受团队邀请
     * 
     * @param inviteCode 邀请码
     * @return 是否成功
     */
    Boolean acceptTeamInvite(String inviteCode);
    
    /**
     * 取消团队邀请
     * 
     * @param inviteId 邀请ID
     * @return 是否成功
     */
    Boolean cancelTeamInvite(Long inviteId);


    /**
     * 删除过期的邀请
     *
     * @return 是否成功
     */
    Integer deleteExpiredInvite();
}