package com.yokior.quartz.task;

import com.yokior.team.service.IQaTeamInviteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Yokior
 * @description 有关团队的定时任务
 * @date 2025/5/15 21:13
 */
@Component("teamTask")
@Slf4j
public class TeamTask
{

    @Autowired
    private IQaTeamInviteService qaTeamInviteService;

    /**
     * 删除过期或者取消的邀请
     */
    public void deleteExpiredInvite()
    {
        log.info("定时任务：删除过期或者取消的邀请================>");

        Integer count = qaTeamInviteService.deleteExpiredInvite();
        log.info("删除过期或者取消的邀请，共删除{}条", count);
    }
}
