package com.yokior.framework.aspectj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.common.annotation.TeamAuth;
import com.yokior.common.core.domain.AjaxResult;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.team.service.IQaTeamService;
import com.yokior.team.service.IQaUserTeamService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Yokior
 * @description 权限校验切面，用于校验用户是否具有某个团队的权限，params和body中的teamId参数
 * @date 2025/5/7 17:38
 */
@Aspect
@Component
public class TeamAuthAspect
{
    @Autowired
    private IQaUserTeamService qaUserTeamService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 切面方法 用于注解@TeamAuth 注解的方法
     */
    @Around("@annotation(teamAuth)")
    public Object around(ProceedingJoinPoint joinPoint, TeamAuth teamAuth) throws Throwable
    {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        Long teamId = null;

        // 1. 先尝试从 query 或 form-data 中获取
        String paramTeamId = request.getParameter("teamId");
        if (paramTeamId != null && !paramTeamId.isEmpty())
        {
            try
            {
                teamId = Long.valueOf(paramTeamId);
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        // 2. 如果没有 则尝试从 body 中获取
        if (teamId == null)
        {
            String body = readBody(request);
            if (body != null && !body.isEmpty())
            {
                try
                {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonMap = objectMapper.readValue(body, Map.class);
                    Object value = jsonMap.get("teamId");
                    if (value != null)
                    {
                        teamId = value instanceof Number ? ((Number) value).longValue() : Long.valueOf(value.toString());
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        }

        if (teamId == null)
        {
            return AjaxResult.error("缺少 teamId 参数");
        }

        // 3. 执行权限校验
        String[] requiredRoles = teamAuth.role();
        boolean hasPermission = checkPermission(teamId, requiredRoles);
        if (!hasPermission)
        {
            return AjaxResult.error("无操作权限");
        }

        // 4. 放行
        return joinPoint.proceed();
    }

    /**
     * 读取请求体内容（使用缓存后可重复读）
     */
    private String readBody(HttpServletRequest request) throws IOException
    {
        if (request instanceof ContentCachingRequestWrapper)
        {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] contentAsByteArray = wrapper.getContentAsByteArray();
            return new String(contentAsByteArray, wrapper.getCharacterEncoding());
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader())
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
    }

    /**
     * 权限校验逻辑（请替换为你的实际业务逻辑）
     */
    private boolean checkPermission(Long teamId, String[] requiredRoles)
    {
        Long userId = SecurityUtils.getUserId();
        String userRole = qaUserTeamService.getUserRole(teamId, userId);

        if (userRole == null)
        {
            return false;
        }

        if (requiredRoles.length == 0)
        {
            return true;
        }

        // 检查用户角色是否在允许的范围内
        for (String requiredRole : requiredRoles)
        {
            if (requiredRole.equals(userRole))
            {
                return true;
            }
        }

        return false;
    }
}
