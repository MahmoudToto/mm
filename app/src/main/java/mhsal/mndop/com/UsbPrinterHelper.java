package mhsal.mndop.com;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

public class UsbPrinterHelper {

    private final UsbManager          usbManager;
    private final UsbDevice           device;
    private       UsbDeviceConnection connection;
    private       UsbEndpoint         endpointOut;
    private       UsbInterface        usbInterface;

    private static final int TIMEOUT_MS = 5000;

    public UsbPrinterHelper(UsbManager manager, UsbDevice device) {
        this.usbManager = manager;
        this.device     = device;
    }

    public boolean open() {
        if (device == null) return false;

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            for (int j = 0; j < iface.getEndpointCount(); j++) {
                UsbEndpoint ep = iface.getEndpoint(j);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                        && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    endpointOut = ep;
                    usbInterface = iface;
                    break;
                }
            }
            if (endpointOut != null) break;
        }

        if (endpointOut == null) return false;

        connection = usbManager.openDevice(device);
        if (connection == null) return false;

        connection.claimInterface(usbInterface, true);
        return true;
    }

    public boolean print(byte[] data) {
        if (connection == null || endpointOut == null) return false;

        int offset = 0;
        int packetSize = Math.max(endpointOut.getMaxPacketSize(), 4096);

        while (offset < data.length) {
            int length = Math.min(packetSize, data.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(data, offset, chunk, 0, length);

            int sent = connection.bulkTransfer(endpointOut, chunk, length, TIMEOUT_MS);
            if (sent < 0) return false;
            offset += sent;
        }
        return true;
    }

    public void close() {
        if (connection != null) {
            if (usbInterface != null) connection.releaseInterface(usbInterface);
            connection.close();
            connection = null;
        }
    }
}