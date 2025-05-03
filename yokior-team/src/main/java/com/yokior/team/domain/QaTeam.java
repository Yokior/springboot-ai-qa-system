package com.yokior.team.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yokior.common.annotation.Excel;
import com.yokior.common.core.domain.BaseEntity;

/**
 * 知识库团队对象 qa_team
 * 
 * @author yokior
 * @date 2025-05-03
 */
public class QaTeam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 团队ID */
    private Long teamId;

    /** 团队名称 */
    @Excel(name = "团队名称")
    private String name;

    /** 团队描述 */
    @Excel(name = "团队描述")
    private String description;

    /** 团队头像地址 */
    @Excel(name = "团队头像地址")
    private String avatar;

    /** 团队创建者用户ID (关联 sys_user.user_id) */
    @Excel(name = "团队创建者用户ID (关联 sys_user.user_id)")
    private Long ownerUserId;

    /** 团队状态（0正常 1停用/冻结 2待审批） */
    @Excel(name = "团队状态", readConverterExp = "0=正常,1=停用/冻结,2=待审批")
    private String status;

    public void setTeamId(Long teamId) 
    {
        this.teamId = teamId;
    }

    public Long getTeamId() 
    {
        return teamId;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setAvatar(String avatar) 
    {
        this.avatar = avatar;
    }

    public String getAvatar() 
    {
        return avatar;
    }

    public void setOwnerUserId(Long ownerUserId) 
    {
        this.ownerUserId = ownerUserId;
    }

    public Long getOwnerUserId() 
    {
        return ownerUserId;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("teamId", getTeamId())
            .append("name", getName())
            .append("description", getDescription())
            .append("avatar", getAvatar())
            .append("ownerUserId", getOwnerUserId())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
