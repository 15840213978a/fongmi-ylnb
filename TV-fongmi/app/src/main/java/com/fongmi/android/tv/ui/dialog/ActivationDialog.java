package com.fongmi.android.tv.ui.dialog;

import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Activation;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogActivationBinding;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ActivationDialog extends BaseAlertDialog {

    private DialogActivationBinding binding;

    public static ActivationDialog create() {
        return new ActivationDialog();
    }

    public ActivationDialog show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        return this;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogActivationBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle("激活").setView(getBinding().getRoot()).setPositiveButton(R.string.dialog_positive, null).setCancelable(false);
    }

    @Override
    protected void initView() {
        binding.deviceId.setText(Activation.getDeviceId());
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this::onConfirm);
    }

    private void onConfirm(View view) {
        String code = binding.code.getText().toString().trim();
        if (code.isEmpty()) {
            Notify.show("请输入激活码");
            return;
        }
        if (Activation.activate(code)) {
            Notify.show("激活成功");
            dismiss();
        } else {
            Notify.show("激活码无效或已过期");
        }
    }
}
