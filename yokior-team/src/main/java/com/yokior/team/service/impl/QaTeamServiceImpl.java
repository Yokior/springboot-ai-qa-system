package com.yokior.team.service.impl;

import java.util.Date;
import java.util.List;

import com.yokior.common.constant.TeamConstants;
import com.yokior.common.core.domain.entity.SysUser;
import com.yokior.common.exception.ServiceException;
import com.yokior.common.utils.DateUtils;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.system.mapper.SysUserMapper;
import com.yokior.team.domain.QaUserTeam;
import com.yokior.team.domain.dto.QaUserTeamDto;
import com.yokior.team.mapper.QaUserTeamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yokior.team.mapper.QaTeamMapper;
import com.yokior.team.domain.QaTeam;
import com.yokior.team.service.IQaTeamService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库团队Service业务层处理
 * 
 * @author yokior
 * @date 2025-05-03
 */
@Service
public class QaTeamServiceImpl implements IQaTeamService 
{
    @Autowired
    private QaTeamMapper qaTeamMapper;

    @Autowired
    private QaUserTeamMapper qaUserTeamMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 查询知识库团队
     * 
     * @param teamId 知识库团队主键
     * @return 知识库团队
     */
    @Override
    public QaTeam selectQaTeamByTeamId(Long teamId)
    {
        return qaTeamMapper.selectQaTeamByTeamId(teamId);
    }

    /**
     * 查询知识库团队列表
     * 
     * @param qaTeam 知识库团队
     * @return 知识库团队
     */
    @Override
    public List<QaTeam> selectQaTeamList(QaTeam qaTeam)
    {
        return qaTeamMapper.selectQaTeamList(qaTeam);
    }

    /**
     * 新增知识库团队 只有超级管理员可操作
     * 
     * @param qaTeam 知识库团队
     * @return 结果
     */
    @Override
    @Transactional
    public int insertQaTeam(QaTeam qaTeam)
    {
        // 获取当前操作用户
        Long operatorId = SecurityUtils.getUserId();

        Date nowDate = DateUtils.getNowDate();
        qaTeam.setCreateTime(nowDate);
        qaTeam.setCreateBy(operatorId.toString());
        qaTeam.setUpdateBy(operatorId.toString());

        qaTeamMapper.insertQaTeam(qaTeam);

        // 新增团队成员 身份为creator
        QaUserTeam qaUserTeam = new QaUserTeam();
        qaUserTeam.setTeamId(qaTeam.getTeamId());
        qaUserTeam.setUserId(qaTeam.getOwnerUserId());
        qaUserTeam.setRole("creator");
        qaUserTeam.setJoinTime(nowDate);

        qaUserTeamMapper.insertQaUserTeam(qaUserTeam);

        return 1;
    }

    /**
     * 修改知识库团队
     * 
     * @param qaTeam 知识库团队
     * @return 结果
     */
    @Override
    public int updateQaTeam(QaTeam qaTeam)
    {
        qaTeam.setUpdateTime(DateUtils.getNowDate());
        return qaTeamMapper.updateQaTeam(qaTeam);
    }

    /**
     * 批量删除知识库团队
     * 
     * @param teamIds 需要删除的知识库团队主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteQaTeamByTeamIds(Long[] teamIds)
    {
        qaTeamMapper.deleteQaTeamByTeamIds(teamIds);

        qaUserTeamMapper.deleteQaUserTeamByTeamIds(teamIds);

        return 1;
    }

    /**
     * 删除知识库团队信息
     * 
     * @param teamId 知识库团队主键
     * @return 结果
     */
    @Override
    public int deleteQaTeamByTeamId(Long teamId)
    {
        return qaTeamMapper.deleteQaTeamByTeamId(teamId);
    }

    /**
     * 转移团队
     * @param qaUserTeamDto
     * @return
     */
    @Transactional
    @Override
    public Boolean transferTeam(QaUserTeamDto qaUserTeamDto)
    {
        // 已经通过@TeamAuth校验，所以这里不需要再校验了

        Long targetUserId = qaUserTeamDto.getTargetUserId();
        Long teamId = qaUserTeamDto.getTeamId();
        String password = qaUserTeamDto.getPassword();

        // 获取正在操作用户的id和密码信息
        Long userId = SecurityUtils.getUserId();

        SysUser sysUser = sysUserMapper.selectUserById(userId);
        boolean pwdIsMatch = SecurityUtils.matchesPassword(password, sysUser.getPassword());

        // 校验密码
        if (!pwdIsMatch)
        {
            throw new ServiceException("密码错误！");
        }

        QaUserTeam userTeam = new QaUserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        userTeam.setRole(TeamConstants.ROLE_MEMBER);
        // 先将操作者变为成员
        int updateInfo1 = qaUserTeamMapper.updateQaUserTeam(userTeam);

        // 将目标用户变为创建者
        userTeam.setUserId(targetUserId);
        userTeam.setRole(TeamConstants.ROLE_CREATOR);
        int updateInfo2 = qaUserTeamMapper.updateQaUserTeam(userTeam);

        return updateInfo1 > 0 && updateInfo2 > 0;
    }
}
