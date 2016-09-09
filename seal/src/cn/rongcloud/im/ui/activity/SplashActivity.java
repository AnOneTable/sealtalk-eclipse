package cn.rongcloud.im.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;

import cn.rongcloud.im.R;
import cn.rongcloud.im.server.utils.NToast;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;

/**
 * Created by AMing on 16/8/5.
 * Company RongCloud
 */
public class SplashActivity extends Activity {

    private Context context;
    private android.os.Handler handler = new android.os.Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        context = this;
        SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);

        if (!isNetworkConnected(context)) {
            NToast.shortToast(context, getString(R.string.network_not_available));
            goToLogin();
            return;
        }

        String cacheToken = sp.getString("loginToken", "");
        if (!TextUtils.isEmpty(cacheToken)) {
            if (RongIM.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        goToMain();
                    }
                }, 800);
            } else {
                RongIM.connect(cacheToken, new RongIMClient.ConnectCallback() {
                    @Override
                    public void onTokenIncorrect() {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                goToLogin();
                            }
                        }, 300);
                    }

                    @Override
                    public void onSuccess(String s) {
                        getSharedPreferences("config", MODE_PRIVATE).edit().putString("loginid", s).apply();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                goToMain();
                            }
                        }, 300);
                    }

                    @Override
                    public void onError(final RongIMClient.ErrorCode e) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                NToast.shortToast(context, "connect error value:" + e.getValue());
                                goToLogin();
                            }
                        }, 300);
                    }
                });
            }
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToLogin();
                }
            }, 800);
        }
    }


    private void goToMain() {
        startActivity(new Intent(context, MainActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(context, LoginActivity.class));
        finish();
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

}
