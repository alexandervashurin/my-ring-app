/* ==========================================================================
   Service Worker для PWA
   Система управления персоналом
   ========================================================================== */

const CACHE_NAME = 'hr-system-v2';
const STATIC_ASSETS = [
  '/css/app.css',
  '/js/app.js',
  '/js/charts.js',
  '/manifest.json'
];

// ============================================================================
// Установка Service Worker
// ============================================================================

self.addEventListener('install', (event) => {
  console.log('[SW] Установка Service Worker...');
  
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('[SW] Кэширование статических ресурсов');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => {
        console.log('[SW] Service Worker установлен');
        return self.skipWaiting();
      })
      .catch((error) => {
        console.error('[SW] Ошибка при установке:', error);
      })
  );
});

// ============================================================================
// Активация Service Worker
// ============================================================================

self.addEventListener('activate', (event) => {
  console.log('[SW] Активация Service Worker...');
  
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames
            .filter((cacheName) => {
              // Удаляем старые версии кэша
              return cacheName !== CACHE_NAME;
            })
            .map((cacheName) => {
              console.log('[SW] Удаление старого кэша:', cacheName);
              return caches.delete(cacheName);
            })
        );
      })
      .then(() => {
        console.log('[SW] Service Worker активирован');
        return self.clients.claim();
      })
  );
});

// ============================================================================
// Перехват запросов
// ============================================================================

self.addEventListener('fetch', (event) => {
  // Игнорируем не-GET запросы
  if (event.request.method !== 'GET') {
    return;
  }
  
  // Игнорируем внешние запросы
  const requestUrl = new URL(event.request.url);
  if (requestUrl.origin !== location.origin) {
    return;
  }
  
  // Не кэшируем HTML — они всегда должны быть свежими
  if (requestUrl.pathname.endsWith('/') || !requestUrl.pathname.includes('.')) {
    event.respondWith(fetchAndCache(event.request));
    return;
  }

  event.respondWith(
    caches.match(event.request)
      .then((cachedResponse) => {
        if (cachedResponse) {
          console.log('[SW] Найдено в кэше:', event.request.url);
          fetchAndCache(event.request);
          return cachedResponse;
        }
        return fetchAndCache(event.request);
      })
      .catch(() => {
        if (event.request.mode === 'navigate') {
          return caches.match('/offline.html');
        }
        return new Response('', { status: 503, statusText: 'Service Unavailable' });
      })
  );
});

// ============================================================================
// Вспомогательные функции
// ============================================================================

function fetchAndCache(request) {
  return fetch(request)
    .then((response) => {
      // Проверяем успешный ответ
      if (response.status === 200) {
        const responseClone = response.clone();
        
        caches.open(CACHE_NAME)
          .then((cache) => {
            cache.put(request, responseClone);
          });
      }
      
      return response;
    })
    .catch((error) => {
      console.error('[SW] Ошибка fetch:', error);
      throw error;
    });
}

// ============================================================================
// Уведомления о обновлениях
// ============================================================================

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// ============================================================================
// Фоновая синхронизация (опционально)
// ============================================================================

self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-data') {
    event.waitUntil(
      // Логика синхронизации данных
      Promise.resolve()
    );
  }
});

console.log('[SW] Service Worker загружен');
