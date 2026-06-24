(function () {
  var charts = {};
  var loadedKey = '';
  var dashboardTemplate = null;
  var activeDashboard = null;
  var colors = ['#1890ff', '#36cfc9', '#b37feb', '#ffc53d'];

  function $(id) {
    return document.getElementById(id);
  }

  function today(offset) {
    var date = new Date();
    date.setDate(date.getDate() + offset);
    return date.toISOString().slice(0, 10);
  }

  function token() {
    return localStorage.getItem('adminToken') || '';
  }

  function moveToMount() {
    var mount = $('dashboard-mount');
    if (!dashboardTemplate || !mount) return false;
    if (!activeDashboard || !document.body.contains(activeDashboard)) {
      activeDashboard = dashboardTemplate.cloneNode(true);
    }
    if (activeDashboard.parentElement !== mount) mount.appendChild(activeDashboard);
    activeDashboard.hidden = false;
    return true;
  }

  function detach() {
    if (activeDashboard && activeDashboard.parentElement) activeDashboard.parentElement.removeChild(activeDashboard);
    activeDashboard = null;
    loadedKey = '';
    Object.keys(charts).forEach(function (id) {
      if (charts[id]) charts[id].dispose();
    });
    charts = {};
  }

  function dashboardEl(id) {
    return activeDashboard ? activeDashboard.querySelector('#' + id) : null;
  }

  function placeholder(id, text) {
    var el = dashboardEl(id);
    if (!el) return;
    el.innerHTML = '<div class="dashboard-empty">' + (text || '暂无数据') + '</div>';
  }

  function formatMoney(value) {
    var number = Number(value || 0);
    return '￥' + number.toFixed(2);
  }

  function request(path) {
    return fetch(path, { headers: { token: token() } })
      .then(function (response) {
        return response.json().then(function (body) {
          if (!response.ok || !body.success) throw new Error(body.message || '数据加载失败');
          return body.data;
        });
      });
  }

  function renderCards(data) {
    var el = dashboardEl('overview-cards');
    if (!el) return;
    var items = [
      { label: '累计营业额', value: formatMoney(data.turnover), color: colors[0] },
      { label: '有效订单', value: data.validOrders || 0, color: colors[1] },
      { label: '注册用户', value: data.users || 0, color: colors[2] },
      { label: '待接单', value: data.pendingOrders || 0, color: colors[3] }
    ];
    el.innerHTML = items.map(function (item) {
      return '<div class="dashboard-card" style="border-top-color:' + item.color + '">' +
        '<span>' + item.label + '</span>' +
        '<strong>' + item.value + '</strong>' +
        '</div>';
    }).join('');
  }

  function chart(id) {
    var el = dashboardEl(id);
    if (!el || !window.echarts) return null;
    if (!charts[id]) charts[id] = echarts.init(el);
    return charts[id];
  }

  function renderTurnover(rows) {
    if (!rows || !rows.length) {
      placeholder('turnover-chart');
      return;
    }
    var instance = chart('turnover-chart');
    if (!instance) {
      placeholder('turnover-chart', '图表组件加载失败');
      return;
    }
    instance.setOption({
      color: [colors[0]],
      tooltip: { trigger: 'axis' },
      grid: { top: 48, right: 24, bottom: 36, left: 56 },
      title: { text: '近7日营业额', left: 0, textStyle: { fontSize: 16, fontWeight: 600 } },
      xAxis: { type: 'category', boundaryGap: false, data: rows.map(function (row) { return String(row.date); }) },
      yAxis: { type: 'value', axisLabel: { formatter: '￥{value}' } },
      series: [{
        name: '营业额',
        type: 'line',
        smooth: true,
        data: rows.map(function (row) { return Number(row.amount || 0); }),
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(24,144,255,0.16)' }
      }]
    });
  }

  function renderSales(rows) {
    if (!rows || !rows.length) {
      placeholder('sales-chart');
      return;
    }
    var instance = chart('sales-chart');
    if (!instance) {
      placeholder('sales-chart', '图表组件加载失败');
      return;
    }
    var sorted = rows.slice().reverse();
    instance.setOption({
      color: [colors[1]],
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { top: 48, right: 48, bottom: 28, left: 96 },
      title: { text: '菜品销量 Top10', left: 0, textStyle: { fontSize: 16, fontWeight: 600 } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: sorted.map(function (row) { return row.name; }) },
      series: [{
        name: '销量',
        type: 'bar',
        data: sorted.map(function (row) { return Number(row.number || 0); }),
        barWidth: 18,
        itemStyle: { borderRadius: [0, 8, 8, 0] },
        label: { show: true, position: 'right' }
      }]
    });
  }

  function render() {
    if (!moveToMount()) {
      detach();
      return;
    }
    if (!token()) {
      detach();
      return;
    }
    var key = token() + ':' + today(-6) + ':' + today(0);
    if (loadedKey === key) {
      resize();
      return;
    }
    loadedKey = key;
    placeholder('overview-cards', '正在加载...');
    placeholder('turnover-chart', '正在加载...');
    placeholder('sales-chart', '正在加载...');
    Promise.all([
      request('/api/admin/reports/overview'),
      request('/api/admin/reports/turnover?begin=' + today(-6) + '&end=' + today(0)),
      request('/api/admin/reports/sales-top10')
    ]).then(function (result) {
      renderCards(result[0] || {});
      renderTurnover(result[1] || []);
      renderSales(result[2] || []);
      resize();
    }).catch(function (error) {
      placeholder('overview-cards', error.message || '暂无数据');
      placeholder('turnover-chart');
      placeholder('sales-chart');
    });
  }

  function resize() {
    Object.keys(charts).forEach(function (id) {
      if (charts[id]) charts[id].resize();
    });
  }

  function injectStyle() {
    if ($('echarts-dashboard-style')) return;
    var style = document.createElement('style');
    style.id = 'echarts-dashboard-style';
    style.textContent = [
      '#dashboard{box-sizing:border-box;}',
      '#dashboard[hidden]{display:none!important;}',
      '#dashboard-mount #dashboard{padding:0!important;margin:0!important;max-width:none!important;}',
      '.dashboard-card{flex:1;min-width:0;background:#fff;border-top:4px solid #1890ff;border-radius:8px;box-shadow:0 8px 24px rgba(15,23,42,.08);padding:18px 20px;}',
      '.dashboard-card span{display:block;color:#6b7280;font-size:13px;margin-bottom:10px;}',
      '.dashboard-card strong{display:block;color:#111827;font-size:28px;line-height:1.15;font-weight:700;}',
      '.dashboard-empty{height:100%;min-height:120px;display:flex;align-items:center;justify-content:center;background:#fff;border:1px dashed #d9e2ec;border-radius:8px;color:#8c98a4;}',
      '#turnover-chart,#sales-chart{background:#fff;border-radius:8px;box-shadow:0 8px 24px rgba(15,23,42,.06);padding:16px;box-sizing:border-box;}',
      '@media (max-width: 760px){#overview-cards{flex-wrap:wrap!important}.dashboard-card{flex-basis:calc(50% - 8px)}#turnover-chart,#sales-chart{height:320px!important}}',
      '@media (max-width: 520px){.dashboard-card{flex-basis:100%}}'
    ].join('');
    document.head.appendChild(style);
  }

  injectStyle();
  dashboardTemplate = $('dashboard');
  if (dashboardTemplate) {
    dashboardTemplate.parentElement.removeChild(dashboardTemplate);
  }
  window.addEventListener('resize', resize);
  window.SnapMealDashboard = { render: render, detach: detach, resize: resize };
  new MutationObserver(function () {
    if ($('dashboard-mount')) render();
  }).observe(document.body, { childList: true, subtree: true });
})();
