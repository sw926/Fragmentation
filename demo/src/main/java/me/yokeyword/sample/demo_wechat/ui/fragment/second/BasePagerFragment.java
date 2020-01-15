package me.yokeyword.sample.demo_wechat.ui.fragment.second;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.yokeyword.fragmentation.SupportFragment;
import timber.log.Timber;

public class BasePagerFragment extends SupportFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.i("page_" + getArguments().getInt("index") + ": onCreate");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Timber.i("page_" + getArguments().getInt("index") + ": onViewCreated");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Timber.i("page_" + getArguments().getInt("index") + ": onDestroyView");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.i("page_" + getArguments().getInt("index") + ": onDestroy");

    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.i("page_" + getArguments().getInt("index") + ": onResume");

    }

    @Override
    public void onPause() {
        super.onPause();
        Timber.i("page_" + getArguments().getInt("index") + ": onPause");

    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.i("page_" + getArguments().getInt("index") + ": onStop");

    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.i("page_" + getArguments().getInt("index") + ": onStart");

    }

    @Override
    public void onLazyFirstInit() {
        super.onLazyFirstInit();
        Timber.i("page_" + getArguments().getInt("index") + ": onLazyFirstInit");

    }
}
