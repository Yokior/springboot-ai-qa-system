package com.yokior.team.service.impl;

import java.util.Arrays;
import java.util.List;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.yokior.common.core.page.TableDataInfo;
import com.yokior.common.exception.ServiceException;
import com.yokior.common.utils.PageUtils;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.team.domain.dto.QaUserTeamDto;
import com.yokior.team.domain.vo.QaTeamVo;
import com.yokior.team.domain.vo.QaUserTeamVo;
import com.yokior.team.domain.vo.TeamMemberVo;
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
    public TableDataInfo selectQaUserTeamById(Long id)
    {
        Long userId = SecurityUtils.getUserId();

        // 查询团队信息
        QaTeamVo qaTeamVo = qaUserTeamMapper.selectQaUserTeamByTeamIdAndUserId(id, userId);

        // 检查是否是团队成员
        // 如果团队信息不存在，抛出异常 （正常来说不会触发这个）
        if (qaTeamVo == null)
        {
            throw new ServiceException("团队信息不存在");
        }

        // 查询团队成员信息
        PageUtils.startPage();
        List<TeamMemberVo> teamMemberVos = qaUserTeamMapper.selectTeamMemberByTeamId(id);

        if (teamMemberVos.isEmpty())
        {
            throw new ServiceException("团队成员不存在");
        }

        // 构造参数返回
        QaUserTeamVo qaUserTeamVo = QaUserTeamVo.builder()
                .teamInfo(qaTeamVo)
                .teamMembers(teamMemberVos)
                .build();

        TableDataInfo tableDataInfo = TableDataInfo.getDataTableFromData(qaUserTeamVo, new PageInfo(teamMemberVos).getTotal());

        return tableDataInfo;
    }

    /**
     * 查询我的团队列表
     * 
     * @param qaUserTeamDto 我的团队
     * @return 我的团队
     */
    @Override
    public List<QaTeamVo> selectQaUserTeamList(QaUserTeamDto qaUserTeamDto)
    {
        return qaUserTeamMapper.selectQaUserTeamList(qaUserTeamDto);
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
        return 1;
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
