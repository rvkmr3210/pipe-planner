package edu.uark.pipeplanparser.util;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import edu.uark.pipeplanparser.R;

public class SnackbarUtil {

    public static void displaySnackbar(Activity context, @StringRes int stringRes) {
        Snackbar snackbar = Snackbar.make(context.findViewById(android.R.id.content), stringRes, Snackbar.LENGTH_LONG).setAction(R.string.ok, null);
        View snackbarView = snackbar.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(2);
        snackbar.show();
    }
}
