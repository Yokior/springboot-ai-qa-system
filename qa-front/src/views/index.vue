<template>
  <div class="app-container home">
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
</template>

<script>
import * as echarts from 'echarts'
import { getServer } from "@/api/monitor/server"

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
      isLoading: false
    }
  },
  computed: {
    serverUptime() {
      return this.server.jvm ? this.server.jvm.runTime : '--'
    }
  },
  mounted() {
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
    
    // 监听路由变化
    this.$router.beforeEach((to, from, next) => {
      // 如果离开当前页面，标记为非激活
      if (from.path === '/' && to.path !== '/') {
        this.isActive = false;
      }
      // 如果进入当前页面，标记为激活并更新数据
      if (to.path === '/' && from.path !== '/') {
        this.isActive = true;
        this.fetchServerData(false);
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
    // 处理页面可见性变化
    handleVisibilityChange() {
      this.isActive = !document.hidden;
      
      // 如果页面恢复可见，立即更新数据
      if (this.isActive && !this.isLoading) {
        this.fetchServerData(false);
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
</style>

