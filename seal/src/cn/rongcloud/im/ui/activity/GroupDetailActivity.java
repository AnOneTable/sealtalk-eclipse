package cn.rongcloud.im.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.SealAppContext;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.Friend;
import cn.rongcloud.im.db.FriendDao;
import cn.rongcloud.im.db.GroupMember;
import cn.rongcloud.im.db.Groups;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.pinyin.FriendInfo;
import cn.rongcloud.im.server.response.DismissGroupResponse;
import cn.rongcloud.im.server.response.GetGroupInfoResponse;
import cn.rongcloud.im.server.response.GetGroupMemberResponse;
import cn.rongcloud.im.server.response.QiNiuTokenResponse;
import cn.rongcloud.im.server.response.QuitGroupResponse;
import cn.rongcloud.im.server.response.SetGroupDisplayNameResponse;
import cn.rongcloud.im.server.response.SetGroupNameResponse;
import cn.rongcloud.im.server.response.SetGroupPortraitResponse;
import cn.rongcloud.im.server.utils.CommonUtils;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.OperationRong;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.utils.photo.PhotoUtils;
import cn.rongcloud.im.server.widget.BottomMenuDialog;
import cn.rongcloud.im.server.widget.DialogWithYesOrNoUtils;
import cn.rongcloud.im.server.widget.LoadDialog;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import cn.rongcloud.im.ui.widget.DemoGridView;
import cn.rongcloud.im.ui.widget.switchbutton.SwitchButton;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/1/27.
 * Company RongCloud
 */
public class GroupDetailActivity extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final int GET_GROUP_MEMBER = 20;
    private static final int DISMISS_GROUP = 26;
    private static final int QUIT_GROUP = 27;
    private static final int SET_GROUP_NAME = 29;
    private static final int GET_GROUP_INFO = 30;
    private static final int GET_GROUP_INFO2 = 31;
    private static final int UPDATE_GROUP_NAME = 32;
    private static final int GET_QI_NIU_TOKEN = 133;
    private static final int UPDATE_GROUP_HEADER = 25;

    private boolean isCreated;
    private DemoGridView mGridView;
    private List<GetGroupMemberResponse.ResultEntity> mGroupMember;
    private TextView mTextViewMemberSize, mGroupDisplayNameText;
    private SelectableRoundedImageView mGroupHeader;
    private SwitchButton messageTop, messageNotification;
    private GetGroupInfoResponse.ResultEntity mGroup;
    private String fromConversationId;
    private boolean isFromConversation;
    private TextView mGroupName;
    private PhotoUtils photoUtils;
    private BottomMenuDialog dialog;
    private UploadManager uploadManager;
    private String imageUrl;
    private Uri selectUri;
    private String newGroupName;
    private LinearLayout mGroupNotice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_group);
        initViews();
        setTitle(R.string.group_info);

        //群组会话界面点进群组详情
        fromConversationId = getIntent().getStringExtra("TargetId");
        if (!TextUtils.isEmpty(fromConversationId)) {
            isFromConversation = true;
        }

        if (isFromConversation) {//群组会话页进入
            LoadDialog.show(mContext);
            request(GET_GROUP_INFO2);
        }
        setPortraitChangeListener();

        SealAppContext.getInstance().pushActivity(this);
    }


    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case GET_GROUP_MEMBER:
                return action.getGroupMember(fromConversationId);
            case QUIT_GROUP:
                return action.quitGroup(fromConversationId);
            case DISMISS_GROUP:
                return action.dissmissGroup(fromConversationId);
            case SET_GROUP_NAME:
                return action.setGroupDisplayName(fromConversationId, newGroupName);
            case GET_GROUP_INFO:
                return action.getGroupInfo(fromConversationId);
            case GET_GROUP_INFO2:
                return action.getGroupInfo(fromConversationId);
            case UPDATE_GROUP_HEADER:
                return action.setGroupPortrait(fromConversationId, imageUrl);
            case GET_QI_NIU_TOKEN:
                return action.getQiNiuToken();
            case UPDATE_GROUP_NAME:
                return action.setGroupName(fromConversationId, newGroupName);
        }
        return super.doInBackground(requestCode, id);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case GET_GROUP_MEMBER:
                    GetGroupMemberResponse res = (GetGroupMemberResponse) result;
                    if (res.getCode() == 200) {
                        mGroupMember = setCreatedToTop(res.getResult());
                        if (mGroupMember != null && mGroupMember.size() > 0) {
                            setTitle(getString(R.string.group_info) + "(" + mGroupMember.size() + ")");
                            mTextViewMemberSize.setText(getString(R.string.group_member_size) + "(" + mGroupMember.size() + ")");
                            mGridView.setAdapter(new GridAdapter(mContext, mGroupMember));
                        }

                        for (GetGroupMemberResponse.ResultEntity g : mGroupMember) {
                            if (g.getUser().getId().equals(getSharedPreferences("config", MODE_PRIVATE).getString("loginid", ""))) {
                                if (!TextUtils.isEmpty(g.getDisplayName())) {
                                    mGroupDisplayNameText.setText(g.getDisplayName());
                                } else {
                                    mGroupDisplayNameText.setText("无");
                                }
                            }
                        }
                        LoadDialog.dismiss(mContext);
                        for (GetGroupMemberResponse.ResultEntity resultEntity : mGroupMember) {
                            DBManager.getInstance(mContext).getDaoSession().getGroupMemberDao().insertOrReplace(new GroupMember(fromConversationId, resultEntity.getUser().getId(),
                                    resultEntity.getUser().getNickname(), resultEntity.getUser().getPortraitUri(), resultEntity.getDisplayName()
                                                                                                                               ));
                        }
                    }
                    break;
                case QUIT_GROUP:
                    QuitGroupResponse response = (QuitGroupResponse) result;
                    if (response.getCode() == 200) {

                        RongIM.getInstance().getConversation(Conversation.ConversationType.GROUP, fromConversationId, new RongIMClient.ResultCallback<Conversation>() {
                            @Override
                            public void onSuccess(Conversation conversation) {
                                RongIM.getInstance().clearMessages(Conversation.ConversationType.GROUP, fromConversationId, new RongIMClient.ResultCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        RongIM.getInstance().removeConversation(Conversation.ConversationType.GROUP, fromConversationId, null);
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode e) {

                                    }
                                });
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {

                            }
                        });
                        setResult(501, new Intent());
                        NToast.shortToast(mContext, getString(R.string.quit_success));
                        LoadDialog.dismiss(mContext);
                        finish();
                    }
                    break;

                case DISMISS_GROUP:
                    DismissGroupResponse response1 = (DismissGroupResponse) result;
                    if (response1.getCode() == 200) {
                        RongIM.getInstance().getConversation(Conversation.ConversationType.GROUP, fromConversationId, new RongIMClient.ResultCallback<Conversation>() {
                            @Override
                            public void onSuccess(Conversation conversation) {
                                RongIM.getInstance().clearMessages(Conversation.ConversationType.GROUP, fromConversationId, new RongIMClient.ResultCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        RongIM.getInstance().removeConversation(Conversation.ConversationType.GROUP, fromConversationId, null);
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode e) {

                                    }
                                });
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {

                            }
                        });
                        setResult(501, new Intent());
                        NToast.shortToast(mContext, getString(R.string.dismiss_success));
                        LoadDialog.dismiss(mContext);
                        finish();
                    }
                    break;

                case SET_GROUP_NAME:
                    SetGroupDisplayNameResponse response2 = (SetGroupDisplayNameResponse) result;
                    if (response2.getCode() == 200) {
                        request(GET_GROUP_INFO);
                    }
                    break;
                case GET_GROUP_INFO:
                    GetGroupInfoResponse response3 = (GetGroupInfoResponse) result;
                    if (response3.getCode() == 200) {
                        int i;
                        if (isCreated) {
                            i = 0;
                        } else {
                            i = 1;
                        }
                        GetGroupInfoResponse.ResultEntity bean = response3.getResult();
                        DBManager.getInstance(mContext).getDaoSession().getGroupsDao().insertOrReplace(
                            new Groups(bean.getId(), bean.getName(), bean.getPortraitUri(), newGroupName, String.valueOf(i), null)
                        );
                        mGroupName.setText(newGroupName);
                        RongIM.getInstance().refreshGroupInfoCache(new Group(fromConversationId, newGroupName, Uri.parse(bean.getPortraitUri())));
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, getString(R.string.update_success));
                    }
                    break;
                case GET_GROUP_INFO2:
                    GetGroupInfoResponse response4 = (GetGroupInfoResponse) result;
                    if (response4.getCode() == 200) {
                        if (response4.getResult() != null) {
                            mGroup = response4.getResult();
                            if (TextUtils.isEmpty(response4.getResult().getPortraitUri())) {
                                ImageLoader.getInstance().displayImage(RongGenerate.generateDefaultAvatar(response4.getResult().getName(), response4.getResult().getId()), mGroupHeader, App.getOptions());
                            } else {
                                ImageLoader.getInstance().displayImage(response4.getResult().getPortraitUri(), mGroupHeader, App.getOptions());
                            }
                            mGroupName.setText(mGroup.getName());

                            if (RongIM.getInstance() != null) {
                                RongIM.getInstance().getConversation(Conversation.ConversationType.GROUP, mGroup.getId(), new RongIMClient.ResultCallback<Conversation>() {
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

                                RongIM.getInstance().getConversationNotificationStatus(Conversation.ConversationType.GROUP, mGroup.getId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
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


                            SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
                            if (sp.getString("loginid", "").equals(response4.getResult().getCreatorId())) {
                                isCreated = true;
                            }
                            if (!isCreated) {
                                mGroupNotice.setVisibility(View.GONE);
                            } else {
                                mGroupNotice.setVisibility(View.VISIBLE);
                            }
                            request(GET_GROUP_MEMBER);
                        }
                    }
                    break;

                case UPDATE_GROUP_HEADER:
                    SetGroupPortraitResponse response5 = (SetGroupPortraitResponse) result;
                    if (response5.getCode() == 200) {
                        ImageLoader.getInstance().displayImage(imageUrl, mGroupHeader, App.getOptions());
                        RongIM.getInstance().refreshGroupInfoCache(new Group(fromConversationId, mGroup.getName(), Uri.parse(imageUrl)));
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, getString(R.string.update_success));
                    }

                    break;
                case GET_QI_NIU_TOKEN:
                    QiNiuTokenResponse response6 = (QiNiuTokenResponse) result;
                    if (response6.getCode() == 200) {
                        uploadImage(response6.getResult().getDomain(), response6.getResult().getToken(), selectUri);
                    }
                    break;
                case UPDATE_GROUP_NAME:
                    SetGroupNameResponse response7 = (SetGroupNameResponse) result;
                    if (response7.getCode() == 200) {
                        DBManager.getInstance(mContext).getDaoSession().getGroupsDao().insertOrReplace(
                            new Groups(mGroup.getId(), mGroup.getName(), mGroup.getPortraitUri(), newGroupName, null, null)
                        );
                        mGroupName.setText(newGroupName);
                        RongIM.getInstance().refreshGroupInfoCache(new Group(fromConversationId, newGroupName, TextUtils.isEmpty(mGroup.getPortraitUri()) ? Uri.parse(RongGenerate.generateDefaultAvatar(newGroupName, mGroup.getId())) : Uri.parse(mGroup.getPortraitUri())));
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, getString(R.string.update_success));
                    }
                    break;
            }
        }
    }


    @Override
    public void onFailure(int requestCode, int state, Object result) {
        switch (requestCode) {
            case GET_GROUP_MEMBER:
                NToast.shortToast(mContext, "获取群组成员请求失败");
                LoadDialog.dismiss(mContext);
                break;
            case QUIT_GROUP:
                NToast.shortToast(mContext, "退出群组请求失败");
                LoadDialog.dismiss(mContext);
                break;
            case DISMISS_GROUP:
                NToast.shortToast(mContext, "解散群组请求失败");
                LoadDialog.dismiss(mContext);
                break;
            case GET_GROUP_INFO2:
                LoadDialog.dismiss(mContext);
                if (!CommonUtils.isNetworkConnected(mContext)) {
                    NToast.shortToast(mContext, "网络不可用");
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.group_quit:
                DialogWithYesOrNoUtils.getInstance().showDialog(mContext, getString(R.string.confirm_quit_group), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {
                        LoadDialog.show(mContext);
                        request(QUIT_GROUP);
                    }

                    @Override
                    public void executeEditEvent(String editText) {

                    }

                    @Override
                    public void updatePassword(String oldPassword, String newPassword) {

                    }
                });
                break;
            case R.id.group_dismiss:
                DialogWithYesOrNoUtils.getInstance().showDialog(mContext, getString(R.string.confirm_dismiss_group), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {
                        LoadDialog.show(mContext);
                        request(DISMISS_GROUP);
                    }

                    @Override
                    public void executeEditEvent(String editText) {

                    }

                    @Override
                    public void updatePassword(String oldPassword, String newPassword) {

                    }
                });
                break;
            case R.id.group_clean:
                DialogWithYesOrNoUtils.getInstance().showDialog(mContext, getString(R.string.clean_history), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {
                        if (RongIM.getInstance() != null) {
                            if (mGroup != null) {
                                RongIM.getInstance().clearMessages(Conversation.ConversationType.GROUP, mGroup.getId(), new RongIMClient.ResultCallback<Boolean>() {
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
            case R.id.group_member_size_item:
                Intent intent = new Intent(mContext, TotalGroupMemberActivity.class);
                intent.putExtra("TotalMember", (Serializable) mGroupMember);
                startActivity(intent);
                break;
            case R.id.ll_group_port:
                if (isCreated) {
                    showPhotoDialog();
                }
                break;
            case R.id.ll_group_name:
                if (isCreated) {
                    DialogWithYesOrNoUtils.getInstance().showEditDialog(mContext, getString(R.string.new_group_name), getString(R.string.confirm), new DialogWithYesOrNoUtils.DialogCallBack() {
                        @Override
                        public void executeEvent() {

                        }

                        @Override
                        public void executeEditEvent(String editText) {
                            if (TextUtils.isEmpty(editText)) {
                                return;
                            }
                            if (editText.length() < 2 && editText.length() > 10) {
                                NToast.shortToast(mContext, "群名称应为 2-10 字");
                                return;
                            }
                            newGroupName = editText;
                            LoadDialog.show(mContext);
                            request(UPDATE_GROUP_NAME);
                        }

                        @Override
                        public void updatePassword(String oldPassword, String newPassword) {

                        }
                    });
                }
                break;
            case R.id.group_announcement:
                Intent tempIntent = new Intent(mContext, GroupNoticeActivity.class);
                tempIntent.putExtra("conversationType", Conversation.ConversationType.GROUP.getValue());
                tempIntent.putExtra("targetId", fromConversationId);
                startActivity(tempIntent);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.sw_group_top:
                if (isChecked) {
                    if (mGroup != null) {
                        OperationRong.setConversationTop(mContext, Conversation.ConversationType.GROUP, mGroup.getId(), true);
                    }
                } else {
                    if (mGroup != null) {
                        OperationRong.setConversationTop(mContext, Conversation.ConversationType.GROUP, mGroup.getId(), false);
                    }
                }
                break;
            case R.id.sw_group_notfaction:
                if (isChecked) {
                    if (mGroup != null) {
                        OperationRong.setConverstionNotif(mContext, Conversation.ConversationType.GROUP, mGroup.getId(), true);
                    }
                } else {
                    if (mGroup != null) {
                        OperationRong.setConverstionNotif(mContext, Conversation.ConversationType.GROUP, mGroup.getId(), false);
                    }
                }

                break;
        }
    }


    private class GridAdapter extends BaseAdapter {

        private List<GetGroupMemberResponse.ResultEntity> list;
        Context context;


        public GridAdapter(Context context, List<GetGroupMemberResponse.ResultEntity> list) {
            if (list.size() >= 20) {
                this.list = list.subList(0, 19);
            } else {
                this.list = list;
            }

            this.context = context;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.social_chatsetting_gridview_item, null);
            }
            SelectableRoundedImageView iv_avatar = (SelectableRoundedImageView) convertView.findViewById(R.id.iv_avatar);
            TextView tv_username = (TextView) convertView.findViewById(R.id.tv_username);
            ImageView badge_delete = (ImageView) convertView.findViewById(R.id.badge_delete);

            // 最后一个item，减人按钮
            if (position == getCount() - 1 && isCreated) {
                tv_username.setText("");
                badge_delete.setVisibility(View.GONE);
                iv_avatar.setImageResource(R.drawable.icon_btn_deleteperson);

                iv_avatar.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(GroupDetailActivity.this, SelectFriendsActivity.class);
                        intent.putExtra("DeleteGroupMember", (Serializable) mGroupMember);
                        if (isFromConversation) {
                            intent.putExtra("DeleteGroupId", mGroup.getId());
                        }
                        startActivityForResult(intent, 101);
                    }

                });
            } else if ((isCreated && position == getCount() - 2) || (!isCreated && position == getCount() - 1)) {
                tv_username.setText("");
                badge_delete.setVisibility(View.GONE);
                iv_avatar.setImageResource(R.drawable.jy_drltsz_btn_addperson);

                iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(GroupDetailActivity.this, SelectFriendsActivity.class);
                        intent.putExtra("AddGroupMember", (Serializable) mGroupMember);
                        if (isFromConversation) {
                            intent.putExtra("GroupId", mGroup.getId());
                        }
                        startActivityForResult(intent, 100);

                    }
                });
            } else { // 普通成员
                final GetGroupMemberResponse.ResultEntity bean = list.get(position);
                Friend friend = getUserInfoById(bean.getUser().getId());
                if (friend != null && !TextUtils.isEmpty(friend.getDisplayName())) {
                    tv_username.setText(friend.getDisplayName());
                } else {
                    tv_username.setText(bean.getUser().getNickname());
                }

                if (TextUtils.isEmpty(bean.getUser().getPortraitUri())) {
                    ImageLoader.getInstance().displayImage(RongGenerate.generateDefaultAvatar(bean.getUser().getNickname(), bean.getUser().getId()), iv_avatar, App.getOptions());
                } else {
                    ImageLoader.getInstance().displayImage(bean.getUser().getPortraitUri(), iv_avatar, App.getOptions());
                }
                iv_avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UserInfo userInfo = new UserInfo(bean.getUser().getId(), bean.getUser().getNickname(), Uri.parse(TextUtils.isEmpty(bean.getUser().getPortraitUri()) ? RongGenerate.generateDefaultAvatar(bean.getUser().getNickname(), bean.getUser().getId()) : bean.getUser().getPortraitUri()));
                        Intent intent = new Intent(context, PersonalProfileActivity.class);
                        intent.putExtra("userinfo", userInfo);
                        intent.putExtra("conversationType", Conversation.ConversationType.GROUP.getValue());
                        intent.putExtra("groupinfo", mGroup);
                        context.startActivity(intent);
                    }

                });

            }

            return convertView;
        }

        @Override
        public int getCount() {
            if (isCreated) {
                return list.size() + 2;
            } else {
                return list.size() + 1;
            }
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * 传入新的数据 刷新UI的方法
         */
        public void updateListView(List<GetGroupMemberResponse.ResultEntity> list) {
            this.list = list;
            notifyDataSetChanged();
        }

    }


    // 拿到新增的成员刷新adapter
    @Override
    @SuppressWarnings("unchecked")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            List<FriendInfo> newMemberData = (List<FriendInfo>) data.getSerializableExtra("newAddMember");
            List<FriendInfo> deleMember = (List<FriendInfo>) data.getSerializableExtra("deleteMember");
            if (newMemberData != null && newMemberData.size() > 0) {
                request(GET_GROUP_MEMBER);
            } else if (deleMember != null && deleMember.size() > 0) {
                request(GET_GROUP_MEMBER);
            }

        }
        switch (requestCode) {
            case PhotoUtils.INTENT_CROP:
            case PhotoUtils.INTENT_TAKE:
            case PhotoUtils.INTENT_SELECT:
                photoUtils.onActivityResult(GroupDetailActivity.this, requestCode, resultCode, data);
                break;
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private List<GetGroupMemberResponse.ResultEntity> setCreatedToTop(List<GetGroupMemberResponse.ResultEntity> groupMember) {
        List<GetGroupMemberResponse.ResultEntity> newList = new ArrayList<>();
        GetGroupMemberResponse.ResultEntity created = null;
        for (GetGroupMemberResponse.ResultEntity gr : groupMember) {
            if (gr.getRole() == 0) {
                created = gr;
            } else {
                newList.add(gr);
            }
        }
        if (created != null) {
            newList.add(created);
        }
        Collections.reverse(newList);
        return newList;
    }


    private void setPortraitChangeListener() {
        photoUtils = new PhotoUtils(new PhotoUtils.OnPhotoResultListener() {
            @Override
            public void onPhotoResult(Uri uri) {
                if (uri != null && !TextUtils.isEmpty(uri.getPath())) {
                    selectUri = uri;
                    LoadDialog.show(mContext);
                    request(133);
                }
            }

            @Override
            public void onPhotoCancel() {

            }
        });
    }


    /**
     * 弹出底部框
     */
    private void showPhotoDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new BottomMenuDialog(mContext);
        dialog.setConfirmListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                photoUtils.takePicture(GroupDetailActivity.this);
            }
        });
        dialog.setMiddleListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                photoUtils.selectPicture(GroupDetailActivity.this);
            }
        });
        dialog.show();
    }


    public void uploadImage(final String domain, String imageToken, Uri imagePath) {
        if (TextUtils.isEmpty(domain) && TextUtils.isEmpty(imageToken) && TextUtils.isEmpty(imagePath.toString())) {
            throw new RuntimeException("upload parameter is null!");
        }
        File imageFile = new File(imagePath.getPath());

        if (this.uploadManager == null) {
            this.uploadManager = new UploadManager();
        }
        this.uploadManager.put(imageFile, null, imageToken, new UpCompletionHandler() {

            @Override
            public void complete(String s, ResponseInfo responseInfo, JSONObject jsonObject) {
                if (responseInfo.isOK()) {
                    try {
                        String key = (String) jsonObject.get("key");
                        imageUrl = "http://" + domain + "/" + key;
                        Log.e("uploadImage", imageUrl);
                        if (!TextUtils.isEmpty(imageUrl)) {
                            request(UPDATE_GROUP_HEADER);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, null);
    }

    private void initViews() {
        messageTop = (SwitchButton) findViewById(R.id.sw_group_top);
        messageNotification = (SwitchButton) findViewById(R.id.sw_group_notfaction);
        messageTop.setOnCheckedChangeListener(this);
        messageNotification.setOnCheckedChangeListener(this);
        LinearLayout groupClean = (LinearLayout) findViewById(R.id.group_clean);
        mGridView = (DemoGridView) findViewById(R.id.gridview);
        mTextViewMemberSize = (TextView) findViewById(R.id.group_member_size);
        mGroupHeader = (SelectableRoundedImageView) findViewById(R.id.group_header);
        LinearLayout mGroupDisplayName = (LinearLayout) findViewById(R.id.group_displayname);
        mGroupDisplayNameText = (TextView) findViewById(R.id.group_displayname_text);
        mGroupName = (TextView) findViewById(R.id.group_name);
        Button mQuitBtn = (Button) findViewById(R.id.group_quit);
        Button mDismissBtn = (Button) findViewById(R.id.group_dismiss);
        RelativeLayout totalGroupMember = (RelativeLayout) findViewById(R.id.group_member_size_item);
        LinearLayout mGroupPortL = (LinearLayout) findViewById(R.id.ll_group_port);
        LinearLayout mGroupNameL = (LinearLayout) findViewById(R.id.ll_group_name);
        mGroupNotice = (LinearLayout) findViewById(R.id.group_announcement);
        mGroupPortL.setOnClickListener(this);
        mGroupNameL.setOnClickListener(this);
        totalGroupMember.setOnClickListener(this);
        mGroupDisplayName.setOnClickListener(this);
        mQuitBtn.setOnClickListener(this);
        mDismissBtn.setOnClickListener(this);
        groupClean.setOnClickListener(this);
        mGroupNotice.setOnClickListener(this);
    }

    private Friend getUserInfoById(String userId) {
        if (!TextUtils.isEmpty(userId)) {
            return DBManager.getInstance(mContext).getDaoSession().getFriendDao().queryBuilder().where(FriendDao.Properties.UserId.eq(userId)).unique();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        SealAppContext.getInstance().popActivity(this);
        super.onBackPressed();
    }
}
