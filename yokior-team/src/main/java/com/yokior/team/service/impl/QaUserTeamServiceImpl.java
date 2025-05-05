package com.yokior.team.service.impl;

import java.util.List;

import com.yokior.team.domain.vo.QaTeamVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yokior.team.mapper.QaUserTeamMapper;
import com.yokior.team.domain.QaUserTeam;
import com.yokior.team.service.IQaUserTeamService;

/**
 * 我的团队Service业务层处理
 * 
 * @author yokior
 * @date 2025-05-04
 */
@Service
public class QaUserTeamServiceImpl implements IQaUserTeamService 
{
    @Autowired
    private QaUserTeamMapper qaUserTeamMapper;

    /**
     * 查询我的团队
     * 
     * @param id 我的团队主键
     * @return 我的团队
     */
    @Override
    public QaUserTeam selectQaUserTeamById(Long id)
    {
        return qaUserTeamMapper.selectQaUserTeamById(id);
    }

    /**
     * 查询我的团队列表
     * 
     * @param qaUserTeam 我的团队
     * @return 我的团队
     */
    @Override
    public List<QaTeamVo> selectQaUserTeamList(QaUserTeam qaUserTeam)
    {
        return qaUserTeamMapper.selectQaUserTeamList(qaUserTeam);
    }

    /**
     * 新增我的团队
     * 
     * @param qaUserTeam 我的团队
     * @return 结果
     */
    @Override
    public int insertQaUserTeam(QaUserTeam qaUserTeam)
    {
        return qaUserTeamMapper.insertQaUserTeam(qaUserTeam);
    }

    /**
     * 修改我的团队
     * 
     * @param qaUserTeam 我的团队
     * @return 结果
     */
    @Override
    public int updateQaUserTeam(QaUserTeam qaUserTeam)
    {
        return qaUserTeamMapper.updateQaUserTeam(qaUserTeam);
    }

    /**
     * 批量删除我的团队
     * 
     * @param ids 需要删除的我的团队主键
     * @return 结果
     */
    @Override
    public int deleteQaUserTeamByIds(Long[] ids)
    {
        return qaUserTeamMapper.deleteQaUserTeamByIds(ids);
    }

    /**
     * 删除我的团队信息
     * 
     * @param id 我的团队主键
     * @return 结果
     */
    @Override
    public int deleteQaUserTeamById(Long id)
    {
        return qaUserTeamMapper.deleteQaUserTeamById(id);
    }
}
