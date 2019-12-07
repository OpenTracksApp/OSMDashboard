package de.storchp.opentracks.osmplugin;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MapInfoFragment extends DialogFragment {


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        TextView textView = new TextView(getContext());
        textView.setTextSize((float) 18);
        textView.setPadding(50, 50, 50, 50);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(R.string.map_info_text);
        textView.setLinkTextColor(Color.parseColor("#c71c4d"));

        builder.setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.map_info_title)
                .setPositiveButton(R.string.app_info_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        builder.setView(textView);

        // Creates the AlertDialog object and return it
        AlertDialog mapInfoDialog = builder.create();
        return mapInfoDialog;
    }


}
