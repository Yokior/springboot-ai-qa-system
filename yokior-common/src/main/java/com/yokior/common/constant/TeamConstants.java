package com.yokior.common.constant;

/**
 * @author Yokior
 * @description 团队常量
 * @date 2025/5/7 15:03
 */
public class TeamConstants
{
    // 团队创建者
    public static final String ROLE_CREATOR = "creator";

    // 团队管理员
    public static final String ROLE_ADMIN = "admin";

    // 团队成员
    public static final String ROLE_MEMBER = "member";

    // 邀请状态
    /** 邀请状态 - 正常 */
    public static final String INVITE_STATUS_NORMAL = "0";
    /** 邀请状态 - 已使用 */
    public static final String INVITE_STATUS_USED = "1";
    /** 邀请状态 - 已取消或过期 */
    public static final String INVITE_STATUS_CANCELED = "2";
}
