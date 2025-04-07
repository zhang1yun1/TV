class RemoteControl {
    constructor() {
        this.devices = [];
        this.socket = null;
        this.selectedDeviceId = null;
        this.accessToken = localStorage.getItem('accessToken');
        this.initElements();
        this.initializeEventListeners();
        this.initSocket();
        this.loadDevices();
    }

    initElements() {
        // 设备管理元素
        this.deviceSelect = document.getElementById('deviceSelect');
        this.addDeviceBtn = document.getElementById('addDeviceBtn');
        this.deleteDeviceBtn = document.getElementById('deleteDeviceBtn');
        this.pairingModal = document.getElementById('pairingModal');
        this.pairingCode = document.getElementById('pairingCode');
        this.pairingStatus = document.getElementById('pairingStatus');
        this.confirmPairingBtn = document.getElementById('confirmPairingBtn');
        this.cancelPairingBtn = document.getElementById('cancelPairingBtn');
        this.deviceName = document.getElementById('deviceName');

        // 搜索相关元素
        this.searchModal = document.getElementById('searchModal');
        this.searchInput = document.getElementById('searchInput');
        this.confirmSearchBtn = document.getElementById('confirmSearchBtn');
        this.cancelSearchBtn = document.getElementById('cancelSearchBtn');
        this.searchBtn = document.querySelector('.search-btn');

        // Toast 元素
        this.toast = document.getElementById('toast');
    }

    initializeEventListeners() {
        // 设备管理事件
        this.deviceSelect.addEventListener('change', () => {
            this.selectedDeviceId = this.deviceSelect.value;
            if (this.selectedDeviceId) {
                localStorage.setItem('selectedDeviceId', this.selectedDeviceId);
                this.connectToDevice();
            }
            // 更新删除按钮状态
            this.updateDeleteButtonState();
        });

        this.addDeviceBtn.addEventListener('click', () => this.showPairingModal());
        this.deleteDeviceBtn.addEventListener('click', () => this.deleteSelectedDevice());
        this.confirmPairingBtn.addEventListener('click', () => this.startPairing());
        this.cancelPairingBtn.addEventListener('click', () => this.hidePairingModal());
        this.pairingCode.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 6);
        });

        // 遥控器按钮事件
        document.querySelectorAll('.remote-btn').forEach(button => {
            button.addEventListener('click', (e) => {
                const action = e.target.closest('.remote-btn').dataset.action;
                if (action) {
                    this.sendCommand(action,"");
                    this.addClickEffect(e.target.closest('.remote-btn'));
                }
            });
        });

        // 键盘事件
        document.addEventListener('keydown', (e) => {
            const keyAction = this.getKeyAction(e.key);
            if (keyAction) {
                e.preventDefault();
                this.sendCommand(keyAction,"");
                const button = document.querySelector(`[data-action="${keyAction}"]`);
                if (button) {
                    this.addClickEffect(button);
                }
            }
        });

        // 搜索按钮点击事件
        this.searchBtn.addEventListener('click', () => {
            this.searchModal.style.display = 'block';
            this.searchInput.value = '';
            this.searchInput.focus();
        });

        // 确认搜索按钮点击事件
        this.confirmSearchBtn.addEventListener('click', () => {
            const searchText = this.searchInput.value.trim();
            if (searchText) {
                this.sendCommand('search', searchText);
                this.searchModal.style.display = 'none';
            }
        });

        // 取消搜索按钮点击事件
        this.cancelSearchBtn.addEventListener('click', () => {
            this.searchModal.style.display = 'none';
        });

        // 搜索输入框回车事件
        this.searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                const searchText = this.searchInput.value.trim();
                if (searchText) {
                    this.sendCommand('search', searchText);
                    this.searchModal.style.display = 'none';
                }
            }
        });

        // 点击模态框外部关闭
        window.addEventListener('click', (e) => {
            if (e.target === this.searchModal) {
                this.searchModal.style.display = 'none';
            }
        });
    }

    initSocket() {
        const serverUrl = window.location.origin;
        console.log('Initializing socket connection to:', serverUrl);
        
        this.socket = io(serverUrl, {
            reconnection: true,
            reconnectionAttempts: 5,
            reconnectionDelay: 1000,
            timeout: 10000,
            transports: ['websocket', 'polling'],
            auth: this.accessToken ? { token: this.accessToken } : undefined
        });
        console.log('Socket configuration:', {
            reconnection: true,
            reconnectionAttempts: 5,
            reconnectionDelay: 1000,
            timeout: 10000,
            hasToken: !!this.accessToken
        });

        this.socket.on('connect', () => {
            console.log('Socket connected, socket id:', this.socket.id);
            if (this.accessToken) {
                console.log('Authenticating with stored token');
                this.socket.emit('auth', { token: this.accessToken });
            }
            this.connectToDevice();
        });

        this.socket.on('connect_error', (error) => {
            console.error('Socket connection error:', error);
            console.log('Connection details:', {
                url: serverUrl,
                transport: this.socket.io.engine.transport.name,
                connected: this.socket.connected,
                id: this.socket.id
            });
        });

        this.socket.on('disconnect', (reason) => {
            console.log('Socket disconnected, reason:', reason);
            this.devices.forEach(device => {
                device.online = false;
            });
            this.updateDeviceList();
        });

        this.socket.on('reconnect', (attemptNumber) => {
            console.log('Socket reconnected after', attemptNumber, 'attempts');
            this.connectToDevice();
        });

        this.socket.on('deviceStatus', (data) => {
            console.log('Received device status update:', data);
            this.updateDeviceStatus(data);
        });

        this.socket.on('pairingResult', (data) => {
            console.log('Received pairing result:', data);
            this.handlePairingResult(data);
        });

        this.socket.on('auth_success', (data) => {
            console.log('Authentication successful:', data);
        });

        this.socket.on('auth_error', (data) => {
            console.error('Authentication failed:', data);
            localStorage.removeItem('accessToken');
            this.accessToken = null;
        });

        // 配对成功处理
        this.socket.on('pairing_success', (data) => {
            this.updatePairingStatus('配对成功！', 'success');
            this.addDeviceToList(data.deviceId, data.deviceName);
            setTimeout(() => {
                this.pairingModal.style.display = 'none';
                this.pairingCode.value = '';
                this.deviceName.value = '';
            }, 1500);
        });
    }

    loadDevices() {
        const savedDevices = localStorage.getItem('devices');
        if (savedDevices) {
            this.devices = JSON.parse(savedDevices);
            this.updateDeviceList();
        }

        const savedDeviceId = localStorage.getItem('selectedDeviceId');
        if (savedDeviceId) {
            this.deviceSelect.value = savedDeviceId;
            this.selectedDeviceId = savedDeviceId;
            this.connectToDevice();
        }
        
        // 初始化删除按钮状态
        this.updateDeleteButtonState();
    }

    updateDeviceList() {
        console.log('Updating device list, current devices:', this.devices);
        
        while (this.deviceSelect.options.length > 1) {
            this.deviceSelect.remove(1);
        }

        this.devices.forEach(device => {
            if (device.online) {
                console.log('Adding online device to select:', device);
                const option = document.createElement('option');
                option.value = device.id;
                option.text = device.name;
                this.deviceSelect.add(option);
            }
        });

        if (this.selectedDeviceId && !this.devices.find(d => d.id === this.selectedDeviceId && d.online)) {
            console.log('Selected device no longer available:', this.selectedDeviceId);
            this.deviceSelect.value = '';
            this.selectedDeviceId = null;
            localStorage.removeItem('selectedDeviceId');
        }
    }

    updateDeviceStatus(data) {
        const device = this.devices.find(d => d.id === data.id);
        if (device) {
            device.online = data.online;
            this.updateDeviceList();
            localStorage.setItem('devices', JSON.stringify(this.devices));
        }
    }

    connectToDevice() {
        if (this.selectedDeviceId && this.socket) {
            console.log('Connecting to device:', this.selectedDeviceId);
            this.socket.emit('selectDevice', { deviceId: this.selectedDeviceId });
        } else {
            console.log('Cannot connect to device:', {
                selectedDeviceId: this.selectedDeviceId,
                socketConnected: this.socket?.connected
            });
        }
    }

    showPairingModal() {
        this.pairingModal.style.display = 'block';
        this.pairingCode.value = '';
        this.pairingStatus.textContent = '';
        this.pairingCode.focus();
    }

    hidePairingModal() {
        this.pairingModal.style.display = 'none';
    }

    startPairing() {
        const code = this.pairingCode.value;
        const deviceName = this.deviceName.value;
        if (code.length !== 6) {
            console.warn('Invalid pairing code length:', code.length);
            this.pairingStatus.textContent = '请输入6位配对码';
            return;
        }

        console.log('Starting pairing process with code:', code);
        this.pairingStatus.textContent = '配对中...';
        
        if (!this.socket || !this.socket.connected) {
            console.error('Socket not connected, cannot start pairing');
            this.pairingStatus.textContent = '网络连接失败';
            return;
        }

        console.log('Emitting pair event with code:', code);
        this.socket.emit('pair', { code,deviceName });
    }

    handlePairingResult(data) {
        console.log('Processing pairing result:', data);
        
        if (data.success) {
            console.log('Pairing successful, device:', data.device);
            this.pairingStatus.textContent = '配对成功';
            
            const newDevice = {
                id: data.device.id,
                name: data.device.name,
                online: true
            };
            console.log('Creating new device object:', newDevice);

            // 保存访问令牌
            if (data.accessToken) {
                console.log('Received access token, storing it');
                this.accessToken = data.accessToken;
                localStorage.setItem('accessToken', data.accessToken);
                this.socket.emit('auth', { token: data.accessToken });
            }

            this.devices.push(newDevice);
            localStorage.setItem('devices', JSON.stringify(this.devices));
            console.log('Updated devices list:', this.devices);
            
            this.updateDeviceList();

            this.deviceSelect.value = newDevice.id;
            this.selectedDeviceId = newDevice.id;
            localStorage.setItem('selectedDeviceId', newDevice.id);
            console.log('Selected new device:', newDevice.id);
            
            this.connectToDevice();

            setTimeout(() => {
                this.hidePairingModal();
            }, 1500);
        } else {
            console.warn('Pairing failed:', data.message);
            this.pairingStatus.textContent = data.message || '配对失败';
        }
    }

    getKeyAction(key) {
        const keyMap = {
            'ArrowUp': 'dpad/up',
            'ArrowDown': 'dpad/down',
            'ArrowLeft': 'dpad/left',
            'ArrowRight': 'dpad/right',
            'Enter': 'dpad/center',
            'Escape': 'back',
            'Home': 'home',
            'ContextMenu': 'menu',
            'PageUp': 'volume/up',
            'PageDown': 'volume/down'
        };
        return keyMap[key];
    }

    showToast(message, type = 'info', duration = 2000) {
        if (!this.toast) return;
        
        // 清除之前的定时器
        if (this.toastTimer) {
            clearTimeout(this.toastTimer);
            this.toast.classList.remove('show', 'error', 'success', 'info');
        }

        // 设置消息和类型
        this.toast.textContent = message;
        this.toast.classList.add('show', type);

        // 设置定时器自动隐藏
        this.toastTimer = setTimeout(() => {
            this.toast.classList.remove('show', type);
        }, duration);
    }

    sendCommand(action,data) {
        if (!this.socket || !this.socket.connected) {
            this.showToast('网络未连接，请检查网络', 'error');
            console.error('Socket connection not available');
            return;
        }
        
        if (!this.selectedDeviceId) {
            this.showToast('请先选择一个设备', 'error');
            console.error('No device selected');
            return;
        }

        const command = {
            action: action,
            deviceId: this.selectedDeviceId,
            timestamp: Date.now(),
            data:data
        };
        console.log('Sending command:', command);
        this.socket.emit('remote_command', command);
    }

    addClickEffect(button) {
        button.classList.add('clicked');
        setTimeout(() => {
            button.classList.remove('clicked');
        }, 200);
    }

    updatePairingStatus(message, type) {
        this.pairingStatus.textContent = message;
        this.pairingStatus.className = `pairing-status ${type}`;
    }

    addDeviceToList(deviceId, deviceName) {
        const newDevice = {
            id: deviceId,
            name: deviceName,
            online: true
        };
        this.devices.push(newDevice);
        localStorage.setItem('devices', JSON.stringify(this.devices));
        this.updateDeviceList();
        this.deviceSelect.value = deviceId;
        this.selectedDeviceId = deviceId;
        localStorage.setItem('selectedDeviceId', deviceId);
        this.connectToDevice();
    }

    updateDeleteButtonState() {
        if (this.selectedDeviceId) {
            this.deleteDeviceBtn.removeAttribute('disabled');
        } else {
            this.deleteDeviceBtn.setAttribute('disabled', 'disabled');
        }
    }

    deleteSelectedDevice() {
        if (!this.selectedDeviceId) {
            this.showToast('请先选择要删除的设备', 'error');
            return;
        }

        if (confirm('确定要删除该设备吗？')) {
            const deviceIndex = this.devices.findIndex(d => d.id === this.selectedDeviceId);
            if (deviceIndex !== -1) {
                this.devices.splice(deviceIndex, 1);
                localStorage.setItem('devices', JSON.stringify(this.devices));
                localStorage.removeItem('selectedDeviceId');
                this.selectedDeviceId = null;
                this.updateDeviceList();
                this.updateDeleteButtonState();
                this.showToast('设备已删除', 'success');
            }
        }
    }
}

// 初始化遥控器
document.addEventListener('DOMContentLoaded', () => {
    window.remoteControl = new RemoteControl();
});