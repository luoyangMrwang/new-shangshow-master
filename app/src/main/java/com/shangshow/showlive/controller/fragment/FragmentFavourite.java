package com.shangshow.showlive.controller.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.shangshow.showlive.R;
import com.shangshow.showlive.base.BaseFragment;
import com.shangshow.showlive.base.MConstants;
import com.shangshow.showlive.base.cache.CacheCenter;
import com.shangshow.showlive.common.utils.CommonUtil;
import com.shangshow.showlive.common.utils.ScreenUtil;
import com.shangshow.showlive.common.utils.ShangXiuUtil;
import com.shangshow.showlive.common.widget.TitleBarView;
import com.shangshow.showlive.common.widget.ultra.loadmore.LoadMoreFooterView;
import com.shangshow.showlive.common.widget.ultra.loadmore.RLPtrFrameLayout;
import com.shangshow.showlive.common.widget.viewpager.CirclePageIndicator;
import com.shangshow.showlive.common.widget.viewpager.LoopViewPager;
import com.shangshow.showlive.controller.adapter.BannerViewPagerAdapter;
import com.shangshow.showlive.controller.adapter.HomeLiveVideoSingleAdapter;
import com.shangshow.showlive.controller.common.LoginActivity;
import com.shangshow.showlive.controller.liveshow.LiveAudienceActivity;
import com.shangshow.showlive.controller.liveshow.video.PlayVideoActivity;
import com.shangshow.showlive.controller.member.MemberListActivity;
import com.shangshow.showlive.model.LiveModel;
import com.shangshow.showlive.model.UserModel;
import com.shangshow.showlive.model.callback.Callback;
import com.shangshow.showlive.network.service.models.AdsInfo;
import com.shangshow.showlive.network.service.models.Pager;
import com.shangshow.showlive.network.service.models.VideoRoom;
import com.shangshow.showlive.network.service.models.body.PageBody;
import com.shaojun.widget.superAdapter.OnItemClickListener;
import com.shaojun.widget.superAdapter.divider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * 星尚
 */

public class
FragmentFavourite extends BaseFragment {

    private LiveModel liveModel;
    private UserModel userModel;
    private Context mContext;
    private TitleBarView titleBarView;
    private RLPtrFrameLayout rlPtrFrameLayout;
    private HomeLiveVideoSingleAdapter homeLiveVideoSingleAdapter;

    private View topView;//顶部试图
    //banner广告
    private RelativeLayout bannerViewPagerLayout;
    private LoopViewPager bannerViewPager;
    private CirclePageIndicator bannerCirclePageIndicator;
    private BannerViewPagerAdapter bannerViewPagerAdapter;

    private long currPage = 1;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tab_favourite;
    }

    public static FragmentFavourite newInstance(String title) {
        FragmentFavourite f = new FragmentFavourite();
        Bundle b = new Bundle();
        b.putString("title", title);
        f.setArguments(b);
        return f;
    }

    @Override
    protected void initWidget(View rootView) {
        liveModel = new LiveModel(mContext);
        userModel = new UserModel(mContext);
        /**
         * 设置title
         */
        title = getArguments().getString("title");
        titleBarView = (TitleBarView) rootView.findViewById(R.id.favourite_title_layout);
        titleBarView.initCenterTitle(title);
        titleBarView.initRight("列表", 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MemberListActivity.class);
                intent.putExtra(MemberListActivity.key_USER_TYPE, MConstants.USER_TYPE_FAVOURITE);
                startActivity(intent);
            }
        });

        rlPtrFrameLayout = (RLPtrFrameLayout) rootView.findViewById(R.id.favourite_rlPtrFrameLayout);
        topView = getFavouriteTopView();
        //解决PtrFrameLayout嵌套ViewPager滑动冲突
        rlPtrFrameLayout.setViewPager(bannerViewPager);
        rlPtrFrameLayout.setOnRefreshOrLoadMoreListener(new RLPtrFrameLayout.OnRefreshOrLoadMoreListener() {
            @Override
            public void onRefresh() {
                loadData();
            }

            @Override
            public void onLoadMore() {
                loadFavouriteVideoRoom(MConstants.DATA_4_LOADMORE);
            }
        });

        /**
         * 星尚直播列表
         */
        homeLiveVideoSingleAdapter = new HomeLiveVideoSingleAdapter(mContext, new ArrayList<VideoRoom>(), R.layout.item_recycler_common_livevideo_list);
        //+分割线
        rlPtrFrameLayout.getRecyclerView().addItemDecoration(new HorizontalDividerItemDecoration.Builder(mContext)
                .color(getResources().getColor(R.color.transparent))
                .sizeResId(R.dimen.common_activity_padding_10)
                .build());
        homeLiveVideoSingleAdapter.addHeaderView(topView);
        rlPtrFrameLayout.setAdapter(homeLiveVideoSingleAdapter, true);
        homeLiveVideoSingleAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int viewType, int position) {
                if (!CommonUtil.isClickSoFast(MConstants.DELAYED)) {
                    if (CacheCenter.getInstance().isLogin()) {
                        VideoRoom videoRoom = homeLiveVideoSingleAdapter.getItem(position);
                        if("LIVE".equals(videoRoom.videoStatus)){
                            LiveAudienceActivity.start(mContext, videoRoom);
                        }
                        if("OFF".equals(videoRoom.videoStatus)){
                            Intent intent = new Intent(mContext, PlayVideoActivity.class);
                            intent.putExtra("uri", videoRoom.videoUrl + "");
                            startActivity(intent);
                        }
                    } else {
                        startActivity(new Intent(mContext, LoginActivity.class));
                    }
                }
            }
        });
    }

    //星尚topView
    public View getFavouriteTopView() {
        View topView = LayoutInflater.from(mContext).inflate(R.layout.layout_tab_favourite_top, null);
        bannerViewPagerLayout = (RelativeLayout) topView.findViewById(R.id.tab_banner_viewpager_layout);
        bannerViewPager = (LoopViewPager) topView.findViewById(R.id.tab_banner_viewpager);
        bannerCirclePageIndicator = (CirclePageIndicator) topView.findViewById(R.id.tab_banner_viewpager_indicator);

        //设置banner的高度
        int width = ScreenUtil.getScreenWidth(mContext);
        bannerViewPagerLayout.setLayoutParams(new LinearLayout.LayoutParams(width, (int) (width * MConstants.RATIO_POINT_BANNER)));
        return topView;
    }

    @Override
    protected void bindEven() {

    }

    @Override
    protected void setView() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                rlPtrFrameLayout.autoRefresh();
            }
        }, MConstants.DELAYED);
    }

    /**
     * 设置广告位视图
     */
    public void setBannerView(List<AdsInfo> dataList) {
        if (dataList == null) {
            return;
        }
        if (bannerViewPagerAdapter == null) {
            bannerViewPagerAdapter = new BannerViewPagerAdapter(
                    mContext, dataList);
            bannerViewPager.setAdapter(bannerViewPagerAdapter);
            bannerViewPager.setBoundaryCaching(true);
            bannerCirclePageIndicator.setViewPager(bannerViewPager);
        } else {
            bannerViewPagerAdapter.changeData(dataList);
        }
    }


    private void loadData() {
        loadFavouriteAds();
        loadFavouriteVideoRoom(MConstants.DATA_4_REFRESH);
    }

    /**
     * 加载星尚广告
     */
    private void loadFavouriteAds() {
        userModel.adsInfoList(MConstants.ADS_NO_FAVOURITE, new Callback<List<AdsInfo>>() {
            @Override
            public void onSuccess(List<AdsInfo> adsInfos) {
                setBannerView(adsInfos);
            }

            @Override
            public void onFailure(int resultCode, String message) {

            }
        }, false);
    }


    /**
     * 加载星尚直播间
     */
    private void loadFavouriteVideoRoom(final long loadType) {
        PageBody pageBody = ShangXiuUtil.refreshPagerBodey(loadType, currPage);
        liveModel.getVideoRoomList(MConstants.USER_TYPE_FAVOURITE, pageBody, new Callback<Pager<VideoRoom>>() {
            @Override
            public void onSuccess(Pager<VideoRoom> pager) {
                currPage = pager.pageNum;
                List<VideoRoom> videoRooms = pager.list;
                if (loadType == MConstants.DATA_4_REFRESH) {
                    homeLiveVideoSingleAdapter.replaceAll(videoRooms);
                    currPage++;
                    rlPtrFrameLayout.refreshComplete();

                } else {
                    homeLiveVideoSingleAdapter.addAll(videoRooms);
                    if (videoRooms.size() < MConstants.PAGE_SIZE) {
                        //nodata
                        rlPtrFrameLayout.loadMoreComplete();
                    } else {
                        currPage++;
                        rlPtrFrameLayout.loadMoreComplete();
                    }
                }
            }

            @Override
            public void onFailure(int resultCode, String message) {
                if (loadType == MConstants.DATA_4_REFRESH) {
                    rlPtrFrameLayout.refreshComplete();
                } else if (loadType == MConstants.DATA_4_LOADMORE) {
                    rlPtrFrameLayout.changeLoadMoreState(LoadMoreFooterView.LOAD_MORE_STATE_ERROR);
                }
            }
        }, false);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        liveModel.unSubscribe();
        userModel.unSubscribe();
    }

}
