package me.yokeyword.sample.demo_wechat.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import me.yokeyword.sample.demo_wechat.ui.fragment.second.FirstPagerFragment;
import me.yokeyword.sample.demo_wechat.ui.fragment.second.OtherPagerFragment;


/**
 * Created by YoKeyword on 16/6/5.
 */
public class WechatPagerFragmentAdapter extends FragmentPagerAdapter {
    private String[] mTitles;

    public WechatPagerFragmentAdapter(FragmentManager fm, String... titles) {
        super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mTitles = titles;
    }

    @Override
    public Fragment getItem(int position) {
        if (position % 2 == 0) {
            return FirstPagerFragment.newInstance(position);
        } else {
            return OtherPagerFragment.newInstance(position);
        }
    }

    @Override
    public int getCount() {
        return 10;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position % 2];
    }
}
