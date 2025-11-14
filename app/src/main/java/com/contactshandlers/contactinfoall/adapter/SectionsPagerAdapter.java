package com.contactshandlers.contactinfoall.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.contactshandlers.contactinfoall.ui.fragment.ContactFragment;
import com.contactshandlers.contactinfoall.ui.fragment.FavouriteFragment;
import com.contactshandlers.contactinfoall.ui.fragment.GroupsFragment;
import com.contactshandlers.contactinfoall.ui.fragment.RecentFragment;

public class SectionsPagerAdapter extends FragmentStateAdapter {
    public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new FavouriteFragment();
            case 1: return new RecentFragment();
            case 2: return new ContactFragment();
            case 3: return new GroupsFragment();
            default: return new ContactFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
