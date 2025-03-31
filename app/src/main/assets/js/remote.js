// 遥控器功能实现
function remoteControl(action) {
    // 显示加载提示
    $('#loadingToast').show();
    
    $.ajax({
        url: '/remote/' + action,
        method: 'POST',
        success: function(response) {
            // 震动反馈（如果设备支持）
            if (navigator.vibrate) {
                navigator.vibrate(50);
            }
            
            // 隐藏加载提示
            $('#loadingToast').hide();
        },
        error: function(xhr, status, error) {
            // 显示错误提示
            $('#warnToastContent').text('遥控器操作失败: ' + error);
            $('#warnToast').show();
            setTimeout(function() {
                $('#warnToast').hide();
            }, 2000);
            
            // 隐藏加载提示
            $('#loadingToast').hide();
        }
    });
}

// 添加触摸事件支持
$(document).ready(function() {
    // 防止移动端双击缩放
    document.addEventListener('touchstart', function(event) {
        if (event.touches.length > 1) {
            event.preventDefault();
        }
    }, { passive: false });
    
    // 防止滑动干扰
    $('.remote-control-pad').on('touchmove', function(e) {
        e.preventDefault();
    });
    
    // 长按支持
    let pressTimer;
    const longPressDelay = 500; // 长按触发时间（毫秒）
    
    $('.remote-btn').on('touchstart mousedown', function(e) {
        e.preventDefault();
        const button = $(this);
        const action = button.attr('onclick').match(/'([^']+)'/)[1];
        
        // 短按立即触发一次
        remoteControl(action);
        
        // 设置长按定时器
        pressTimer = setInterval(function() {
            remoteControl(action);
        }, 200); // 长按后每200ms触发一次
    });
    
    $('.remote-btn').on('touchend touchcancel mouseup mouseleave', function(e) {
        e.preventDefault();
        clearInterval(pressTimer);
    });
}); 