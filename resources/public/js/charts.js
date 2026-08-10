/* ==========================================================================
   Система управления персоналом - Графики и диаграммы (Chart.js)
   Отзывчивые графики: maintainAspectRatio=false, адаптивные подписи,
   поддержка high-DPI, обработка пустых данных.
   ========================================================================== */

(function() {
  'use strict';

  // Глобальное хранилище для экземпляров графиков
  var charts = {};

  var EMPTY_MESSAGE = 'Нет данных';

  function isMobile() {
    return window.innerWidth < 768;
  }

  function dashboardData(key) {
    var data = window.DashboardData ? window.DashboardData[key] : null;
    return data || [];
  }

  function chartFontSize(mobileSize, desktopSize) {
    return isMobile() ? mobileSize : desktopSize;
  }

  /**
   * Базовые опции для всех графиков: заполняют контейнер,
   * адаптируются к любому размеру экрана.
   */
  function baseOptions() {
    return {
      responsive: true,
      maintainAspectRatio: false,
      devicePixelRatio: Math.min(window.devicePixelRatio || 1, 2),
      resizeDelay: 100,
      layout: {
        padding: {
          top: 10,
          right: 10,
          bottom: 10,
          left: 10
        }
      },
      animation: {
        duration: 400
      },
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            boxWidth: 12,
            padding: 8,
            font: {
              size: chartFontSize(10, 11)
            }
          }
        },
        title: {
          display: true,
          font: {
            size: chartFontSize(14, 16),
            weight: 'bold'
          }
        }
      }
    };
  }

  /**
   * Глубокое слияние объектов опций Chart.js (вложенные plugins/scales)
   */
  function mergeOptions(target, source) {
    if (!source) return target;
    Object.keys(source).forEach(function(key) {
      var sv = source[key];
      var tv = target[key];
      if (sv && typeof sv === 'object' && !Array.isArray(sv) &&
          tv && typeof tv === 'object' && !Array.isArray(tv)) {
        mergeOptions(tv, sv);
      } else {
        target[key] = sv;
      }
    });
    return target;
  }

  /**
   * Рендеринг placeholder'а для пустых данных
   */
  function showEmptyState(canvas) {
    var container = canvas.closest('.chart-body') || canvas.parentNode;
    if (!container) return;

    var empty = document.createElement('div');
    empty.className = 'chart-empty';
    empty.textContent = EMPTY_MESSAGE;
    empty.setAttribute('data-chart-empty', 'true');
    container.appendChild(empty);
  }

  function clearEmptyState(canvas) {
    var container = canvas.closest('.chart-body') || canvas.parentNode;
    if (!container) return;
    var empty = container.querySelector('[data-chart-empty="true"]');
    if (empty) empty.remove();
  }

  /**
   * Создание графика. Если данных нет — показывает placeholder.
   */
  function createChart(id, config) {
    var canvas = document.getElementById(id);
    if (!canvas) return;

    clearEmptyState(canvas);

    var items = config.getItems();
    if (!items || items.length === 0) {
      showEmptyState(canvas);
      return;
    }

    var ctx = canvas.getContext('2d');
    var options = baseOptions();
    options.plugins.title.text = config.title;

    if (config.options) {
      options = mergeOptions(options, config.options);
    }

    charts[id] = new Chart(ctx, {
      type: config.type,
      data: config.getData(items),
      options: options
    });
  }

  /**
   * Обновление данных существующего графика.
   */
  function refreshChart(id, config) {
    var chart = charts[id];
    var canvas = document.getElementById(id);

    if (!canvas) return;

    var items = config.getItems();

    if (!items || items.length === 0) {
      if (chart) {
        chart.destroy();
        charts[id] = null;
      }
      showEmptyState(canvas);
      return;
    }

    if (!chart) {
      clearEmptyState(canvas);
      createChart(id, config);
      return;
    }

    var data = config.getData(items);
    chart.data.labels = data.labels;
    chart.data.datasets = data.datasets;
    chart.update();
  }

  function byNameCount(items) {
    return {
      labels: items.map(function(item) { return item.name; }),
      datasets: [{
        data: items.map(function(item) { return item.count; })
      }]
    };
  }

  // --------------------------------------------------------------------------
  // Конфигурация графиков
  // --------------------------------------------------------------------------

  var CHART_CONFIG = {
    'chart-workers-by-shop': {
      type: 'bar',
      title: 'Распределение по цехам',
      getItems: function() { return dashboardData('byShop'); },
      getData: function(items) {
        return {
          labels: items.map(function(item) { return item.name; }),
          datasets: [{
            label: 'Количество работников',
            data: items.map(function(item) { return item.count; }),
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
            borderWidth: 1,
            maxBarThickness: 42
          }]
        };
      },
      options: {
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function(context) {
                return context.label + ': ' + context.parsed.y + ' чел.';
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1
            }
          },
          x: {
            ticks: {
              autoSkip: true,
              maxRotation: isMobile() ? 45 : 30,
              minRotation: 0,
              maxTicksLimit: isMobile() ? 8 : 12,
              font: {
                size: chartFontSize(10, 11)
              }
            }
          }
        }
      }
    },

    'chart-workers-by-category': {
      type: 'pie',
      title: 'Распределение по категориям',
      getItems: function() { return dashboardData('byCategory'); },
      getData: function(items) {
        var data = byNameCount(items);
        data.datasets[0].backgroundColor = [
          'rgba(102, 126, 234, 0.8)',
          'rgba(118, 75, 162, 0.8)',
          'rgba(67, 233, 123, 0.8)',
          'rgba(79, 172, 254, 0.8)',
          'rgba(250, 112, 154, 0.8)',
          'rgba(161, 140, 209, 0.8)'
        ];
        data.datasets[0].borderColor = '#fff';
        data.datasets[0].borderWidth = 2;
        return data;
      },
      options: {
        plugins: {
          tooltip: {
            callbacks: {
              label: function(context) {
                var label = context.label || '';
                var value = context.parsed || 0;
                var total = context.dataset.data.reduce(function(a, b) { return a + b; }, 0);
                var percentage = total > 0 ? ((value / total) * 100).toFixed(1) + '%' : '';
                return label + ': ' + value + ' чел. (' + percentage + ')';
              }
            }
          }
        }
      }
    },

    'chart-salary-distribution': {
      type: 'doughnut',
      title: 'Распределение по зарплате (₽)',
      getItems: function() { return dashboardData('salaryDistribution'); },
      getData: function(items) {
        var data = byNameCount(items);
        data.datasets[0].backgroundColor = [
          'rgba(244, 67, 54, 0.8)',
          'rgba(255, 152, 0, 0.8)',
          'rgba(76, 175, 80, 0.8)',
          'rgba(33, 150, 243, 0.8)'
        ];
        data.datasets[0].borderColor = '#fff';
        data.datasets[0].borderWidth = 2;
        return data;
      },
      options: {
        cutout: isMobile() ? '55%' : '62%',
        plugins: {
          tooltip: {
            callbacks: {
              label: function(context) {
                var label = context.label || '';
                var value = context.parsed || 0;
                var total = context.dataset.data.reduce(function(a, b) { return a + b; }, 0);
                var percentage = total > 0 ? ((value / total) * 100).toFixed(1) + '%' : '';
                return label + ': ' + value + ' чел. (' + percentage + ')';
              }
            }
          }
        }
      }
    },

    'chart-payroll-by-month': {
      type: 'line',
      title: 'Фонд оплаты по месяцам',
      getItems: function() { return dashboardData('payrollByMonth'); },
      getData: function(items) {
        return {
          labels: items.map(function(item) { return item.month + '/' + item.year; }),
          datasets: [{
            label: 'Фонд оплаты труда (₽)',
            data: items.map(function(item) { return item.total; }),
            backgroundColor: 'rgba(102, 126, 234, 0.2)',
            borderColor: 'rgba(102, 126, 234, 1)',
            borderWidth: 2,
            fill: true,
            tension: 0.4,
            pointRadius: chartFontSize(3, 4)
          }]
        };
      },
      options: {
        plugins: {
          legend: { display: false }
        },
        interaction: {
          mode: 'index',
          intersect: false
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                return (value / 1000) + ' тыс ₽';
              }
            }
          },
          x: {
            ticks: {
              autoSkip: true,
              maxTicksLimit: isMobile() ? 6 : 12,
              font: {
                size: chartFontSize(10, 11)
              }
            }
          }
        }
      }
    },

    'chart-workers-by-rank': {
      type: 'radar',
      title: 'Распределение по разрядам',
      getItems: function() { return dashboardData('byRank'); },
      getData: function(items) {
        return {
          labels: items.map(function(item) { return item.name + ' разряд'; }),
          datasets: [{
            label: 'Количество работников',
            data: items.map(function(item) { return item.count; }),
            backgroundColor: 'rgba(102, 126, 234, 0.2)',
            borderColor: 'rgba(102, 126, 234, 1)',
            pointBackgroundColor: 'rgba(102, 126, 234, 1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: 'rgba(102, 126, 234, 1)'
          }]
        };
      },
      options: {
        plugins: {
          legend: { display: false }
        },
        scales: {
          r: {
            beginAtZero: true,
            ticks: {
              backdropPadding: 2,
              font: {
                size: chartFontSize(9, 10)
              }
            },
            pointLabels: {
              font: {
                size: chartFontSize(9, 10)
              }
            }
          }
        }
      }
    }
  };

  // --------------------------------------------------------------------------
  // Инициализация
  // --------------------------------------------------------------------------

  function initCharts() {
    Object.keys(CHART_CONFIG).forEach(function(id) {
      createChart(id, CHART_CONFIG[id]);
    });
  }

  /**
   * Обновление данных всех графиков
   */
  function updateCharts(newData) {
    if (newData) {
      window.DashboardData = newData;
    }
    Object.keys(CHART_CONFIG).forEach(function(id) {
      refreshChart(id, CHART_CONFIG[id]);
    });
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

  /**
   * Перерисовка после изменения размеров контейнеров
   */
  function resizeAllCharts() {
    Object.keys(charts).forEach(function(key) {
      if (charts[key]) charts[key].resize();
    });
  }

  // Реакция на изменение размеров окна (с задержкой)
  var resizeTimer = null;
  window.addEventListener('resize', function() {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(resizeAllCharts, 150);
  });

  // Реакция на изменение раскладки (мобильная навигация, sidebar и т.п.)
  if (typeof ResizeObserver !== 'undefined') {
    var observer = new ResizeObserver(function() {
      resizeAllCharts();
    });
    Object.keys(CHART_CONFIG).forEach(function(id) {
      var canvas = document.getElementById(id);
      if (canvas) observer.observe(canvas.parentNode || canvas);
    });
    window.__chartResizeObserver = observer;
  }

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
    destroy: destroyCharts,
    resize: resizeAllCharts
  };

})();
