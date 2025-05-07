package com.yokior.team.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.yokior.common.annotation.TeamAuth;
import com.yokior.common.config.YokiorConfig;
import com.yokior.common.constant.TeamConstants;
import com.yokior.common.exception.ServiceException;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.common.utils.file.FileUploadUtils;
import com.yokior.common.utils.file.MimeTypeUtils;
import com.yokior.team.domain.QaTeam;
import com.yokior.team.domain.dto.QaUserTeamDto;
import com.yokior.team.domain.dto.TeamMemberDto;
import com.yokior.team.domain.vo.QaTeamVo;
import com.yokior.team.domain.vo.QaUserTeamVo;
import com.yokior.team.domain.vo.TeamMemberVo;
import com.yokior.team.service.IQaTeamService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.yokior.common.annotation.Log;
import com.yokior.common.core.controller.BaseController;
import com.yokior.common.core.domain.AjaxResult;
import com.yokior.common.enums.BusinessType;
import com.yokior.team.domain.QaUserTeam;
import com.yokior.team.service.IQaUserTeamService;
import com.yokior.common.utils.poi.ExcelUtil;
import com.yokior.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 我的团队Controller
 *
 * @author yokior
 * @date 2025-05-04
 */
@RestController
@RequestMapping("/team/my_team")
public class QaUserTeamController extends BaseController
{
    @Autowired
    private IQaTeamService qaTeamService;

    @Autowired
    private IQaUserTeamService qaUserTeamService;

    /**
     * 查询我的团队列表
     */
    @PreAuthorize("@ss.hasPermi('team:my_team:list')")
    @GetMapping("/list")
    public TableDataInfo list(QaUserTeamDto qaUserTeamDto)
    {
        qaUserTeamDto.setUserId(SecurityUtils.getUserId());

        // 检查userId是否为空
        if (qaUserTeamDto.getUserId() == null)
        {
            throw new ServiceException("userId不能为空");
        }

        startPage();
        List<QaTeamVo> list = qaUserTeamService.selectQaUserTeamList(qaUserTeamDto);
        return getDataTable(list);
    }

    /**
     * 导出我的团队列表
     */
//    @PreAuthorize("@ss.hasPermi('team:my_team:export')")
//    @Log(title = "我的团队", businessType = BusinessType.EXPORT)
//    @PostMapping("/export")
//    public void export(HttpServletResponse response, QaUserTeam qaUserTeam)
//    {
//        List<QaUserTeam> list = qaUserTeamService.selectQaUserTeamList(qaUserTeam);
//        ExcelUtil<QaUserTeam> util = new ExcelUtil<QaUserTeam>(QaUserTeam.class);
//        util.exportExcel(response, list, "我的团队数据");
//    }

    /**
     * 获取我的团队详细信息
     */
    @PreAuthorize("@ss.hasPermi('team:my_team:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        QaTeamVo qaTeamVo = qaUserTeamService.selectQaUserTeamById(id);
        return success(qaTeamVo);
    }


    /**
     * 获取我的团队成员信息
     */
    @PreAuthorize("@ss.hasPermi('team:my_team:query')")
    @GetMapping("/member")
    public TableDataInfo getTeamMemberInfo(TeamMemberDto teamMemberDto)
    {
        List<TeamMemberVo> teamMemberVoList = qaUserTeamService.selectTeamMember(teamMemberDto);

        return getDataTable(teamMemberVoList);
    }

    /**
     * 新增我的团队
     */
//    @PreAuthorize("@ss.hasPermi('team:my_team:add')")
//    @Log(title = "我的团队", businessType = BusinessType.INSERT)
//    @PostMapping
//    public AjaxResult add(@RequestBody QaUserTeam qaUserTeam)
//    {
//        return toAjax(qaUserTeamService.insertQaUserTeam(qaUserTeam));
//    }

    /**
     * 修改我的团队详情
     */
    @PreAuthorize("@ss.hasPermi('team:my_team:edit')")
    @Log(title = "我的团队", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QaTeam qaTeam)
    {
        Boolean isSuccess = qaUserTeamService.updateMyTeamInfo(qaTeam);

        return isSuccess ? success("团队信息更新成功") : error("更新团队信息遇到错误");
    }

    /**
     * 修改我的团队角色信息
     */
    @PreAuthorize("@ss.hasPermi('team:my_team:edit')")
    @Log(title = "我的团队", businessType = BusinessType.UPDATE)
    @PutMapping("/update_role")
    public AjaxResult updateRole(@RequestBody QaUserTeam qaUserTeam)
    {
        Boolean isSuccess = qaUserTeamService.updateRole(qaUserTeam);

        return isSuccess ? success("角色信息更新成功") : error("更新角色遇到错误");
    }

    /**
     * 修改团队头像 (创建者和管理员可操作)
     */
    @TeamAuth(role = {TeamConstants.ROLE_CREATOR, TeamConstants.ROLE_ADMIN})
    @PreAuthorize("@ss.hasPermi('team:my_team:edit')")
    @Log(title = "团队头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("teamAvatarFile") MultipartFile file, @RequestParam("teamId") Long teamId) throws Exception
    {
        if (!file.isEmpty())
        {
            // 上传团队头像到指定路径（创建团队头像专用文件夹）
            String avatar = FileUploadUtils.upload(YokiorConfig.getAvatarPath() + "/team", file, MimeTypeUtils.IMAGE_EXTENSION);

            // 更新团队头像信息
            QaTeam qaTeam = new QaTeam();
            qaTeam.setTeamId(teamId);
            qaTeam.setAvatar(avatar);

            if (qaTeamService.updateQaTeam(qaTeam) > 0)
            {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }

    /**
     * 删除我的团队
     */
//    @PreAuthorize("@ss.hasPermi('team:my_team:remove')")
//    @Log(title = "我的团队", businessType = BusinessType.DELETE)
//    @DeleteMapping("/{ids}")
//    public AjaxResult remove(@PathVariable Long[] ids)
//    {
//        return toAjax(qaUserTeamService.deleteQaUserTeamByIds(ids));
//    }
}
