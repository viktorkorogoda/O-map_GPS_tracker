package by.vk.o_map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BtDeviceAdapter extends RecyclerView.Adapter<BtDeviceAdapter.ViewHolder> {

    private List<BtDevice> btDevices;

    public BtDeviceAdapter() {}

    public BtDeviceAdapter(List<BtDevice> btDevices) {
        this.btDevices = btDevices;
    }

    public List<BtDevice> getBtDevices() {
        if (btDevices == null) {
            btDevices = new ArrayList<>();
        }
        return btDevices;
    }

    public void setBtDevices(List<BtDevice> btDevices) {
        this.btDevices = btDevices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View contactView = inflater.inflate(R.layout.bt_device_layout, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        BtDevice btDevice = btDevices.get(position);

        TextView textView = holder.nameTextView;
        textView.setText(btDevice.getDevice().getName());
        Button button = holder.messageButton;
        button.setText(btDevice.isConnected() ? "Disconnect" : "Connect");
        button.setEnabled(!btDevice.isConnected());
    }

    @Override
    public int getItemCount() {
        return btDevices.size();
    }

    public void add(BtDevice newDevice) {
        if (newDevice != null) {
            getBtDevices().add(newDevice);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public Button messageButton;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.bt_device_name_text);
            messageButton = itemView.findViewById(R.id.bt_device_action_btn);
        }
    }
}
