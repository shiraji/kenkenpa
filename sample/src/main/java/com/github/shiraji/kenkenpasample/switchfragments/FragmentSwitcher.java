package com.github.shiraji.kenkenpasample.switchfragments;

import android.support.v4.app.FragmentManager;
import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.annotations.KenKenPa;
import com.github.shiraji.kenkenpa.annotations.Land;
import com.github.shiraji.kenkenpasample.R;

@KenKenPa(defaultState = FragmentSwitcher.FRAGMENT1)
public abstract class FragmentSwitcher {
    static final String FRAGMENT1 = "Fragment1";
    static final String FRAGMENT2 = "Fragment2";
    static final String FRAGMENT3 = "Fragment3";

    FragmentManager mFragmentManager;

    public static FragmentSwitcher create(FragmentManager fm) {
        return new KenKenPa_FragmentSwitcher(fm);
    }

    public FragmentSwitcher(FragmentManager fm) {
        mFragmentManager = fm;
    }

    @Hops({
            @Hop(from = FRAGMENT1, to = FRAGMENT2),
            @Hop(from = FRAGMENT2, to = FRAGMENT3),
            @Hop(from = FRAGMENT3, to = FRAGMENT1)
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
