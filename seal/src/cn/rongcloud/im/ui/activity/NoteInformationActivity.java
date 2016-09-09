package cn.rongcloud.im.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import cn.rongcloud.im.R;
import cn.rongcloud.im.SealAppContext;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.server.broadcast.BroadcastManager;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.pinyin.FriendInfo;
import cn.rongcloud.im.server.response.SetFriendDisplayNameResponse;
import cn.rongcloud.im.server.widget.LoadDialog;
import io.rong.imkit.RongIM;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/8/10.
 * Company RongCloud
 */
@SuppressWarnings("deprecation")
public class NoteInformationActivity extends BaseActivity {

    private static final int SET_DISPLAYNAME = 12;
    private FriendInfo mFriendInfo;
    private EditText mNoteEdit;
    private TextView mNoteSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noteinfo);
        setHeadVisibility(View.GONE);
        mNoteEdit = (EditText) findViewById(R.id.notetext);
        mNoteSave = (TextView) findViewById(R.id.notesave);
        mFriendInfo = (FriendInfo) getIntent().getSerializableExtra("friendInfo");
        if (mFriendInfo != null) {
            mNoteSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LoadDialog.show(mContext);
                    request(SET_DISPLAYNAME);
                }
            });
            mNoteSave.setClickable(false);
            mNoteEdit.setText(mFriendInfo.getDisplayName());
            mNoteEdit.setSelection(mNoteEdit.getText().length());
            mNoteEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (TextUtils.isEmpty(s.toString())) {
                        mNoteSave.setClickable(false);
                        mNoteSave.setTextColor(Color.parseColor("#9fcdfd"));
                    } else if (s.toString().equals(mFriendInfo.getDisplayName())) {
                        mNoteSave.setClickable(false);
                        mNoteSave.setTextColor(Color.parseColor("#9fcdfd"));
                    } else {
                        mNoteSave.setClickable(true);
                        mNoteSave.setTextColor(getResources().getColor(R.color.white));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });


        }
    }

    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        if (requestCode == SET_DISPLAYNAME) {
            return action.setFriendDisplayName(mFriendInfo.getUserId(), mNoteEdit.getText().toString().trim());
        }
        return super.doInBackground(requestCode, id);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            if (requestCode == SET_DISPLAYNAME) {
                SetFriendDisplayNameResponse response = (SetFriendDisplayNameResponse) result;
                if (response.getCode() == 200) {
                    DBManager.getInstance(mContext).getDaoSession().getFriendDao().insertOrReplace(new cn.rongcloud.im.db.Friend(mFriendInfo.getUserId(), mFriendInfo.getName(), mFriendInfo.getPortraitUri(), mNoteEdit.getText().toString().trim(), mFriendInfo.getStatus(), mFriendInfo.getTimestamp()));
                    RongIM.getInstance().refreshUserInfoCache(new UserInfo(mFriendInfo.getUserId(), mNoteEdit.getText().toString().trim(), Uri.parse(mFriendInfo.getPortraitUri())));
                    BroadcastManager.getInstance(mContext).sendBroadcast(SealAppContext.UPDATE_FRIEND);
                    Intent intent = new Intent(mContext, SingleContactActivity.class);
                    intent.putExtra("displayName", mNoteEdit.getText().toString().trim());
                    setResult(155, intent);
                    LoadDialog.dismiss(mContext);
                    finish();
                }
            }
        }
    }

    public void finishPage(View view) {
        this.finish();
    }
}
