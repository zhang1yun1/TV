package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.android.cast.dlna.dmr.DLNARendererService;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Button;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivityHome2Binding;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomTitleView;
import com.fongmi.android.tv.ui.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.dialog.MenuDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.fragment.HomeFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.ui.presenter.TypePresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Tbs;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.Trans;
import com.permissionx.guolindev.PermissionX;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Home2Activity extends BaseActivity implements CustomTitleView.Listener, TypePresenter.OnClickListener, ConfigCallback {

    public ActivityHome2Binding mBinding;
    private ArrayObjectAdapter mAdapter;
    private Home2Activity.PageAdapter mPageAdapter;
    private SiteViewModel mViewModel;
    public Result mResult;
    private boolean loading;
    private boolean coolDown;
    private View mOldView;
    private boolean confirm;
    private Clock mClock;
    private View mFocus;
    private boolean hasCursorMoved;
    private Runnable mAutoPlayRunnable;
    private static final int AUTO_PLAY_DELAY = 5000; // 5秒后自动播放

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHome2Binding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView() {
        DLNARendererService.Companion.start(this, R.drawable.ic_logo);
        mClock = Clock.create(mBinding.clock).format("MM/dd HH:mm:ss");
        //Updater.get().release().start(this);
        Server.get().start();
        //Tbs.init();
        setTitleView();
        setRecyclerView();
        setViewModel();
        setHomeType();
        setPager();
        initConfig();
    }

    @Override
    protected void initEvent() {
        mBinding.title.setListener(this);
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.setSelectedPosition(position);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onChildSelected(child);
            }
        });
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
                loadLive("file:/" + FileChooser.getPathFromUri(this, intent.getData()));
            } else {
                VideoActivity.push(this, intent.getData().toString());
            }
        }
    }

    private void setTitleView() {
        mBinding.homeSiteLock.setVisibility(Setting.isHomeSiteLock() ? View.VISIBLE : View.GONE);
        if (Setting.getHomeUI() == 0) {
            mBinding.title.setTextSize(24);
            mBinding.clock.setTextSize(24);
        } else {
            mBinding.title.setTextSize(20);
            mBinding.clock.setTextSize(20);
        }
    }

    private void setRecyclerView() {
        setHomeUI();
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(new TypePresenter(this))));
    }

    private void setHomeUI() {
        if (Setting.getHomeUI() == 0) mBinding.recycler.setVisibility(View.GONE);
        else mBinding.recycler.setVisibility(View.VISIBLE);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, result -> {
            setTypes(mResult = result);
        });
    }

    private List<Class> getTypes(Result result) {
        List<Class> items = new ArrayList<>();
        for (String cate : getHome().getCategories()) for (Class item : result.getTypes()) if (Trans.s2t(cate).equals(item.getTypeName())) items.add(item);
        return items;
    }

    private String getKey() {
        return getHome().getKey();
    }

    private List<Filter> getFilter(String typeId) {
        return Filter.arrayFrom(Prefers.getString("filter_" + getKey() + "_" + typeId));
    }

    private void setHomeType() {
        Class home = new Class();
        home.setTypeId("home");
        home.setTypeName(ResUtil.getString(R.string.home));
        mAdapter.add(home);
    }

    public void homeContent() {
        mResult = Result.empty();
        String title = getHome().getName();
        mBinding.title.setText(title.isEmpty() ? ResUtil.getString(R.string.app_name) : title);
        if (getHome().getKey().isEmpty()) return;
        mFocus = getCurrentFocus();
        getHomeFragment().mBinding.progressLayout.showProgress();
        mViewModel.homeContent();
    }

    public void setTypes(Result result) {
        result.setTypes(getTypes(result));
        for (Map.Entry<String, List<Filter>> entry : result.getFilters().entrySet()) Prefers.put("filter_" + getKey() + "_" + entry.getKey(), App.gson().toJson(entry.getValue()));
        for (Class item : result.getTypes()) item.setFilters(getFilter(item.getTypeId()));
        if (mAdapter.size() > 1) mAdapter.removeItems(1, mAdapter.size() - 1);
        if (result.getTypes().size() > 0) mAdapter.addAll(1, result.getTypes());
        setPager();
        mPageAdapter.notifyDataSetChanged();
        getHomeFragment().addVideo(result);
        getHomeFragment().mBinding.progressLayout.showContent();
        App.post(() -> setFocus(), 200);
    }

    private void setPager() {
        mBinding.pager.setAdapter(mPageAdapter = new Home2Activity.PageAdapter(getSupportFragmentManager()));
        mBinding.pager.setNoScrollItem(0);
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        if (mOldView != null) mOldView.setActivated(false);
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setActivated(true);
        App.post(mRunnable, 100);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            int position = mBinding.recycler.getSelectedPosition();
            mBinding.pager.setCurrentItem(position);
            if (position == 0) showToolBar();
            else hideToolBar();
        }
    };

    private void updateFilter(Class item) {
        if (item.getFilter() == null) return;
        getFragment().toggleFilter(item.toggleFilter());
        mAdapter.notifyArrayItemRangeChanged(1, mAdapter.size() - 1);
    }

    public void hideToolBar() {
        mBinding.toolbar.setVisibility(View.GONE);
        if (mBinding.recycler.getVisibility() == View.VISIBLE) mBinding.blank.setVisibility(View.VISIBLE);
        else mBinding.blank.setVisibility(View.GONE);
    }

    public void showToolBar() {
        mBinding.toolbar.setVisibility(View.VISIBLE);
        mBinding.blank.setVisibility(View.GONE);
    }

    private HomeFragment getHomeFragment() {
        return (HomeFragment) mPageAdapter.instantiateItem(mBinding.pager, 0);
    }

    private VodFragment getFragment() {
        return (VodFragment) mPageAdapter.instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    private void setCoolDown() {
        App.post(() -> coolDown = false, 2000);
        coolDown = true;
    }

    private boolean hasSettingButton() {
        return Setting.getHomeButtons(Button.getDefaultButtons()).contains("6");
    }

    @Override
    public void onItemClick(Class item) {
        if (mBinding.pager.getCurrentItem() == 0) {
            SiteDialog.create(this).show();
        } else {
            updateFilter(item);
        }
    }

    @Override
    public void onRefresh(Class item) {
        if (mBinding.pager.getCurrentItem() == 0) mBinding.title.requestFocus();
        else getFragment().onRefresh();
    }

    @Override
    public void setConfig(Config config) {
        setConfig(config, "");
    }

    private void setConfig(Config config, String success) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config, success));
        } else {
            load(config, success);
        }
    }

    public void initConfig() {
        if (isLoading()) return;
        WallConfig.get().init();
        LiveConfig.get().init().load(getLiveCallback(""));
        VodConfig.get().init().load(getCallback(""), true);
        setLoading(true);
    }
    private Callback getLiveCallback(String success) {
        return new Callback() {
            @Override
            public void success(String result) {
               Log.d("getLiveCallback", "getLiveCallback:success,result:"+result);
            }

            @Override
            public void success() {
                if(Setting.isGoLive()){
                    Log.d("isGoLive", "isGoLive:true,isEmpty:"+LiveConfig.isEmpty());
                    LiveActivity.start(getActivity());
                }
            }

            @Override
            public void error(String msg) {

            }
        };
    }
    private Callback getCallback(String success) {
        return new Callback() {
            @Override
            public void success(String result) {
                Notify.show(result);
            }

            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.video();
                setLogo();
                if (!TextUtils.isEmpty(success)) Notify.show(success);
            }

            @Override
            public void error(String msg) {
                if (getHomeFragment().inited) getHomeFragment().mBinding.progressLayout.showContent();
                else App.post(() -> getHomeFragment().mBinding.progressLayout.showContent(), 1000);
                mResult = Result.empty();
                Notify.show(msg);
                setLoading(false);
            }
        };
    }

    private void load(Config config, String success) {
        switch (config.getType()) {
            case 0:
                getHomeFragment().mBinding.progressLayout.showProgress();
                VodConfig.load(config, getCallback(success));
                break;
        }
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 5000);
    }


    @Override
    public void showDialog() {
        if (!hasSettingButton()) {
            MenuDialog.create(this).show();
            return;
        }
        if (Setting.isHomeSiteLock()) return;
        SiteDialog.create(this).show();
    }

    @Override
    public void onRefresh() {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                Config config = VodConfig.get().getConfig().json("").save();
                if (!config.isEmpty()) setConfig(config, ResUtil.getString(R.string.config_refreshed));
            }
        });
    }

    @Override
    public boolean onItemLongClick(Class item) {
        if (mBinding.pager.getCurrentItem() != 0) return true;
        onRefresh();
        return true;
    }


    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        homeContent();
    }

    @Override
    public void onChanged() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        switch (event.getType()) {
            case CONFIG:
                setLogo();
                break;
            case VIDEO:
                homeContent();
                break;
            case IMAGE:
                getHomeFragment().refreshRecommond();
                break;
            case HISTORY:
                getHomeFragment().getHistory();
                break;
            case SIZE:
                homeContent();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.getType()) {
            case SEARCH:
                CollectActivity.start(this, event.getText());
                break;
            case PUSH:
                VideoActivity.push(this, event.getText());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.getConfig())) {
            VideoActivity.cast(this, event.getHistory().update(VodConfig.getCid()));
        } else {
            VodConfig.load(event.getConfig(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                RefreshEvent.history();
                RefreshEvent.config();
                RefreshEvent.video();
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    private void setLogo() {
        Glide.with(App.get()).load(UrlUtil.convert(VodConfig.get().getConfig().getLogo())).circleCrop().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).listener(getListener()).into(mBinding.logo);
    }

    private RequestListener<Drawable> getListener() {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                mBinding.logo.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                mBinding.logo.setVisibility(View.VISIBLE);
                return false;
            }
        };
    }

    private void setFocus() {
        setLoading(false);
        if (!mBinding.title.isFocusable()) App.post(() -> mBinding.title.setFocusable(true), 500);
        if (mFocus != mBinding.title) {
            HomeFragment homeFragment = getHomeFragment();
            if (homeFragment == null || !homeFragment.inited) {
                if (Setting.getHomeUI() == 0) mBinding.pager.requestFocus();
                else mBinding.recycler.requestFocus();
                return;
            }
            
            // 检查历史记录
            List<History> histories = History.get();
            int historyIndex = homeFragment.getHistoryIndex();
            if (historyIndex != -1 && histories.size() > 0) {
                // 如果有历史记录，先滚动到历史记录部分
                homeFragment.mBinding.recycler.scrollToPosition(historyIndex);
                
                // 等待滚动完成后聚焦到历史记录项
                App.post(() -> {
                    // 尝试找到历史记录行的ViewHolder
                    RecyclerView.ViewHolder holder = homeFragment.mBinding.recycler.findViewHolderForAdapterPosition(historyIndex);
                    if (holder != null) {
                        // 找到历史记录行，请求焦点
                        holder.itemView.requestFocus();
                        
                        // 延迟之后尝试进一步获得历史记录列表的第一个项目的焦点
                        App.post(() -> {
                            try {
                                // 使用tag找到历史记录行内的RecyclerView（水平列表）
                                View historyRow = holder.itemView;
                                if (historyRow instanceof ViewGroup) {
                                    ViewGroup viewGroup = (ViewGroup) historyRow;
                                    // 找到内部的RecyclerView
                                    for (int i = 0; i < viewGroup.getChildCount(); i++) {
                                        View child = viewGroup.getChildAt(i);
                                        if (child instanceof RecyclerView) {
                                            RecyclerView historyList = (RecyclerView) child;
                                            if (historyList.getChildCount() > 0) {
                                                // 聚焦到第一个历史记录
                                                historyList.getChildAt(0).requestFocus();
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略异常，已经聚焦到历史记录行
                            }
                        }, 200);
                    } else {
                        // 回退到默认焦点行为
                        if (Setting.getHomeUI() == 0) homeFragment.mBinding.recycler.requestFocus();
                        else mBinding.recycler.requestFocus();
                    }
                }, 100);
                
                // 启动自动播放计时器
                if (Setting.isAutoPlayHistory()) {
                    startAutoPlayTimer(histories.get(0));
                }
            } else {
                // 没有历史记录，使用默认行为
                if (Setting.getHomeUI() == 0) homeFragment.mBinding.recycler.requestFocus();
                else mBinding.recycler.requestFocus();
            }
        }
    }

    /**
     * 启动自动播放计时器
     * @param history 要播放的历史记录
     */
    private void startAutoPlayTimer(History history) {
        // 取消之前的计时器
        cancelAutoPlayTimer();
        
        // 重置光标移动标志
        hasCursorMoved = false;
        
        // 创建新的自动播放任务
        mAutoPlayRunnable = () -> {
            if (!hasCursorMoved && !isFinishing() && history != null) {
                // 如果光标未移动且Activity未销毁，则自动播放第一个历史记录
                VideoActivity.start(this, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic());
            }
        };
        
        // 延迟执行
        App.post(mAutoPlayRunnable, AUTO_PLAY_DELAY);
    }
    
    /**
     * 取消自动播放计时器
     */
    private void cancelAutoPlayTimer() {
        if (mAutoPlayRunnable != null) {
            App.removeCallbacks(mAutoPlayRunnable);
            mAutoPlayRunnable = null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // 任何按键操作都表示用户已移动光标
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            hasCursorMoved = true;
            cancelAutoPlayTimer();
        }
        
        boolean isHomeFragment = mBinding.pager.getCurrentItem() == 0;
        if (isHomeFragment && KeyUtil.isMenuKey(event)) {
            if (Setting.getHomeMenuKey() == 0) MenuDialog.create(this).show();
            else if (Setting.getHomeMenuKey() == 1) SiteDialog.create(this).show();
            else if (Setting.getHomeMenuKey() == 2) HistoryDialog.create(this).type(0).show();
            else if (Setting.getHomeMenuKey() == 3) LiveActivity.start(this);
            else if (Setting.getHomeMenuKey() == 4) HistoryActivity.start(this);
            else if (Setting.getHomeMenuKey() == 5) SearchActivity.start(this);
            else if (Setting.getHomeMenuKey() == 6) PushActivity.start(this);
            else if (Setting.getHomeMenuKey() == 7) KeepActivity.start(this);
            else if (Setting.getHomeMenuKey() == 8) SettingActivity.start(this);
        }
        if (!isHomeFragment && KeyUtil.isMenuKey(event)) updateFilter((Class) mAdapter.get(mBinding.pager.getCurrentItem()));
        if (!isHomeFragment && KeyUtil.isBackKey(event) && event.isLongPress() && getFragment().goRoot()) setCoolDown();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        setTitleView();
        setHomeUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
        cancelAutoPlayTimer();
    }

    @Override
    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        if (isVisible(mBinding.recycler) && mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else if (mPageAdapter != null && getHomeFragment().inited && getHomeFragment().mBinding.progressLayout.isProgress()) {
            getHomeFragment().mBinding.progressLayout.showContent();
        } else if (mPageAdapter != null && getHomeFragment().inited && getHomeFragment().mPresenter != null && getHomeFragment().mPresenter.isDelete()) {
            getHomeFragment().setHistoryDelete(false);
        } else if (getHomeFragment().canBack()) {
            getHomeFragment().goBack();
        } else if (!confirm) {
            setConfirm();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        boolean isHomeFragment = mBinding.pager.getCurrentItem() == 0;
        if (isHomeFragment) {
            super.onBackPressed();
            return;
        }
        Class item = (Class) mAdapter.get(mBinding.pager.getCurrentItem());
        if (item.getFilter() != null && item.getFilter()) updateFilter(item);
        else if (getFragment().canBack()) getFragment().goBack();
        else if (!coolDown) super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAutoPlayTimer();
        WallConfig.get().clear();
        LiveConfig.get().clear();
        VodConfig.get().clear();
        AppDatabase.backup();
        Server.get().stop();
        Source.get().exit();
    }

    class PageAdapter extends FragmentStatePagerAdapter {
        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) return new HomeFragment();
            Class type = (Class) mAdapter.get(position);
            return VodFragment.newInstance(getHome().getKey(), type.getTypeId(), type.getStyle(), type.getExtend(false), "1".equals(type.getTypeFlag()));
        }

        @Override
        public int getCount() {
            return mAdapter.size();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
