package com.maning.gankmm.fragment;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.maning.gankmm.R;
import com.maning.gankmm.adapter.RecyclePicAdapter;
import com.maning.gankmm.app.MyApplication;
import com.maning.gankmm.bean.GankEntity;
import com.maning.gankmm.constant.Constants;
import com.maning.gankmm.db.PublicDao;
import com.maning.gankmm.utils.IntentUtils;
import com.maning.gankmm.base.BaseFragment;
import com.maning.gankmm.callback.MyCallBack;
import com.maning.gankmm.http.GankApi;
import com.maning.gankmm.utils.MyToast;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * 福利Fragment
 */
public class WelFareFragment extends BaseFragment implements OnRefreshListener, OnLoadMoreListener {

    @Bind(R.id.swipe_target)
    RecyclerView swipeTarget;
    @Bind(R.id.swipeToLoadLayout)
    SwipeToLoadLayout swipeToLoadLayout;

    private MyCallBack httpCallBack = new MyCallBack() {
        @Override
        public void onSuccessList(int what, List results) {
            if (results == null) {
                overRefresh();
                dissmissProgressDialog();
                return;
            }
            switch (what) {
                case 0x001:
                    dissmissProgressDialog();
                    if (commonDataResults == null) {
                        commonDataResults = new ArrayList<>();
                    }
                    if (pageIndex == 1 && commonDataResults.size() > 0) {
                        commonDataResults.clear();
                    }
                    List<GankEntity> gankEntityList = results;
                    //过滤一下数据,筛除重的
                    if (commonDataResults != null && commonDataResults.size() > 0) {
                        for (int i = 0; i < results.size(); i++) {
                            GankEntity resultEntity2 = (GankEntity) results.get(i);
                            for (int j = 0; j < commonDataResults.size(); j++) {
                                GankEntity resultsEntity1 = commonDataResults.get(j);
                                if (resultEntity2.get_id().equals(resultsEntity1.get_id())) {
                                    //删除
                                    gankEntityList.remove(i);
                                }
                            }
                        }
                    }
                    commonDataResults.addAll(gankEntityList);
                    initRecycleView();
                    if (commonDataResults == null || commonDataResults.size() == 0 || commonDataResults.size() < pageIndex * pageSize) {
                        swipeToLoadLayout.setLoadMoreEnabled(false);
                    } else {
                        swipeToLoadLayout.setLoadMoreEnabled(true);
                    }
                    pageIndex++;
                    //数据重复了，再请求一遍
                    if (pageIndex == 2 && commonDataResults.size() == 20) {
                        onLoadMore();
                    }else{
                        overRefresh();
                    }
                    break;
                case 0x002: //下拉刷新
                    pageIndex = 1;
                    pageIndex++;
                    commonDataResults = results;
                    if (commonDataResults.size() > 0) {
                        //把网络数据保存到数据库中去
                        saveToDB(commonDataResults);
                        initRecycleView();
                    }
                    overRefresh();
                    break;
            }
        }

        @Override
        public void onSuccess(int what, Object result) {

        }

        @Override
        public void onFail(int what, String result) {
            dissmissProgressDialog();
            overRefresh();
            if (!TextUtils.isEmpty(result)) {
                MyToast.showShortToast(result);
            }
            if (what == 0x001 && pageIndex == 1) {
                getDBDatas();
            }
        }
    };

    /**
     * 保存到数据库
     *
     * @param results
     */
    private void saveToDB(final List<GankEntity> results) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new PublicDao().insertList(results, Constants.FlagWelFare);
            }
        }).start();
    }

    private List<GankEntity> commonDataResults;
    private RecyclePicAdapter recyclePicAdapter;
    private int pageSize = 20;
    private int pageIndex = 1;

    public static WelFareFragment newInstance() {
        return new WelFareFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wel_fare, container, false);
        ButterKnife.bind(this, view);

        initRefresh();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getDBDatas();

    }

    private void getDBDatas() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                commonDataResults = new PublicDao().queryAllCollectByType(Constants.FlagWelFare);
                MyApplication.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (commonDataResults != null && commonDataResults.size() > 0) {
                            initRecycleView();
                        } else {
                            swipeToLoadLayout.post(new Runnable() {
                                @Override
                                public void run() {
                                    swipeToLoadLayout.setRefreshing(true);
                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }


    private void initRecycleView() {
        if (recyclePicAdapter == null) {
            recyclePicAdapter = new RecyclePicAdapter(context, commonDataResults);
            swipeTarget.setAdapter(recyclePicAdapter);
            //点击事件
            recyclePicAdapter.setOnItemClickLitener(new RecyclePicAdapter.OnItemClickLitener() {
                @Override
                public void onItemClick(View view, int position) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (int i = 0; i < commonDataResults.size(); i++) {
                        arrayList.add(commonDataResults.get(i).getUrl());
                    }
                    IntentUtils.startToImageShow(context, arrayList, position);
                }
            });

        } else {
            recyclePicAdapter.updateDatas(commonDataResults);
        }

    }

    private void initRefresh() {
        swipeToLoadLayout.setOnRefreshListener(this);
        swipeToLoadLayout.setOnLoadMoreListener(this);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        swipeTarget.setLayoutManager(staggeredGridLayoutManager);
        swipeTarget.setItemAnimator(new DefaultItemAnimator());
        //到底自动刷新
//        swipeTarget.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    if (!ViewCompat.canScrollVertically(recyclerView, 1)) {
//                        swipeToLoadLayout.setLoadingMore(true);
//                    }
//                }
//            }
//        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onRefresh() {
        GankApi.getCommonDataNew(Constants.FlagWelFare, pageSize, 1, 0x002, httpCallBack);

    }

    private void overRefresh() {
        swipeToLoadLayout.setRefreshing(false);
        swipeToLoadLayout.setLoadingMore(false);
    }

    @Override
    public void onLoadMore() {
        GankApi.getCommonDataNew(Constants.FlagWelFare, pageSize, pageIndex, 0x001, httpCallBack);
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart("WelFareFragment");
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd("WelFareFragment");
    }
}
