package com.yokior.team.service.impl;

import java.util.Arrays;
import java.util.List;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.yokior.common.constant.TeamConstants;
import com.yokior.common.core.page.TableDataInfo;
import com.yokior.common.exception.ServiceException;
import com.yokior.common.utils.PageUtils;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.team.domain.QaTeam;
import com.yokior.team.domain.dto.QaUserTeamDto;
import com.yokior.team.domain.dto.TeamMemberDto;
import com.yokior.team.domain.vo.QaTeamVo;
import com.yokior.team.domain.vo.QaUserTeamVo;
import com.yokior.team.domain.vo.TeamMemberVo;
import com.yokior.team.mapper.QaTeamMapper;
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

    @Autowired
    private QaTeamMapper qaTeamMapper;

    /**
     * 查询我的团队
     * 
     * @param id 我的团队主键
     * @return 我的团队
     */
    @Override
    public QaTeamVo selectQaUserTeamById(Long id)
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
//        PageUtils.startPage();
//        List<TeamMemberVo> teamMemberVos = qaUserTeamMapper.selectTeamMemberByTeamId(id);
//
//        if (teamMemberVos.isEmpty())
//        {
//            throw new ServiceException("团队成员不存在");
//        }
//
//        // 构造参数返回
//        QaUserTeamVo qaUserTeamVo = QaUserTeamVo.builder()
//                .teamInfo(qaTeamVo)
//                .teamMembers(teamMemberVos)
//                .build();
//
//        TableDataInfo tableDataInfo = TableDataInfo.getDataTableFromData(qaUserTeamVo, new PageInfo(teamMemberVos).getTotal());

        return qaTeamVo;
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

    /**
     * 查询团队成员
     *
     * @param teamMemberDto 团队成员
     * @return 团队成员
     */
    @Override
    public List<TeamMemberVo> selectTeamMember(TeamMemberDto teamMemberDto)
    {
        PageUtils.startPage();
        List<TeamMemberVo> teamMemberVoList = qaUserTeamMapper.selectTeamMember(teamMemberDto);

        return teamMemberVoList;
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

    /**
     * 修改团队成员角色
     *
     * @param qaUserTeam 团队成员
     * @return 结果
     */
    @Override
    public Boolean updateRole(QaUserTeam qaUserTeam)
    {
        Long userId = SecurityUtils.getUserId();
        Long teamId = qaUserTeam.getTeamId();
        Long updateUserId = qaUserTeam.getUserId();
        String role = qaUserTeam.getRole();

        // 检查需要更新的用户是否为自己
        if (updateUserId.equals(userId))
        {
            throw new ServiceException("不能更新自己的角色 更新失败");
        }

        // 检查操作用户是否在团队中 检查权限
        String userRole = getUserRole(teamId, userId);
        if (userRole == null)
        {
            throw new ServiceException("用户不在团队中 更新失败");
        }
        // 检查操作者是否是团队创建者
        if (!TeamConstants.ROLE_CREATOR.equals(userRole))
        {
            throw new ServiceException("用户不是创建者 更新失败");
        }

        // 检查被操作用户是否在团队中
        String updateUserRole = getUserRole(teamId, updateUserId);
        if (updateUserRole == null)
        {
            throw new ServiceException("被操作用户不在团队中 更新失败");
        }

        // 检查role参数是否合法
        if (!TeamConstants.ROLE_MEMBER.equals(role) && !TeamConstants.ROLE_ADMIN.equals(role))
        {
            throw new ServiceException("角色身份不合法 更新失败");
        }



        QaUserTeam userTeam = new QaUserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(updateUserId);
        userTeam.setRole(role);

        int i = qaUserTeamMapper.updateQaUserTeam(userTeam);

        return i > 0;
    }

    /**
     * 修改团队信息
     *
     * @param qaTeam 团队信息
     * @return 结果
     */
    @Override
    public Boolean updateMyTeamInfo(QaTeam qaTeam)
    {
        Long userId = SecurityUtils.getUserId();
        Long teamId = qaTeam.getTeamId();

        // 检查操作者角色信息
        String userRole = getUserRole(teamId, userId);
        if (userRole == null)
        {
            throw new ServiceException("用户或者团队不存在");
        }
        // 如果不是创建者或者管理员
        if (!TeamConstants.ROLE_CREATOR.equals(userRole) && !TeamConstants.ROLE_ADMIN.equals(userRole))
        {
            throw new ServiceException("非法操作");
        }

        QaTeam newTeamInfo = new QaTeam();
        newTeamInfo.setTeamId(teamId);
        newTeamInfo.setAvatar(qaTeam.getAvatar());
        newTeamInfo.setDescription(qaTeam.getDescription());
        newTeamInfo.setName(qaTeam.getName());

        int i = qaTeamMapper.updateQaTeam(newTeamInfo);

        return i > 0;
    }
}
