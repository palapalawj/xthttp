package com.xt.basecommon.http.subsciber;

import android.app.Dialog;
import android.content.DialogInterface;

import com.xt.basecommon.http.callback.CallBack;
import com.xt.basecommon.http.exception.ApiException;

import io.reactivex.disposables.Disposable;

/**
 * Describe: 自定义加载进度框回调
 * Created by lijin on 2017/9/25.
 */

public abstract class ProgressDialogCallBack<T> extends CallBack<T>
        implements ProgressCancelListener {

    private IProgressDialog progressDialog;
    private Dialog dialog;
    private boolean isShowProgress = true;

    public ProgressDialogCallBack(IProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
        init(false);
    }

    public ProgressDialogCallBack(IProgressDialog progressDialog, boolean isShowProgress,
                                  boolean isCancel) {
        this.progressDialog = progressDialog;
        this.isShowProgress = isShowProgress;
        init(isCancel);
    }

    private void init(boolean isCancel) {
        if (null == progressDialog) {
            return;
        }
        dialog = progressDialog.getDialog();
        if (null == dialog) {
            return;
        }
        dialog.setCancelable(isCancel);
        if (isCancel) {
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    ProgressDialogCallBack.this.onCancelProgress();
                }
            });
        }
    }

    private void showProgress() {
        if (!isShowProgress) {
            return;
        }
        if (dialog != null) {
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }
    }

    private void dismissProgress() {
        if (!isShowProgress) {
            return;
        }
        if (null != dialog) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    @Override
    public void onStart() {
        showProgress();
    }

    @Override
    public void onCompleted() {
        dismissProgress();
    }

    @Override
    public void onError(ApiException e) {
        dismissProgress();
    }

    @Override
    public void onCancelProgress() {
        if (disposed != null && !disposed.isDisposed()) {
            disposed.dispose();
        }
    }

    private Disposable disposed;

    public void subscription(Disposable disposed) {
        this.disposed = disposed;
    }
}
