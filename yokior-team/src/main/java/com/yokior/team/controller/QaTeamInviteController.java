package com.yokior.team.controller;

import com.yokior.common.annotation.Log;
import com.yokior.common.annotation.TeamAuth;
import com.yokior.common.constant.TeamConstants;
import com.yokior.common.core.controller.BaseController;
import com.yokior.common.core.domain.AjaxResult;
import com.yokior.common.enums.BusinessType;
import com.yokior.team.domain.dto.TeamInviteDto;
import com.yokior.team.service.IQaTeamInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/team/my_team/invite")
public class QaTeamInviteController extends BaseController
{
    @Autowired
    private IQaTeamInviteService qaTeamInviteService;
    
    /**
     * 创建团队邀请
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN})
    @PreAuthorize("@ss.hasPermi('team:my_team:invite')")
    @Log(title = "创建团队邀请", businessType = BusinessType.INSERT)
    @PostMapping("/create")
    public AjaxResult createInvite(@RequestBody TeamInviteDto teamInviteDto)
    {
        return success(qaTeamInviteService.createTeamInvite(teamInviteDto));
    }
    
    /**
     * 获取邀请信息
     */
    @GetMapping("/info/{code}")
    public AjaxResult getInviteInfo(@PathVariable("code") String inviteCode)
    {
        return success(qaTeamInviteService.getTeamInviteInfo(inviteCode));
    }
    
    /**
     * 接受团队邀请
     */
    @Log(title = "接受团队邀请", businessType = BusinessType.INSERT)
    @PostMapping("/accept/{code}")
    public AjaxResult acceptInvite(@PathVariable("code") String inviteCode)
    {
        return toAjax(qaTeamInviteService.acceptTeamInvite(inviteCode));
    }
    
    /**
     * 取消团队邀请
     */
    @Log(title = "取消团队邀请", businessType = BusinessType.UPDATE)
    @DeleteMapping("/cancel/{inviteId}")
    public AjaxResult cancelInvite(@PathVariable("inviteId") Long inviteId)
    {
        return toAjax(qaTeamInviteService.cancelTeamInvite(inviteId));
    }
}