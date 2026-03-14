package app.organicmaps.navigator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.organicmaps.R;

import java.util.ArrayList;
import java.util.List;

public class NavigatorSettingsFragment extends Fragment
{
  private static final long SCAN_PERIOD_MS = 15_000;

  private BluetoothAdapter mBluetoothAdapter;
  @Nullable private BluetoothLeScanner mScanner;
  private BleDeviceAdapter mAdapter;
  private Button mScanButton;
  private TextView mStatusText;
  private boolean mScanning;
  private final Handler mHandler = new Handler(Looper.getMainLooper());

  private final ActivityResultLauncher<String[]> mPermissionLauncher =
      registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
      {
        boolean allGranted = !result.containsValue(false);
        if (allGranted)
          startScan();
        else
          Toast.makeText(requireContext(), R.string.navigator_permission_denied, Toast.LENGTH_SHORT).show();
      });

  private final ScanCallback mScanCallback = new ScanCallback()
  {
    @Override
    @SuppressLint("MissingPermission")
    public void onScanResult(int callbackType, ScanResult result)
    {
      BluetoothDevice device = result.getDevice();
      String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
      if (name == null)
        name = device.getName();
      mAdapter.addDevice(device, name, result.getRssi());
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onBatchScanResults(List<ScanResult> results)
    {
      for (ScanResult result : results)
      {
        BluetoothDevice device = result.getDevice();
        String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
        if (name == null)
          name = device.getName();
        mAdapter.addDevice(device, name, result.getRssi());
      }
    }

    @Override
    public void onScanFailed(int errorCode)
    {
      mScanning = false;
      updateScanUI();
    }
  };

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState)
  {
    return inflater.inflate(R.layout.fragment_navigator_settings, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);

    BluetoothManager btManager =
        (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
    if (btManager != null)
      mBluetoothAdapter = btManager.getAdapter();

    mStatusText = view.findViewById(R.id.status_text);
    mScanButton = view.findViewById(R.id.scan_button);
    mScanButton.setOnClickListener(v -> onScanButtonClicked());

    RecyclerView recycler = view.findViewById(R.id.devices_list);
    recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    mAdapter = new BleDeviceAdapter();
    mAdapter.setOnDeviceClickListener(this::onDeviceSelected);
    recycler.setAdapter(mAdapter);

    if (mBluetoothAdapter == null ||
        !requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
    {
      mScanButton.setEnabled(false);
      mStatusText.setText(R.string.navigator_ble_not_supported);
    }
  }

  @Override
  public void onDestroyView()
  {
    stopScan();
    super.onDestroyView();
  }

  private void onScanButtonClicked()
  {
    if (mScanning)
      stopScan();
    else
      requestPermissionsAndScan();
  }

  private void requestPermissionsAndScan()
  {
    List<String> needed = new ArrayList<>();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    {
      if (ContextCompat.checkSelfPermission(requireContext(),
          android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
        needed.add(android.Manifest.permission.BLUETOOTH_SCAN);
      if (ContextCompat.checkSelfPermission(requireContext(),
          android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        needed.add(android.Manifest.permission.BLUETOOTH_CONNECT);
    }
    else
    {
      if (ContextCompat.checkSelfPermission(requireContext(),
          android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        needed.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    if (!needed.isEmpty())
      mPermissionLauncher.launch(needed.toArray(new String[0]));
    else
      startScan();
  }

  @SuppressLint("MissingPermission")
  private void startScan()
  {
    if (mBluetoothAdapter == null)
      return;

    if (!mBluetoothAdapter.isEnabled())
    {
      Toast.makeText(requireContext(), R.string.navigator_bt_disabled, Toast.LENGTH_SHORT).show();
      return;
    }

    mScanner = mBluetoothAdapter.getBluetoothLeScanner();
    if (mScanner == null)
      return;

    mAdapter.clear();
    mScanning = true;
    updateScanUI();

    mScanner.startScan(mScanCallback);

    mHandler.postDelayed(this::stopScan, SCAN_PERIOD_MS);
  }

  @SuppressLint("MissingPermission")
  private void stopScan()
  {
    if (!mScanning)
      return;

    mScanning = false;
    mHandler.removeCallbacksAndMessages(null);

    if (mScanner != null)
    {
      try
      {
        mScanner.stopScan(mScanCallback);
      }
      catch (IllegalStateException ignored) {}
    }

    updateScanUI();
  }

  private void updateScanUI()
  {
    if (mScanButton == null)
      return;

    if (mScanning)
    {
      mScanButton.setText(R.string.navigator_stop_scan);
      mStatusText.setText(R.string.navigator_scanning);
    }
    else
    {
      mScanButton.setText(R.string.navigator_scan);
      if (mAdapter.getItemCount() == 0)
        mStatusText.setText(R.string.navigator_no_devices);
      else
        mStatusText.setText("");
    }
  }

  @SuppressLint("MissingPermission")
  private void onDeviceSelected(@NonNull BluetoothDevice device)
  {
    stopScan();
    String name = device.getName();
    if (name == null)
      name = device.getAddress();
    Toast.makeText(requireContext(),
        getString(R.string.navigator_connecting) + " " + name,
        Toast.LENGTH_SHORT).show();
    mAdapter.setConnectedAddress(device.getAddress());
    mStatusText.setText(getString(R.string.navigator_connected) + ": " + name);
  }
}
