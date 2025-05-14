<template>
  <div class="app-container team-detail-container">
    <el-page-header @back="goBack" :content="pageTitle" class="page-header"></el-page-header>
    
    <!-- 加载中状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton style="width: 100%" animated>
        <template slot="template">
          <el-skeleton-item variant="image" style="width: 100%; height: 160px" />
          <div style="padding: 14px;">
            <el-skeleton-item variant="h3" style="width: 50%" />
            <div style="display: flex; justify-content: space-between; margin-top: 20px;">
              <el-skeleton-item variant="text" style="margin-right: 16px; width: 30%" />
              <el-skeleton-item variant="text" style="width: 30%" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
    
    <!-- 团队详情内容 -->
    <div v-else-if="teamInfo">
      <!-- 团队基本信息 -->
      <el-card class="box-card info-card" :body-style="{ padding: '0px' }">
        <div class="team-info-header">
          <div class="team-avatar-container">
            <el-avatar :size="80" :src="getImageUrl(teamInfo.avatar)"></el-avatar>
          </div>
          <div class="team-info-content">
            <div class="team-name-container">
              <h2 class="team-name">{{ teamInfo.name }}</h2>
              <el-tag class="role-tag" :type="getRoleTagType(teamInfo.role)">{{ getRoleText(teamInfo.role) }}</el-tag>
            </div>
            <div class="team-stats">
              <div class="stat-item">
                <i class="el-icon-user"></i> 
                <span>创建者: {{ teamInfo.ownerUserName }}</span>
              </div>
              <div class="stat-item">
                <i class="el-icon-time"></i> 
                <span>加入时间: {{ formatTime(teamInfo.joinTime) }}</span>
              </div>
              <div class="stat-item">
                <i class="el-icon-user-solid"></i> 
                <span>成员数量: {{ total }}</span>
              </div>
            </div>
            <p class="team-description">{{ teamInfo.description || '暂无团队描述' }}</p>
          </div>
        </div>
        <div class="team-actions">
          <!-- 创建者可以编辑、转让和解散团队 -->
          <div v-if="isCreator">
            <el-button type="primary" size="small" icon="el-icon-edit" @click="handleEdit">编辑团队</el-button>
            <el-button type="warning" size="small" icon="el-icon-share" @click="handleTransfer">转让团队</el-button>
            <el-button type="danger" size="small" icon="el-icon-delete" @click="handleDissolve">解散团队</el-button>
            <el-button type="success" size="small" icon="el-icon-plus" @click="handleInvite">邀请成员</el-button>
          </div>
        </div>
      </el-card>

      <!-- 成员列表 -->
      <el-card class="box-card member-card">
        <div slot="header" class="member-header">
          <div class="header-left">
            <span class="card-title">团队成员</span>
            <span class="member-count" v-if="total > 0">(共 {{ total }} 人)</span>
          </div>
          <div class="member-search-container">
            <el-input
              v-model="queryParams.name"
              placeholder="搜索成员"
              prefix-icon="el-icon-search"
              clearable
              size="small"
              class="member-search"
              @keyup.enter.native="handleMemberSearch"
            ></el-input>
            <el-select 
              v-model="queryParams.role" 
              placeholder="角色筛选" 
              clearable 
              size="small"
              class="role-filter"
              @change="handleMemberSearch">
              <el-option
                v-for="item in roleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button 
              type="primary" 
              icon="el-icon-search" 
              size="small"
              @click="handleMemberSearch">搜索</el-button>
            <el-button 
              icon="el-icon-refresh" 
              size="small"
              @click="resetMemberSearch">重置</el-button>
          </div>
        </div>
        <el-table
          :data="teamMembers"
          style="width: 100%"
          row-key="userId"
          border
          stripe
          :header-cell-style="{background:'#f8f8f9',color:'#606266'}"
          :cell-style="{ padding: '8px 0' }"
          :table-layout="'fixed'"
        >
          <el-table-column label="头像" prop="avatar" width="60" align="center">
            <template slot-scope="scope">
              <el-avatar :size="32" :src="getImageUrl(scope.row.avatar)"></el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="nickName" label="昵称" min-width="100" show-overflow-tooltip>
            <template slot-scope="scope">
              <div class="nickname-cell">
                <span>{{ scope.row.nickName || '未设置昵称' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="role" label="角色" width="120" align="center">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getRoleTagType(scope.row.role)">{{ getRoleText(scope.row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="joinTime" label="加入时间" width="300" align="center">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.joinTime) || '未知' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template slot-scope="scope">
              <div class="member-action-container">
                <!-- 仅创建者可以设置成员角色 -->
                <el-tooltip content="设置角色" placement="top" :enterable="false" v-if="isCreator && scope.row.role !== 'creator'">
                  <el-dropdown @command="(command) => handleRoleChange(command, scope.row)" size="mini">
                    <el-button type="primary" size="mini" circle>
                      <i class="el-icon-setting"></i>
                    </el-button>
                    <el-dropdown-menu slot="dropdown">
                      <el-dropdown-item command="admin" :disabled="scope.row.role === 'admin'">设为管理员</el-dropdown-item>
                      <el-dropdown-item command="member" :disabled="scope.row.role === 'member'">设为普通成员</el-dropdown-item>
                    </el-dropdown-menu>
                  </el-dropdown>
                </el-tooltip>
                
                <!-- 创建者和管理员可以踢出成员 -->
                <el-tooltip content="移除成员" placement="top" :enterable="false" v-if="(isCreator || isAdmin) && scope.row.role !== 'creator' && (isCreator || scope.row.role !== 'admin')">
                  <el-button 
                    type="danger" 
                    size="mini"
                    icon="el-icon-delete" 
                    circle
                    @click="handleRemoveMember(scope.row)"
                  ></el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页组件 -->
        <div class="pagination-container">
          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="queryParams.pageNum"
            :page-sizes="[10, 20, 30, 50]"
            :page-size="queryParams.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total">
          </el-pagination>
        </div>
      </el-card>
    </div>
    <div v-else class="no-team-info">
      <el-empty description="未找到团队信息" :image-size="200">
        <el-button type="primary" @click="goBack">返回团队列表</el-button>
      </el-empty>
    </div>
    
    <!-- 编辑团队对话框 -->
    <el-dialog :title="'编辑团队信息'" :visible.sync="editDialogVisible" width="500px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="团队名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入团队名称" />
        </el-form-item>
        <el-form-item label="团队描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" placeholder="请输入团队描述" 
            maxlength="200" show-word-limit :autosize="{ minRows: 3, maxRows: 6 }" />
        </el-form-item>
        <el-form-item label="团队头像">
          <div class="team-avatar-container">
            <team-avatar v-model="editForm.avatar" :teamId="editForm.teamId" @upload-success="handleAvatarSuccess" ref="teamAvatar"></team-avatar>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEditForm">确 定</el-button>
      </div>
    </el-dialog>
    
    <!-- 转让团队对话框 -->
    <el-dialog title="转让团队" :visible.sync="transferDialogVisible" width="500px" append-to-body>
      <el-form ref="transferForm" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-form-item label="转让给" prop="targetUserId">
          <el-select v-model="transferForm.targetUserId" filterable placeholder="请选择团队成员">
            <el-option
              v-for="member in teamMembers.filter(m => m.role !== 'creator')"
              :key="member.userId"
              :label="member.nickName || '未设置昵称'"
              :value="member.userId">
              <div class="member-option">
                <el-avatar :size="24" :src="getImageUrl(member.avatar)"></el-avatar>
                <span style="margin-left: 8px">{{ member.nickName || '未设置昵称' }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="确认密码" prop="password">
          <el-input v-model="transferForm.password" type="password" placeholder="请输入您的密码以确认转让" show-password />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="transferDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitTransferForm">确 定</el-button>
      </div>
    </el-dialog>
    
    <!-- 解散团队对话框 -->
    <el-dialog title="解散团队" :visible.sync="dissolveDialogVisible" width="500px" append-to-body>
      <div class="dissolve-warning">
        <i class="el-icon-warning" style="font-size: 24px; color: #E6A23C; margin-right: 10px;"></i>
        <span>解散团队后，所有团队数据将被<strong style="color: #F56C6C;">永久删除</strong>且无法恢复！</span>
      </div>
      <el-form ref="dissolveForm" :model="dissolveForm" :rules="dissolveRules" label-width="100px" style="margin-top: 20px;">
        <el-form-item label="确认密码" prop="password">
          <el-input v-model="dissolveForm.password" type="password" placeholder="请输入您的密码以确认解散团队" show-password />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dissolveDialogVisible = false">取 消</el-button>
        <el-button type="danger" @click="submitDissolveForm">确认解散</el-button>
      </div>
    </el-dialog>
    
    <!-- 邀请成员对话框 -->
    <el-dialog title="邀请成员" :visible.sync="inviteDialogVisible" width="500px" append-to-body>
      <el-form ref="inviteForm" :model="inviteForm" :rules="inviteRules" label-width="100px">
        <el-form-item label="有效期" prop="expireTimeHour">
          <el-select v-model="inviteForm.expireTimeHour" placeholder="请选择邀请链接有效期">
            <el-option label="1小时" value="1"></el-option>
            <el-option label="1天" value="24"></el-option>
            <el-option label="7天" value="168"></el-option>
            <el-option label="30天" value="720"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="inviteDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="generateInviteLink">生成邀请链接</el-button>
      </div>
    </el-dialog>
    
    <!-- 邀请链接对话框 -->
    <el-dialog title="邀请链接" :visible.sync="inviteLinkDialogVisible" width="500px" append-to-body>
      <div class="invite-link-container">
        <p>邀请链接已生成，有效期至: {{ formatTime(inviteLink.expireTime) }}</p>
        <el-input
          ref="inviteLinkInput"
          v-model="inviteLink.url"
          readonly
          size="medium"
        >
          <el-button slot="append" icon="el-icon-copy-document" @click="copyInviteLink">复制</el-button>
        </el-input>
        <div class="qrcode-container" v-if="qrCodeUrl">
          <p>或使用二维码邀请：</p>
          <img :src="qrCodeUrl" alt="邀请二维码" class="invite-qrcode" />
          <el-button size="small" type="text" @click="downloadQRCode">下载二维码</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getMy_team, updateMy_team, delMy_team, transferTeam, removeMember, updateMemberRole, uploadTeamAvatar, listTeamMembers, createTeamInvite, getTeamInviteInfo, acceptTeamInvite, cancelTeamInvite } from "@/api/team/my_team";
import { getToken } from "@/utils/auth";
import TeamAvatar from "@/views/team/qa_team/teamAvatar"; // 导入 TeamAvatar 组件

export default {
  name: "TeamDetail",
  components: { TeamAvatar }, // 注册组件
  dicts: ['sys_normal_disable'],
  data() {
    return {
      // 基础信息
      teamId: null,
      pageTitle: "团队详情",
      loading: true,
      teamInfo: null,
      teamMembers: [],
      defaultAvatar: require('@/assets/images/profile.jpg'), // 默认头像
      
      // 分页参数
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: '',
        role: '',
        teamId: null
      },
      
      // 成员搜索
      memberSearchKeyword: '',
      lastSearchKeyword: '',
      
      // 编辑团队表单相关
      editDialogVisible: false,
      editForm: {
        teamId: null,
        name: '',
        description: '',
        avatar: ''
      },
      editRules: {
        name: [
          { required: true, message: "团队名称不能为空", trigger: "blur" },
          { min: 2, max: 30, message: "长度在 2 到 30 个字符", trigger: "blur" }
        ]
      },
      
      // 转让团队对话框相关
      transferDialogVisible: false,
      transferForm: {
        teamId: null,
        targetUserId: null,
        password: ''
      },
      transferRules: {
        targetUserId: [
          { required: true, message: "请选择要转让的成员", trigger: "change" }
        ],
        password: [
          { required: true, message: "请输入密码", trigger: "blur" }
        ]
      },
      
      // 解散团队对话框相关
      dissolveDialogVisible: false,
      dissolveForm: {
        teamId: null,
        password: ''
      },
      dissolveRules: {
        password: [
          { required: true, message: "请输入密码", trigger: "blur" }
        ]
      },
      
      // 角色选项
      roleOptions: [
        { value: 'creator', label: '创建者' },
        { value: 'admin', label: '管理员' },
        { value: 'member', label: '普通成员' }
      ],
      
      // 邀请相关
      inviteDialogVisible: false,
      inviteForm: {
        teamId: null,
        expireTimeHour: '24', // 默认1天
      },
      inviteRules: {
        expireTime: [
          { required: true, message: "请选择有效期", trigger: "change" }
        ]
      },
      inviteLink: {
        code: '',
        url: '',
        expireTime: null
      },
      qrCodeUrl: '',
      inviteLinkDialogVisible: false
    };
  },
  computed: {
    // 当前用户是否为创建者
    isCreator() {
      return this.teamInfo && this.teamInfo.role === 'creator';
    },
    
    // 当前用户是否为管理员
    isAdmin() {
      return this.teamInfo && this.teamInfo.role === 'admin';
    }
  },
  watch: {
    // 监听查询参数变化
    'queryParams.name'() {
      if (this.queryParams.name !== this.lastSearchKeyword) {
        this.lastSearchKeyword = this.queryParams.name;
      }
    }
  },
  created() {
    // 从路由参数中获取 teamId
    console.log('路由对象:', this.$route);
    console.log('路由参数:', this.$route.params);
    
    this.teamId = this.$route.params.teamId;
    // 转换为数字类型，因为路由参数通常是字符串
    if (this.teamId) {
      this.teamId = parseInt(this.teamId, 10);
      console.log('转换后的团队ID:', this.teamId);
      this.pageTitle = `团队详情`;
      this.queryParams.teamId = this.teamId; // 设置查询参数中的teamId
      this.getTeamDetails();
      this.getTeamMembers(); // 获取团队成员
    } else {
      this.loading = false;
      this.pageTitle = "无效的团队";
      console.error('未从路由参数获取到团队ID');
    }
  },
  methods: {
    // 获取图片URL
    getImageUrl(avatar) {
      if (!avatar) return this.defaultAvatar;
      if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
        return avatar;
      }
      if (avatar.startsWith('data:image/')) {
        return avatar;
      }
      return process.env.VUE_APP_BASE_API + avatar;
    },
    
    // 获取团队详情
    getTeamDetails() {
      this.loading = true;
      console.log('获取团队详情 - 团队ID:', this.teamId);
      
      getMy_team(this.teamId).then(response => {
        console.log('团队详情响应:', response);
        if (response.code === 200 && response.data) {
          // 直接使用返回的团队信息对象作为 teamInfo
          this.teamInfo = response.data;
          
          // 设置页面标题
          this.pageTitle = `团队详情 - ${this.teamInfo.name}`;
          
          // 初始化编辑表单数据
          this.editForm = {
            teamId: this.teamInfo.teamId,
            name: this.teamInfo.name,
            description: this.teamInfo.description,
            avatar: this.teamInfo.avatar
          };
          
          // 初始化转让表单
          this.transferForm.teamId = this.teamInfo.teamId;
          
          // 初始化解散表单
          this.dissolveForm.teamId = this.teamInfo.teamId;
        } else {
          this.$modal.msgError(response.msg || "获取团队详情失败");
        }
        this.loading = false;
      }).catch(error => {
        console.error("获取团队详情出错:", error);
        this.$modal.msgError("获取团队详情时发生错误");
        this.loading = false;
      });
    },
    
    // 获取团队成员列表
    getTeamMembers() {
      listTeamMembers(this.queryParams).then(response => {
        if (response.code === 200) {
          this.teamMembers = response.rows || [];
          this.total = response.total || 0;
        } else {
          this.$modal.msgError(response.msg || "获取团队成员失败");
          this.teamMembers = [];
          this.total = 0;
        }
      }).catch(error => {
        console.error("获取团队成员出错:", error);
        this.$modal.msgError("获取团队成员时发生错误");
        this.teamMembers = [];
        this.total = 0;
      });
    },
    
    // 返回团队列表
    goBack() {
      console.log('返回团队列表');
      this.$router.push('/team/my_team/index');
    },
    
    // 获取角色显示文本
    getRoleText(role) {
      const roleMap = {
        creator: '创建者',
        admin: '管理员',
        member: '普通成员'
      };
      return roleMap[role] || '未知角色';
    },
    
    // 获取角色标签类型
    getRoleTagType(role) {
      switch (role) {
        case 'creator': return 'success';
        case 'admin': return 'warning';
        case 'member': return 'info';
        default: return 'info';
      }
    },
    
    // 处理编辑团队信息
    handleEdit() {
      this.editDialogVisible = true;
    },
    
    // 提交编辑表单
    submitEditForm() {
      this.$refs.editForm.validate(valid => {
        if (valid) {
          // 头像上传处理
          const uploadAvatar = async () => {
            // 获取TeamAvatar组件实例
            const teamAvatarRef = this.$refs.teamAvatar;
            
            // 检查是否有新选择的头像
            if (teamAvatarRef && this.editForm.avatar === 'pending_upload') {
              try {
                // 上传头像
                this.$modal.loading("正在上传头像，请稍候...");
                const avatarPath = await teamAvatarRef.uploadImage();
                this.$modal.closeLoading();
                // 更新表单中的头像路径
                this.editForm.avatar = avatarPath;
              } catch (error) {
                this.$modal.closeLoading();
                this.$modal.msgError("头像上传失败: " + (error.message || "未知错误"));
                return false;
              }
            }
            return true;
          };
          
          // 执行头像上传和表单提交
          const submitFormData = async () => {
            // 先上传头像
            const uploadSuccess = await uploadAvatar();
            if (!uploadSuccess) return;
            
            const data = {
              teamId: this.editForm.teamId,
              name: this.editForm.name,
              description: this.editForm.description,
              avatar: this.editForm.avatar
            };
            
            updateMy_team(data).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("更新团队信息成功");
                this.editDialogVisible = false;
                this.getTeamDetails(); // 刷新团队信息
              } else {
                this.$modal.msgError(response.msg || "更新团队信息失败");
              }
            }).catch(error => {
              console.error("更新团队信息出错:", error);
              this.$modal.msgError("更新团队信息时发生错误: " + (error.message || "请检查网络或联系管理员"));
            });
          };
          
          submitFormData();
        }
      });
    },
    
    // 处理团队转让
    handleTransfer() {
      this.transferDialogVisible = true;
      this.transferForm.targetUserId = null;
      this.transferForm.password = '';
    },
    
    // 提交转让表单
    submitTransferForm() {
      this.$refs.transferForm.validate(valid => {
        if (valid) {
          this.$modal.confirm('确定要将团队转让给该成员吗？转让后您将变成普通成员，且无法撤销。').then(() => {
            transferTeam(this.transferForm).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("团队转让成功");
                this.transferDialogVisible = false;
                this.getTeamDetails(); // 刷新团队信息
                this.getTeamMembers(); // 刷新团队成员列表
              } else {
                this.$modal.msgError(response.msg || "团队转让失败");
              }
            }).catch(error => {
              console.error("团队转让出错:", error);
              this.$modal.msgError("团队转让时发生错误");
            });
          }).catch(() => {});
        }
      });
    },
    
    // 处理解散团队
    handleDissolve() {
      this.dissolveDialogVisible = true;
      this.dissolveForm.password = '';
    },
    
    // 提交解散表单
    submitDissolveForm() {
      this.$refs.dissolveForm.validate(valid => {
        if (valid) {
          this.$modal.confirm('确定要解散该团队吗？所有团队数据将被永久删除且无法恢复！').then(() => {
            // 构建解散请求数据
            const data = {
              teamId: this.dissolveForm.teamId,
              password: this.dissolveForm.password
            };
            
            delMy_team(data).then(response => {
              if (response.code === 200) {
                this.$modal.msgSuccess("团队已解散");
                this.dissolveDialogVisible = false;
                this.goBack(); // 返回团队列表
              } else {
                this.$modal.msgError(response.msg || "解散团队失败");
              }
            }).catch(error => {
              console.error("解散团队出错:", error);
              this.$modal.msgError("解散团队时发生错误");
            });
          }).catch(() => {});
        }
      });
    },
    
    // 移除团队成员
    handleRemoveMember(member) {
      this.$modal.confirm(`确定要移除团队成员 "${member.nickName || '未设置昵称'}" 吗？`).then(() => {
        const data = {
          teamId: this.teamId,
          userId: member.userId
        };
        
        removeMember(data).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("移除成员成功");
            this.getTeamDetails(); // 刷新团队信息
            this.getTeamMembers(); // 刷新团队成员列表
          } else {
            this.$modal.msgError(response.msg || "移除成员失败");
          }
        }).catch(error => {
          console.error("移除成员出错:", error);
          this.$modal.msgError("移除成员时发生错误");
        });
      }).catch(() => {});
    },
    
    // 更改成员角色
    handleRoleChange(role, member) {
      this.$modal.confirm(`确定要将 "${member.nickName || '未设置昵称'}" 的角色修改为"${this.getRoleText(role)}"吗？`).then(() => {
        const data = {
          teamId: this.teamId,
          userId: member.userId,
          role: role
        };
        
        updateMemberRole(data).then(response => {
          if (response.code === 200) {
            this.$modal.msgSuccess("角色修改成功");
            this.getTeamDetails(); // 刷新团队信息
            this.getTeamMembers(); // 刷新团队成员列表
          } else {
            this.$modal.msgError(response.msg || "角色修改失败");
          }
        }).catch(error => {
          console.error("修改角色出错:", error);
          this.$modal.msgError("修改角色时发生错误");
        });
      }).catch(() => {});
    },
    
    // 头像上传成功处理
    handleAvatarSuccess(avatarPath) {
      this.editForm.avatar = avatarPath;
      this.$message.success('头像上传成功');
    },
    
    // 处理页码变化
    handleCurrentChange(pageNum) {
      this.queryParams.pageNum = pageNum;
      this.getTeamMembers();
    },
    
    // 处理每页条数变化
    handleSizeChange(pageSize) {
      this.queryParams.pageSize = pageSize;
      this.queryParams.pageNum = 1;
      this.getTeamMembers();
    },
    
    // 格式化时间 (与 parseTime 相同但更健壮)
    formatTime(time) {
      if (!time) return '未知';
      try {
        // 确保time是Date对象
        let date = time;
        if (!(time instanceof Date)) {
          // 如果是时间戳或字符串，转换为Date对象
          date = new Date(time);
        }
        
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.error('无效的日期:', time);
          return '无效的日期';
        }
        
        // 使用更明确的方式格式化日期
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      } catch (error) {
        console.error('格式化日期时出错:', error, time);
        return '日期格式错误';
      }
    },
    
    // 兼容原有代码的解析时间方法
    parseTime(time, pattern) {
      return this.formatTime(time);
    },
    
    // 处理成员搜索
    handleMemberSearch() {
      this.queryParams.pageNum = 1;
      this.getTeamMembers();
    },
    
    // 重置成员搜索
    resetMemberSearch() {
      this.queryParams.name = '';
      this.queryParams.role = '';
      this.queryParams.pageNum = 1;
      this.getTeamMembers();
    },
    
    // 处理邀请
    handleInvite() {
      this.inviteDialogVisible = true;
      this.inviteForm.teamId = this.teamId;
    },
    
    // 生成邀请链接
    generateInviteLink() {
      this.$refs.inviteForm.validate(valid => {
        if (valid) {
          // 调用API生成邀请链接
          createTeamInvite(this.inviteForm).then(response => {
            if (response.code === 200) {
              const inviteData = response.data;
              console.log('收到的邀请数据:', inviteData); // 添加日志查看接收到的数据
              
              // 确保正确处理日期
              let expireTime = inviteData.expireTime;
              if (expireTime) {
                // 如果是时间戳（数字），转换为日期对象
                if (typeof expireTime === 'number') {
                  expireTime = new Date(expireTime);
                } else if (typeof expireTime === 'string') {
                  // 尝试解析日期字符串
                  expireTime = new Date(expireTime);
                }
              }
              
              this.inviteLink = {
                code: inviteData.inviteCode,
                url: `${window.location.origin}/team/invite/accept/${inviteData.inviteCode}`,
                expireTime: expireTime
              };
              
              // 生成二维码
              this.generateQRCode(this.inviteLink.url);
              
              this.inviteDialogVisible = false;
              this.inviteLinkDialogVisible = true;
            } else {
              this.$modal.msgError(response.msg || "生成邀请链接失败");
            }
          }).catch(error => {
            console.error("生成邀请链接出错:", error);
            this.$modal.msgError("生成邀请链接时发生错误");
          });
        }
      });
    },
    
    // 生成二维码
    generateQRCode(url) {
      // 使用QRCode.js库生成二维码
      import('qrcode').then(QRCode => {
        QRCode.toDataURL(url, { width: 200 }, (err, url) => {
          if (!err) {
            this.qrCodeUrl = url;
          }
        });
      });
    },
    
    // 复制邀请链接
    copyInviteLink() {
      const input = this.$refs.inviteLinkInput.$el.querySelector('input');
      input.select();
      document.execCommand('copy');
      this.$message.success('邀请链接已复制到剪贴板');
    },
    
    // 下载二维码
    downloadQRCode() {
      const link = document.createElement('a');
      link.href = this.qrCodeUrl;
      link.download = `${this.teamInfo.name}_邀请.png`;
      link.click();
    }
  }
};
</script>

<style scoped lang="scss">
.team-detail-container {
  padding: 20px;
  background-color: #e8ecf0;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px 0 rgba(0,0,0,0.1);
  border: 1px solid #dcdfe6;
}

.loading-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px 0 rgba(0,0,0,0.1);
  border: 1px solid #dcdfe6;
}

.info-card, .member-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px 0 rgba(0,0,0,0.15);
  overflow: hidden;
  border: 1px solid #d0d7de;
}

.team-info-header {
  display: flex;
  padding: 24px;
  background: linear-gradient(135deg, #1a365d, #153254);
  color: white;
}

.team-avatar-container {
  margin-right: 24px;
  flex-shrink: 0;
}

.team-info-content {
  flex-grow: 1;
}

.team-name-container {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.team-name {
  margin: 0;
  font-size: 24px;
  margin-right: 12px;
  color: white;
}

.role-tag {
  font-size: 12px;
}

.team-stats {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.stat-item {
  margin-right: 24px;
  display: flex;
  align-items: center;
  
  i {
    margin-right: 8px;
  }
}

.team-description {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  line-height: 1.6;
}

.team-actions {
  padding: 16px 24px;
  background-color: #f8f9fa;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #dcdfe6;
}

.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f2f6fc;
  
  .header-left {
    display: flex;
    align-items: center;
    
    .member-count {
      margin-left: 8px;
      font-size: 14px;
      color: #909399;
    }
  }
  
  .member-search-container {
    display: flex;
    align-items: center;
    gap: 10px;
    
    .member-search {
      width: 200px;
    }
    
    .role-filter {
      width: 120px;
    }
  }
}

.card-title {
  font-size: 18px;
  font-weight: bold;
}

.no-team-info {
  margin-top: 60px;
}

// 头像上传相关样式
.avatar-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    line-height: 100px;
    text-align: center;
    border: 1px dashed #d9d9d9;
    border-radius: 50%;
    cursor: pointer;
  }
  
  .avatar-preview {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    object-fit: cover;
  }
  
  .avatar-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #606266;
  }
}

.member-option {
  display: flex;
  align-items: center;
}

.member-action-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 5px;
  
  .el-button {
    padding: 5px;
  }
}

.nickname-cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

// 响应式表格样式
@media screen and (max-width: 768px) {
  .member-card {
    .el-table {
      width: 100%;
      overflow-x: auto;
      white-space: nowrap;
      
      .el-table__body,
      .el-table__header {
        min-width: 100%;
      }
    }
    
    .member-header {
      flex-direction: column;
      align-items: flex-start;
      
      .header-left {
        margin-bottom: 10px;
      }
      
      .member-search-container {
        width: 100%;
        flex-wrap: wrap;
        
        .member-search, .role-filter {
          margin-bottom: 10px;
        }
        
        .el-button {
          margin-right: 10px;
        }
      }
    }
  }
  
  .team-info-header {
    flex-direction: column;
    
    .team-avatar-container {
      margin-right: 0;
      margin-bottom: 15px;
      align-self: center;
    }
    
    .team-info-content {
      .team-name-container {
        justify-content: center;
      }
      
      .team-stats {
        justify-content: center;
      }
    }
  }
  
  .pagination-container {
    .el-pagination {
      padding: 0 5px;
      
      .el-pagination__sizes {
        display: none;
      }
    }
  }
}

.member-card {
  .el-card__header {
    padding: 15px 20px;
    border-bottom: 1px solid #dcdfe6;
    background-color: #f2f6fc;
  }
  
  .el-card__body {
    padding: 0;
  }
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 15px 0;
  background-color: #f8f9fa;
  border-top: 1px solid #ebeef5;
}

// 表格样式增强 - 使用正确的深度选择器语法
::v-deep .el-table {
  &::before {
    height: 0;
  }
  
  th {
    background-color: #eef1f6 !important;
    font-weight: bold;
  }
  
  td {
    border-bottom: 1px solid #dcdfe6;
  }
  
  .el-table__row:hover > td {
    background-color: #f0f7ff !important;
  }
  
  .el-table__row--striped td {
    background-color: #f5f7fa !important;
  }
}

.team-avatar-container {
  display: flex;
  justify-content: center;
  margin: 10px 0;
}

.dissolve-warning {
  display: flex;
  align-items: center;
  padding: 15px;
  background-color: #FEF0F0;
  border-radius: 4px;
  border-left: 3px solid #F56C6C;
  margin-bottom: 15px;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.invite-link-container {
  padding: 15px;
}

.qrcode-container {
  margin-top: 15px;
  text-align: center;
}

.invite-qrcode {
  width: 200px;
  height: 200px;
  margin: 10px 0;
}
</style> 