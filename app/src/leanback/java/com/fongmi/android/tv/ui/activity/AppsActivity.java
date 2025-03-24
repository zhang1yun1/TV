package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.AppInfo;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityAppsBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.AppsAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

public class AppsActivity extends BaseActivity {

    private ActivityAppsBinding mBinding;

    private AppsAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, AppsActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityAppsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        mAdapter.getApp();
    }

    @Override
    protected void initEvent() {
        mAdapter.setOnItemClickListener(this::openApp);
    }
    private void openApp(AppInfo item) {
        try {
            Log.d("openapp", "openApp: "+item.getName()+",appid:"+item.getPack());
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(item.getPack());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mAdapter = new AppsAdapter());
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn()));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(Product.getColumn(), 16));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Util.hideSystemUI(this);
    }
}
