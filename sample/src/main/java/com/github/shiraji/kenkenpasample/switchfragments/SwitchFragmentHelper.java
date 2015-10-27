package com.github.shiraji.kenkenpasample.switchfragments;

import android.support.v4.app.FragmentManager;
import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.annotations.KenKenPa;
import com.github.shiraji.kenkenpa.annotations.Land;
import com.github.shiraji.kenkenpasample.R;

@KenKenPa("Fragment1")
public class SwitchFragmentHelper {
    FragmentManager mFragmentManager;

    public static SwitchFragmentHelper create(FragmentManager fm) {
        return new KenKenPa_SwitchFragmentHelper(fm);
    }

    public SwitchFragmentHelper(FragmentManager fm) {
        mFragmentManager = fm;
    }

    @Hops({
            @Hop(from = "Fragment1", to = "Fragment2"),
            @Hop(from = "Fragment2", to = "Fragment3"),
            @Hop(from = "Fragment3", to = "Fragment1")
    })
    public void switchFragment(FragmentManager fm) {
        mFragmentManager = fm;
    }

    @Land("Fragment1")
    public void switchToFragment1() {
        mFragmentManager.beginTransaction().replace(R.id.fragment_container, SwitchFirstFragment.newInstance()).commit();
    }

    @Land("Fragment2")
    public void switchToFragment2() {
        mFragmentManager.beginTransaction().replace(R.id.fragment_container, SwitchSecondFragment.newInstance()).commit();
    }

    @Land("Fragment3")
    public void switchToFragment3() {
        mFragmentManager.beginTransaction().replace(R.id.fragment_container, SwitchThirdFragment.newInstance()).commit();
    }

}
