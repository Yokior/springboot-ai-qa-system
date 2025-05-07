package com.yokior.common.annotation;

import com.yokior.common.constant.TeamConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * 自定义注解 团队权限控制
 *
 * @author yokior
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TeamAuth
{
    // 角色权限
    String[] role() default {};
}
