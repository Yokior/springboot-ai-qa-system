package com.yokior.team.service.impl;

import java.util.Date;
import java.util.List;
import com.yokior.common.utils.DateUtils;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.team.domain.QaUserTeam;
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
}
