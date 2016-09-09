package cn.rongcloud.im.ui.activity;

import android.os.Bundle;
import android.view.View;

import cn.rongcloud.im.R;
import io.rong.imkit.tools.RongWebviewActivity;

/**
 * Created by AMing on 16/9/6.
 * Company RongCloud
 */
@SuppressWarnings("deprecation")
public class SealWebActivity extends RongWebviewActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebViewTitle.setVisibility(View.VISIBLE);
        mLeftImage.setImageDrawable(getResources().getDrawable(R.drawable.actionbar_back));
        mLeftBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
