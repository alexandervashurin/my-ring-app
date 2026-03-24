/* ==========================================================================
   Система управления персоналом - Графики и диаграммы (Chart.js)
   ========================================================================== */

(function() {
  'use strict';

  // Глобальное хранилище для экземпляров графиков
  var charts = {};

  // --------------------------------------------------------------------------
  // Инициализация графиков
  // --------------------------------------------------------------------------

  /**
   * Инициализация всех графиков на странице
   */
  function initCharts() {
    // График распределения по цехам
    initWorkersByShopChart();
    
    // График распределения по категориям
    initWorkersByCategoryChart();
    
    // График распределения по зарплате
    initSalaryDistributionChart();
    
    // График фонда оплаты по месяцам
    initPayrollByMonthChart();
    
    // График разрядов
    initWorkersByRankChart();
  }

  /**
   * График: Распределение работников по цехам
   */
  function initWorkersByShopChart() {
    var canvas = document.getElementById('chart-workers-by-shop');
    if (!canvas) return;

    var ctx = canvas.getContext('2d');
    var data = window.DashboardData ? window.DashboardData.byShop : [];
    
    charts.byShop = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(function(item) { return item.name; }),
        datasets: [{
          label: 'Количество работников',
          data: data.map(function(item) { return item.count; }),
          backgroundColor: [
            'rgba(102, 126, 234, 0.8)',
            'rgba(118, 75, 162, 0.8)',
            'rgba(67, 233, 123, 0.8)',
            'rgba(79, 172, 254, 0.8)',
            'rgba(250, 112, 154, 0.8)',
            'rgba(161, 140, 209, 0.8)',
            'rgba(254, 164, 175, 0.8)',
            'rgba(94, 231, 223, 0.8)'
          ],
          borderColor: [
            'rgba(102, 126, 234, 1)',
            'rgba(118, 75, 162, 1)',
            'rgba(67, 233, 123, 1)',
            'rgba(79, 172, 254, 1)',
            'rgba(250, 112, 154, 1)',
            'rgba(161, 140, 209, 1)',
            'rgba(254, 164, 175, 1)',
            'rgba(94, 231, 223, 1)'
          ],
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            display: false
          },
          title: {
            display: true,
            text: 'Распределение по цехам',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1
            }
          }
        }
      }
    });
  }

  /**
   * График: Распределение по категориям
   */
  function initWorkersByCategoryChart() {
    var canvas = document.getElementById('chart-workers-by-category');
    if (!canvas) return;

    var ctx = canvas.getContext('2d');
    var data = window.DashboardData ? window.DashboardData.byCategory : [];
    
    charts.byCategory = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: data.map(function(item) { return item.name; }),
        datasets: [{
          data: data.map(function(item) { return item.count; }),
          backgroundColor: [
            'rgba(102, 126, 234, 0.8)',
            'rgba(118, 75, 162, 0.8)',
            'rgba(67, 233, 123, 0.8)',
            'rgba(79, 172, 254, 0.8)',
            'rgba(250, 112, 154, 0.8)',
            'rgba(161, 140, 209, 0.8)'
          ],
          borderColor: '#fff',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            position: 'bottom'
          },
          title: {
            display: true,
            text: 'Распределение по категориям',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        }
      }
    });
  }

  /**
   * График: Распределение по зарплате
   */
  function initSalaryDistributionChart() {
    var canvas = document.getElementById('chart-salary-distribution');
    if (!canvas) return;

    var ctx = canvas.getContext('2d');
    var data = window.DashboardData ? window.DashboardData.salaryDistribution : [];
    
    charts.salaryDistribution = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: data.map(function(item) { return item.name; }),
        datasets: [{
          data: data.map(function(item) { return item.count; }),
          backgroundColor: [
            'rgba(244, 67, 54, 0.8)',   // Менее 40 000
            'rgba(255, 152, 0, 0.8)',   // 40 000 - 60 000
            'rgba(76, 175, 80, 0.8)',   // 60 000 - 90 000
            'rgba(33, 150, 243, 0.8)'   // Более 90 000
          ],
          borderColor: '#fff',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            position: 'bottom'
          },
          title: {
            display: true,
            text: 'Распределение по зарплате (₽)',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        }
      }
    });
  }

  /**
   * График: Фонд оплаты по месяцам
   */
  function initPayrollByMonthChart() {
    var canvas = document.getElementById('chart-payroll-by-month');
    if (!canvas) return;

    var ctx = canvas.getContext('2d');
    var data = window.DashboardData ? window.DashboardData.payrollByMonth : [];
    
    charts.payrollByMonth = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(function(item) { return item.месяц + '/' + item.год; }),
        datasets: [{
          label: 'Фонд оплаты труда (₽)',
          data: data.map(function(item) { return item.total; }),
          backgroundColor: 'rgba(102, 126, 234, 0.2)',
          borderColor: 'rgba(102, 126, 234, 1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            display: false
          },
          title: {
            display: true,
            text: 'Фонд оплаты по месяцам',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    });
  }

  /**
   * График: Распределение по разрядам
   */
  function initWorkersByRankChart() {
    var canvas = document.getElementById('chart-workers-by-rank');
    if (!canvas) return;

    var ctx = canvas.getContext('2d');
    var data = window.DashboardData ? window.DashboardData.byRank : [];
    
    charts.byRank = new Chart(ctx, {
      type: 'radar',
      data: {
        labels: data.map(function(item) { return item.name + ' разряд'; }),
        datasets: [{
          label: 'Количество работников',
          data: data.map(function(item) { return item.count; }),
          backgroundColor: 'rgba(102, 126, 234, 0.2)',
          borderColor: 'rgba(102, 126, 234, 1)',
          pointBackgroundColor: 'rgba(102, 126, 234, 1)',
          pointBorderColor: '#fff',
          pointHoverBackgroundColor: '#fff',
          pointHoverBorderColor: 'rgba(102, 126, 234, 1)'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            display: false
          },
          title: {
            display: true,
            text: 'Распределение по разрядам',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        scales: {
          r: {
            beginAtZero: true
          }
        }
      }
    });
  }

  // --------------------------------------------------------------------------
  // Утилиты
  // --------------------------------------------------------------------------

  /**
   * Обновление данных графиков
   */
  function updateCharts(newData) {
    if (newData.byShop && charts.byShop) {
      charts.byShop.data.labels = newData.byShop.map(function(item) { return item.name; });
      charts.byShop.data.datasets[0].data = newData.byShop.map(function(item) { return item.count; });
      charts.byShop.update();
    }
    
    if (newData.byCategory && charts.byCategory) {
      charts.byCategory.data.labels = newData.byCategory.map(function(item) { return item.name; });
      charts.byCategory.data.datasets[0].data = newData.byCategory.map(function(item) { return item.count; });
      charts.byCategory.update();
    }
    
    if (newData.salaryDistribution && charts.salaryDistribution) {
      charts.salaryDistribution.data.labels = newData.salaryDistribution.map(function(item) { return item.name; });
      charts.salaryDistribution.data.datasets[0].data = newData.salaryDistribution.map(function(item) { return item.count; });
      charts.salaryDistribution.update();
    }
    
    if (newData.payrollByMonth && charts.payrollByMonth) {
      charts.payrollByMonth.data.labels = newData.payrollByMonth.map(function(item) { return item.месяц + '/' + item.год; });
      charts.payrollByMonth.data.datasets[0].data = newData.payrollByMonth.map(function(item) { return item.total; });
      charts.payrollByMonth.update();
    }
    
    if (newData.byRank && charts.byRank) {
      charts.byRank.data.labels = newData.byRank.map(function(item) { return item.name + ' разряд'; });
      charts.byRank.data.datasets[0].data = newData.byRank.map(function(item) { return item.count; });
      charts.byRank.update();
    }
  }

  /**
   * Уничтожение всех графиков
   */
  function destroyCharts() {
    Object.keys(charts).forEach(function(key) {
      if (charts[key]) {
        charts[key].destroy();
        charts[key] = null;
      }
    });
  }

  // --------------------------------------------------------------------------
  // Инициализация
  // --------------------------------------------------------------------------

  // Запуск после загрузки DOM
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCharts);
  } else {
    initCharts();
  }

  // Экспорт функций для глобального доступа
  window.DashboardCharts = {
    init: initCharts,
    update: updateCharts,
    destroy: destroyCharts
  };

})();
