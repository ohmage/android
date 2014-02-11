/*
 * Copyright (C) 2014 ohmage
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

package org.ohmage.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.dagger.InjectedActionBarActivity;
import org.ohmage.dagger.InjectedFragment;
import org.ohmage.models.Ohmlet;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;

public class OhmletActivity extends InjectedActionBarActivity {

    private static final String TAG = OhmletActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ohmlet);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .add(R.id.container, new OhmletFragment())
                                       .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ohmlet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class OhmletFragment extends InjectedFragment implements Observer<Ohmlet> {

        @Inject OhmageService ohmageService;

        private TextView mDescription;

        private Ohmlet mOhmlet;

        private Subscription ohmletSupscription;

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            Observable<Ohmlet> ohmletObservable =
                    ohmageService.getOhmlet("d9fdbad3-3888-4540-a872-5fbcc38dfaf6")
                                 .single();

            ohmletSupscription = AndroidObservable.fromFragment(this, ohmletObservable)
                                                  .subscribe(this);
        }

        @Override public void onDestroy() {
            super.onDestroy();
            ohmletSupscription.unsubscribe();
        }

        @Override public void onCompleted() {
            updateView();
        }

        @Override public void onError(Throwable e) {
            //TODO: handle error correctly
            Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        @Override public void onNext(Ohmlet ohmlet) {
            setOhmlet(ohmlet);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_ohmlet, container, false);

            mDescription = (TextView) rootView.findViewById(R.id.description);

            return rootView;
        }

        public void setOhmlet(Ohmlet ohmlet) {
            mOhmlet = ohmlet;
        }

        private void updateView() {
            getActivity().setTitle(mOhmlet.name);

            if (getView() != null) {
                mDescription.setText(mOhmlet.description);
            }
        }
    }
}