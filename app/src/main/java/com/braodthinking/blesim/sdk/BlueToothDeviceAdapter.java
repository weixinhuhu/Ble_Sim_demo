package com.braodthinking.blesim.sdk;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Created by ${User} on 2018/9/29
 */
public class BlueToothDeviceAdapter extends BaseAdapter {

    private List<BluetoothDevice> mBluelist;
    private LayoutInflater layoutInflater;

    public BlueToothDeviceAdapter(List<BluetoothDevice> list, Context context) {
        mBluelist = list;
        layoutInflater = LayoutInflater.from(context);//context :要使用当前的Adapter的界面对象 layoutInflater: 布局装载器对象
    }

    @Override
    public int getCount() {
        return mBluelist.size();
    }

    @Override
    public Object getItem(int i) {
        return mBluelist.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        /**
         * 文艺式,避免两个耗时操作,第一个就是创建concerView对象,和findviewbyid
         */
        //2.创建ViewHlder
        ViewHolder viewHolder;
        //3.判断concerView是否为空
        if (view == null) {
            //实例化ViewHolder
            viewHolder = new ViewHolder();
            //View对象还没有被实例化过,缓存池中没有缓存,那么就创建一个convertView对象
            view = layoutInflater.inflate(R.layout.list_item, null);
            //把findviewbyid找的的保存到 viewHolder 中
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            //通过setTag将 viewHolder 和 convertview 进行绑定
            view.setTag(viewHolder);

        } else {
            //可以直接通过 viewHolder 来找到对象所对应的控件
            viewHolder = (ViewHolder) view.getTag();

        }

        BluetoothDevice blueDevice = mBluelist.get(i);
        if (blueDevice!=null) {
            final String deviceName = blueDevice.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(blueDevice.getName());

            } else {
                viewHolder.deviceName.setText("未知设备");

            }
            viewHolder.deviceAddress.setText(blueDevice.getAddress());
        }
        return view;

    }

    //创建内部类ViewHlder
    class ViewHolder {
        /**
         * 避免重复的findviewbyId
         */
        TextView deviceName;
        TextView deviceAddress;
    }
}
