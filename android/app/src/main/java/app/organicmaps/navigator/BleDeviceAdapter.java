package app.organicmaps.navigator;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import app.organicmaps.R;

import java.util.ArrayList;

public class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>
{
  public interface OnDeviceClickListener
  {
    void onDeviceClick(@NonNull BluetoothDevice device);
  }

  static class DeviceEntry
  {
    @NonNull final BluetoothDevice device;
    @Nullable final String name;
    final int rssi;

    DeviceEntry(@NonNull BluetoothDevice device, @Nullable String name, int rssi)
    {
      this.device = device;
      this.name = name;
      this.rssi = rssi;
    }
  }

  private final ArrayList<DeviceEntry> mDevices = new ArrayList<>();
  @Nullable private OnDeviceClickListener mListener;
  @Nullable private String mConnectedAddress;

  public void setOnDeviceClickListener(@Nullable OnDeviceClickListener listener)
  {
    mListener = listener;
  }

  public void setConnectedAddress(@Nullable String address)
  {
    mConnectedAddress = address;
    notifyDataSetChanged();
  }

  public void addDevice(@NonNull BluetoothDevice device, @Nullable String name, int rssi)
  {
    for (int i = 0; i < mDevices.size(); i++)
    {
      if (mDevices.get(i).device.getAddress().equals(device.getAddress()))
      {
        mDevices.set(i, new DeviceEntry(device, name, rssi));
        notifyItemChanged(i);
        return;
      }
    }
    mDevices.add(new DeviceEntry(device, name, rssi));
    notifyItemInserted(mDevices.size() - 1);
  }

  public void clear()
  {
    int size = mDevices.size();
    mDevices.clear();
    notifyItemRangeRemoved(0, size);
  }

  @Override
  @NonNull
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
  {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_ble_device, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position)
  {
    DeviceEntry entry = mDevices.get(position);
    String displayName = entry.name != null ? entry.name : "Unknown";
    holder.mName.setText(displayName);
    holder.mAddress.setText(entry.device.getAddress());

    boolean isConnected = entry.device.getAddress().equals(mConnectedAddress);
    if (isConnected)
    {
      holder.mStatus.setText(R.string.navigator_connected);
      holder.mStatus.setVisibility(View.VISIBLE);
    }
    else
    {
      holder.mStatus.setVisibility(View.GONE);
    }

    holder.itemView.setOnClickListener(v ->
    {
      if (mListener != null)
        mListener.onDeviceClick(entry.device);
    });
  }

  @Override
  public int getItemCount()
  {
    return mDevices.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder
  {
    final ImageView mIcon;
    final TextView mName;
    final TextView mAddress;
    final TextView mStatus;

    ViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mIcon = itemView.findViewById(R.id.device_icon);
      mName = itemView.findViewById(R.id.device_name);
      mAddress = itemView.findViewById(R.id.device_address);
      mStatus = itemView.findViewById(R.id.device_status);
    }
  }
}
