package cn.rongcloud.im.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import cn.rongcloud.im.R;
import cn.rongcloud.im.server.widget.DialogWithYesOrNoUtils;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.utils.AndroidEmoji;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;

@SuppressWarnings("deprecation")
public class GroupNoticeActivity extends Activity implements View.OnClickListener, TextWatcher {
    TextView mCancelButton;
    TextView mDoneButton;
    EditText mEdit;
    Conversation.ConversationType mConversationType;
    String mTargetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_group_notice);
        mCancelButton = (TextView) findViewById(R.id.cancel);
        mDoneButton = (TextView) findViewById(R.id.done);
        mEdit = (EditText) findViewById(R.id.edit_area);
        Intent intent = getIntent();
        mConversationType = Conversation.ConversationType.setValue(intent.getIntExtra("conversationType", 0));
        mTargetId = getIntent().getStringExtra("targetId");
        mCancelButton.setOnClickListener(this);
        mDoneButton.setOnClickListener(this);
        mDoneButton.setClickable(false);
        mEdit.addTextChangedListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                DialogWithYesOrNoUtils.getInstance().showDialog(this, getString(R.string.group_notice_exist_confirm), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {
                        finish();
                    }

                    @Override
                    public void executeEditEvent(String editText) {

                    }

                    @Override
                    public void updatePassword(String oldPassword, String newPassword) {

                    }
                });
                break;
            case R.id.done:
                DialogWithYesOrNoUtils.getInstance().showDialog(this, getString(R.string.group_notice_post_confirm), new DialogWithYesOrNoUtils.DialogCallBack() {
                    @Override
                    public void executeEvent() {
                        TextMessage textMessage = TextMessage.obtain(RongContext.getInstance().getString(R.string.group_notice_prefix) + mEdit.getText().toString());
                        MentionedInfo mentionedInfo = new MentionedInfo(MentionedInfo.MentionedType.ALL, null, null);
                        textMessage.setMentionedInfo(mentionedInfo);

                        RongIM.getInstance().sendMessage(Message.obtain(mTargetId, mConversationType, textMessage), null, null, new IRongCallback.ISendMessageCallback() {
                            @Override
                            public void onAttached(Message message) {

                            }

                            @Override
                            public void onSuccess(Message message) {

                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode errorCode) {

                            }
                        });

                        finish();
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
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().length() > 0) {
            mDoneButton.setClickable(true);
            mDoneButton.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            mDoneButton.setClickable(false);
            mDoneButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if ( s != null) {
            int start = mEdit.getSelectionStart();
            int end = mEdit.getSelectionEnd();
            mEdit.removeTextChangedListener(this);
            mEdit.setText(AndroidEmoji.ensure(s.toString()));
            mEdit.addTextChangedListener(this);
            mEdit.setSelection(start, end);
        }
    }
}
