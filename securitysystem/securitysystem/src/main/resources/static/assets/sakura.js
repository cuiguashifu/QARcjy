var SakuraEffect = (function() {

  var canvas = null;
  var cxt = null;
  var animId = null;
  var petals = [];
  var PETAL_COUNT = 35;

  function Petal() {
    this.reset(true);
  }

  Petal.prototype.reset = function(isInit) {
    this.x = Math.random() * window.innerWidth;
    this.y = isInit ? Math.random() * window.innerHeight : -20;
    this.size = 8 + Math.random() * 16;
    this.speedY = 0.5 + Math.random() * 1.5;
    this.speedX = -0.5 + Math.random() * 1;
    this.rotation = Math.random() * Math.PI * 2;
    this.rotationSpeed = (Math.random() - 0.5) * 0.03;
    this.opacity = 0.4 + Math.random() * 0.5;
    this.swingAmp = 30 + Math.random() * 40;
    this.swingSpeed = 0.01 + Math.random() * 0.02;
    this.swingOffset = Math.random() * Math.PI * 2;
    this.tilt = Math.random() * 0.6 + 0.2;
    var hue = 330 + Math.random() * 30;
    var sat = 50 + Math.random() * 30;
    var light = 70 + Math.random() * 20;
    this.color = 'hsla(' + hue + ',' + sat + '%,' + light + '%,' + this.opacity + ')';
    this.colorInner = 'hsla(' + hue + ',' + (sat - 10) + '%,' + (light + 10) + '%,' + this.opacity + ')';
  };

  Petal.prototype.update = function(time) {
    this.y += this.speedY;
    this.x += this.speedX + Math.sin(time * this.swingSpeed + this.swingOffset) * 0.5;
    this.rotation += this.rotationSpeed;

    if (this.y > window.innerHeight + 20 || this.x < -50 || this.x > window.innerWidth + 50) {
      this.reset(false);
    }
  };

  Petal.prototype.draw = function(ctx) {
    ctx.save();
    ctx.translate(this.x, this.y);
    ctx.rotate(this.rotation);
    ctx.scale(1, this.tilt);

    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.bezierCurveTo(
      -this.size * 0.5, -this.size * 0.8,
      -this.size * 0.1, -this.size * 1.2,
      0, -this.size * 0.7
    );
    ctx.bezierCurveTo(
      this.size * 0.1, -this.size * 1.2,
      this.size * 0.5, -this.size * 0.8,
      0, 0
    );
    ctx.fillStyle = this.color;
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(0, -this.size * 0.1);
    ctx.quadraticCurveTo(-this.size * 0.05, -this.size * 0.5, 0, -this.size * 0.6);
    ctx.quadraticCurveTo(this.size * 0.05, -this.size * 0.5, 0, -this.size * 0.1);
    ctx.fillStyle = this.colorInner;
    ctx.fill();

    ctx.restore();
  };

  function init() {
    stop();

    canvas = document.createElement('canvas');
    canvas.id = 'sakura-canvas';
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    canvas.style.cssText = 'position:fixed;left:0;top:0;pointer-events:none;z-index:1;';
    document.body.appendChild(canvas);
    cxt = canvas.getContext('2d');

    petals = [];
    for (var i = 0; i < PETAL_COUNT; i++) {
      petals.push(new Petal());
    }

    var startTime = Date.now();
    function animate() {
      var time = Date.now() - startTime;
      cxt.clearRect(0, 0, canvas.width, canvas.height);
      for (var i = 0; i < petals.length; i++) {
        petals[i].update(time);
        petals[i].draw(cxt);
      }
      animId = requestAnimationFrame(animate);
    }
    animate();
  }

  function stop() {
    if (animId) {
      cancelAnimationFrame(animId);
      animId = null;
    }
    if (canvas && canvas.parentNode) {
      canvas.parentNode.removeChild(canvas);
    }
    canvas = null;
    cxt = null;
    petals = [];
  }

  window.addEventListener('resize', function() {
    if (canvas) {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }
  });

  return {
    init: init,
    stop: stop
  };

})();

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', SakuraEffect.init);
} else {
  SakuraEffect.init();
}
