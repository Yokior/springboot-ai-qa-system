package com.yokior.team.mapper;

import java.util.List;
import com.yokior.team.domain.QaTeam;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库团队Mapper接口
 * 
 * @author yokior
 * @date 2025-05-03
 */
public interface QaTeamMapper 
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
     * 删除知识库团队
     * 
     * @param teamId 知识库团队主键
     * @return 结果
     */
    public int deleteQaTeamByTeamId(Long teamId);

    /**
     * 批量删除知识库团队
     * 
     * @param teamIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQaTeamByTeamIds(@Param("teamIds") Long[] teamIds);
}
