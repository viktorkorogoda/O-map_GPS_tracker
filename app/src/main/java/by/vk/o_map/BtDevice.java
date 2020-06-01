package by.vk.o_map;

import android.bluetooth.BluetoothDevice;

public class BtDevice {
    private BluetoothDevice device;
    private boolean connected;

    public BtDevice(BluetoothDevice device, boolean connected) {
        this.device = device;
        this.connected = connected;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
