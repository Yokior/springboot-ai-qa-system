package com.yokior.team.mapper;

import java.util.List;
import com.yokior.team.domain.QaTeamInvite;

/**
 * 团队邀请 Mapper 接口
 * 
 * @author yokior
 * @date 2023-05-20
 */
public interface QaTeamInviteMapper 
{
    /**
     * 查询团队邀请
     * 
     * @param inviteId 团队邀请ID
     * @return 团队邀请
     */
    public QaTeamInvite selectQaTeamInviteById(Long inviteId);

    /**
     * 通过邀请码查询团队邀请
     * 
     * @param inviteCode 邀请码
     * @return 团队邀请
     */
    public QaTeamInvite selectQaTeamInviteByCode(String inviteCode);

    /**
     * 查询团队邀请列表
     * 
     * @param qaTeamInvite 团队邀请
     * @return 团队邀请集合
     */
    public List<QaTeamInvite> selectQaTeamInviteList(QaTeamInvite qaTeamInvite);

    /**
     * 查询团队有效邀请数量
     * 
     * @param teamId 团队ID
     * @return 邀请数量
     */
    public int selectValidInviteCount(Long teamId);

    /**
     * 新增团队邀请
     * 
     * @param qaTeamInvite 团队邀请
     * @return 结果
     */
    public int insertQaTeamInvite(QaTeamInvite qaTeamInvite);

    /**
     * 修改团队邀请
     * 
     * @param qaTeamInvite 团队邀请
     * @return 结果
     */
    public int updateQaTeamInvite(QaTeamInvite qaTeamInvite);

    /**
     * 删除团队邀请
     * 
     * @param inviteId 团队邀请ID
     * @return 结果
     */
    public int deleteQaTeamInviteById(Long inviteId);

    /**
     * 批量删除团队邀请
     * 
     * @param inviteIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteQaTeamInviteByIds(Long[] inviteIds);

    /**
     * 清理过期的邀请
     * 
     * @return 结果
     */
    public int cleanExpiredInvites();

    /**
     * 清理过期的邀请
     *
     * @return 结果
     */
    int deleteExpiredInvite();
}