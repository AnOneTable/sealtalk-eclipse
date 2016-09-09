package cn.rongcloud.im.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.server.pinyin.FriendInfo;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.ui.widget.SinglePopWindow;
//VoIP start 1
import io.rong.calllib.RongCallClient;
import io.rong.calllib.RongCallSession;
import io.rong.imkit.RongCallAction;
import io.rong.imkit.RongVoIPIntent;
//VoIP end 1
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imkit.RongIM;

/**
 * Created by AMing on 16/7/12.
 * Company RongCloud
 */
public class SingleContactActivity extends BaseActivity implements View.OnClickListener {

    private TextView mContactName;
    private TextView mDisplayName;
    private ImageView mMore;
    private FriendInfo friendInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_contact);
        setHeadVisibility(View.GONE);
        mContactName = (TextView) findViewById(R.id.contact_name);
        mDisplayName = (TextView) findViewById(R.id.contact_dispalyname);
        ImageView mContactHeader = (ImageView) findViewById(R.id.contact_header);
        mMore = (ImageView) findViewById(R.id.contact_more);
        mMore.setOnClickListener(this);
        friendInfo = (FriendInfo) getIntent().getSerializableExtra("FriendDetails");
        if (friendInfo != null) {
            if (friendInfo.isExitsDisplayName()) {
                mDisplayName.setVisibility(View.VISIBLE);
                mDisplayName.setText(friendInfo.getDisplayName());
                mContactName.setText(getString(R.string.nickname_show) + friendInfo.getName());
                mContactName.setTextSize(14);
            } else {
                mContactName.setTextSize(16);
                mContactName.setTextColor(Color.parseColor("#000000"));
                mContactName.setText(friendInfo.getName());
            }
            ImageLoader.getInstance().displayImage(TextUtils.isEmpty(friendInfo.getPortraitUri()) ? RongGenerate.generateDefaultAvatar(friendInfo.getName(), friendInfo.getUserId()) : friendInfo.getPortraitUri(), mContactHeader, App.getOptions());
        }
    }

    public void startChat(View view) {
        RongIM.getInstance().startPrivateChat(mContext, friendInfo.getUserId(), friendInfo.getName());
        finish();
    }

    //VoIP start 2
    public void startVoice(View view) {
        RongCallSession profile = RongCallClient.getInstance().getCallSession();
        if (profile != null && profile.getActiveTime() > 0) {
            Toast.makeText(mContext, getString(io.rong.imkit.R.string.rc_voip_call_start_fail), Toast.LENGTH_SHORT).show();
            return;
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
            Toast.makeText(mContext, getString(io.rong.imkit.R.string.rc_voip_call_network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RongVoIPIntent.RONG_INTENT_ACTION_VOIP_SINGLEAUDIO);
        intent.putExtra("conversationType", Conversation.ConversationType.PRIVATE.getName().toLowerCase());
        intent.putExtra("targetId", friendInfo.getUserId());
        intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(getPackageName());
        getApplicationContext().startActivity(intent);
    }

    public void startVideo(View view) {
        RongCallSession profile = RongCallClient.getInstance().getCallSession();
        if (profile != null && profile.getActiveTime() > 0) {
            Toast.makeText(mContext, getString(io.rong.imkit.R.string.rc_voip_call_start_fail), Toast.LENGTH_SHORT).show();
            return;
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected() || !networkInfo.isAvailable()) {
            Toast.makeText(mContext, getString(io.rong.imkit.R.string.rc_voip_call_network_error), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(RongVoIPIntent.RONG_INTENT_ACTION_VOIP_SINGLEVIDEO);
        intent.putExtra("conversationType", Conversation.ConversationType.PRIVATE.getName().toLowerCase());
        intent.putExtra("targetId", friendInfo.getUserId());
        intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(getPackageName());
        getApplicationContext().startActivity(intent);
    }
    //VoIP end 2

    public void finishPage(View view) {
        this.finish();
    }

    public void setDisplayName(View view) {
        Intent intent = new Intent(mContext, NoteInformationActivity.class);
        intent.putExtra("friendInfo", friendInfo);
        startActivityForResult(intent, 99);
    }

    @Override
    public void onClick(View v) {
        RongIM.getInstance().getBlacklistStatus(friendInfo.getUserId(), new RongIMClient.ResultCallback<RongIMClient.BlacklistStatus>() {
            @Override
            public void onSuccess(RongIMClient.BlacklistStatus blacklistStatus) {
                SinglePopWindow morePopWindow = new SinglePopWindow(SingleContactActivity.this, friendInfo, blacklistStatus);
                morePopWindow.showPopupWindow(mMore);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 155 && data != null) {
            String displayName = data.getStringExtra("displayName");
            mContactName.setTextSize(14);
            mContactName.setTextColor(Color.parseColor("#999999"));
            mContactName.setText(getString(R.string.nickname_show) + friendInfo.getName());
            mDisplayName.setText(displayName);
            mDisplayName.setVisibility(View.VISIBLE);
        }
    }
}
