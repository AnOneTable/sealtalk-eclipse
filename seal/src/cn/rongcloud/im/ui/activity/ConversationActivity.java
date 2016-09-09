package cn.rongcloud.im.ui.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import cn.rongcloud.im.GroupMemberEngine;
import cn.rongcloud.im.R;
import cn.rongcloud.im.SealAppContext;
import cn.rongcloud.im.model.RongEvent;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.GetGroupMemberResponse;
import cn.rongcloud.im.server.response.GetUserInfoByIdResponse;
import cn.rongcloud.im.server.utils.NLog;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.ui.widget.LoadingDialog;
import io.rong.eventbus.EventBus;
import io.rong.imkit.RongCallKit;
import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.ConversationFragment;
import io.rong.imkit.fragment.UriFragment;
import io.rong.imkit.widget.AlterDialogFragment;
import io.rong.imkit.widget.InputView;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.TypingMessage.TypingStatus;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

//CallKit start 1
//CallKit end 1

/**
 * 会话页面
 * 1，设置 ActionBar title
 * 2，加载会话页面
 * 3，push 和 通知 判断
 */
public class ConversationActivity extends BaseActivity implements RongIMClient.RealTimeLocationListener, View.OnClickListener {

    private static final int GET_USER_INFO = 111;
    private static final int GET_GROUP_MEMBER = 100;
    private String TAG = ConversationActivity.class.getSimpleName();
    /**
     * 对方id
     */
    private String mTargetId;
    /**
     * 会话类型
     */
    private Conversation.ConversationType mConversationType;
    /**
     * title
     */
    private String title;
    /**
     * 是否在讨论组内，如果不在讨论组内，则进入不到讨论组设置页面
     */
    private boolean isFromPush = false;

    private RelativeLayout mRealTimeBar;//real-time bar
    private RealTimeLocationConstant.RealTimeLocationStatus currentLocationStatus;
    private LoadingDialog mDialog;

    private SharedPreferences sp;

    private final String TextTypingTitle = "对方正在输入...";
    private final String VoiceTypingTitle = "对方正在讲话...";

    private Handler mHandler;
    private RongIM.IGroupMemberCallback mMentionMemberCallback;

    public static final int SET_TEXT_TYPING_TITLE = 1;
    public static final int SET_VOICE_TYPING_TITLE = 2;
    public static final int SET_TARGET_ID_TITLE = 0;

    private Button mRinghtButton;

    @Override
    @TargetApi(23)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation);
        sp = getSharedPreferences("config", MODE_PRIVATE);
        mDialog = new LoadingDialog(this);

//        setRightVisibility(View.INVISIBLE);
        mRinghtButton = getBtn_right();

        Intent intent = getIntent();

        if (intent == null || intent.getData() == null)
            return;

        mTargetId = intent.getData().getQueryParameter("targetId");
        //10000 为 Demo Server 加好友的 id，若 targetId 为 10000，则为加好友消息，默认跳转到 NewFriendListActivity
        // Demo 逻辑
        if (mTargetId != null && mTargetId.equals("10000")) {
            startActivity(new Intent(ConversationActivity.this, NewFriendListActivity.class));
            return;
        }
        //intent.getData().getLastPathSegment();//获得当前会话类型
        mConversationType = Conversation.ConversationType.valueOf(intent.getData()
                            .getLastPathSegment().toUpperCase(Locale.getDefault()));

        title = intent.getData().getQueryParameter("title");

        setActionBarTitle(mConversationType, mTargetId);


        if (mConversationType.equals(Conversation.ConversationType.GROUP)) {
            mRinghtButton.setBackground(getResources().getDrawable(R.drawable.icon2_menu));
        } else if (mConversationType.equals(Conversation.ConversationType.PRIVATE) | mConversationType.equals(Conversation.ConversationType.PUBLIC_SERVICE) | mConversationType.equals(Conversation.ConversationType.DISCUSSION)) {
            mRinghtButton.setBackground(getResources().getDrawable(R.drawable.icon1_menu));
        } else {
            mRinghtButton.setVisibility(View.GONE);
            mRinghtButton.setClickable(false);
        }
        mRinghtButton.setOnClickListener(this);

        isPushMessage(intent);

        //地理位置共享，若不是用地理位置共享，可忽略
        setRealTime();

        if ("ConversationActivity".equals(this.getClass().getSimpleName()))
            EventBus.getDefault().register(this);

        // android 6.0 以上版本，监听SDK权限请求，弹出对应请求框。
        if (Build.VERSION.SDK_INT >= 23) {
            RongIM.getInstance().setRequestPermissionListener(new RongIM.RequestPermissionsListener() {
                @Override
                public void onPermissionRequest(String[] permissions, final int requestCode) {
                    for (final String permission : permissions) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(ConversationActivity.this, permission)) {
                        	ActivityCompat.requestPermissions(null, new String[] {permission}, requestCode);
                        } else {
                            int isPermissionGranted = ContextCompat.checkSelfPermission(ConversationActivity.this, permission);
                            if (isPermissionGranted != PackageManager.PERMISSION_GRANTED) {
                                new AlertDialog.Builder(ConversationActivity.this)
                                .setMessage("你需要在设置里打开以下权限:" + permission)
                                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    	ActivityCompat.requestPermissions(ConversationActivity.this, new String[] {permission}, requestCode);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .create().show();
                            }
                            return;
                        }
                    }
                }
            });
        }


        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case SET_TEXT_TYPING_TITLE:
                        setTitle(TextTypingTitle);
                        break;
                    case SET_VOICE_TYPING_TITLE:
                        setTitle(VoiceTypingTitle);
                        break;
                    case SET_TARGET_ID_TITLE:
                        setActionBarTitle(mConversationType, mTargetId);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        RongIMClient.setTypingStatusListener(new RongIMClient.TypingStatusListener() {
            @Override
            public void onTypingStatusChanged(Conversation.ConversationType type, String targetId, Collection<TypingStatus> typingStatusSet) {
                //当输入状态的会话类型和targetID与当前会话一致时，才需要显示
                if (type.equals(mConversationType) && targetId.equals(mTargetId)) {
                    int count = typingStatusSet.size();
                    //count表示当前会话中正在输入的用户数量，目前只支持单聊，所以判断大于0就可以给予显示了
                    if (count > 0) {
                        Iterator iterator = typingStatusSet.iterator();
                        TypingStatus status = (TypingStatus) iterator.next();
                        String objectName = status.getTypingContentType();

                        MessageTag textTag = TextMessage.class.getAnnotation(MessageTag.class);
                        MessageTag voiceTag = VoiceMessage.class.getAnnotation(MessageTag.class);
                        //匹配对方正在输入的是文本消息还是语音消息
                        if (objectName.equals(textTag.value())) {
                            mHandler.sendEmptyMessage(SET_TEXT_TYPING_TITLE);
                        } else if (objectName.equals(voiceTag.value())) {
                            mHandler.sendEmptyMessage(SET_VOICE_TYPING_TITLE);
                        }
                    } else {//当前会话没有用户正在输入，标题栏仍显示原来标题
                        mHandler.sendEmptyMessage(SET_TARGET_ID_TITLE);
                    }
                }
            }
        });

        SealAppContext.getInstance().pushActivity(this);

        RongIM.getInstance().setGroupMembersProvider(new RongIM.IGroupMembersProvider() {
            @Override
            public void getGroupMembers(String groupId, RongIM.IGroupMemberCallback callback) {
                request(GET_GROUP_MEMBER);
                mMentionMemberCallback = callback;
            }
        });

        //CallKit start 2
        RongCallKit.setGroupMemberProvider(new RongCallKit.GroupMembersProvider() {
            @Override
            public ArrayList<String> getMemberList(String groupId, final RongCallKit.OnGroupMembersResult result) {
                (new GroupMemberEngine(ConversationActivity.this)).startEngine(groupId, new GroupMemberEngine.IGroupMembersCallback() {
                    @Override
                    public void onResult(ArrayList<String> members) {
                        result.onGotMemberList(members);
                    }
                });
                return null;
            }
        });
        //CallKit end 2
    }

    /**
     * 判断是否是 Push 消息，判断是否需要做 connect 操作
     */
    private void isPushMessage(Intent intent) {

        if (intent == null || intent.getData() == null)
            return;

        //push
        if (intent.getData().getScheme().equals("rong") && intent.getData().getQueryParameter("isFromPush") != null) {

            //通过intent.getData().getQueryParameter("push") 为true，判断是否是push消息
            if (intent.getData().getQueryParameter("isFromPush").equals("true")) {
                //只有收到系统消息和不落地 push 消息的时候，pushId 不为 null。而且这两种消息只能通过 server 来发送，客户端发送不了。
//                RongIM.getInstance().getRongIMClient().recordNotificationEvent(id);
                if (mDialog != null && !mDialog.isShowing()) {
                    mDialog.show();
                }
                isFromPush = true;
                enterActivity();
            } else if (RongIM.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.DISCONNECTED)) {
                if (mDialog != null && !mDialog.isShowing()) {
                    mDialog.show();
                }
                if (intent.getData().getPath().contains("conversation/system")) {
                    Intent intent1 = new Intent(mContext, MainActivity.class);
                    intent1.putExtra("systemconversation", true);
                    startActivity(intent1);
                    finish();
                    return;
                }
                enterActivity();
            } else {
                if (intent.getData().getPath().contains("conversation/system")) {
                    Intent intent1 = new Intent(mContext, MainActivity.class);
                    intent1.putExtra("systemconversation", true);
                    startActivity(intent1);
                    finish();
                    return;
                }
                enterFragment(mConversationType, mTargetId);
            }

        } else {
            if (RongIM.getInstance().getCurrentConnectionStatus().equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.DISCONNECTED)) {
                if (mDialog != null && !mDialog.isShowing()) {
                    mDialog.show();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enterActivity();
                    }
                }, 300);
            } else {
                enterFragment(mConversationType, mTargetId);
            }
        }
    }


    /**
     * 收到 push 消息后，选择进入哪个 Activity
     * 如果程序缓存未被清理，进入 MainActivity
     * 程序缓存被清理，进入 LoginActivity，重新获取token
     * <p/>
     * 作用：由于在 manifest 中 intent-filter 是配置在 ConversationActivity 下面，所以收到消息后点击notifacition 会跳转到 DemoActivity。
     * 以跳到 MainActivity 为例：
     * 在 ConversationActivity 收到消息后，选择进入 MainActivity，这样就把 MainActivity 激活了，当你读完收到的消息点击 返回键 时，程序会退到
     * MainActivity 页面，而不是直接退回到 桌面。
     */
    private void enterActivity() {

        String token = sp.getString("loginToken", "");//loginToken

        if (token.equals("default")) {
            NLog.e("ConversationActivity push", "push2");
            startActivity(new Intent(ConversationActivity.this, LoginActivity.class));
            finish();
        } else {
            NLog.e("ConversationActivity push", "push3");
            reconnect(token);
        }
    }

    private void reconnect(String token) {

        RongIM.connect(token, new RongIMClient.ConnectCallback() {
            @Override
            public void onTokenIncorrect() {

                Log.e(TAG, "---onTokenIncorrect--");
            }

            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "---onSuccess--" + s);
                NLog.e("ConversationActivity push", "push4");

                if (mDialog != null)
                    mDialog.dismiss();

//
//                Intent intent = new Intent();
//                intent.setClass(ConversationActivity.this, MainActivity.class);
//                intent.putExtra("PUSH_CONVERSATIONTYPE", mConversationType.toString());
//                intent.putExtra("PUSH_TARGETID", mTargetId);
//                startActivity(intent);
//                finish();

                enterFragment(mConversationType, mTargetId);

            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                Log.e(TAG, "---onError--" + e);
                if (mDialog != null)
                    mDialog.dismiss();

                enterFragment(mConversationType, mTargetId);
            }
        });

    }

    private ConversationFragment fragment;

    /**
     * 加载会话页面 ConversationFragment
     *
     * @param mConversationType 会话类型
     * @param mTargetId         会话 Id
     */
    private void enterFragment(Conversation.ConversationType mConversationType, String mTargetId) {

        fragment = new ConversationFragment();

        Uri uri = Uri.parse("rong://" + getApplicationInfo().packageName).buildUpon()
                  .appendPath("conversation").appendPath(mConversationType.getName().toLowerCase())
                  .appendQueryParameter("targetId", mTargetId).build();

        fragment.setUri(uri);
        fragment.setInputBoardListener(new InputView.IInputBoardListener() {
            @Override
            public void onBoardExpanded(int height) {
                NLog.e(TAG, "onBoardExpanded h : " + height);
            }

            @Override
            public void onBoardCollapsed() {
                NLog.e(TAG, "onBoardCollapsed");
            }
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //xxx 为你要加载的 id
        transaction.add(R.id.rong_content, fragment);
        transaction.commitAllowingStateLoss();
    }


    /**
     * 设置会话页面 Title
     *
     * @param conversationType 会话类型
     * @param targetId         目标 Id
     */
    private void setActionBarTitle(Conversation.ConversationType conversationType, String targetId) {

        if (conversationType == null)
            return;

        if (conversationType.equals(Conversation.ConversationType.PRIVATE)) {
            setPrivateActionBar(targetId);
        } else if (conversationType.equals(Conversation.ConversationType.GROUP)) {
            setGroupActionBar(targetId);
        } else if (conversationType.equals(Conversation.ConversationType.DISCUSSION)) {
            setDiscussionActionBar(targetId);
        } else if (conversationType.equals(Conversation.ConversationType.CHATROOM)) {
            setTitle(title);
        } else if (conversationType.equals(Conversation.ConversationType.SYSTEM)) {
            setTitle(R.string.de_actionbar_system);
        } else if (conversationType.equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
            setAppPublicServiceActionBar(targetId);
        } else if (conversationType.equals(Conversation.ConversationType.PUBLIC_SERVICE)) {
            setPublicServiceActionBar(targetId);
        } else if (conversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
            setTitle(R.string.main_customer);
        } else {
            setTitle(R.string.de_actionbar_sub_defult);
        }

    }

    /**
     * 设置群聊界面 ActionBar
     *
     * @param targetId 会话 Id
     */
    private void setGroupActionBar(String targetId) {
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        } else {
            setTitle(targetId);
        }
    }

    /**
     * 设置应用公众服务界面 ActionBar
     */
    private void setAppPublicServiceActionBar(String targetId) {
        if (targetId == null)
            return;

        RongIM.getInstance().getPublicServiceProfile(Conversation.PublicServiceType.APP_PUBLIC_SERVICE
        , targetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
            @Override
            public void onSuccess(PublicServiceProfile publicServiceProfile) {
                setTitle(publicServiceProfile.getName());
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
    }

    /**
     * 设置公共服务号 ActionBar
     */
    private void setPublicServiceActionBar(String targetId) {

        if (targetId == null)
            return;


        RongIM.getInstance().getPublicServiceProfile(Conversation.PublicServiceType.PUBLIC_SERVICE
        , targetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
            @Override
            public void onSuccess(PublicServiceProfile publicServiceProfile) {
                setTitle(publicServiceProfile.getName());
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
    }

    /**
     * 设置讨论组界面 ActionBar
     */
    private void setDiscussionActionBar(String targetId) {

        if (targetId != null) {

            RongIM.getInstance().getDiscussion(targetId
            , new RongIMClient.ResultCallback<Discussion>() {
                @Override
                public void onSuccess(Discussion discussion) {
                    setTitle(discussion.getName());
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    if (e.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION)) {
                        setTitle("不在讨论组中");
                        supportInvalidateOptionsMenu();
                    }
                }
            });
        } else {
            setTitle("讨论组");
        }
    }


    /**
     * 设置私聊界面 ActionBar
     */
    private void setPrivateActionBar(String targetId) {
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        } else {
            setTitle(targetId);
        }
    }

    /**
     * 根据 targetid 和 ConversationType 进入到设置页面
     */
    private void enterSettingActivity() {

        if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE
                || mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE) {

            RongIM.getInstance().startPublicServiceProfile(this, mConversationType, mTargetId);
        } else {
            UriFragment fragment = (UriFragment) getSupportFragmentManager().getFragments().get(0);
            //得到讨论组的 targetId
            mTargetId = fragment.getUri().getQueryParameter("targetId");

            if (TextUtils.isEmpty(mTargetId)) {
                NToast.shortToast(mContext, "讨论组尚未创建成功");
            }


            Intent intent = null;
            if (mConversationType == Conversation.ConversationType.GROUP) {
                intent = new Intent(this, GroupDetailActivity.class);
            } else if (mConversationType == Conversation.ConversationType.PRIVATE) {
                intent = new Intent(this, FriendDetailActivity.class);
            } else if (mConversationType == Conversation.ConversationType.DISCUSSION) {
                intent = new Intent(this, DiscussionDetailActivity.class);
                intent.putExtra("TargetId", mTargetId);
                startActivityForResult(intent, 166);
                return;
            }
            intent.putExtra("TargetId", mTargetId);
            if (intent != null) {
                startActivityForResult(intent, 500);
            }

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if (data!=null && data.getStringExtra("disFinish").equals("disFinish")) {
//            finish();
//        }
        if (resultCode == 501) {
            finish();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        showRealTimeLocationBar(null);
    }


    /*－－－－－－－－－－－－－地理位置共享 start－－－－－－－－－*/

    private void setRealTime() {

        mRealTimeBar = (RelativeLayout) this.findViewById(R.id.layout);

        mRealTimeBar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (currentLocationStatus == null)
                    currentLocationStatus = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

                if (currentLocationStatus == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {

                    final AlterDialogFragment alterDialogFragment = AlterDialogFragment.newInstance("", "加入位置共享", "取消", "加入");
                    alterDialogFragment.setOnAlterDialogBtnListener(new AlterDialogFragment.AlterDialogBtnListener() {

                        @Override
                        public void onDialogPositiveClick(AlterDialogFragment dialog) {
                            RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

                            if (status == null || status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_IDLE) {
                                startRealTimeLocation();
                            } else {
                                joinRealTimeLocation();
                            }

                        }

                        @Override
                        public void onDialogNegativeClick(AlterDialogFragment dialog) {
                            alterDialogFragment.dismiss();
                        }
                    });
                    alterDialogFragment.show(getSupportFragmentManager());

                } else {
                    Intent intent = new Intent(ConversationActivity.this, RealTimeLocationActivity.class);
                    intent.putExtra("conversationType", mConversationType.getValue());
                    intent.putExtra("targetId", mTargetId);
                    startActivity(intent);
                }
            }
        });

        if (!TextUtils.isEmpty(mTargetId) && mConversationType != null) {

            RealTimeLocationConstant.RealTimeLocationErrorCode errorCode = RongIMClient.getInstance().getRealTimeLocation(mConversationType, mTargetId);
            if (errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_SUCCESS || errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_IS_ON_GOING) {
                RongIMClient.getInstance().addRealTimeLocationListener(mConversationType, mTargetId, this);//设置监听
                currentLocationStatus = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

                if (currentLocationStatus == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
                    showRealTimeLocationBar(currentLocationStatus);
                }
            }
        }


    }

    //real-time location method beign

    private void startRealTimeLocation() {
        RongIMClient.getInstance().startRealTimeLocation(mConversationType, mTargetId);
        Intent intent = new Intent(ConversationActivity.this, RealTimeLocationActivity.class);
        intent.putExtra("conversationType", mConversationType.getValue());
        intent.putExtra("targetId", mTargetId);
        startActivity(intent);
    }

    private void joinRealTimeLocation() {
        RongIMClient.getInstance().joinRealTimeLocation(mConversationType, mTargetId);
        Intent intent = new Intent(ConversationActivity.this, RealTimeLocationActivity.class);
        intent.putExtra("conversationType", mConversationType.getValue());
        intent.putExtra("targetId", mTargetId);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            closeRealTimeLocation();
        }
        if (isFromPush) {
            isFromPush = false;
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }


    private boolean closeRealTimeLocation() {

        if (mConversationType == null || TextUtils.isEmpty(mTargetId))
            return false;

        if (mConversationType != null && !TextUtils.isEmpty(mTargetId)) {

            RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

            if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_IDLE || status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
                return false;
            }
        }

        final AlterDialogFragment alterDialogFragment =
            AlterDialogFragment.newInstance(getApplicationContext().getString(R.string.prompt),
                                            getApplicationContext().getString(R.string.location_sharing_exit_promt),
                                            getApplicationContext().getString(R.string.cancel),
                                            getApplicationContext().getString(R.string.confirm));
        alterDialogFragment.setOnAlterDialogBtnListener(new AlterDialogFragment.AlterDialogBtnListener() {
            @Override
            public void onDialogPositiveClick(AlterDialogFragment dialog) {
                RongIMClient.getInstance().quitRealTimeLocation(mConversationType, mTargetId);
                SealAppContext.getInstance().popAllActivity();
            }

            @Override
            public void onDialogNegativeClick(AlterDialogFragment dialog) {
                alterDialogFragment.dismiss();
            }
        });
        alterDialogFragment.show(getSupportFragmentManager());

        return true;
    }


    private String locationid;

    private void showRealTimeLocationBar(RealTimeLocationConstant.RealTimeLocationStatus status) {

        if (status == null)
            status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);

        final List<String> userIds = RongIMClient.getInstance().getRealTimeLocationParticipants(mConversationType, mTargetId);

        if (status != null && status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {

            if (userIds != null && userIds.get(0) != null && userIds.size() == 1) {
                locationid = userIds.get(0);
                request(GET_USER_INFO);
            } else {
                if (userIds != null && userIds.size() > 0) {
                    if (mRealTimeBar != null) {
                        TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
                        if (userIds.size() <= 1) {
                            textView.setText(userIds.size() + " " + getApplicationContext().getString(R.string.person_is_sharing_loaction));
                        } else {
                            textView.setText(userIds.size() + " " + getApplicationContext().getString(R.string.persons_are_sharing_loaction));
                        }
                    }
                } else {
                    if (mRealTimeBar != null && mRealTimeBar.getVisibility() == View.VISIBLE) {
                        mRealTimeBar.setVisibility(View.GONE);
                    }
                }
            }

        } else if (status != null && status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_OUTGOING) {
            TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
            textView.setText(getApplicationContext().getString(R.string.you_are_sharing_location));
        } else {

            if (mRealTimeBar != null && userIds != null) {
                TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
                if (userIds.size() <= 1) {
                    textView.setText(userIds.size() + " " + getApplicationContext().getString(R.string.person_is_sharing_loaction));
                } else {
                    textView.setText(userIds.size() + " " + getApplicationContext().getString(R.string.persons_are_sharing_loaction));
                }
            }
        }

        if (userIds != null && userIds.size() > 0) {

            if (mRealTimeBar != null && mRealTimeBar.getVisibility() == View.GONE) {
                mRealTimeBar.setVisibility(View.VISIBLE);
            }
        } else {

            if (mRealTimeBar != null && mRealTimeBar.getVisibility() == View.VISIBLE) {
                mRealTimeBar.setVisibility(View.GONE);
            }
        }

    }

    public void onEventMainThread(RongEvent.RealTimeLocationMySelfJoinEvent event) {

        onParticipantsJoin(RongIM.getInstance().getCurrentUserId());
    }

    private void hideRealTimeBar() {
        if (mRealTimeBar != null) {
            mRealTimeBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if ("ConversationActivity".equals(this.getClass().getSimpleName()))
            EventBus.getDefault().unregister(this);

        //CallKit start 3
        RongCallKit.setGroupMemberProvider(null);
        //CallKit end 3

        RongIM.getInstance().setGroupMembersProvider(null);
        RongIM.getInstance().setRequestPermissionListener(null);
        RongIMClient.setTypingStatusListener(null);
        super.onDestroy();
    }


    @Override
    public void onStatusChange(final RealTimeLocationConstant.RealTimeLocationStatus status) {
        currentLocationStatus = status;

        EventBus.getDefault().post(status);

        if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_IDLE) {
            hideRealTimeBar();

            RealTimeLocationConstant.RealTimeLocationErrorCode errorCode = RongIMClient.getInstance().getRealTimeLocation(mConversationType, mTargetId);

            if (errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_SUCCESS) {
                RongIM.getInstance().insertMessage(mConversationType, mTargetId, RongIM.getInstance().getCurrentUserId(),
                                                   InformationNotificationMessage.obtain(getApplicationContext().getString(R.string.location_sharing_ended)));
            }
        } else if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_OUTGOING) {//发自定义消息
            showRealTimeLocationBar(status);
        } else if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
            showRealTimeLocationBar(status);
        } else if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_CONNECTED) {
            showRealTimeLocationBar(status);
        }

    }


    @Override
    public void onReceiveLocation(double latitude, double longitude, String userId) {
        Log.e(TAG, "userid = " + userId + ", lat: " + latitude + " long :" + longitude);
        EventBus.getDefault().post(RongEvent.RealTimeLocationReceiveEvent.obtain(userId, latitude, longitude));
    }

    @Override
    public void onParticipantsJoin(String userId) {
        EventBus.getDefault().post(RongEvent.RealTimeLocationJoinEvent.obtain(userId));

        if (RongIMClient.getInstance().getCurrentUserId().equals(userId)) {
            showRealTimeLocationBar(null);
        }
    }

    @Override
    public void onParticipantsQuit(String userId) {
        EventBus.getDefault().post(RongEvent.RealTimeLocationQuitEvent.obtain(userId));
    }

    @Override
    public void onError(RealTimeLocationConstant.RealTimeLocationErrorCode errorCode) {
        Log.e(TAG, "onError:---" + errorCode);
    }

    /*－－－－－－－－－－－－－地理位置共享 end－－－－－－－－－*/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
            if (!closeRealTimeLocation()) {
                if (fragment != null && !fragment.onBackPressed()) {
                    if (isFromPush) {
                        isFromPush = false;
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    SealAppContext.getInstance().popAllActivity();
                }
            }
        }
        return false;
    }


    private void hintKbTwo() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case GET_USER_INFO:
                return action.getUserInfoById(locationid);
            case GET_GROUP_MEMBER:
                return action.getGroupMember(mTargetId);
        }
        return super.doInBackground(requestCode, id);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case GET_USER_INFO:
                    GetUserInfoByIdResponse res = (GetUserInfoByIdResponse) result;
                    if (res.getCode() == 200) {
                        TextView textView = (TextView) mRealTimeBar.findViewById(android.R.id.text1);
                        textView.setText(res.getResult().getNickname() + " 正在共享位置");
                    }
                    break;
                case GET_GROUP_MEMBER:
                    GetGroupMemberResponse response = (GetGroupMemberResponse) result;
                    if (response.getCode() == 200) {
                        List<GetGroupMemberResponse.ResultEntity> list = response.getResult();
                        List<UserInfo> userInfos = new ArrayList<>();
                        for (GetGroupMemberResponse.ResultEntity entity : list) {
                            GetGroupMemberResponse.ResultEntity.UserEntity user = entity.getUser();
                            if (!TextUtils.isEmpty(entity.getDisplayName())) {
                                userInfos.add(new UserInfo(user.getId(), entity.getDisplayName(), Uri.parse(user.getPortraitUri())));
                            } else {
                                userInfos.add(new UserInfo(user.getId(), user.getNickname(), Uri.parse(user.getPortraitUri())));
                            }
                        }
                        mMentionMemberCallback.onGetGroupMembersResult(userInfos);
                    }
            }

        }
    }

    @Override
    public void onClick(View v) {
        enterSettingActivity();
    }

    @Override
    public void onLeftClick(View v) {
        hintKbTwo();
        SealAppContext.getInstance().popAllActivity();
//        super.onLeftClick(v);
    }
}
