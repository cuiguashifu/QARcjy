class Meteor {
  constructor(ctx, width, height, options) {
    this.ctx = ctx;
    this.color = options.color;
    this.duration = options.duration;
    this.angle = (options.direction * Math.PI) / 180;
    this.speed = Math.random() * 5 + 2;
    this.length = Math.random() * 20 + 10;
    this.startTime = Date.now();

    this.x = Math.random() * width;
    this.y = Math.random() * height;
  }

  draw() {
    const elapsed = Date.now() - this.startTime;
    if (elapsed > this.duration) return false;

    const alpha = 1 - elapsed / this.duration;

    this.ctx.beginPath();
    this.ctx.strokeStyle = `${this.color}${Math.floor(alpha * 255)
      .toString(16)
      .padStart(2, "0")}`;
    this.ctx.lineWidth = 1;

    const dx = Math.cos(this.angle) * this.length;
    const dy = Math.sin(this.angle) * this.length;

    this.ctx.moveTo(this.x, this.y);
    this.ctx.lineTo(this.x + dx, this.y + dy);
    this.ctx.stroke();

    this.x += Math.cos(this.angle) * this.speed;
    this.y += Math.sin(this.angle) * this.speed;

    return true;
  }
}

class MeteorShower {
  constructor(options = {}) {
    this.options = {
      color: options.color ?? "#ffffff",
      density: options.density ?? 2,
      duration: options.duration ?? 1000,
      direction: options.direction ?? 45,
    };

    this.canvas = document.createElement("canvas");
    this.canvas.style.position = "fixed";
    this.canvas.style.top = "0";
    this.canvas.style.left = "0";
    this.canvas.style.pointerEvents = "none";
    this.canvas.style.zIndex = "9999";
    this.resizeCanvas();
    document.body.appendChild(this.canvas);

    const context = this.canvas.getContext('2d');
    if (!context) {
      throw new Error('Failed to get 2D context from canvas');
    }
    this.ctx = context;
    this.meteors = [];
    this.animationFrameId = null;
    this.start();

    window.addEventListener("resize", () => this.resizeCanvas());
  }

  resizeCanvas() {
    this.canvas.width = window.innerWidth;
    this.canvas.height = window.innerHeight;
  }

  animate() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

    if (Math.random() < this.options.density / 60) {
      this.meteors.push(
        new Meteor(this.ctx, this.canvas.width, this.canvas.height, {
          color: this.options.color,
          duration: this.options.duration,
          direction: this.options.direction,
        })
      );
    }

    this.meteors = this.meteors.filter((meteor) => meteor.draw());
    this.animationFrameId = requestAnimationFrame(() => this.animate());
  }

  start() {
    if (!this.animationFrameId) {
      this.animate();
    }
  }

  stop() {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
      this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }
  }

  destroy() {
    this.stop();
    this.canvas.remove();
    window.removeEventListener("resize", () => this.resizeCanvas());
  }
}

// 初始化流星效果
document.addEventListener('DOMContentLoaded', function() {
  new MeteorShower({
    color: '#ffffff',
    density: 3,
    duration: 1500,
    direction: 45
  });
});