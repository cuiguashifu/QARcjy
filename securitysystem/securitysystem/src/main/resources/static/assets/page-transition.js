var PageTransitions = (function() {

  var loader = null;
  var isAnimating = false;

  var effects = {
    1: {
      opening: 'M20,15 50,30 50,30 30,30 Z;M0,0 80,0 50,30 20,45 Z;M0,0 80,0 60,45 0,60 Z;M0,0 80,0 80,60 0,60 Z',
      closing: 'M0,0 80,0 60,45 0,60 Z;M0,0 80,0 50,30 20,45 Z;M20,15 50,30 50,30 30,30 Z;M30,30 50,30 50,30 30,30 Z',
      initial: 'M30,30 50,30 50,30 30,30 Z',
      speedIn: 100
    },
    4: {
      opening: 'M 0,0 0,60 80,60 80,0 Z M 40,30 40,30 40,30 40,30 Z',
      closing: '',
      initial: 'M 0,0 0,60 80,60 80,0 Z M 80,0 80,60 0,60 0,0 Z',
      speedIn: 300
    },
    7: {
      opening: 'M 0,60 80,60 80,50 0,40 0,60;M 0,60 80,60 80,25 0,40 0,60;M 0,60 80,60 80,25 0,10 0,60;M 0,60 80,60 80,0 0,0 0,60',
      closing: 'M 0,60 80,60 80,20 0,0 0,60;M 0,60 80,60 80,20 0,40 0,60;m 0,60 80,0 0,0 -80,0',
      initial: 'm 0,60 80,0 0,0 -80,0',
      speedIn: 200
    },
    10: {
      opening: 'M 40,-65 145,80 -65,80 40,-65',
      closing: 'm 40,-65 0,0 L -65,80 40,-65',
      initial: 'M 40,-65 145,80 40,-65',
      speedIn: 500
    },
    13: {
      opening: 'm 40,-80 190,0 -305,290 C -100,140 0,0 40,-80 z',
      closing: '',
      initial: 'm 75,-80 155,0 0,225 C 90,85 100,30 75,-80 z',
      speedIn: 700
    }
  };

  var currentEffect = effects[1];

  function createLoaderOverlay() {
    var existing = document.getElementById('pageload-overlay');
    if (existing) return existing;

    var overlay = document.createElement('div');
    overlay.id = 'pageload-overlay';
    overlay.className = 'pageload-overlay';

    if (currentEffect.opening) overlay.setAttribute('data-opening', currentEffect.opening);
    if (currentEffect.closing) overlay.setAttribute('data-closing', currentEffect.closing);

    var svgNS = 'http://www.w3.org/2000/svg';
    var svg = document.createElementNS(svgNS, 'svg');
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.setAttribute('viewBox', '0 0 80 60');
    svg.setAttribute('preserveAspectRatio', 'none');

    var path = document.createElementNS(svgNS, 'path');
    path.setAttribute('d', currentEffect.initial);
    svg.appendChild(path);
    overlay.appendChild(svg);

    document.body.appendChild(overlay);
    return overlay;
  }

  function initLoader() {
    var overlayEl = createLoaderOverlay();
    if (typeof SVGLoader !== 'undefined') {
      loader = new SVGLoader(overlayEl, {
        speedIn: currentEffect.speedIn || 300,
        easingIn: typeof mina !== 'undefined' ? mina.easeinout : undefined
      });
    }
  }

  function navigateTo(url) {
    if (isAnimating) return;
    if (!loader) {
      window.location.href = url;
      return;
    }

    isAnimating = true;
    loader.show();

    setTimeout(function() {
      loader.hide();
      setTimeout(function() {
        window.location.href = url;
      }, Math.max(currentEffect.speedIn || 300, 350));
    }, 600);
  }

  function playInAnimation() {
    if (!loader) return;

    var shouldAnimate = sessionStorage.getItem('pt-pageload-in');
    if (!shouldAnimate) return;
    sessionStorage.removeItem('pt-pageload-in');

    isAnimating = true;
    loader.show();

    setTimeout(function() {
      loader.hide();
      setTimeout(function() {
        var overlay = document.getElementById('pageload-overlay');
        if (overlay) {
          overlay.classList.remove('pageload-loading', 'show');
          overlay.style.visibility = 'hidden';
        }
        isAnimating = false;
      }, Math.max(currentEffect.speedIn || 300, 400));
    }, 400);
  }

  function init() {
    initLoader();

    playInAnimation();

    document.addEventListener('click', function(e) {
      var link = e.target.closest('a');
      if (!link) return;

      var href = link.getAttribute('href');
      if (!href || href.startsWith('#') || href.startsWith('javascript:') || link.target === '_blank') return;

      var isInternal = href.startsWith('/') || (href.indexOf(window.location.host) > -1);
      if (isInternal && !href.startsWith('/api/')) {
        e.preventDefault();
        sessionStorage.setItem('pt-pageload-in', '1');
        navigateTo(href);
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  return {
    navigateTo: navigateTo,
    setEffect: function(id) {
      if (effects[id]) {
        currentEffect = effects[id];
        var overlay = document.getElementById('pageload-overlay');
        if (overlay) {
          overlay.parentNode.removeChild(overlay);
        }
        initLoader();
      }
    }
  };

})();
