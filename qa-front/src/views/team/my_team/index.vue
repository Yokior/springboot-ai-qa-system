<template>
  <div class="app-container my-team-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px" class="search-form">
      <el-form-item label="团队名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入团队名称"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="角色身份" prop="role">
        <el-select v-model="queryParams.role" placeholder="请选择角色" clearable style="width: 240px">
          <el-option
            v-for="item in roleOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="20" class="team-list-row" v-loading="loading">
      <el-col :span="8" v-for="(team, index) in teamList" :key="team.teamId" class="team-col">
         <transition name="el-zoom-in-center">
          <el-card shadow="hover" class="team-card" @click.native="handleTeamClick(team)">
            <div slot="header" class="clearfix card-header">
              <el-avatar size="medium" :src="team.avatar || defaultAvatar" class="team-avatar"></el-avatar>
              <span class="team-name">{{ team.name }}</span>
              <el-tag size="mini" :type="getRoleTagType(team.role)" class="role-tag">{{ getRoleText(team.role) }}</el-tag>
            </div>
            <div class="card-body">
              <p class="team-description">
                <i class="el-icon-document"></i> {{ team.description || '暂无描述' }}
              </p>
              <p class="team-owner">
                <i class="el-icon-user"></i> 创建者: {{ team.ownerUserName }}
              </p>
              <p class="join-time">
                <i class="el-icon-time"></i> 加入时间: {{ parseTime(team.joinTime, '{y}-{m}-{d} {h}:{i}') }}
              </p>
            </div>
             <div class="card-index">#{{ (queryParams.pageNum - 1) * queryParams.pageSize + index + 1 }}</div>
          </el-card>
         </transition>
      </el-col>
      <el-empty v-if="!loading && teamList.length === 0" description="暂无团队数据" :image-size="100"></el-empty>
    </el-row>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listMy_team } from "@/api/team/my_team"

export default {
  name: "MyTeam",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 团队数据列表
      teamList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined, // 对应团队名称搜索
        role: undefined   // 对应角色身份搜索
      },
      // 角色选项
      roleOptions: [
        { value: 'creator', label: '创建者' },
        { value: 'admin', label: '管理员' },
        { value: 'member', label: '普通成员' }
      ],
      // 默认头像路径，根据实际情况修改
      defaultAvatar: require('@/assets/images/profile.jpg') // 或者其他默认图片路径
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询团队列表 */
    getList() {
      this.loading = true
      listMy_team(this.queryParams).then(response => {
        if (response.code === 200) {
          this.teamList = response.rows
          this.total = response.total
        } else {
          this.$modal.msgError(response.msg || "获取团队列表失败")
          this.teamList = []
          this.total = 0
        }
        this.loading = false
      }).catch(() => {
        this.loading = false
        this.$modal.msgError("请求团队列表时发生错误")
        this.teamList = []
        this.total = 0
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 处理团队卡片点击事件 */
    handleTeamClick(team) {
      // 跳转到详情页，确保路由已配置
      this.$router.push(`/team/detail/${team.teamId}`)
      // 或者如果你使用命名路由：
      // this.$router.push({ name: 'TeamDetail', params: { teamId: team.teamId } })
    },
    /** 获取角色显示文本 */
    getRoleText(role) {
      const roleMap = {
        creator: '创建者',
        admin: '管理员',
        member: '普通成员'
      }
      return roleMap[role] || '未知角色'
    },
    /** 获取角色标签类型 */
    getRoleTagType(role) {
      switch (role) {
        case 'creator': return 'success'
        case 'admin': return 'warning'
        case 'member': return 'info'
        default: return 'info'
      }
    }
  }
}
</script>

<style scoped lang="scss">
.my-team-container {
  padding: 20px;
  background-color: #f0f2f5; // 淡灰色背景，提升质感
}

.search-form {
  background-color: #ffffff;
  padding: 15px 20px 0;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.team-list-row {
  min-height: 300px; /* 防止加载时高度塌陷 */
}

.team-col {
  margin-bottom: 20px;
}

.team-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative; /* For absolute positioning of index */
  overflow: hidden; /* Hide index overflow */

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  }

  .card-header {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #ebeeef; // 更柔和的分割线
    padding-bottom: 10px;
  }

  .team-avatar {
    margin-right: 12px;
    flex-shrink: 0; // 防止头像被压缩
  }

  .team-name {
    font-weight: bold;
    font-size: 16px;
    color: #303133;
    flex-grow: 1; // 占据剩余空间
    margin-right: 10px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .role-tag {
    flex-shrink: 0; // 防止标签被压缩
  }

  .card-body {
    font-size: 14px;
    color: #606266;
    line-height: 1.6;

    p {
      margin: 8px 0;
      display: flex;
      align-items: center;
      i {
        margin-right: 6px;
        color: #909399;
      }
    }

    .team-description {
       min-height: 1.6em; // 保证即使没描述也有行高
       display: -webkit-box;
       -webkit-line-clamp: 2; // 最多显示两行
       -webkit-box-orient: vertical;
       overflow: hidden;
       text-overflow: ellipsis;
    }
  }

   .card-index {
    position: absolute;
    top: 0;
    right: 0;
    background-color: rgba(0, 0, 0, 0.4);
    color: white;
    padding: 2px 8px;
    font-size: 12px;
    border-bottom-left-radius: 8px;
   }
}

/* 列表动画 */
.list-anim-enter-active, .list-anim-leave-active {
  transition: all 0.5s ease;
}
.list-anim-enter, .list-anim-leave-to /* .list-anim-leave-active for <2.1.8 */ {
  opacity: 0;
  transform: translateY(30px);
}

.el-empty {
  width: 100%;
}
</style>
