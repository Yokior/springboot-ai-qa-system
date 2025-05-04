<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch"
            label-width="68px">
            <el-form-item label="团队名称" prop="name">
                <el-input v-model="queryParams.name" placeholder="请输入团队名称" clearable
                    @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                    v-hasPermi="['team:qa_team:add']">新增</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
                    v-hasPermi="['team:qa_team:edit']">修改</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
                    @click="handleDelete" v-hasPermi="['team:qa_team:remove']">删除</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                    v-hasPermi="['team:qa_team:export']">导出</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="qa_teamList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="序号" align="center" width="60">
                <template slot-scope="scope">
                    {{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}
                </template>
            </el-table-column>

            <el-table-column label="团队头像" align="center" width="80">
                <template slot-scope="scope">
                    <img v-if="scope.row.avatar" :src="getImageUrl(scope.row.avatar)" class="team-avatar-img">
                    <div v-else class="team-avatar-empty">
                        <i class="el-icon-picture-outline"></i>
                    </div>
                </template>
            </el-table-column>

            <el-table-column label="团队名称" align="center" prop="name" />
            <el-table-column label="团队描述" align="center" prop="description" width="200">
                <template slot-scope="scope">
                    <el-tooltip v-if="scope.row.description && scope.row.description.length > 20"
                        :content="scope.row.description" placement="top" effect="light">
                        <span>{{ scope.row.description.slice(0, 20) }}...</span>
                    </el-tooltip>
                    <span v-else>{{ scope.row.description }}</span>
                </template>
            </el-table-column>

            <el-table-column label="团队创建者用户ID" align="center" prop="ownerUserId" />
            <el-table-column label="团队状态" align="center" prop="status">
                <template slot-scope="scope">
                    <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                        v-hasPermi="['team:qa_team:edit']">修改</el-button>
                    <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                        v-hasPermi="['team:qa_team:remove']">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize" @pagination="getList" />

        <!-- 添加或修改知识库团队对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="团队名称" prop="name">
                    <el-input v-model="form.name" placeholder="请输入团队名称" />
                </el-form-item>
                <el-form-item label="团队描述" prop="description">
                    <el-input v-model="form.description" type="textarea" placeholder="请输入内容" maxlength="200"
                        show-word-limit :autosize="{ minRows: 3, maxRows: 6 }" />
                </el-form-item>
                <el-form-item label="团队头像" prop="avatar">
                    <div class="team-avatar-container">
                        <team-avatar v-if="!isAdd || (isAdd && tempTeamId)" v-model="form.avatar"
                            :teamId="form.teamId || tempTeamId" @upload-success="handleAvatarSuccess"></team-avatar>
                        <div v-else class="team-avatar-notice">
                            <i class="el-icon-info"></i>
                            <span>请先保存团队基本信息后再上传头像</span>
                        </div>
                    </div>
                </el-form-item>
                <el-form-item label="团队状态">
                    <el-radio-group v-model="form.status">
                        <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{
                            dict.label }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="团队创建者" prop="ownerUserId">
                    <el-select v-model="form.ownerUserId" filterable remote reserve-keyword placeholder="请输入创建者用户名或昵称搜索"
                        :remote-method="remoteSearchUsers" :loading="userSearchLoading" style="width: 100%;">
                        <el-option v-for="item in userOptions" :key="item.userId"
                            :label="item.nickName + ' (' + item.userName + ')'" :value="item.userId">
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button type="primary" @click="submitForm">确 定</el-button>
                <el-button @click="cancel">取 消</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import { listQa_team, getQa_team, delQa_team, addQa_team, updateQa_team } from "@/api/team/qa_team"
import TeamAvatar from "./teamAvatar"
import { getUser, searchUserSelect } from "@/api/system/user"; // 引入新的 API 函数

export default {
    name: "Qa_team",
    dicts: ['sys_normal_disable'],
    components: { TeamAvatar },
    data() {
        return {
            // 遮罩层
            loading: true,
            // 选中数组
            ids: [],
            // 非单个禁用
            single: true,
            // 非多个禁用
            multiple: true,
            // 显示搜索条件
            showSearch: true,
            // 总条数
            total: 0,
            // 知识库团队表格数据
            qa_teamList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 是否为新增操作
            isAdd: false,
            // 临时团队ID（用于新增团队时的头像上传）
            tempTeamId: null,
            // 头像上传临时数据
            tempAvatar: '',
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                name: null,
                description: null,
                ownerUserId: null,
                status: null,
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                name: [
                    { required: true, message: "团队名称不能为空", trigger: "blur" }
                ],
                ownerUserId: [
                    { required: true, message: "团队创建者用户ID不能为空", trigger: "blur" }
                ],
                status: [
                    { required: true, message: "团队状态不能为空", trigger: "change" }
                ],
            },
            // 用户搜索加载状态
            userSearchLoading: false,
            // 用户下拉选项
            userOptions: [],
        }
    },
    created() {
        this.getList()
    },
    methods: {
        // 获取图片URL
        getImageUrl(avatar) {
            if (!avatar) return '';
            if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
                return avatar;
            }
            if (avatar.startsWith('data:image/')) {
                return avatar;
            }
            return process.env.VUE_APP_BASE_API + avatar;
        },
        // 头像上传成功处理
        handleAvatarSuccess(avatarPath) {
            this.form.avatar = avatarPath;
            // 如果是新增团队，则保存临时头像数据，等团队创建后再上传
            if (this.isAdd && !this.form.teamId) {
                this.tempAvatar = avatarPath;
            }
        },
        /** 查询知识库团队列表 */
        getList() {
            this.loading = true
            listQa_team(this.queryParams).then(response => {
                this.qa_teamList = response.rows
                this.total = response.total
                this.loading = false
            })
        },
        // 取消按钮
        cancel() {
            this.open = false
            this.reset()
        },
        // 表单重置
        reset() {
            this.isAdd = false;
            this.tempTeamId = null;
            this.tempAvatar = '';
            this.form = {
                teamId: null,
                name: null,
                description: null,
                avatar: null,
                ownerUserId: null,
                status: "0",
                createBy: null,
                createTime: null,
                updateBy: null,
                updateTime: null,
                remark: null
            }
            this.resetForm("form")
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
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.teamId)
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset()
            this.isAdd = true
            this.form.status = "0"
            this.open = true
            this.title = "添加知识库团队"
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset()
            const teamId = row.teamId || this.ids
            getQa_team(teamId).then(response => {
                this.form = response.data
                this.open = true
                this.title = "修改知识库团队"

                // 回显创建者信息
                if (this.form.ownerUserId) {
                    this.userOptions = [] // 清空旧选项
                    getUser(this.form.ownerUserId).then(userResponse => {
                        if (userResponse && userResponse.data) {
                            this.userOptions.push(userResponse.data);
                        }
                    });
                }
            })
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.teamId != null) {
                        // 修改操作
                        updateQa_team(this.form).then(response => {
                            this.$modal.msgSuccess("修改成功")
                            this.open = false
                            this.getList()
                        })
                    } else {
                        // 新增操作
                        addQa_team(this.form).then(response => {
                            this.$modal.msgSuccess("新增成功")
                            
                            // 这是问题所在的部分，需要调整响应处理逻辑
                            try {
                                // 根据实际返回情况调整，这里尝试几种可能的格式
                                
                                // 方式1：如果返回的是 {code: 200, data: {teamId: xxx}}
                                if (response.code === 200 && response.data && response.data.teamId) {
                                    this.tempTeamId = response.data.teamId;
                                } 
                                // 方式2：如果返回的是 {code: 200, data: teamId}
                                else if (response.code === 200 && response.data) {
                                    this.tempTeamId = response.data;
                                }
                                // 方式3：如果 data 字段就是整个团队对象
                                else if (response.code === 200 && typeof response.data === 'object') {
                                    for (let key in response.data) {
                                        if (key === 'teamId' || key === 'team_id') {
                                            this.tempTeamId = response.data[key];
                                            break;
                                        }
                                    }
                                }
                                
                                // 不管能否获取到新团队ID，都关闭对话框并刷新列表
                                this.open = false;
                                this.getList();
                            } catch (error) {
                                console.error("处理新增响应出错:", error);
                                // 发生错误时，仍然关闭对话框并刷新列表
                                this.open = false;
                                this.getList();
                            }
                        }).catch(error => {
                            console.error("新增请求出错:", error);
                            // 即使请求出错，也给用户一些反馈
                            this.$modal.msgError("新增请求发生错误，请检查控制台或联系管理员");
                        });
                    }
                }
            })
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const teamIds = row.teamId || this.ids
            this.$modal.confirm('是否确认删除知识库团队编号为"' + teamIds + '"的数据项？').then(function () {
                return delQa_team(teamIds)
            }).then(() => {
                this.getList()
                this.$modal.msgSuccess("删除成功")
            }).catch(() => { })
        },
        /** 导出按钮操作 */
        handleExport() {
            this.download('team/qa_team/export', {
                ...this.queryParams
            }, `qa_team_${new Date().getTime()}.xlsx`)
        },
        // 远程搜索用户方法 (修改后)
        remoteSearchUsers(query) {
            if (query !== '') {
                this.userSearchLoading = true;
                // 调用新的搜索接口
                searchUserSelect(query).then(response => {
                    this.userSearchLoading = false;
                    if (response && response.data) {
                        this.userOptions = response.data;
                    } else {
                        this.userOptions = [];
                    }
                }).catch(() => {
                    this.userSearchLoading = false;
                    this.userOptions = []; // 出错时清空
                });
            } else {
                this.userOptions = [];
            }
        }
    }
}
</script>

<style scoped>
.team-avatar-container {
    display: flex;
    justify-content: center;
}
.team-avatar-img {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    object-fit: cover;
}
.team-avatar-empty {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background-color: #f5f7fa;
    border: 1px dashed #d9d9d9;
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 0 auto;
}
.team-avatar-empty i {
    font-size: 20px;
    color: #c0c4cc;
}
.team-avatar-notice {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #909399;
    font-size: 14px;
}
.team-avatar-notice i {
    font-size: 24px;
    margin-bottom: 8px;
}
</style>
