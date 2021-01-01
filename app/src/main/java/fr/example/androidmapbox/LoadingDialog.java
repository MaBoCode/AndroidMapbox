package fr.example.androidmapbox;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import fr.example.androidmapbox.databinding.ActivityMainBinding;
import fr.example.androidmapbox.databinding.DialogLoaderBinding;

public class LoadingDialog {

    protected static AlertDialog alertDialog = null;

    public static void showLoader(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        DialogLoaderBinding binding = DialogLoaderBinding.inflate(activity.getLayoutInflater());
        builder.setView(binding.getRoot());
        builder.setCancelable(true);

        if (alertDialog == null) {
            alertDialog = builder.create();
        }

        alertDialog.show();
    }

    public static void dismissLoader() {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

}
