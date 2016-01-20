package com.github.shiraji.kenkenpa.compiler.model

class StateModel(private val mDefaultState: String) {
    fun isDefaultState(state: String): Boolean {
        return mDefaultState == state
    }
}