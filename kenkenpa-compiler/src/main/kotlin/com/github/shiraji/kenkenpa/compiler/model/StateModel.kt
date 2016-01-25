package com.github.shiraji.kenkenpa.compiler.model

class StateModel(val defaultState: String) {
    fun isDefaultState(state: String): Boolean {
        return defaultState == state
    }
}