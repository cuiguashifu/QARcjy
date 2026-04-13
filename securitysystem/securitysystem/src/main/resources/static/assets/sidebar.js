// sidebarJS implementation
if (typeof window.jQuery !== 'undefined') {
    (function($) {
        'use strict';
        
        $.fn.sidebarMenu = function(options) {
            var defaults = {
                speed: 200
            };
            
            var options = $.extend(defaults, options);
            
            return this.each(function() {
                var $sidebar = $(this);
                
                // Add click event to menu items with sub-menu
                $sidebar.find('li').each(function() {
                    var $li = $(this);
                    var $a = $li.find('a:first');
                    var $subMenu = $li.find('.sub-menu');
                    
                    if ($subMenu.length > 0) {
                        $a.on('click', function(e) {
                            e.preventDefault();
                            
                            // Toggle sub-menu
                            if ($li.hasClass('active')) {
                                $subMenu.slideUp(options.speed);
                                $li.removeClass('active');
                            } else {
                                // Close other sub-menus
                                $sidebar.find('li.active .sub-menu').slideUp(options.speed);
                                $sidebar.find('li.active').removeClass('active');
                                
                                // Open current sub-menu
                                $subMenu.slideDown(options.speed);
                                $li.addClass('active');
                            }
                        });
                    }
                });
            });
        };
        
        // Mobile menu toggle
        $(document).ready(function() {
            $('.menu_icon, .close_btn').on('click', function(e) {
                e.preventDefault();
                $('.menu_icon').toggleClass('active');
                $('.sidenav_menu').toggleClass('active');
                $('.animate-menu-push').toggleClass('animate-menu-push-right');
            });
            
            // Initialize sidebar menu
            if ($('.sidebar-menu').length > 0) {
                $('.sidebar-menu').sidebarMenu();
            }
        });
        
    })(window.jQuery);
} else {
    // Fallback if jQuery is not loaded
    console.log('jQuery is not loaded, sidebar menu functionality will be limited');
    
    // Add basic mobile menu toggle without jQuery
    document.addEventListener('DOMContentLoaded', function() {
        var menuIcon = document.querySelector('.menu_icon');
        var closeBtn = document.querySelector('.close_btn');
        var sideNav = document.querySelector('.sidenav_menu');
        var pushElement = document.querySelector('.animate-menu-push');
        
        function toggleMenu() {
            if (menuIcon) menuIcon.classList.toggle('active');
            if (sideNav) sideNav.classList.toggle('active');
            if (pushElement) pushElement.classList.toggle('animate-menu-push-right');
        }
        
        if (menuIcon) {
            menuIcon.addEventListener('click', toggleMenu);
        }
        
        if (closeBtn) {
            closeBtn.addEventListener('click', toggleMenu);
        }
    });
}