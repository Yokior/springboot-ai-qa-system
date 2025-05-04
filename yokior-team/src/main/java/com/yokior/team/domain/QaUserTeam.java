package com.yokior.team.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yokior.common.annotation.Excel;
import com.yokior.common.core.domain.BaseEntity;

/**
 * 我的团队对象 qa_user_team
 * 
 * @author yokior
 * @date 2025-05-04
 */
public class QaUserTeam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID (关联 sys_user.user_id) */
    @Excel(name = "用户ID (关联 sys_user.user_id)")
    private Long userId;

    /** 团队ID (关联 qa_team.team_id) */
    @Excel(name = "团队ID (关联 qa_team.team_id)")
    private Long teamId;

    /** 用户在团队中的角色（creator, admin, member） */
    @Excel(name = "用户在团队中的角色", readConverterExp = "c=reator,,a=dmin,,m=ember")
    private String role;

    /** 加入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "加入时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date joinTime;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setTeamId(Long teamId) 
    {
        this.teamId = teamId;
    }

    public Long getTeamId() 
    {
        return teamId;
    }

    public void setRole(String role) 
    {
        this.role = role;
    }

    public String getRole() 
    {
        return role;
    }

    public void setJoinTime(Date joinTime) 
    {
        this.joinTime = joinTime;
    }

    public Date getJoinTime() 
    {
        return joinTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("teamId", getTeamId())
            .append("role", getRole())
            .append("joinTime", getJoinTime())
            .toString();
    }
}
