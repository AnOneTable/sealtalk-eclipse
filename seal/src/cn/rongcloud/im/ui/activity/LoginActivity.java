package cn.rongcloud.im.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cn.rongcloud.im.R;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.Friend;
import cn.rongcloud.im.db.Groups;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.GetGroupResponse;
import cn.rongcloud.im.server.response.GetTokenResponse;
import cn.rongcloud.im.server.response.GetUserInfoByIdResponse;
import cn.rongcloud.im.server.response.LoginResponse;
import cn.rongcloud.im.server.response.SyncTotalDataResponse;
import cn.rongcloud.im.server.response.UserRelationshipResponse;
import cn.rongcloud.im.server.utils.AMUtils;
import cn.rongcloud.im.server.utils.CommonUtils;
import cn.rongcloud.im.server.utils.NLog;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.ClearWriteEditText;
import cn.rongcloud.im.server.widget.LoadDialog;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;

/**
 * Created by AMing on 16/1/15.
 * Company RongCloud
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final int LOGIN = 5;
    private static final int GET_TOKEN = 6;
    private static final int SYNC_USER_INFO = 9;
    private static final int SYNC_GROUP = 17;
    private static final int AUTO_LOGIN = 19;
    private static final int SYNC_FRIEND = 14;
    private static final int SYNC_TOTAL_DATA = 20;


    private ImageView mImg_Background;
    private ClearWriteEditText mPhoneEdit, mPasswordEdit;
    private String phoneString;
    private String passwordString;
    private String connectResultId;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private List<GetGroupResponse.ResultEntity> groupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setHeadVisibility(View.GONE);
        sp = getSharedPreferences("config", MODE_PRIVATE);
        editor = sp.edit();
        initView();
    }

    private void initView() {
        mPhoneEdit = (ClearWriteEditText) findViewById(R.id.de_login_phone);
        mPasswordEdit = (ClearWriteEditText) findViewById(R.id.de_login_password);
        Button mConfirm = (Button) findViewById(R.id.de_login_sign);
        TextView mRegister = (TextView) findViewById(R.id.de_login_register);
        TextView forgetPassword = (TextView) findViewById(R.id.de_login_forgot);
        forgetPassword.setOnClickListener(this);
        mConfirm.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        mImg_Background = (ImageView) findViewById(R.id.de_img_backgroud);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.translate_anim);
                mImg_Background.startAnimation(animation);
            }
        }, 200);
        mPhoneEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 11) {
                    AMUtils.onInactive(mContext, mPhoneEdit);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        String oldPhone = sp.getString("loginphone", "");
        String oldPassword = sp.getString("loginpassword", "");
        if (!oldPhone.equals(mPhoneEdit.getText().toString().trim())) {
            //和上次登录账户不一致 或者 换设备登录  重新网络拉取好友 和 群组数据
            DBManager.getInstance(mContext).getDaoSession().getFriendDao().deleteAll();//清空上个用户的数据库
            DBManager.getInstance(mContext).getDaoSession().getGroupsDao().deleteAll();
        }
        if (!TextUtils.isEmpty(oldPhone) && !TextUtils.isEmpty(oldPassword)) {
            mPhoneEdit.setText(oldPhone);
            mPasswordEdit.setText(oldPassword);
        }


        if (!sp.getBoolean("exit", false) && !TextUtils.isEmpty(oldPhone) && !TextUtils.isEmpty(oldPassword)) {
            editor.putBoolean("exit", false);
            editor.apply();
            phoneString = oldPhone;
            passwordString = oldPassword;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LoadDialog.show(mContext);
                    request(AUTO_LOGIN);
                }
            }, 100);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.de_login_sign:
                phoneString = mPhoneEdit.getText().toString().trim();
                passwordString = mPasswordEdit.getText().toString().trim();

                if (TextUtils.isEmpty(phoneString)) {
                    NToast.shortToast(mContext, R.string.phone_number_is_null);
                    mPhoneEdit.setShakeAnimation();
                    return;
                }

//                if (!AMUtils.isMobile(phoneString)) {
//                    NToast.shortToast(mContext, R.string.Illegal_phone_number);
//                    mPhoneEdit.setShakeAnimation();
//                    return;
//                }

                if (TextUtils.isEmpty(passwordString)) {
                    NToast.shortToast(mContext, R.string.password_is_null);
                    mPasswordEdit.setShakeAnimation();
                    return;
                }
                if (passwordString.contains(" ")) {
                    NToast.shortToast(mContext, R.string.password_cannot_contain_spaces);
                    mPasswordEdit.setShakeAnimation();
                    return;
                }
                LoadDialog.show(mContext);
                editor.putBoolean("exit", false);
                editor.apply();
                request(LOGIN);
                break;
            case R.id.de_login_register:
                startActivityForResult(new Intent(this, RegisterActivity.class), 1);
                break;
            case R.id.de_login_forgot:
                startActivityForResult(new Intent(this, ForgetPasswordActivity.class), 2);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && data != null) {
            String phone = data.getStringExtra("phone");
            String password = data.getStringExtra("password");
            mPhoneEdit.setText(phone);
            mPasswordEdit.setText(password);
        } else if (data != null && requestCode == 1) {
            String phone = data.getStringExtra("phone");
            String password = data.getStringExtra("password");
            String id = data.getStringExtra("id");
            String nickname = data.getStringExtra("nickname");
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(id) && !TextUtils.isEmpty(nickname)) {
                mPhoneEdit.setText(phone);
                mPasswordEdit.setText(password);
                editor.putString("loginphone", phone);
                editor.putString("loginpassword", password);
                editor.putString("loginid", id);
                editor.putString("loginnickname", nickname);
                editor.apply();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case LOGIN:
                return action.login("86", phoneString, passwordString);
            case AUTO_LOGIN:
                return action.login("86", phoneString, passwordString);
            case GET_TOKEN:
                return action.getToken();
            case SYNC_USER_INFO:
                return action.getUserInfoById(connectResultId);
            case SYNC_GROUP:
                return action.getGroups();
            case SYNC_FRIEND:
                return action.getAllUserRelationship();
            case SYNC_TOTAL_DATA:
                return action.syncTotalData("0");
        }
        return null;
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            String loginToken;
            switch (requestCode) {
                case LOGIN:
                    LoginResponse loginResponse = (LoginResponse) result;
                    if (loginResponse.getCode() == 200) {
                        loginToken = loginResponse.getResult().getToken();
                        if (!TextUtils.isEmpty(loginToken)) {
                            editor.putString("loginToken", loginToken);
                            editor.putString("loginphone", phoneString);
                            editor.putString("loginpassword", passwordString);
                            editor.apply();

                            RongIM.connect(loginToken, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    NLog.e("connect", "onTokenIncorrect");
                                    reGetToken();
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString("loginid", s);
                                    editor.apply();
                                    request(SYNC_USER_INFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    NLog.e("connect", "onError errorcode:" + errorCode.getValue());
                                }
                            });
                        }
                    } else if (loginResponse.getCode() == 100) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    } else if (loginResponse.getCode() == 1000) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    }
                    break;
                case AUTO_LOGIN:
                    LoginResponse autoLoginResponse = (LoginResponse) result;
                    if (autoLoginResponse.getCode() == 200) {
                        loginToken = autoLoginResponse.getResult().getToken();
                        if (!TextUtils.isEmpty(loginToken)) {
                            editor.putString("loginToken", loginToken);
                            editor.putString("loginphone", phoneString);
                            editor.putString("loginpassword", passwordString);
                            editor.apply();

                            RongIM.connect(loginToken, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    reGetToken();
                                    NLog.e("connect", "onTokenIncorrect");
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString("loginid", s);
                                    editor.apply();
                                    request(SYNC_USER_INFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    NLog.e("connect", "onError errorcode:" + errorCode.getValue());
                                }
                            });
                        }
                    } else if (autoLoginResponse.getCode() == 100) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    } else if (autoLoginResponse.getCode() == 1000) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, R.string.phone_or_psw_error);
                    }
                    break;
                case SYNC_USER_INFO:
                    GetUserInfoByIdResponse userInfoByIdResponse = (GetUserInfoByIdResponse) result;
                    if (userInfoByIdResponse.getCode() == 200) {
                        editor.putString("loginnickname", userInfoByIdResponse.getResult().getNickname());
                        editor.putString("loginPortrait", userInfoByIdResponse.getResult().getPortraitUri());
                        editor.apply();
                        if (TextUtils.isEmpty(userInfoByIdResponse.getResult().getPortraitUri())) {
                            userInfoByIdResponse.getResult().setPortraitUri(RongGenerate.generateDefaultAvatar(userInfoByIdResponse.getResult().getNickname(), userInfoByIdResponse.getResult().getId()));
                        }
                        request(SYNC_GROUP);
                    }
                    break;
                case SYNC_GROUP:
                    GetGroupResponse groupResponse = (GetGroupResponse) result;
                    if (groupResponse.getCode() == 200) {
                        groupList = groupResponse.getResult();
                        if (groupList.size() > 0) {
                            for (GetGroupResponse.ResultEntity g : groupList) {
                                DBManager.getInstance(mContext).getDaoSession().getGroupsDao().insertOrReplace(
                                    new Groups(g.getGroup().getId(), g.getGroup().getName(), g.getGroup().getPortraitUri(), String.valueOf(g.getRole()))
                                );
                            }
                        }
                        request(SYNC_FRIEND);
                    }
                    break;
                case SYNC_FRIEND:
                    UserRelationshipResponse userRelationshipResponse = (UserRelationshipResponse) result;
                    if (userRelationshipResponse.getCode() == 200) {
                        List<UserRelationshipResponse.ResultEntity> list = userRelationshipResponse.getResult();
                        if (list != null && list.size() > 0) {
                            for (UserRelationshipResponse.ResultEntity friend : list) {
                                if (friend.getStatus() == 20) {
                                    DBManager.getInstance(mContext).getDaoSession().getFriendDao().insertOrReplace(new Friend(
                                                friend.getUser().getId(),
                                                friend.getUser().getNickname(),
                                                friend.getUser().getPortraitUri(),
                                                friend.getDisplayName(),
                                                null,
                                                null
                                            ));
                                }
                            }

                        }
                        request(SYNC_TOTAL_DATA);
                    }
                    break;
                case GET_TOKEN:
                    GetTokenResponse tokenResponse = (GetTokenResponse) result;
                    if (tokenResponse.getCode() == 200) {
                        String token = tokenResponse.getResult().getToken();
                        if (!TextUtils.isEmpty(token)) {
                            RongIM.connect(token, new RongIMClient.ConnectCallback() {
                                @Override
                                public void onTokenIncorrect() {
                                    Log.e("LoginActivity", "reToken Incorrect");
                                }

                                @Override
                                public void onSuccess(String s) {
                                    connectResultId = s;
                                    NLog.e("connect", "onSuccess userid:" + s);
                                    editor.putString("loginid", s);
                                    editor.apply();

                                    request(SYNC_USER_INFO, true);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {

                                }
                            });
                        }
                    }

                    break;
                case SYNC_TOTAL_DATA:
                    SyncTotalDataResponse syncTotalDataResponse = (SyncTotalDataResponse) result;
                    if (syncTotalDataResponse.getCode() == 200) {
                        List<SyncTotalDataResponse.ResultEntity.GroupMembersEntity> groupMembersEntityList = syncTotalDataResponse.getResult().getGroup_members();
                        if (groupMembersEntityList.size() > 0) {

                        }
                        List<SyncTotalDataResponse.ResultEntity.FriendsEntity> friendsEntityList = syncTotalDataResponse.getResult().getFriends();
                        if (friendsEntityList.size() > 0) {

                        }
                        List<SyncTotalDataResponse.ResultEntity.GroupsEntity> groupsEntityList = syncTotalDataResponse.getResult().getGroups();
                        if (groupsEntityList.size() > 0) {
                        }
                        LoadDialog.dismiss(mContext);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        NToast.shortToast(mContext, R.string.login_success);
                        finish();
                    }
                    break;
            }
        }
    }

    private void reGetToken() {
        request(GET_TOKEN);
    }

    @Override
    public void onFailure(int requestCode, int state, Object result) {
        if (!CommonUtils.isNetworkConnected(mContext)) {
            NToast.shortToast(mContext, getString(R.string.network_not_available));
            return;
        }
        switch (requestCode) {
            case LOGIN:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.login_api_fail);
                break;
            case SYNC_USER_INFO:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.sync_userinfo_api_fail);
                break;
            case GET_TOKEN:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, R.string.get_token_api_fail);
                break;
            case SYNC_GROUP:
                NToast.shortToast(mContext, R.string.sync_group_api_fail);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
