// WebGL 云朵效果
if (typeof THREE === 'undefined') {
    console.error('THREE.js is not loaded');
}

if (typeof Detector === 'undefined') {
    console.error('Detector.js is not loaded');
}

if (Detector.webgl) {
    var container, camera, scene, renderer, meshes = [];
    var mouseX = 0, mouseY = 0;
    var start_time = Date.now();
    var windowHalfX = window.innerWidth / 2;
    var windowHalfY = window.innerHeight / 2;

    function initClouds() {
        container = document.createElement('div');
        container.style.position = 'fixed';
        container.style.top = '0';
        container.style.left = '0';
        container.style.width = '100%';
        container.style.height = '100%';
        container.style.pointerEvents = 'none';
        container.style.zIndex = '1';
        container.style.background = 'transparent';
        document.body.appendChild(container);

        // Setup camera, scene, and renderer
        camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 1, 10000);
        camera.position.z = 3000;

        scene = new THREE.Scene();

        var fog = new THREE.Fog(0x4584b4, -100, 8000);

        var material = new THREE.ShaderMaterial({
            uniforms: {
                "map": { type: "t", value: null },
                "fogColor": { type: "c", value: fog.color },
                "fogNear": { type: "f", value: fog.near },
                "fogFar": { type: "f", value: fog.far },
            },
            vertexShader: `
                varying vec2 vUv;
                void main() {
                    vUv = uv;
                    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
                }
            `,
            fragmentShader: `
                uniform sampler2D map;
                uniform vec3 fogColor;
                uniform float fogNear;
                uniform float fogFar;
                varying vec2 vUv;
                void main() {
                    float depth = gl_FragCoord.z / gl_FragCoord.w;
                    float fogFactor = smoothstep(fogNear, fogFar, depth);
                    gl_FragColor = texture2D(map, vUv);
                    gl_FragColor.w *= pow(gl_FragCoord.z, 20.0);
                    gl_FragColor = mix(gl_FragColor, vec4(fogColor, gl_FragColor.w), fogFactor);
                }
            `,
            depthWrite: false,
            depthTest: false,
            transparent: true
        });

        // 创建多个云平面而不是合并几何体
        for (var i = 0; i < 200; i++) {
            var geometry = new THREE.PlaneGeometry(128, 128);
            var cloud = new THREE.Mesh(geometry, material);
            cloud.position.x = Math.random() * 1000 - 500;
            cloud.position.y = -Math.random() * Math.random() * 200 + 100;
            cloud.position.z = Math.random() * 8000;
            cloud.rotation.z = Math.random() * Math.PI;
            cloud.scale.x = cloud.scale.y = Math.random() * Math.random() * 2 + 1;
            scene.add(cloud);
            meshes.push(cloud);
        }

        renderer = new THREE.WebGLRenderer({ antialias: false });
        renderer.setSize(window.innerWidth, window.innerHeight);
        container.appendChild(renderer.domElement);

        document.addEventListener('mousemove', onDocumentMouseMove, false);
        window.addEventListener('resize', onWindowResize, false);

        // 加载纹理
        var textureLoader = new THREE.TextureLoader();
        textureLoader.load('/assets/webgl/cloud10.png', function(texture) {
            texture.magFilter = THREE.LinearMipMapLinearFilter;
            texture.minFilter = THREE.LinearMipMapLinearFilter;
            material.uniforms.map.value = texture;
            animateClouds();
        }, undefined, function(error) {
            console.error('Error loading cloud texture:', error);
        });
    }

    function onDocumentMouseMove(event) {
        mouseX = (event.clientX - windowHalfX) * 0.25;
        mouseY = (event.clientY - windowHalfY) * 0.15;
    }

    function onWindowResize(event) {
        windowHalfX = window.innerWidth / 2;
        windowHalfY = window.innerHeight / 2;
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    }

    function animateClouds() {
        requestAnimationFrame(animateClouds);
        var position = ((Date.now() - start_time) * 0.03) % 8000;
        
        // 更新相机位置
        camera.position.x += (mouseX - camera.position.x) * 0.01;
        camera.position.y += (-mouseY - camera.position.y) * 0.01;
        camera.position.z = -position + 8000;
        
        // 更新云的位置
        for (var i = 0; i < meshes.length; i++) {
            var cloud = meshes[i];
            cloud.position.z -= 0.3;
            if (cloud.position.z < -8000) {
                cloud.position.z = 8000;
            }
        }
        
        renderer.render(scene, camera);
    }

    // Initialize clouds when DOM is loaded
    document.addEventListener('DOMContentLoaded', function() {
        try {
            initClouds();
            console.log('WebGL clouds initialized successfully');
        } catch (error) {
            console.error('Error initializing WebGL clouds:', error);
        }
    });
} else {
    console.log('WebGL not supported');
    Detector.addGetWebGLMessage();
}