package com.yokior.team.mapper;

import java.util.List;
import com.yokior.team.domain.QaUserTeam;
import com.yokior.team.domain.dto.QaUserTeamDto;
import com.yokior.team.domain.dto.TeamMemberDto;
import com.yokior.team.domain.vo.QaTeamVo;
import com.yokior.team.domain.vo.TeamMemberVo;
import org.apache.ibatis.annotations.Param;

/**
 * 我的团队Mapper接口
 * 
 * @author yokior
 * @date 2025-05-04
 */
public interface QaUserTeamMapper 
{
    /**
     * 查询我的团队
     * 
     * @param id 我的团队主键
     * @return 我的团队
     */
    public QaUserTeam selectQaUserTeamById(Long id);

    /**
     * 根据teamId 查询团队成员
     * @param teamId
     * @return
     */
    public List<TeamMemberVo> selectTeamMemberByTeamId(@Param("teamId") Long teamId);

    /**
     * 查询我的团队列表
     * 
     * @param qaUserTeamDto 我的团队
     * @return 我的团队集合
     */
    public List<QaTeamVo> selectQaUserTeamList(QaUserTeamDto qaUserTeamDto);

    /**
     * 根据teamId 和 userId查询我的团队列表
     * @param teamId
     * @param userId
     * @return
     */
    public QaTeamVo selectQaUserTeamByTeamIdAndUserId(@Param("teamId") Long teamId,@Param("userId") Long userId);

    /**
     * 新增我的团队
     * 
     * @param qaUserTeam 我的团队
     * @return 结果
     */
    public int insertQaUserTeam(QaUserTeam qaUserTeam);

    /**
     * 修改我的团队
     * 
     * @param qaUserTeam 我的团队
     * @return 结果
     */
    public int updateQaUserTeam(QaUserTeam qaUserTeam);

    /**
     * 删除我的团队
     * 
     * @param id 我的团队主键
     * @return 结果
     */
    public int deleteQaUserTeamById(Long id);

    /**
     * 批量删除我的团队
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteQaUserTeamByTeamIds(@Param("teamIds") Long[] ids);

    /**
     * 根据条件 查询团队成员
     * @param teamMemberDto
     * @return
     */
    public List<TeamMemberVo> selectTeamMember(@Param("dto") TeamMemberDto teamMemberDto);
}
