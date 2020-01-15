package me.yokeyword.sample.demo_wechat.ui.fragment.second;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import me.yokeyword.sample.R;

/**
 * Created by YoKeyword on 16/6/30.
 */
public class OtherPagerFragment extends BasePagerFragment {

    public static OtherPagerFragment newInstance(int index) {

        Bundle args = new Bundle();
        args.putInt("index", index);
        OtherPagerFragment fragment = new OtherPagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wechat_fragment_tab_second_pager_other, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
