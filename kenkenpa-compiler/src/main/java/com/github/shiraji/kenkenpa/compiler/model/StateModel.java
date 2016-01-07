package com.github.shiraji.kenkenpa.compiler.model;

import org.jetbrains.annotations.NotNull;

public class StateModel {
    private String mDefaultState;

    public StateModel(@NotNull String defaultState) {
        mDefaultState = defaultState;
    }

    public boolean isDefaultState(String state) {
        return mDefaultState.equals(state);
    }
}
