package org.ohmage.auth;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Wraps the {@link Dialog} returned by
 * {@link GooglePlayServicesUtil#getErrorDialog} so that it can be properly
 * managed by the {@link android.app.Activity}.
 */
public class GooglePlayServicesErrorDialogFragment extends DialogFragment {

    /**
     * The error code returned by the
     * {@link GooglePlayServicesUtil#isGooglePlayServicesAvailable(android.content.Context)}
     * call.
     */
    public static final String ARG_ERROR_CODE = "errorCode";

    /**
     * The request code given when calling
     * {@link android.app.Activity#startActivityForResult}.
     */
    public static final String ARG_REQUEST_CODE = "requestCode";

    /**
     * Create a {@link DialogFragment} for displaying the
     * {@link GooglePlayServicesUtil#getErrorDialog}.
     *
     * @param errorCode   The error code returned by
     *                    {@link com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener}
     * @param requestCode The request code for resolving the resolution
     *                    activity.
     * @return The {@link DialogFragment}.
     */
    public static Bundle createArguments(int errorCode, int requestCode) {
        Bundle args = new Bundle();
        args.putInt(GooglePlayServicesErrorDialogFragment.ARG_ERROR_CODE, errorCode);
        args.putInt(GooglePlayServicesErrorDialogFragment.ARG_REQUEST_CODE, requestCode);
        return args;
    }

    /**
     * Returns a {@link Dialog} created by
     * {@link GooglePlayServicesUtil#getErrorDialog} with the provided
     * errorCode, activity, and request code.
     *
     * @param savedInstanceState Not used.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        return GooglePlayServicesUtil.getErrorDialog(args.getInt(ARG_ERROR_CODE), getActivity(),
                args.getInt(ARG_REQUEST_CODE));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (getActivity() instanceof PlusClientFragment.OnSignInListener) {
            ((PlusClientFragment.OnSignInListener) getActivity()).onSignInFailed();
        }
    }
}