package com.example.nappula3

import androidx.fragment.app.FragmentTransaction

fun FragmentTransaction.applyFadeAnimations(): FragmentTransaction {
    return setCustomAnimations(
        R.anim.fragment_fade_in,
        R.anim.fragment_fade_out,
        R.anim.fragment_fade_in,
        R.anim.fragment_fade_out
    )
}
