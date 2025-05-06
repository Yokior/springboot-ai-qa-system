package com.yokior.team.service;

import java.util.List;

import com.yokior.common.core.page.TableDataInfo;
import com.yokior.team.domain.QaUserTeam;
import com.yokior.team.domain.dto.QaUserTeamDto;
import com.yokior.team.domain.dto.TeamMemberDto;
import com.yokior.team.domain.vo.QaTeamVo;
import com.yokior.team.domain.vo.QaUserTeamVo;
import com.yokior.team.domain.vo.TeamMemberVo;

/**
 * 我的团队Service接口
 * 
 * @author yokior
 * @date 2025-05-04
 */
public interface IQaUserTeamService 
{
    /**
     * 查询我的团队
     * 
     * @param id 我的团队主键
     * @return 我的团队
     */
    public QaTeamVo selectQaUserTeamById(Long id);

    /**
     * 查询我的团队列表
     * 
     * @param qaUserTeamDto 我的团队
     * @return 我的团队集合
     */
    public List<QaTeamVo> selectQaUserTeamList(QaUserTeamDto qaUserTeamDto);

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
     * 批量删除我的团队
     * 
     * @param ids 需要删除的我的团队主键集合
     * @return 结果
     */
    public int deleteQaUserTeamByIds(Long[] ids);

    /**
     * 删除我的团队信息
     * 
     * @param id 我的团队主键
     * @return 结果
     */
    public int deleteQaUserTeamById(Long id);


    /**
     * 查询团队成员
     *
     * @param teamMemberDto 团队成员
     * @return 团队成员集合
     */
    public List<TeamMemberVo> selectTeamMember(TeamMemberDto teamMemberDto);
}
