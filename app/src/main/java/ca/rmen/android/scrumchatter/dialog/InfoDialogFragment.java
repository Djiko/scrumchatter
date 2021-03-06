/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Shows a dialog with a title, message, and a single button to dismiss the dialog.
 */
public class InfoDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + InfoDialogFragment.class.getSimpleName();

    public InfoDialogFragment() {
        super();
    }

    /**
     * @return an AlertDialog with a title, message, and single button to dismiss the dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        Context context = new ContextThemeWrapper(getActivity(), R.style.dialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE)).setMessage(arguments.getString(DialogFragmentFactory.EXTRA_MESSAGE))
                .setNeutralButton(android.R.string.ok, null);
        final AlertDialog dialog = builder.create();
        DialogStyleHacks.styleDialog(getActivity(), dialog);
        return dialog;
    }
}
