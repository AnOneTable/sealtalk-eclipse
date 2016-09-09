package cn.rongcloud.im.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import cn.rongcloud.im.R;
import cn.rongcloud.im.ui.widget.switchbutton.SwitchButton;


public class NewMessageRemindActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message_remind);
        setTitle(R.string.new_message_notice);

        final RelativeLayout mNotice = (RelativeLayout) findViewById(R.id.seal_notice);
        SwitchButton switchButton = (SwitchButton) findViewById(R.id.remind_switch);

        mNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NewMessageRemindActivity.this, DisturbActivity.class));
            }
        });
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mNotice.setClickable(true);
                    mNotice.setBackgroundColor(Color.parseColor("#ffffff"));
                } else {
                    mNotice.setClickable(false);
                    mNotice.setBackgroundColor(Color.parseColor("#f0f0f6"));
                }
            }
        });
    }
}
