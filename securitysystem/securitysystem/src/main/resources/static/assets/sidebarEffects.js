;(function() {
  'use strict';

  function init() {
    var container = document.getElementById('st-container');
    if (!container) return;

    var pusher = container.querySelector('.st-pusher');
    var menuBtns = container.querySelectorAll('[data-effect]');
    var isOpen = false;
    var currentEffect = '';

    function openMenu(effect) {
      if (isOpen) return;
      currentEffect = effect;
      isOpen = true;

      var menu = document.getElementById(effect) || document.querySelector('.' + effect);
      if (!menu) return;

      container.classList.add(effect);
      container.classList.add('st-menu-open');
      menu.classList.add('st-menu-open');

      if (pusher) {
        pusher.addEventListener('click', onPusherClick);
      }
    }

    function closeMenu() {
      if (!isOpen) return;

      var menu = document.getElementById(currentEffect) || document.querySelector('.' + currentEffect);
      if (menu) {
        menu.classList.remove('st-menu-open');
      }

      container.classList.remove('st-menu-open');
      container.classList.remove(currentEffect);
      isOpen = false;
      currentEffect = '';

      if (pusher) {
        pusher.removeEventListener('click', onPusherClick);
      }
    }

    function onPusherClick(e) {
      if (!e.target.closest('.menu-trigger') && !e.target.closest('.st-menu')) {
        closeMenu();
      }
    }

    function toggleMenu(effect) {
      if (isOpen) {
        closeMenu();
      } else {
        openMenu(effect);
      }
    }

    menuBtns.forEach(function(btn) {
      btn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        var effect = this.getAttribute('data-effect');
        if (effect) {
          toggleMenu(effect);
        }
      });
    });

    document.addEventListener('keydown', function(e) {
      if (e.keyCode === 27 && isOpen) {
        closeMenu();
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
