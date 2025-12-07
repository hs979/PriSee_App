package com.example.demo_323;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class AppListAdapter extends BaseAdapter {
    private Context context;
    private List<AppInfo> appList;
    private LayoutInflater inflater;
    private OnAppSelectedListener onAppSelectedListener;
    
    public AppListAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        this.inflater = LayoutInflater.from(context);
    }
    
    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo, boolean isSelected);
    }
    
    public void setOnAppSelectedListener(OnAppSelectedListener listener) {
        this.onAppSelectedListener = listener;
    }
    
    @Override
    public int getCount() {
        return appList.size();
    }
    
    @Override
    public Object getItem(int position) {
        return appList.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.appIcon = convertView.findViewById(R.id.app_icon);
            holder.appName = convertView.findViewById(R.id.app_name);
            holder.appPackage = convertView.findViewById(R.id.app_package);
            holder.supportedIndicator = convertView.findViewById(R.id.supported_indicator);
            holder.checkBox = convertView.findViewById(R.id.app_checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo appInfo = (AppInfo) getItem(position);
        holder.appName.setText(appInfo.appName);
        holder.appPackage.setText(appInfo.packageName);
        
        // 设置应用图标
        if (appInfo.appIcon != null) {
            holder.appIcon.setImageDrawable(appInfo.appIcon);
        } else {
            holder.appIcon.setImageResource(R.mipmap.ic_launcher);
        }

        // 显示支持状态指示器
        holder.supportedIndicator.setVisibility(appInfo.isSupported ? View.VISIBLE : View.GONE);

        // 设置复选框状态
        holder.checkBox.setChecked(appInfo.isSelected);
        // 禁用不支持的应用的复选框
        holder.checkBox.setEnabled(appInfo.isSupported);

        // 设置复选框监听器
        holder.checkBox.setOnCheckedChangeListener(null); // 先移除监听器避免冲突
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appInfo.isSelected = isChecked;
            if (onAppSelectedListener != null) {
                onAppSelectedListener.onAppSelected(appInfo, isChecked);
            }
        });

        return convertView;
    }
    
    static class ViewHolder {
        ShapeableImageView appIcon;
        TextView appName;
        TextView appPackage;
        TextView supportedIndicator;
        MaterialCheckBox checkBox;
    }
}