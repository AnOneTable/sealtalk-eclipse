package cn.rongcloud.im.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.Friend;
import cn.rongcloud.im.db.FriendDao;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.pinyin.FriendInfo;
import cn.rongcloud.im.server.response.GetUserInfoByIdResponse;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.OperationRong;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.DialogWithYesOrNoUtils;
import cn.rongcloud.im.server.widget.LoadDialog;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import cn.rongcloud.im.ui.widget.switchbutton.SwitchButton;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

/**
 * Created by AMing on 16/3/9.
 * Company RongCloud
 */
public class FriendDetailActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {


    private static final int GET_USER_INFO = 91;
    private FriendInfo friendInfo;
    private SwitchButton messageTop, messageNotification;
    private SelectableRoundedImageView mImageView;
    private TextView friendName;
    private boolean isFromConversation;
    private String fromConversationId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fr_friend_detail);
        setTitle(R.string.user_details);
        initView();
        fromConversationId = getIntent().getStringExtra("TargetId");
        if (!TextUtils.isEmpty(fromConversationId)) {
            isFromConversation = true;
            LoadDialog.show(mContext);
            //TODO 后续需要将网络获取 更改为 去数据库获取 再 设置备注
            request(GET_USER_INFO);
        } else {
            //好友界面进入详情界面
            friendInfo = (FriendInfo) getIntent().getSerializableExtra("FriendDetails");
            initData();
            getState(friendInfo);
        }

    }


    private void initData() {
        if (friendInfo != null) {
            if (TextUtils.isEmpty(friendInfo.getPortraitUri())) {
                ImageLoader.getInstance().displayImage(RongGenerate.generateDefaultAvatar(friendInfo.getName(), friendInfo.getUserId()), mImageView, App.getOptions());
            } else {
                ImageLoader.getInstance().displayImage(friendInfo.getPortraitUri(), mImageView, App.getOptions());
            }

            Friend friend = getUserInfoById(friendInfo.getUserId());
            if (friend != null && !TextUtils.isEmpty(friend.getDisplayName())) {
                friendName.setText(friend.getDisplayName());
            } else {
                friendName.setText(friendInfo.getName());
            }
        }
    }

    private void initView() {
        LinearLayout cleanMessage = (LinearLayout) findViewById(R.id.clean_friend);
        mImageView = (SelectableRoundedImageView) findViewById(R.id.friend_header);
        messageTop = (SwitchButton) findViewById(R.id.sw_freind_top);
        messageNotification = (SwitchButton) findViewById(R.id.sw_friend_notfaction);
        friendName = (TextView) findViewById(R.id.friend_name);
        cleanMessage.setOnClickListener(this);
        messageNotification.setOnCheckedChangeListener(this);
        messageTop.setOnCheckedChangeListener(this);
    }


    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case GET_USER_INFO:
                return action.getUserInfoById(fromConversationId);
        }
        return super.doInBackground(requestCode, id);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case GET_USER_INFO:
                    GetUserInfoByIdResponse userInfoByIdResponse = (GetUserInfoByIdResponse) result;
                    if (userInfoByIdResponse.getCode() == 200) {
                        userInfo = userInfoByIdResponse.getResult();

                        if (TextUtils.isEmpty(userInfo.getPortraitUri())) {
                            ImageLoader.getInstance().displayImage(RongGenerate.generateDefaultAvatar(userInfo.getNickname(), userInfo.getId()), mImageView, App.getOptions());
                        } else {
                            ImageLoader.getInstance().displayImage(userInfo.getPortraitUri(), mImageView, App.getOptions());
                        }

                        Friend friend = getUserInfoById(userInfo.getId());
                        if (friend != null && !TextUtils.isEmpty(friend.getDisplayName())) {
                            friendName.setText(friend.getDisplayName());
                        } else {
                            friendName.setText(userInfo.getNickname());
                        }
                        getState2(userInfo);
                        LoadDialog.dismiss(mContext);
                    }

                    break;
            }
        }
    }

    private GetUserInfoByIdResponse.ResultEntity userInfo;


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clean_friend:
                DialogWithYesOrNoUtils.getInstance().showDialog(mContext, getString(R.string.clean_history), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {
                        if (RongIM.getInstance() != null) {
                            if (friendInfo != null) {
                                RongIM.getInstance().clearMessages(Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), new RongIMClient.ResultCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        NToast.shortToast(mContext, getString(R.string.clear_success));
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode errorCode) {
                                        NToast.shortToast(mContext, getString(R.string.clear_failure));
                                    }
                                });
                            } else if (userInfo != null) {
                                RongIM.getInstance().clearMessages(Conversation.ConversationType.PRIVATE, userInfo.getId(), new RongIMClient.ResultCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        NToast.shortToast(mContext, getString(R.string.clear_success));
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode errorCode) {
                                        NToast.shortToast(mContext, getString(R.string.clear_failure));
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void executeEditEvent(String editText) {

                    }

                    @Override
                    public void updatePassword(String oldPassword, String newPassword) {

                    }
                });
                break;

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.sw_friend_notfaction:
                if (isChecked) {
                    if (userInfo != null) {
                        OperationRong.setConverstionNotif(mContext, Conversation.ConversationType.PRIVATE, userInfo.getId(), true);
                    } else if (friendInfo != null) {
                        OperationRong.setConverstionNotif(mContext, Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), true);
                    }
                } else {
                    if (userInfo != null) {
                        OperationRong.setConverstionNotif(mContext, Conversation.ConversationType.PRIVATE, userInfo.getId(), false);
                    } else if (friendInfo != null) {
                        OperationRong.setConverstionNotif(mContext, Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), false);
                    }
                }
                break;
            case R.id.sw_freind_top:
                if (isChecked) {
                    if (userInfo != null) {
                        OperationRong.setConversationTop(mContext, Conversation.ConversationType.PRIVATE, userInfo.getId(), true);
                    } else if (friendInfo != null) {
                        OperationRong.setConversationTop(mContext, Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), true);
                    }
                } else {
                    if (userInfo != null) {
                        OperationRong.setConversationTop(mContext, Conversation.ConversationType.PRIVATE, userInfo.getId(), false);
                    } else if (friendInfo != null) {
                        OperationRong.setConversationTop(mContext, Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), false);
                    }
                }
                break;
        }
    }

    private void getState(FriendInfo friendInfo) {
        if (friendInfo != null) {//群组列表 page 进入
            if (RongIM.getInstance() != null) {
                RongIM.getInstance().getConversation(Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), new RongIMClient.ResultCallback<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        if (conversation == null) {
                            return;
                        }

                        if (conversation.isTop()) {
                            messageTop.setChecked(true);
                        } else {
                            messageTop.setChecked(false);
                        }

                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {

                    }
                });

                RongIM.getInstance().getConversationNotificationStatus(Conversation.ConversationType.PRIVATE, friendInfo.getUserId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
                    @Override
                    public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {

                        if (conversationNotificationStatus == Conversation.ConversationNotificationStatus.DO_NOT_DISTURB) {
                            messageNotification.setChecked(true);
                        } else {
                            messageNotification.setChecked(false);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {

                    }
                });
            }
        }
    }

    private void getState2(GetUserInfoByIdResponse.ResultEntity friend) {
        if (friend != null) {//群组列表 page 进入
            if (RongIM.getInstance() != null) {
                RongIM.getInstance().getConversation(Conversation.ConversationType.PRIVATE, friend.getId(), new RongIMClient.ResultCallback<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        if (conversation == null) {
                            return;
                        }

                        if (conversation.isTop()) {
                            messageTop.setChecked(true);
                        } else {
                            messageTop.setChecked(false);
                        }

                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {

                    }
                });

                RongIM.getInstance().getConversationNotificationStatus(Conversation.ConversationType.PRIVATE, friend.getId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
                    @Override
                    public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {

                        if (conversationNotificationStatus == Conversation.ConversationNotificationStatus.DO_NOT_DISTURB) {
                            messageNotification.setChecked(true);
                        } else {
                            messageNotification.setChecked(false);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {

                    }
                });
            }
        }
    }


    private Friend getUserInfoById(String userId) {
        if (!TextUtils.isEmpty(userId)) {
            return DBManager.getInstance(mContext).getDaoSession().getFriendDao().queryBuilder().where(FriendDao.Properties.UserId.eq(userId)).unique();
        }
        return null;
    }
}
