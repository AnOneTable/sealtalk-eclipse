package cn.rongcloud.im.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.SealAppContext;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.FriendDao;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.FriendInvitationResponse;
import cn.rongcloud.im.server.response.GetGroupInfoResponse;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.widget.DialogWithYesOrNoUtils;
import cn.rongcloud.im.server.widget.LoadDialog;
//VoIP start 1
import io.rong.calllib.RongCallClient;
import io.rong.calllib.RongCallSession;
import io.rong.imkit.RongCallAction;
import io.rong.imkit.RongVoIPIntent;
//VoIP end 1
import io.rong.imkit.RongIM;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/6/22.
 * Company RongCloud
 */
public class PersonalProfileActivity extends BaseActivity implements View.OnClickListener {

    private static final int ADD_FRIEND = 10086;
    private ImageView mPersonalPortrait;
    private TextView mPersonalName;
    private Button mAddFriend;
    private UserInfo userInfo;
    private String addMessage;
    private GetGroupInfoResponse.ResultEntity mGroup;
    private LinearLayout mChatGroupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);
        setTitle(R.string.user_details);
        initViews();
        userInfo = getIntent().getParcelableExtra("userinfo");
        mGroup = (GetGroupInfoResponse.ResultEntity) getIntent().getSerializableExtra("groupinfo");
        initData(userInfo);
        SealAppContext.getInstance().pushActivity(this);
    }

    private void initData(UserInfo userInfo) {
        mPersonalName.setText(userInfo.getName());
        ImageLoader.getInstance().displayImage(userInfo.getPortraitUri().toString(), mPersonalPortrait, App.getOptions());
        if (!TextUtils.isEmpty(userInfo.getUserId())) {
            String mySelf = getSharedPreferences("config", MODE_PRIVATE).getString("loginid", "");
            if (mySelf.equals(userInfo.getUserId())) {
                mChatGroupBtn.setVisibility(View.VISIBLE);
                return;
            }
            if (getFriendShip(userInfo.getUserId())) {
                mChatGroupBtn.setVisibility(View.VISIBLE);
            } else {
                mAddFriend.setVisibility(View.VISIBLE);
            }
            mAddFriend.setOnClickListener(this);
        }
    }


    public void startChat(View view) {
        RongIM.getInstance().startPrivateChat(mContext, userInfo.getUserId(), userInfo.getName());
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
        intent.putExtra("targetId", userInfo.getUserId());
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
        intent.putExtra("targetId", userInfo.getUserId());
        intent.putExtra("callAction", RongCallAction.ACTION_OUTGOING_CALL.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(getPackageName());
        getApplicationContext().startActivity(intent);
    }
    //VoIP end 2

    private void initViews() {
        mPersonalPortrait = (ImageView) findViewById(R.id.per_friend_header);
        mPersonalName = (TextView) findViewById(R.id.per_friend_name);
        mAddFriend = (Button) findViewById(R.id.per_add_friend);
        mChatGroupBtn = (LinearLayout) findViewById(R.id.chat_groupbtn);
    }

    /**
     * 从本地缓存的数据库中查询是否存在好友关系
     *
     * @param userId 用户 Id
     * @return true or false
     */
    private boolean getFriendShip(String userId) {
        return DBManager.getInstance(mContext).getDaoSession().getFriendDao().queryBuilder().where(FriendDao.Properties.UserId.eq(userId)).unique() != null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.per_start_friend_chat:
//                if (SealAppContext.getInstance().containsInQue(mConversationType, userInfo.getUserId())) {
//                    finish();
//                } else {
//                    RongIM.getInstance().startPrivateChat(mContext, userInfo.getUserId(), userInfo.getName());
//                }
//                break;
            case R.id.per_add_friend:
                DialogWithYesOrNoUtils.getInstance().showEditDialog(mContext, getString(R.string.add_text), getString(R.string.confirm), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {

                    }

                    @Override
                    public void executeEditEvent(String editText) {
                        if (TextUtils.isEmpty(editText)) {
                            if (mGroup != null && !TextUtils.isEmpty(mGroup.getName())) {
                                addMessage = "我是" + mGroup.getName() + "群的" + getSharedPreferences("config", MODE_PRIVATE).getString("loginnickname", "");
                            } else {
                                addMessage = "我是" + getSharedPreferences("config", MODE_PRIVATE).getString("loginnickname", "");
                            }
                        } else {
                            addMessage = editText;
                        }
                        LoadDialog.show(mContext);
                        request(ADD_FRIEND);
                    }

                    @Override
                    public void updatePassword(String oldPassword, String newPassword) {

                    }
                });
                break;
        }
    }

    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        return action.sendFriendInvitation(userInfo.getUserId(), addMessage);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            FriendInvitationResponse response = (FriendInvitationResponse) result;
            if (response.getCode() == 200) {
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, getString(R.string.request_success));
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        SealAppContext.getInstance().popActivity(this);
        super.onBackPressed();
    }

    @Override
    public void onLeftClick(View v) {
        SealAppContext.getInstance().popActivity(this);
        super.onLeftClick(v);
    }
}
