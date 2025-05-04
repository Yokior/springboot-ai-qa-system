package com.yokior.framework.aspectj;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import com.yokior.common.annotation.DataScope;
import com.yokior.common.constant.UserConstants;
import com.yokior.common.core.domain.BaseEntity;
import com.yokior.common.core.domain.entity.SysRole;
import com.yokior.common.core.domain.entity.SysUser;
import com.yokior.common.core.domain.model.LoginUser;
import com.yokior.common.core.text.Convert;
import com.yokior.common.utils.SecurityUtils;
import com.yokior.common.utils.StringUtils;
import com.yokior.framework.security.context.PermissionContextHolder;

/**
 * 数据过滤处理切面
 * 本切面用于实现数据权限控制，基于用户角色自动为SQL查询添加过滤条件
 * 适用于各种需要根据用户权限级别控制数据访问范围的场景
 *
 * @author yokior
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 全部数据权限
     * 使用此权限级别的用户可以查看系统中的所有数据，不会添加任何过滤条件
     */
    public static final String DATA_SCOPE_ALL = "1";

    /**
     * 自定数据权限
     * 使用此权限级别的用户只能查看被明确分配给其角色的部门数据
     */
    public static final String DATA_SCOPE_CUSTOM = "2";

    /**
     * 数据权限过滤关键字
     * 用于在SQL语句中添加数据权限过滤条件的参数名
     */
    public static final String DATA_SCOPE = "dataScope";

    /**
     * 前置通知，在执行标注了@DataScope注解的方法之前，进行数据权限处理
     *
     * @param point               切点，包含被拦截方法的信息
     * @param controllerDataScope 数据权限注解对象，包含注解上配置的参数
     * @throws Throwable 可能抛出的异常
     */
    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        // 清除之前的数据权限参数，避免参数注入安全问题
        clearDataScope(point);
        // 根据注解配置处理数据权限
        handleDataScope(point, controllerDataScope);
    }

    /**
     * 处理数据权限过滤逻辑
     * 根据用户角色的数据权限配置生成相应的SQL过滤条件
     *
     * @param joinPoint           切点
     * @param controllerDataScope 数据权限注解
     */
    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope)
    {
        // 获取当前的用户登录信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            SysUser currentUser = loginUser.getUser();
            // 如果是超级管理员，则不进行数据过滤，可以查看所有数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                // 获取注解中配置的权限字符串，如果为空则从上下文中获取
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                // 调用数据权限过滤方法，生成SQL过滤条件
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.teamAlias(), controllerDataScope.userAlias(), permission);
            }
        }
    }

    /**
     * 数据范围过滤方法
     * 根据用户角色的数据权限配置，构建相应的SQL过滤条件
     *
     * @param joinPoint  切点
     * @param user       当前用户
     * @param teamAlias  团队表的别名
     * @param userAlias  用户表的别名
     * @param permission 权限字符
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String teamAlias, String userAlias, String permission)
    {
        // 用于构建SQL条件的StringBuilder
        StringBuilder sqlString = new StringBuilder();
        // 存储已处理的数据权限类型，避免重复处理
        List<String> conditions = new ArrayList<String>();
        // 存储自定义数据权限的角色ID，用于IN查询优化
        List<String> scopeCustomIds = new ArrayList<String>();

        // 首先筛选出具有自定义数据权限的角色ID
        user.getRoles().forEach(role ->
        {
            if (DATA_SCOPE_CUSTOM.equals(role.getDataScope()) && StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });

        // 遍历用户所有角色，根据角色的数据权限类型构建SQL条件
        for (SysRole role : user.getRoles())
        {
            String dataScope = role.getDataScope();
            // 如果此类型数据权限已处理或角色已禁用，则跳过
            if (conditions.contains(dataScope) || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE))
            {
                continue;
            }
            // 如果角色没有包含当前操作的权限，则跳过
            if (!StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                continue;
            }

            // 根据不同的数据权限类型构建不同的SQL条件
            if (DATA_SCOPE_ALL.equals(dataScope))
            {
                // 全部数据权限，清空已构建的条件并退出循环
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            }
//            else if (DATA_SCOPE_CUSTOM.equals(dataScope))
//            {
//                // 自定义数据权限，通过角色和部门关联表查询
//                if (scopeCustomIds.size() > 1)
//                {
//                    // 多个自定数据权限角色使用IN查询，提升性能
//                    sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id in ({}) ) ", teamAlias, String.join(",", scopeCustomIds)));
//                }
//                else
//                {
//                    // 单个自定数据权限角色
//                    sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", teamAlias, role.getRoleId()));
//                }
//            }
            else if (DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                // 查询2个表关联，查询团队和用户关联表
                sqlString.append(StringUtils.format(" OR {}.team_id IN ( SELECT team_id FROM qa_user_team WHERE user_id = {} ) ", teamAlias, user.getUserId()));
            }

            conditions.add(dataScope);
        }

        // 如果角色没有包含任何权限，添加一个不可能满足的条件确保查不到数据
        if (StringUtils.isEmpty(conditions))
        {
            sqlString.append(StringUtils.format(" OR {}.owner_user_id = 0 ", teamAlias));
        }

        // 如果生成了SQL条件，将其添加到查询参数中
        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            Object params = joinPoint.getArgs()[0];
            if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
            {
                // 将构建好的条件加入到BaseEntity的参数中，后续SQL执行时会使用这个条件
                // 注意：去掉条件最前面的" OR "，转为" AND (...)"形式
                BaseEntity baseEntity = (BaseEntity) params;
                baseEntity.getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    /**
     * 拼接权限SQL前先清空params.dataScope参数，防止SQL注入风险
     * 确保每次构建数据权限条件前都是从空白开始
     *
     * @param joinPoint 切点
     */
    private void clearDataScope(final JoinPoint joinPoint)
    {
        Object params = joinPoint.getArgs()[0];
        if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
        {
            BaseEntity baseEntity = (BaseEntity) params;
            // 清空之前可能存在的数据权限参数
            baseEntity.getParams().put(DATA_SCOPE, "");
        }
    }
}
