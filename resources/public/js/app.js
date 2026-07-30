(function() {
  'use strict';

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', function() {
      navigator.serviceWorker.register('/sw.js')
        .then(function(registration) {
          console.log('[PWA] Service Worker зарегистрирован:', registration.scope);
        })
        .catch(function(error) {
          console.log('[PWA] Ошибка регистрации Service Worker:', error);
        });
    });
  }
})();
