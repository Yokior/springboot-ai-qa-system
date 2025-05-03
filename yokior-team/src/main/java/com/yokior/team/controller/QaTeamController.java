package com.yokior.team.controller;

import com.yokior.common.annotation.Log;
import com.yokior.common.core.controller.BaseController;
import com.yokior.common.core.domain.AjaxResult;
import com.yokior.common.core.page.TableDataInfo;
import com.yokior.common.enums.BusinessType;
import com.yokior.common.utils.poi.ExcelUtil;
import com.yokior.team.domain.QaTeam;
import com.yokior.team.service.IQaTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.yokior.common.config.YokiorConfig;
import com.yokior.common.utils.file.FileUploadUtils;
import com.yokior.common.utils.file.MimeTypeUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 知识库团队Controller
 * 
 * @author yokior
 * @date 2025-05-03
 */
@RestController
@RequestMapping("/team/qa_team")
public class QaTeamController extends BaseController
{
    @Autowired
    private IQaTeamService qaTeamService;

    /**
     * 查询知识库团队列表
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:list')")
    @GetMapping("/list")
    public TableDataInfo list(QaTeam qaTeam)
    {
        startPage();
        List<QaTeam> list = qaTeamService.selectQaTeamList(qaTeam);
        return getDataTable(list);
    }

    /**
     * 导出知识库团队列表
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:export')")
    @Log(title = "知识库团队", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QaTeam qaTeam)
    {
        List<QaTeam> list = qaTeamService.selectQaTeamList(qaTeam);
        ExcelUtil<QaTeam> util = new ExcelUtil<QaTeam>(QaTeam.class);
        util.exportExcel(response, list, "知识库团队数据");
    }

    /**
     * 获取知识库团队详细信息
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:query')")
    @GetMapping(value = "/{teamId}")
    public AjaxResult getInfo(@PathVariable("teamId") Long teamId)
    {
        return success(qaTeamService.selectQaTeamByTeamId(teamId));
    }

    /**
     * 新增知识库团队
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:add')")
    @Log(title = "知识库团队", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QaTeam qaTeam)
    {
        return toAjax(qaTeamService.insertQaTeam(qaTeam));
    }

    /**
     * 修改知识库团队
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:edit')")
    @Log(title = "知识库团队", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody QaTeam qaTeam)
    {
        return toAjax(qaTeamService.updateQaTeam(qaTeam));
    }

    /**
     * 删除知识库团队
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:remove')")
    @Log(title = "知识库团队", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamIds}")
    public AjaxResult remove(@PathVariable Long[] teamIds)
    {
        return toAjax(qaTeamService.deleteQaTeamByTeamIds(teamIds));
    }
    
    /**
     * 团队头像上传
     */
    @PreAuthorize("@ss.hasPermi('team:qa_team:edit')")
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
}
