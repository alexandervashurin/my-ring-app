/* ==========================================================================
   Система управления персоналом - Клиентская валидация форм
   ========================================================================== */

(function() {
  'use strict';

  // ========================================================================
  // Утилиты
  // ========================================================================

  function showError(input, message) {
    clearError(input);
    var group = input.closest('.form-group');
    if (!group) return;
    var err = document.createElement('div');
    err.className = 'field-error';
    err.textContent = message;
    group.appendChild(err);
    input.classList.add('input-error');
  }

  function clearError(input) {
    var group = input.closest('.form-group');
    if (!group) return;
    var existing = group.querySelector('.field-error');
    if (existing) existing.remove();
    input.classList.remove('input-error');
  }

  function clearAllErrors(form) {
    form.querySelectorAll('.field-error').forEach(function(e) { e.remove(); });
    form.querySelectorAll('.input-error').forEach(function(e) { e.classList.remove('input-error'); });
  }

  // ========================================================================
  // Валидация формы работника
  // ========================================================================

  function validateWorkerForm(form) {
    clearAllErrors(form);
    var errors = [];

    // Фамилия
    var fam = form.querySelector('[name="фамилия"]');
    if (fam) {
      var val = fam.value.trim();
      if (!val) { errors.push('Фамилия обязательна'); showError(fam, 'Фамилия обязательна'); }
      else if (val.length > 50) { errors.push('Фамилия не должна превышать 50 символов'); showError(fam, 'Максимум 50 символов'); }
    }

    // Имя
    var imya = form.querySelector('[name="имя"]');
    if (imya) {
      var val = imya.value.trim();
      if (!val) { errors.push('Имя обязательно'); showError(imya, 'Имя обязательно'); }
      else if (val.length > 50) { errors.push('Имя не должно превышать 50 символов'); showError(imya, 'Максимум 50 символов'); }
    }

    // Отчество
    var otch = form.querySelector('[name="отчество"]');
    if (otch) {
      var val = otch.value.trim();
      if (val.length > 50) { errors.push('Отчество не должно превышать 50 символов'); showError(otch, 'Максимум 50 символов'); }
    }

    // Дата приема
    var date = form.querySelector('[name="дата_приема"]');
    if (date) {
      var val = date.value.trim();
      if (!val) { errors.push('Дата приема обязательна'); showError(date, 'Дата приема обязательна'); }
      else if (!/^\d{4}-\d{2}-\d{2}$/.test(val)) { errors.push('Неверный формат даты'); showError(date, 'Формат: ГГГГ-ММ-ДД'); }
      else {
        var d = new Date(val);
        var today = new Date(); today.setHours(0,0,0,0);
        if (d > today) { errors.push('Дата приема не может быть в будущем'); showError(date, 'Дата не может быть в будущем'); }
      }
    }

    // Обязательные select'ы
    var requiredSelects = [
      {name: 'цех_id', label: 'Цех'},
      {name: 'система_оплаты_id', label: 'Система оплаты'},
      {name: 'категория_работника_id', label: 'Категория работника'},
      {name: 'разряд_id', label: 'Разряд'},
      {name: 'режим_работы_id', label: 'Режим работы'}
    ];
    requiredSelects.forEach(function(s) {
      var el = form.querySelector('[name="' + s.name + '"]');
      if (el && !el.value) { errors.push('Необходимо выбрать ' + s.label); showError(el, 'Выберите ' + s.label); }
    });

    // Условная валидация оклад/ставка
    var sysSelect = form.querySelector('[name="система_оплаты_id"]');
    if (sysSelect) {
      if (sysSelect.value === '1') {
        var oklad = form.querySelector('[name="оклад_id"]');
        if (oklad && !oklad.value) { errors.push('Для окладной системы необходимо выбрать оклад'); showError(oklad, 'Выберите оклад'); }
      } else if (sysSelect.value === '2') {
        var stavka = form.querySelector('[name="почасовая_ставка_id"]');
        if (stavka && !stavka.value) { errors.push('Для почасовой системы необходимо выбрать ставку'); showError(stavka, 'Выберите ставку'); }
      }
    }

    return errors.length === 0;
  }

  // ========================================================================
  // Валидация формы учета времени
  // ========================================================================

  function validateWorkTimeForm(form) {
    clearAllErrors(form);
    var errors = [];

    // Год
    var god = form.querySelector('[name="год"]');
    if (god) {
      var val = god.value.trim();
      if (!val) { errors.push('Год обязателен'); showError(god, 'Год обязателен'); }
      else if (!/^\d{4}$/.test(val)) { errors.push('Неверный формат года'); showError(god, '4 цифры'); }
    }

    // Месяц
    var mes = form.querySelector('[name="месяц"]');
    if (mes) {
      var val = mes.value.trim();
      if (!val) { errors.push('Месяц обязателен'); showError(mes, 'Месяц обязателен'); }
      else if (!/^\d{1,2}$/.test(val)) { errors.push('Неверный формат месяца'); showError(mes, '1-12'); }
      else {
        var n = parseInt(val, 10);
        if (n < 1 || n > 12) { errors.push('Месяц должен быть от 1 до 12'); showError(mes, '1-12'); }
      }
    }

    // Плановые часы
    var plan = form.querySelector('[name="всего_часов_за_месяц_по_плану"]');
    if (plan) {
      var val = plan.value.trim();
      if (!val) { errors.push('Плановые часы обязательны'); showError(plan, 'Обязательно'); }
      else if (!/^\d+$/.test(val)) { errors.push('Плановые часы должны быть числом'); showError(plan, 'Только числа'); }
    }

    // Фактические часы
    var fact = form.querySelector('[name="всего_часов_в_месяц_по_факту"]');
    if (fact) {
      var val = fact.value.trim();
      if (!val) { errors.push('Фактические часы обязательны'); showError(fact, 'Обязательно'); }
      else if (!/^\d+$/.test(val)) { errors.push('Фактические часы должны быть числом'); showError(fact, 'Только числа'); }
    }

    // Числовые необязательные поля
    var numericFields = [
      'количество_отработанных_дней',
      'количество_рабочих_часов_в_день',
      'больничные_дни',
      'командировочные_дни'
    ];
    numericFields.forEach(function(name) {
      var el = form.querySelector('[name="' + name + '"]');
      if (el) {
        var val = el.value.trim();
        if (val && !/^\d+$/.test(val)) { errors.push(name + ' должно быть числом'); showError(el, 'Только числа'); }
      }
    });

    return errors.length === 0;
  }

  // ========================================================================
  // Переключение полей оплаты (Оклад / Почасовая)
  // ========================================================================

  window.togglePaymentFields = function(systemId) {
    var okladField = document.getElementById('oklad-field');
    var stavkaField = document.getElementById('stavka-field');
    if (!okladField || !stavkaField) return;
    if (systemId === '1') {
      okladField.style.display = 'block';
      stavkaField.style.display = 'none';
    } else if (systemId === '2') {
      okladField.style.display = 'none';
      stavkaField.style.display = 'block';
    } else {
      okladField.style.display = 'none';
      stavkaField.style.display = 'none';
    }
  };

  // ========================================================================
  // Инициализация
  // ========================================================================

  function initValidation() {
    // Worker forms
    var workerForms = document.querySelectorAll('form[action*="/workers/create"], form[action*="/update"]');
    workerForms.forEach(function(form) {
      // Restore payment fields visibility on edit
      var sysSelect = form.querySelector('[name="система_оплаты_id"]');
      if (sysSelect && sysSelect.value) {
        window.togglePaymentFields(sysSelect.value);
      }
      if (sysSelect) {
        sysSelect.addEventListener('change', function() {
          window.togglePaymentFields(this.value);
        });
      }

      form.addEventListener('submit', function(e) {
        if (!validateWorkerForm(form)) {
          e.preventDefault();
          var firstError = form.querySelector('.input-error');
          if (firstError) firstError.focus();
        }
      });
    });

    // Work-time forms
    var workTimeForms = document.querySelectorAll('form[action*="/work-time/"]');
    workTimeForms.forEach(function(form) {
      form.addEventListener('submit', function(e) {
        if (!validateWorkTimeForm(form)) {
          e.preventDefault();
          var firstError = form.querySelector('.input-error');
          if (firstError) firstError.focus();
        }
      });
    });
  }

  // Run on DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initValidation);
  } else {
    initValidation();
  }

})();
