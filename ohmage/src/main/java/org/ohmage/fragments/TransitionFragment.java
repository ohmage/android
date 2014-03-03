/*
 * Copyright (C) 2013 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.fragments;

import android.support.v4.app.FragmentTransaction;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.ohmage.app.R;
import org.ohmage.dagger.InjectedFragment;

/**
 * This is a base class which stores the animations it will perform. This is useful because
 * after an orientation change, fragment animations set via
 * {@link android.support.v4.app.FragmentTransaction#setCustomAnimations(int, int, int, int)}
 * are lost.
 */
public class TransitionFragment extends InjectedFragment {

    private int mAnimationIn = -1;
    private int mAnimationOut = -1;

    /**
     * Set the default in and out animation for this fragment
     *
     * @param animationIn
     * @param animationOut
     */
    public void setDefaultAnimation(int animationIn, int animationOut) {
        mAnimationIn = animationIn;
        mAnimationOut = animationOut;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                return AnimationUtils.loadAnimation(getActivity(),
                        enter ? mAnimationIn : mAnimationOut);
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                return AnimationUtils.loadAnimation(getActivity(),
                        enter ? R.anim.abc_fade_in : R.anim.abc_fade_out);
        }

        return super.onCreateAnimation(transit, enter, nextAnim);
    }
}
