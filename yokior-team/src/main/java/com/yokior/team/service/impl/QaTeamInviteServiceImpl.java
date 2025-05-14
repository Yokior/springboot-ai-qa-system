package com.yokior.team.service.impl;

import cn.hutool.core.util.IdUtil;
import com.yokior.common.constant.TeamConstants;
import com.yokior.common.core.domain.entity.SysUser;
import com.yokior.common.exception.ServiceException;
import com.yokior.common.utils.DateUtils;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.system.mapper.SysUserMapper;
import com.yokior.team.domain.QaTeam;
import com.yokior.team.domain.QaTeamInvite;
import com.yokior.team.domain.QaUserTeam;
import com.yokior.team.domain.dto.TeamInviteDto;
import com.yokior.team.domain.dto.TeamInviteInfoDto;
import com.yokior.team.domain.vo.QaTeamVo;
import com.yokior.team.mapper.QaTeamInviteMapper;
import com.yokior.team.mapper.QaTeamMapper;
import com.yokior.team.mapper.QaUserTeamMapper;
import com.yokior.team.service.IQaTeamInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class QaTeamInviteServiceImpl implements IQaTeamInviteService
{
    @Autowired
    private QaTeamInviteMapper qaTeamInviteMapper;
    
    @Autowired
    private QaTeamMapper qaTeamMapper;
    
    @Autowired
    private QaUserTeamMapper qaUserTeamMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 创建团队邀请
     */
    @Override
    public TeamInviteDto createTeamInvite(TeamInviteDto teamInviteDto)
    {
        // 已经使用@TeamAuth校验，所以这里不需要再校验了

        Long teamId = teamInviteDto.getTeamId();
        Integer expireHours = teamInviteDto.getExpireTimeHour();
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 生成唯一邀请码
        String inviteCode = IdUtil.fastSimpleUUID();
        
        // 计算过期时间
        Date expireTime = DateUtils.addHours(new Date(), expireHours);
        
        // 创建邀请记录
        QaTeamInvite qaTeamInvite = new QaTeamInvite();
        qaTeamInvite.setTeamId(teamId);
        qaTeamInvite.setInviteCode(inviteCode);
        qaTeamInvite.setInviterId(userId);
        qaTeamInvite.setExpireTime(expireTime);
        qaTeamInvite.setStatus("0"); // 正常状态
        qaTeamInvite.setCreateTime(new Date());
        
        qaTeamInviteMapper.insertQaTeamInvite(qaTeamInvite);
        
        // 返回邀请信息
        TeamInviteDto resultDto = new TeamInviteDto();
        resultDto.setTeamId(teamId);
        resultDto.setInviteCode(inviteCode);
        resultDto.setExpireTime(expireTime);
        
        return resultDto;
    }

    /**
     * 获取邀请信息
     * @param inviteCode
     * @return
     */
    @Override
    public TeamInviteInfoDto getTeamInviteInfo(String inviteCode)
    {
        // 查询邀请记录
        QaTeamInvite qaTeamInvite = qaTeamInviteMapper.selectQaTeamInviteByCode(inviteCode);
        if (qaTeamInvite == null || !TeamConstants.INVITE_STATUS_NORMAL.equals(qaTeamInvite.getStatus()) || new Date().after(qaTeamInvite.getExpireTime()))
        {
            return null; // 邀请不存在、已使用、已取消或已过期
        }
        
        // 查询团队信息
        Long teamId = qaTeamInvite.getTeamId();
        QaTeam qaTeam = qaTeamMapper.selectQaTeamByTeamId(teamId);
        if (qaTeam == null)
        {
            return null; // 团队不存在
        }
        
        // 查询邀请人信息
        Long inviterId = qaTeamInvite.getInviterId();
        SysUser inviter = sysUserMapper.selectUserById(inviterId);
        
        // 查询团队创建者信息
        Long creatorId = qaUserTeamMapper.getTeamCreatorId(teamId);
        SysUser creator = sysUserMapper.selectUserById(creatorId);
        
        // 组装邀请信息DTO
        TeamInviteInfoDto teamInviteInfoDto = new TeamInviteInfoDto();
        teamInviteInfoDto.setTeamId(teamId);
        teamInviteInfoDto.setTeamName(qaTeam.getName());
        teamInviteInfoDto.setTeamAvatar(qaTeam.getAvatar());
        teamInviteInfoDto.setTeamDescription(qaTeam.getDescription());
        teamInviteInfoDto.setOwnerName(creator != null ? creator.getNickName() : "未知");
        teamInviteInfoDto.setInviterName(inviter != null ? inviter.getNickName() : "未知");
        teamInviteInfoDto.setInviterAvatar(inviter != null ? inviter.getAvatar() : null);
        teamInviteInfoDto.setExpireTime(qaTeamInvite.getExpireTime());
        
        return teamInviteInfoDto;
    }

    /**
     * 接收团队邀请
     * @param inviteCode 邀请码
     * @return 邀请信息
     */
    @Override
    @Transactional
    public Boolean acceptTeamInvite(String inviteCode)
    {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 查询邀请记录
        QaTeamInvite qaTeamInvite = qaTeamInviteMapper.selectQaTeamInviteByCode(inviteCode);
        if (qaTeamInvite == null || !TeamConstants.INVITE_STATUS_NORMAL.equals(qaTeamInvite.getStatus()) || new Date().after(qaTeamInvite.getExpireTime()))
        {
            throw new ServiceException("邀请链接无效或已过期");
        }
        
        Long teamId = qaTeamInvite.getTeamId();
        
        // 检查用户是否已经在团队中
        if (qaUserTeamMapper.isUserInTeam(teamId, userId))
        {
            throw new ServiceException("您已经是该团队成员");
        }
        
        // 将用户添加到团队中
        QaUserTeam qaUserTeam = new QaUserTeam();
        qaUserTeam.setTeamId(teamId);
        qaUserTeam.setUserId(userId);
        qaUserTeam.setRole(TeamConstants.ROLE_MEMBER); // 普通成员
        qaUserTeam.setJoinTime(new Date());
        
        qaUserTeamMapper.insertQaUserTeam(qaUserTeam);
        
        // 更新邀请状态为已使用
        QaTeamInvite updateInvite = new QaTeamInvite();
        updateInvite.setInviteId(qaTeamInvite.getInviteId());
        updateInvite.setStatus(TeamConstants.INVITE_STATUS_USED); // 已使用
        
        qaTeamInviteMapper.updateQaTeamInvite(updateInvite);
        
        return true;
    }

    /**
     * 取消团队邀请
     *
     * @param inviteId 邀请id
     * @return 邀请信息
     */
    @Override
    public Boolean cancelTeamInvite(Long inviteId)
    {
        // 获取当前登录用户ID
        Long userId = SecurityUtils.getUserId();
        
        // 查询邀请记录
        QaTeamInvite qaTeamInvite = qaTeamInviteMapper.selectQaTeamInviteById(inviteId);
        if (qaTeamInvite == null)
        {
            throw new ServiceException("邀请不存在");
        }
        
        // 检查是否有权限取消（只有邀请人或团队管理员可以取消）
        Long inviterId = qaTeamInvite.getInviterId();
        Long teamId = qaTeamInvite.getTeamId();
        
        if (!inviterId.equals(userId))
        {
            // 如果不是邀请人，检查是否是管理员
            String userRole = getUserRole(teamId, userId);
            if (!TeamConstants.ROLE_CREATOR.equals(userRole) && !TeamConstants.ROLE_ADMIN.equals(userRole))
            {
                throw new ServiceException("您没有权限取消该邀请");
            }
        }
        
        // 更新邀请状态为已取消
        QaTeamInvite updateInvite = new QaTeamInvite();
        updateInvite.setInviteId(inviteId);
        updateInvite.setStatus(TeamConstants.INVITE_STATUS_CANCELED); // 已取消
        
        qaTeamInviteMapper.updateQaTeamInvite(updateInvite);
        
        return true;
    }

    /**
     * 获取用户角色
     *
     * @return 角色
     */
    public String getUserRole(Long teamId, Long userId)
    {
        if (teamId == null || userId == null)
        {
            return null;
        }

        QaTeamVo qaTeamVo = qaUserTeamMapper.selectQaUserTeamByTeamIdAndUserId(teamId, userId);
        if (qaTeamVo == null)
        {
            return null;
        }

        return qaTeamVo.getRole();
    }
}