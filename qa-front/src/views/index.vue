<template>
  <div class="app-container home">
    <!-- 超级管理员界面 - 系统监控中心 -->
    <div v-if="isSuperAdmin">
      <div class="welcome-info">
        <h2>智能问答系统监控中心</h2>
        <div class="version">当前版本: {{ version }}</div>
      </div>
      
      <el-row :gutter="20">
        <!-- 状态卡片区域 -->
        <el-col :xs="24" :sm="12" :md="6" class="status-card-wrapper" v-if="server.cpu && server.mem && server.jvm">
          <el-card shadow="hover" class="status-card">
            <div class="status-title"><i class="el-icon-cpu"></i> CPU使用率</div>
            <div class="status-value" :class="{'warning': server.cpu.used > 70, 'danger': server.cpu.used > 90}">{{ server.cpu.used }}%</div>
            <div class="status-progress">
              <el-progress :percentage="server.cpu.used" :color="getCpuProgressColor(server.cpu.used)" :show-text="false"></el-progress>
            </div>
            <div class="status-detail">核心数: {{ server.cpu.cpuNum }}</div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6" class="status-card-wrapper" v-if="server.mem">
          <el-card shadow="hover" class="status-card">
            <div class="status-title"><i class="el-icon-tickets"></i> 内存使用率</div>
            <div class="status-value" :class="{'warning': server.mem.usage > 70, 'danger': server.mem.usage > 90}">{{ server.mem.usage }}%</div>
            <div class="status-progress">
              <el-progress :percentage="server.mem.usage" :color="getMemProgressColor(server.mem.usage)" :show-text="false"></el-progress>
            </div>
            <div class="status-detail">{{ server.mem.used }}G / {{ server.mem.total }}G</div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6" class="status-card-wrapper" v-if="server.jvm">
          <el-card shadow="hover" class="status-card">
            <div class="status-title"><i class="el-icon-coffee-cup"></i> JVM使用率</div>
            <div class="status-value" :class="{'warning': server.jvm.usage > 70, 'danger': server.jvm.usage > 90}">{{ server.jvm.usage }}%</div>
            <div class="status-progress">
              <el-progress :percentage="server.jvm.usage" :color="getJvmProgressColor(server.jvm.usage)" :show-text="false"></el-progress>
            </div>
            <div class="status-detail">{{ server.jvm.used }}M / {{ server.jvm.total }}M</div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6" class="status-card-wrapper" v-if="serverUptime">
          <el-card shadow="hover" class="status-card uptime-card">
            <div class="status-title"><i class="el-icon-alarm-clock"></i> 系统运行</div>
            <div class="status-value uptime">{{ serverUptime }}</div>
            <div class="status-detail" v-if="server.jvm">启动时间: {{ server.jvm.startTime }}</div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 图表区域 -->
      <el-row :gutter="20" class="chart-row">
        <!-- CPU图表 -->
        <el-col :xs="24" :sm="24" :md="12" class="chart-wrapper">
          <el-card shadow="hover" class="chart-card">
            <div slot="header" class="chart-header">
              <span><i class="el-icon-cpu"></i> CPU使用率分析</span>
            </div>
            <div class="chart-container">
              <div ref="cpuChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
        
        <!-- 内存图表 -->
        <el-col :xs="24" :sm="24" :md="12" class="chart-wrapper">
          <el-card shadow="hover" class="chart-card">
            <div slot="header" class="chart-header">
              <span><i class="el-icon-tickets"></i> 内存使用情况</span>
            </div>
            <div class="chart-container">
              <div ref="memChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 磁盘使用情况图表 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :xs="24" :sm="24" :md="24" class="chart-wrapper">
          <el-card shadow="hover" class="chart-card">
            <div slot="header" class="chart-header">
              <span><i class="el-icon-receiving"></i> 磁盘使用情况</span>
            </div>
            <div class="chart-container">
              <div ref="diskChart" class="chart disk-chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 服务器信息卡片 -->
      <el-row :gutter="20" class="info-row">
        <el-col :xs="24" :sm="24" :md="12" class="info-wrapper">
          <el-card shadow="hover" class="info-card">
            <div slot="header" class="info-header">
              <span><i class="el-icon-monitor"></i> 服务器信息</span>
            </div>
            <div class="info-content" v-if="server.sys">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="服务器名称">{{ server.sys.computerName }}</el-descriptions-item>
                <el-descriptions-item label="操作系统">{{ server.sys.osName }}</el-descriptions-item>
                <el-descriptions-item label="服务器IP">{{ server.sys.computerIp }}</el-descriptions-item>
                <el-descriptions-item label="系统架构">{{ server.sys.osArch }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="24" :md="12" class="info-wrapper">
          <el-card shadow="hover" class="info-card">
            <div slot="header" class="info-header">
              <span><i class="el-icon-coffee-cup"></i> JVM信息</span>
            </div>
            <div class="info-content" v-if="server.jvm">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="Java名称">{{ server.jvm.name }}</el-descriptions-item>
                <el-descriptions-item label="Java版本">{{ server.jvm.version }}</el-descriptions-item>
                <el-descriptions-item label="安装路径">{{ server.jvm.home }}</el-descriptions-item>
                <el-descriptions-item label="项目路径" v-if="server.sys">{{ server.sys.userDir }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- 普通用户界面 - 智能问答系统欢迎页 -->
    <div v-else class="regular-user-home">
      <div class="welcome-banner">
        <h1>欢迎使用智能问答系统</h1>
        <p class="welcome-subtitle">便捷、智能的团队知识库与问答平台</p>
      </div>
      
      <el-row :gutter="20" class="feature-cards">
        <el-col :xs="24" :sm="12" :md="8" class="feature-card-wrapper">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <i class="el-icon-data-analysis"></i>
            </div>
            <h3>智能问答</h3>
            <p>基于先进AI模型，快速准确回答您的问题，提供相关知识库信息</p>
            <el-button type="primary" size="small" @click="goToQA">开始提问</el-button>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="8" class="feature-card-wrapper">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <i class="el-icon-collection"></i>
            </div>
            <h3>知识库管理</h3>
            <p>整理、分类和管理您的团队知识资源，让信息检索更加便捷</p>
            <el-button type="primary" size="small" @click="goToKnowledgeBase">进入知识库</el-button>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="8" class="feature-card-wrapper">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon">
              <i class="el-icon-user"></i>
            </div>
            <h3>团队协作</h3>
            <p>邀请团队成员，协同构建完善的知识体系，提高团队效率</p>
            <el-button type="primary" size="small" @click="goToTeam">团队管理</el-button>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 快捷工具和公告区域 -->
      <el-row :gutter="20" class="tools-announcement">
        <el-col :xs="24" :sm="24" :md="12" class="tools-wrapper">
          <el-card shadow="hover" class="tools-card">
            <div slot="header" class="tools-header">
              <span><i class="el-icon-s-tools"></i> 快捷工具</span>
            </div>
            <div class="tools-grid">
              <div class="tool-item" @click="goToQA">
                <i class="el-icon-chat-dot-round"></i>
                <span>在线问答</span>
              </div>
              <div class="tool-item" @click="goToKnowledgeBase">
                <i class="el-icon-reading"></i>
                <span>浏览知识库</span>
              </div>
              <div class="tool-item" @click="goToTeam">
                <i class="el-icon-user"></i>
                <span>团队管理</span>
              </div>
              <div class="tool-item" @click="goToProfile">
                <i class="el-icon-s-custom"></i>
                <span>个人中心</span>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="24" :md="12" class="announcement-wrapper">
          <el-card shadow="hover" class="announcement-card">
            <div slot="header" class="announcement-header">
              <span><i class="el-icon-bell"></i> 系统公告</span>
              <span class="announcement-count" v-if="total > 0">共 {{ total }} 条</span>
            </div>
            <div v-loading="announcements === null" element-loading-text="加载公告中...">
              <div v-if="announcements && announcements.length > 0" class="announcement-list">
                <div v-for="(item, index) in announcements" :key="index" class="announcement-item">
                  <div class="announcement-title">{{ item.title }}</div>
                  <div class="announcement-content" v-html="item.content"></div>
                  <div class="announcement-time">{{ item.time }}</div>
                </div>
              </div>
              <div v-else class="empty-announcement">
                <el-empty description="暂无系统公告" :image-size="100"></el-empty>
              </div>
            </div>
            <div class="announcement-pagination" v-if="total > queryParams.pageSize">
              <el-pagination
                background
                layout="prev, pager, next"
                :page-size="queryParams.pageSize"
                :total="total"
                @current-change="pageChange"
              ></el-pagination>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getServer } from "@/api/monitor/server"
// 导入store以获取用户角色
import store from '@/store'
// 引入系统公告查询接口
import request from '@/utils/request'

export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "1.0.0",
      // 服务器信息
      server: {},
      // 图表实例
      charts: {
        cpu: null,
        mem: null,
        disk: null
      },
      // 图表颜色
      chartColors: {
        primary: '#4090f7',
        success: '#52c41a',
        warning: '#faad14',
        danger: '#f5222d',
        info: '#909399',
        cpu: ['#4090f7', '#b7d2ff', '#e6f7ff'],
        mem: ['#52c41a', '#b7eb8f', '#f6ffed'],
        jvm: ['#722ed1', '#d3adf7', '#f9f0ff'],
        disk: ['#2f9688', '#5ab1ef', '#ffb980', '#d87a80', '#8d98b3', '#97b552', '#dc69aa']
      },
      // 更新计时器
      updateTimer: null,
      // CPU历史数据
      cpuHistory: Array(10).fill(0),
      // 内存历史数据
      memHistory: Array(10).fill(0),
      // 时间轴标签
      timeLabels: Array(10).fill(''),
      // 是否首次加载
      isFirstLoad: true,
      // 是否在当前页面
      isActive: true,
      // 是否正在加载数据
      isLoading: false,
      
      // 系统公告数据
      announcements: [],
      // 系统公告分页参数
      queryParams: {
        pageNum: 1,
        pageSize: 5,
        noticeTitle: undefined,
        createBy: undefined,
        status: '0' // 只查询状态为正常的公告
      },
      // 公告总数
      total: 0
    }
  },
  computed: {
    // 判断是否为超级管理员
    isSuperAdmin() {
      return store.getters.roles.includes('admin');
    },
    
    serverUptime() {
      return this.server.jvm ? this.server.jvm.runTime : '--'
    }
  },
  mounted() {
    // 根据用户角色决定是否加载服务器数据
    if (this.isSuperAdmin) {
      // 首次加载时显示加载状态
      this.isFirstLoad = true;
      this.fetchServerData();
      
      // 每30秒更新一次数据
      this.updateTimer = setInterval(() => {
        // 只有当前页面激活时才更新数据
        if (this.isActive) {
          this.fetchServerData(false);
        }
      }, 30000);
      
      // 窗口大小改变时重新调整图表大小
      window.addEventListener('resize', this.resizeCharts);
      
      // 监听页面可见性变化
      document.addEventListener('visibilitychange', this.handleVisibilityChange);
    }
    
    // 加载系统公告数据
    this.getNoticeList();
    
    // 监听路由变化
    this.$router.beforeEach((to, from, next) => {
      // 如果离开当前页面，标记为非激活
      if (from.path === '/' && to.path !== '/') {
        this.isActive = false;
      }
      // 如果进入当前页面，标记为激活并更新数据
      if (to.path === '/' && from.path !== '/') {
        this.isActive = true;
        if (this.isSuperAdmin) {
          this.fetchServerData(false);
        }
        // 重新加载系统公告
        this.getNoticeList();
      }
      next();
    });
  },
  beforeDestroy() {
    // 清除定时器
    if (this.updateTimer) {
      clearInterval(this.updateTimer);
    }
    
    // 移除事件监听
    window.removeEventListener('resize', this.resizeCharts);
    document.removeEventListener('visibilitychange', this.handleVisibilityChange);
    
    // 销毁图表实例
    Object.values(this.charts).forEach(chart => {
      chart && chart.dispose();
    });
  },
  methods: {
    // 获取系统公告列表
    getNoticeList() {
      request({
        url: '/system/notice/list',
        method: 'get',
        params: this.queryParams
      }).then(response => {
        this.announcements = response.rows.map(item => {
          return {
            id: item.noticeId,
            title: item.noticeTitle,
            content: item.noticeContent,
            // 将后端日期格式化为更友好的显示
            time: this.formatTime(item.createTime)
          };
        });
        this.total = response.total;
      });
    },

    // 分页查询处理方法
    pageChange(page) {
      this.queryParams.pageNum = page;
      this.getNoticeList();
    },

    // 格式化时间显示
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    },
    
    // 导航方法
    goToQA() {
      this.$router.push('/qa/index');
    },
    
    goToKnowledgeBase() {
      this.$router.push('/knowledge/index');
    },
    
    goToTeam() {
      this.$router.push('/team/my_team/index');
    },
    
    goToProfile() {
      this.$router.push('/user/profile');
    },
    
    // 处理页面可见性变化
    handleVisibilityChange() {
      this.isActive = !document.hidden;
      
      // 如果页面恢复可见，立即更新数据
      if (this.isActive) {
        if (this.isSuperAdmin && !this.isLoading) {
          this.fetchServerData(false);
        }
        // 重新加载系统公告
        this.getNoticeList();
      }
    },
    
    // 获取服务器信息
    fetchServerData(showLoading = true) {
      // 如果已经在加载中，则跳过本次请求
      if (this.isLoading) {
        return;
      }
      
      this.isLoading = true;
      
      // 仅在首次加载时显示加载提示
      if (showLoading && this.isFirstLoad) {
        this.$modal.loading("正在加载服务监控数据...");
      }
      
      getServer()
        .then(response => {
          const newData = response.data;
          
          // 动态更新数据
          this.updateServerData(newData);
          
          // 首次加载完成后，关闭加载提示并标记首次加载完成
          if (this.isFirstLoad) {
            this.$modal.closeLoading();
            this.isFirstLoad = false;
          }
          
          this.isLoading = false;
        })
        .catch(() => {
          if (this.isFirstLoad) {
            this.$modal.closeLoading();
          }
          this.isLoading = false;
        });
    },
    
    // 更新服务器数据
    updateServerData(newData) {
      // 将新数据赋值给server对象
      this.server = newData;
      
      // 更新历史数据
      if (this.server.cpu) {
        this.updateHistoryData();
      }
      
      // 更新图表数据
      this.$nextTick(() => {
        this.updateCharts();
      });
    },
    
    // 更新历史数据
    updateHistoryData() {
      // 获取当前时间作为标签
      const now = new Date();
      const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
      
      // 更新CPU历史数据
      this.cpuHistory.push(this.server.cpu.used);
      this.cpuHistory.shift();
      
      // 更新内存历史数据
      this.memHistory.push(this.server.mem.usage);
      this.memHistory.shift();
      
      // 更新时间标签
      this.timeLabels.push(timeStr);
      this.timeLabels.shift();
    },
    
    // 初始化所有图表
    initCharts() {
      this.initCpuChart();
      this.initMemChart();
      this.initDiskChart();
    },
    
    // 更新所有图表
    updateCharts() {
      // 如果图表尚未初始化，先初始化
      if (!this.charts.cpu || !this.charts.mem || !this.charts.disk) {
        this.initCharts();
        return;
      }
      
      // 更新现有图表数据
      this.updateCpuChart();
      this.updateMemChart();
      this.updateDiskChart();
    },
    
    // 重新调整图表大小
    resizeCharts() {
      Object.values(this.charts).forEach(chart => {
        chart && chart.resize();
      });
    },
    
    // 初始化CPU图表
    initCpuChart() {
      // 如果已经存在图表实例，先销毁
      if (this.charts.cpu) {
        this.charts.cpu.dispose();
      }
      
      // 创建图表
      this.charts.cpu = echarts.init(this.$refs.cpuChart);
      
      // 设置图表选项
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: this.timeLabels,
          axisLine: {
            lineStyle: {
              color: '#909399'
            }
          },
          axisLabel: {
            color: '#606266'
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLine: {
            lineStyle: {
              color: '#909399'
            }
          },
          axisLabel: {
            color: '#606266',
            formatter: '{value}%'
          },
          splitLine: {
            lineStyle: {
              color: '#eee'
            }
          }
        },
        series: [
          {
            name: 'CPU使用率',
            type: 'line',
            data: this.cpuHistory,
            markPoint: {
              data: [
                { type: 'max', name: '最大值' },
                { type: 'min', name: '最小值' }
              ]
            },
            lineStyle: {
              width: 3,
              color: this.chartColors.primary
            },
            itemStyle: {
              color: this.chartColors.primary
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 144, 247, 0.6)' },
                { offset: 1, color: 'rgba(64, 144, 247, 0.1)' }
              ])
            }
          }
        ]
      };
      
      // 设置图表
      this.charts.cpu.setOption(option);
    },
    
    // 更新CPU图表数据
    updateCpuChart() {
      if (!this.charts.cpu) return;
      
      this.charts.cpu.setOption({
        xAxis: {
          data: this.timeLabels
        },
        series: [
          {
            data: this.cpuHistory
          }
        ]
      });
    },
    
    // 初始化内存图表
    initMemChart() {
      // 如果已经存在图表实例，先销毁
      if (this.charts.mem) {
        this.charts.mem.dispose();
      }
      
      // 创建图表
      this.charts.mem = echarts.init(this.$refs.memChart);
      
      // 设置图表选项
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 10,
          top: 'center',
          data: ['已用内存', '剩余内存', '已用JVM', '剩余JVM'],
          textStyle: {
            color: '#606266'
          }
        },
        series: [
          {
            name: '内存使用',
            type: 'pie',
            selectedMode: 'single',
            radius: [0, '35%'],
            label: {
              position: 'inner',
              fontSize: 12
            },
            labelLine: {
              show: false
            },
            data: [
              { value: this.server.mem.used, name: '已用内存', itemStyle: { color: this.chartColors.mem[0] } },
              { value: this.server.mem.free, name: '剩余内存', itemStyle: { color: this.chartColors.mem[1] } }
            ]
          },
          {
            name: 'JVM使用',
            type: 'pie',
            radius: ['45%', '60%'],
            labelLine: {
              length: 20
            },
            label: {
              formatter: '{a|{a}}{abg|}\n{hr|}\n {b|{b}：}{c} {per|{d}%}',
              backgroundColor: '#eee',
              borderColor: '#aaa',
              borderWidth: 1,
              borderRadius: 4,
              rich: {
                a: {
                  color: '#999',
                  lineHeight: 22,
                  align: 'center'
                },
                hr: {
                  borderColor: '#aaa',
                  width: '100%',
                  borderWidth: 0.5,
                  height: 0
                },
                b: {
                  fontSize: 12,
                  lineHeight: 22
                },
                per: {
                  color: '#eee',
                  backgroundColor: '#334455',
                  padding: [2, 4],
                  borderRadius: 2
                }
              }
            },
            data: [
              { value: this.server.jvm.used, name: '已用JVM', itemStyle: { color: this.chartColors.jvm[0] } },
              { value: this.server.jvm.free, name: '剩余JVM', itemStyle: { color: this.chartColors.jvm[1] } }
            ]
          }
        ]
      };
      
      // 设置图表
      this.charts.mem.setOption(option);
    },
    
    // 更新内存图表数据
    updateMemChart() {
      if (!this.charts.mem || !this.server.mem || !this.server.jvm) return;
      
      this.charts.mem.setOption({
        series: [
          {
            data: [
              { value: this.server.mem.used, name: '已用内存' },
              { value: this.server.mem.free, name: '剩余内存' }
            ]
          },
          {
            data: [
              { value: this.server.jvm.used, name: '已用JVM' },
              { value: this.server.jvm.free, name: '剩余JVM' }
            ]
          }
        ]
      });
    },
    
    // 初始化磁盘图表
    initDiskChart() {
      // 如果已经存在图表实例，先销毁
      if (this.charts.disk) {
        this.charts.disk.dispose();
      }
      
      // 如果没有磁盘数据，则返回
      if (!this.server.sysFiles || this.server.sysFiles.length === 0) {
        return;
      }
      
      // 创建图表
      this.charts.disk = echarts.init(this.$refs.diskChart);
      
      // 准备数据
      const diskNames = this.server.sysFiles.map(item => item.dirName);
      const usedData = this.server.sysFiles.map(item => parseFloat(item.used));
      const freeData = this.server.sysFiles.map(item => parseFloat(item.free));
      
      // 设置图表选项
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: function(params) {
            const disk = params[0].name;
            const used = params[0].value;
            const free = params[1].value;
            const total = used + free;
            const usage = (used / total * 100).toFixed(2);
            
            return `${disk}<br/>总大小: ${total.toFixed(2)}GB<br/>已用: ${used.toFixed(2)}GB (${usage}%)<br/>可用: ${free.toFixed(2)}GB`;
          }
        },
        legend: {
          data: ['已用空间', '可用空间'],
          textStyle: {
            color: '#606266'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLine: {
            lineStyle: {
              color: '#909399'
            }
          },
          axisLabel: {
            color: '#606266',
            formatter: '{value}GB'
          },
          splitLine: {
            lineStyle: {
              color: '#eee'
            }
          }
        },
        yAxis: {
          type: 'category',
          data: diskNames,
          axisLine: {
            lineStyle: {
              color: '#909399'
            }
          },
          axisLabel: {
            color: '#606266'
          }
        },
        series: [
          {
            name: '已用空间',
            type: 'bar',
            stack: '总量',
            itemStyle: {
              color: this.chartColors.warning
            },
            label: {
              show: true,
              formatter: '{c}GB'
            },
            data: usedData
          },
          {
            name: '可用空间',
            type: 'bar',
            stack: '总量',
            itemStyle: {
              color: this.chartColors.success
            },
            label: {
              show: true,
              formatter: '{c}GB'
            },
            data: freeData
          }
        ]
      };
      
      // 设置图表
      this.charts.disk.setOption(option);
    },
    
    // 更新磁盘图表数据
    updateDiskChart() {
      if (!this.charts.disk || !this.server.sysFiles || this.server.sysFiles.length === 0) return;
      
      // 准备数据
      const diskNames = this.server.sysFiles.map(item => item.dirName);
      const usedData = this.server.sysFiles.map(item => parseFloat(item.used));
      const freeData = this.server.sysFiles.map(item => parseFloat(item.free));
      
      this.charts.disk.setOption({
        yAxis: {
          data: diskNames
        },
        series: [
          {
            data: usedData
          },
          {
            data: freeData
          }
        ]
      });
    },
    
    // 获取CPU进度条颜色
    getCpuProgressColor(value) {
      if (value > 90) return this.chartColors.danger;
      if (value > 70) return this.chartColors.warning;
      return this.chartColors.primary;
    },
    
    // 获取内存进度条颜色
    getMemProgressColor(value) {
      if (value > 90) return this.chartColors.danger;
      if (value > 70) return this.chartColors.warning;
      return this.chartColors.success;
    },
    
    // 获取JVM进度条颜色
    getJvmProgressColor(value) {
      if (value > 90) return this.chartColors.danger;
      if (value > 70) return this.chartColors.warning;
      return '#722ed1';
    }
  }
}
</script>

<style scoped lang="scss">
.home {
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
  color: #303133;
  overflow-x: hidden;
  
  .welcome-info {
    text-align: center;
    margin-bottom: 20px;
    
    h2 {
      font-size: 28px;
      font-weight: 500;
      margin-top: 0;
      margin-bottom: 10px;
      color: #303133;
    }
    
    .version {
      font-size: 14px;
      color: #909399;
    }
  }
  
  .status-card-wrapper {
    margin-bottom: 20px;
  }
  
  .status-card {
    height: 180px;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
    }
    
    .status-title {
      font-size: 16px;
      font-weight: 500;
      margin-bottom: 20px;
      color: #606266;
      
      i {
        margin-right: 6px;
      }
    }
    
    .status-value {
      font-size: 36px;
      font-weight: 600;
      margin-bottom: 15px;
      color: #303133;
      
      &.warning {
        color: #faad14;
      }
      
      &.danger {
        color: #f5222d;
      }
      
      &.uptime {
        font-size: 24px;
      }
    }
    
    .status-progress {
      margin-bottom: 15px;
    }
    
    .status-detail {
      font-size: 14px;
      color: #909399;
    }
  }
  
  .chart-row {
    margin-bottom: 20px;
  }

  .chart-wrapper {
    margin-bottom: 20px;
  }
  
  .chart-card {
    border-radius: 8px;
    overflow: hidden;
    
    .chart-header {
      display: flex;
      align-items: center;
      
      i {
        margin-right: 6px;
      }
    }
    
    .chart-container {
      padding: 10px 0;
    }
    
    .chart {
      height: 350px;
      width: 100%;
    }
    
    .disk-chart {
      height: 300px;
    }
  }
  
  .info-row {
    margin-bottom: 20px;
  }
  
  .info-wrapper {
    margin-bottom: 20px;
  }
  
  .info-card {
    border-radius: 8px;
    overflow: hidden;
    
    .info-header {
      display: flex;
      align-items: center;
      
      i {
        margin-right: 6px;
      }
    }
    
    .info-content {
      padding: 10px 0;
    }
  }
  
  // 响应式调整
  @media (max-width: 768px) {
    .chart {
      height: 300px !important;
    }
    
    .disk-chart {
      height: 400px !important;
    }
    
    .status-card {
      height: auto;
      min-height: 160px;
    }
  }
  
  // 文本颜色样式
  .text-danger {
    color: #f5222d;
  }
}

/* 普通用户首页样式 */
.regular-user-home {
  padding: 20px 0;
}

.welcome-banner {
  text-align: center;
  padding: 30px 0;
  margin-bottom: 30px;
  background: linear-gradient(135deg, #1a365d, #153254);
  color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.welcome-banner h1 {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}

.welcome-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.feature-cards {
  margin-bottom: 30px;
}

.feature-card-wrapper {
  margin-bottom: 20px;
}

.feature-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s, box-shadow 0.3s;
  border-radius: 8px;
  overflow: hidden;
  text-align: center;
  padding: 20px;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.feature-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 15px;
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #333;
}

.feature-card p {
  flex-grow: 1;
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.tools-card, .announcement-card {
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.tools-header, .announcement-header {
  font-weight: bold;
  font-size: 16px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  padding: 15px;
}

.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.tool-item:hover {
  background-color: #ecf5ff;
  transform: translateY(-3px);
}

.tool-item i {
  font-size: 28px;
  color: #409EFF;
  margin-bottom: 10px;
}

.tool-item span {
  font-size: 14px;
  color: #333;
}

.announcement-list {
  padding: 10px;
}

.announcement-item {
  padding: 15px;
  margin-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.announcement-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.announcement-title {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 8px;
  color: #303133;
}

.announcement-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
  word-break: break-word;
  
  // 设置富文本样式
  ::v-deep p {
    margin: 5px 0;
  }
  
  ::v-deep img {
    max-width: 100%;
    height: auto;
  }
  
  ::v-deep a {
    color: #409EFF;
    text-decoration: none;
  }
  
  ::v-deep ul, ::v-deep ol {
    padding-left: 20px;
  }
}

.announcement-time {
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.empty-announcement {
  padding: 20px;
  text-align: center;
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .welcome-banner h1 {
    font-size: 24px;
  }
  
  .tools-grid {
    grid-template-columns: 1fr;
  }
}

.announcement-header {
  font-weight: bold;
  font-size: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.announcement-count {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}

.announcement-pagination {
  text-align: center;
  padding: 10px 0;
  margin-top: 10px;
}
</style>

