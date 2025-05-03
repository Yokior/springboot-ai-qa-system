package com.yokior.team.service;

import java.util.List;
import com.yokior.team.domain.QaTeam;

/**
 * 知识库团队Service接口
 * 
 * @author yokior
 * @date 2025-05-03
 */
public interface IQaTeamService 
{
    /**
     * 查询知识库团队
     * 
     * @param teamId 知识库团队主键
     * @return 知识库团队
     */
    public QaTeam selectQaTeamByTeamId(Long teamId);

    /**
     * 查询知识库团队列表
     * 
     * @param qaTeam 知识库团队
     * @return 知识库团队集合
     */
    public List<QaTeam> selectQaTeamList(QaTeam qaTeam);

    /**
     * 新增知识库团队
     * 
     * @param qaTeam 知识库团队
     * @return 结果
     */
    public int insertQaTeam(QaTeam qaTeam);

    /**
     * 修改知识库团队
     * 
     * @param qaTeam 知识库团队
     * @return 结果
     */
    public int updateQaTeam(QaTeam qaTeam);

    /**
     * 批量删除知识库团队
     * 
     * @param teamIds 需要删除的知识库团队主键集合
     * @return 结果
     */
    public int deleteQaTeamByTeamIds(Long[] teamIds);

    /**
     * 删除知识库团队信息
     * 
     * @param teamId 知识库团队主键
     * @return 结果
     */
    public int deleteQaTeamByTeamId(Long teamId);
}
