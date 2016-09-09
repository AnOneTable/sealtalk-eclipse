package cn.rongcloud.im.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.FriendDao;
import cn.rongcloud.im.server.broadcast.BroadcastManager;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.pinyin.CharacterParser;
import cn.rongcloud.im.server.pinyin.FriendInfo;
import cn.rongcloud.im.server.pinyin.PinyinComparator;
import cn.rongcloud.im.server.pinyin.SideBar;
import cn.rongcloud.im.server.response.AddGroupMemberResponse;
import cn.rongcloud.im.server.response.DeleteGroupMemberResponse;
import cn.rongcloud.im.server.response.GetGroupMemberResponse;
import cn.rongcloud.im.server.utils.NLog;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.DialogWithYesOrNoUtils;
import cn.rongcloud.im.server.widget.LoadDialog;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/1/21.
 * Company RongCloud
 */
public class SelectFriendsActivity extends BaseActivity implements View.OnClickListener {

    private static final int ADD_GROUP_MEMBER = 21;
    private static final int DELETE_GROUP_MEMBER = 23;
    public static final String DISCUSSION_UPDATE = "DISCUSSION_UPDATE";
    /**
     * 好友列表的 ListView
     */
    private ListView mListView;
    /**
     * 发起讨论组的 adapter
     */
    private StartDiscussionAdapter adapter;
    /**
     * 中部展示的字母提示
     */
    public TextView dialog;

    /**
     * 汉字转换成拼音的类
     */
    private CharacterParser characterParser;
    /**
     * 根据拼音来排列ListView里面的数据类
     */
    private PinyinComparator pinyinComparator;
    private TextView mNoFriends;
    private TextView mConfirmTextView;
    private List<FriendInfo> data_list = new ArrayList<>();
    private List<FriendInfo> sourceDataList = new ArrayList<>();
    private LinearLayout mSelectedFriendsLinearLayout;
    private boolean isCrateGroup;
    private boolean isConversationActivityStartDiscussion;
    private boolean isConversationActivityStartPrivate;
    private List<GetGroupMemberResponse.ResultEntity> addGroupMemberList;
    private List<GetGroupMemberResponse.ResultEntity> deleteGroupMemberList;
    private String groupId;
    private String delete_group_id;
    private String conversationStartId;
    private String conversationStartType = "null";
    private ArrayList<String> discListMember;
    private ArrayList<UserInfo> addDisList, deleDisList;
    private boolean isStartPrivateChat;
    private List<FriendInfo> mSelectedFriendInfo;


    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_disc);
        setHeadVisibility(View.GONE);
        TextView mTitleTextView = (TextView) findViewById(R.id.select_title);
        mConfirmTextView = (TextView) findViewById(R.id.select_ok);
        mSelectedFriendsLinearLayout = (LinearLayout) findViewById(R.id.ll_selected_friends);
        mConfirmTextView.setOnClickListener(this);
        isCrateGroup = getIntent().getBooleanExtra("createGroup", false);
        isConversationActivityStartDiscussion = getIntent().getBooleanExtra("CONVERSATION_DISCUSSION", false);
        isConversationActivityStartPrivate = getIntent().getBooleanExtra("CONVERSATION_PRIVATE", false);
        addGroupMemberList = (List<GetGroupMemberResponse.ResultEntity>) getIntent().getSerializableExtra("AddGroupMember");
        groupId = getIntent().getStringExtra("GroupId");
        deleteGroupMemberList = (List<GetGroupMemberResponse.ResultEntity>) getIntent().getSerializableExtra("DeleteGroupMember");
        delete_group_id = getIntent().getStringExtra("DeleteGroupId");
        addDisList = (ArrayList<UserInfo>) getIntent().getSerializableExtra("AddDiscuMember");
        deleDisList = (ArrayList<UserInfo>) getIntent().getSerializableExtra("DeleteDiscuMember");

        if (isConversationActivityStartPrivate) {
            conversationStartType = "PRIVATE";
            conversationStartId = getIntent().getStringExtra("DEMO_FRIEND_TARGETID");
            mTitleTextView.setText("选择讨论组成员");
        } else if (isConversationActivityStartDiscussion) {
            conversationStartType = "DISCUSSION";
            conversationStartId = getIntent().getStringExtra("DEMO_FRIEND_TARGETID");
            discListMember = getIntent().getStringArrayListExtra("DISCUSSIONMEMBER");
            mTitleTextView.setText("选择讨论组成员");
        } else if (deleteGroupMemberList != null) {
            mTitleTextView.setText(getString(R.string.remove_group_member));
        } else if (addGroupMemberList != null) {
            mTitleTextView.setText(getString(R.string.add_group_member));
        } else if (isCrateGroup) {
            mTitleTextView.setText(getString(R.string.select_group_member));
        } else if (addDisList != null) {
            mTitleTextView.setText("增加讨论组成员");
        } else if (deleDisList != null) {
            mTitleTextView.setText("移除讨论组成员");
        } else {
            mTitleTextView.setText(getString(R.string.select_contact));
            if (!getSharedPreferences("config", MODE_PRIVATE).getBoolean("isDebug", false)) {
                isStartPrivateChat = true;
            }
        }

        initView();
        initData();

        if (addDisList != null && addDisList.size() > 0) {
            for (UserInfo u : addDisList) {
                for (int i = 0; i < sourceDataList.size(); i++) {
                    if (sourceDataList.get(i).getUserId().contains(u.getUserId())) {
                        sourceDataList.remove(sourceDataList.get(i));
                    }
                }
            }
        } else if (addGroupMemberList != null && addGroupMemberList.size() > 0) {
            for (GetGroupMemberResponse.ResultEntity g : addGroupMemberList) {
                for (int i = 0; i < sourceDataList.size(); i++) {
                    if (sourceDataList.get(i).getUserId().contains(g.getUser().getId())) {
                        sourceDataList.remove(sourceDataList.get(i));
                    }
                }
            }
        } else if (conversationStartType.equals("DISCUSSION")) {
            if (discListMember != null && discListMember.size() > 1) {
                for (String s : discListMember) {
                    for (int i = 0; i < sourceDataList.size(); i++) {
                        if (sourceDataList.get(i).getUserId().contains(s)) {
                            sourceDataList.remove(sourceDataList.get(i));
                        }
                    }
                }
            }
        } else if (conversationStartType.equals("PRIVATE")) {
            for (int i = 0; i < sourceDataList.size(); i++) {
                if (sourceDataList.get(i).getUserId().contains(conversationStartId)) {
                    sourceDataList.remove(sourceDataList.get(i));
                }
            }
        }

        adapter = new StartDiscussionAdapter(mContext, sourceDataList);
        mListView.setAdapter(adapter);
    }

    private void initView() {
        //实例化汉字转拼音类
        characterParser = CharacterParser.getInstance();
        pinyinComparator = PinyinComparator.getInstance();
        mListView = (ListView) findViewById(R.id.dis_friendlistview);
        mNoFriends = (TextView) findViewById(R.id.dis_show_no_friend);
        SideBar mSidBar = (SideBar) findViewById(R.id.dis_sidrbar);
        dialog = (TextView) findViewById(R.id.dis_dialog);
        mSidBar.setTextView(dialog);
        //设置右侧触摸监听
        mSidBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                //该字母首次出现的位置
                int position = adapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListView.setSelection(position);
                }

            }
        });
    }

    private void initData() {
        mSelectedFriendInfo = new ArrayList<>();
        if (deleDisList != null && deleDisList.size() > 0) {
            for (int i = 0; i < deleDisList.size(); i++) {
                if (deleDisList.get(i).getUserId().contains(getSharedPreferences("config", MODE_PRIVATE).getString("loginid", ""))) {
                    continue;
                }
                data_list.add(new FriendInfo(deleDisList.get(i).getUserId(),
                                             deleDisList.get(i).getName(),
                                             deleDisList.get(i).getPortraitUri().toString(),
                                             null //TODO displayName 需要处理 暂为 null
                                            ));
            }
        } else if (deleteGroupMemberList != null && deleteGroupMemberList.size() > 0) {
            for (int i = 0; i < deleteGroupMemberList.size(); i++) {
                if (deleteGroupMemberList.get(i).getUser().getId().contains(getSharedPreferences("config", MODE_PRIVATE).getString("loginid", ""))) {
                    continue;
                }
                data_list.add(new FriendInfo(deleteGroupMemberList.get(i).getUser().getId(),
                                             deleteGroupMemberList.get(i).getUser().getNickname(),
                                             deleteGroupMemberList.get(i).getUser().getPortraitUri(),
                                             null //TODO displayName 需要处理 暂为 null
                                            ));
            }
        } else {
            List<cn.rongcloud.im.db.Friend> list = DBManager.getInstance(mContext).getDaoSession().getFriendDao().loadAll();
            if (list != null && list.size() > 0) {
                for (cn.rongcloud.im.db.Friend friend : list) {
                    data_list.add(new FriendInfo(friend.getUserId(), friend.getName(), friend.getPortraitUri(), friend.getDisplayName(), null, null));
                }

            }
        }
        if (data_list != null && data_list.size() > 0) {
            sourceDataList = filledData(data_list); //过滤数据为有字母的字段  现在有字母 别的数据没有
        } else {
            mNoFriends.setVisibility(View.VISIBLE);
        }

        //还原除了带字母字段的其他数据
        for (int i = 0; i < data_list.size(); i++) {
            sourceDataList.get(i).setName(data_list.get(i).getName());
            sourceDataList.get(i).setUserId(data_list.get(i).getUserId());
            sourceDataList.get(i).setPortraitUri(data_list.get(i).getPortraitUri());
            sourceDataList.get(i).setDisplayName(data_list.get(i).getDisplayName());
        }
        // 根据a-z进行排序源数据
        Collections.sort(sourceDataList, pinyinComparator);
    }


    //用于存储CheckBox选中状态
    public Map<Integer, Boolean> mCBFlag;

    public List<FriendInfo> adapterList;


    class StartDiscussionAdapter extends BaseAdapter implements SectionIndexer {

        private Context context;
        private ArrayList<CheckBox> checkBoxList = new ArrayList<>();

        public StartDiscussionAdapter(Context context, List<FriendInfo> list) {
            this.context = context;
            adapterList = list;
            mCBFlag = new HashMap<>();
            init();

        }

        void init() {
            for (int i = 0; i < adapterList.size(); i++) {
                mCBFlag.put(i, false);
            }
        }

        /**
         * 传入新的数据 刷新UI的方法
         */
        public void updateListView(List<FriendInfo> list) {
            adapterList = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return adapterList.size();
        }

        @Override
        public Object getItem(int position) {
            return adapterList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            final ViewHolder viewHolder;
            final FriendInfo friendInfo = adapterList.get(position);
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.item_start_discussion, null);
                viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.dis_friendname);
                viewHolder.tvLetter = (TextView) convertView.findViewById(R.id.dis_catalog);
                viewHolder.mImageView = (SelectableRoundedImageView) convertView.findViewById(R.id.dis_frienduri);
                viewHolder.isSelect = (CheckBox) convertView.findViewById(R.id.dis_select);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            //根据position获取分类的首字母的Char ascii值
            int section = getSectionForPosition(position);
            //如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == getPositionForSection(section)) {
                viewHolder.tvLetter.setVisibility(View.VISIBLE);
                viewHolder.tvLetter.setText(friendInfo.getLetters());
            } else {
                viewHolder.tvLetter.setVisibility(View.GONE);
            }

            if (isStartPrivateChat) {
                viewHolder.isSelect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        if (cb != null) {
                            if (cb.isChecked()) {
                                for (CheckBox c : checkBoxList) {
                                    c.setChecked(false);
                                }
                                checkBoxList.clear();
                                checkBoxList.add(cb);
                            } else {
                                checkBoxList.clear();
                            }
                        }
                    }
                });
                viewHolder.isSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mCBFlag.put(position, viewHolder.isSelect.isChecked());
                    }
                });
            } else {
                viewHolder.isSelect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCBFlag.put(position, viewHolder.isSelect.isChecked());
                        updateSelectedSizeView(mCBFlag);
                        if (mSelectedFriendInfo.contains(friendInfo)) {
                            int index = mSelectedFriendInfo.indexOf(friendInfo);
                            if (index > -1) {
                                mSelectedFriendsLinearLayout.removeViewAt(index);
                            }
                            mSelectedFriendInfo.remove(friendInfo);
                        } else {
                            mSelectedFriendInfo.add(friendInfo);
                            LinearLayout view = (LinearLayout) View.inflate(SelectFriendsActivity.this, R.layout.item_selected_friends, null);
                            SelectableRoundedImageView asyncImageView = (SelectableRoundedImageView) view.findViewById(R.id.iv_selected_friends);
                            ImageLoader.getInstance().displayImage(TextUtils.isEmpty(friendInfo.getPortraitUri()) ? RongGenerate.generateDefaultAvatar(friendInfo.getName(), friendInfo.getUserId()) : friendInfo.getPortraitUri(), asyncImageView);
                            view.removeView(asyncImageView);
                            mSelectedFriendsLinearLayout.addView(asyncImageView);
                        }
                    }
                });
            }
            viewHolder.isSelect.setChecked(mCBFlag.get(position));

            if (TextUtils.isEmpty(adapterList.get(position).getDisplayName())) {
                viewHolder.tvTitle.setText(adapterList.get(position).getName());
            } else {
                viewHolder.tvTitle.setText(adapterList.get(position).getDisplayName());
            }

            String url = adapterList.get(position).getPortraitUri();
            if (!TextUtils.isEmpty(url)) {
                ImageLoader.getInstance().displayImage(url, viewHolder.mImageView, App.getOptions());
            } else {
                ImageLoader.getInstance().displayImage(RongGenerate.generateDefaultAvatar(adapterList.get(position).getName(), adapterList.get(position).getUserId()), viewHolder.mImageView, App.getOptions());
            }
            return convertView;
        }

        private void updateSelectedSizeView(Map<Integer, Boolean> mCBFlag) {
            if (!isStartPrivateChat && mCBFlag != null) {
                int size = 0;
                for (int i = 0; i < mCBFlag.size(); i++) {
                    if (mCBFlag.get(i)) {
                        size++;
                    }
                }
                if (size == 0) {
                    mConfirmTextView.setText("确定");
//                mSelectedListView.setVisibility(View.GONE);
                    mSelectedFriendsLinearLayout.setVisibility(View.GONE);
                } else {
                    mConfirmTextView.setText("确定(" + size + ")");
                    List<FriendInfo> selectedList = new ArrayList<>();
                    for (int i = 0; i < sourceDataList.size(); i++) {
                        if (mCBFlag.get(i)) {
                            selectedList.add(sourceDataList.get(i));
                        }
                    }
//                mSelectedListView.setVisibility(View.VISIBLE);
                    mSelectedFriendsLinearLayout.setVisibility(View.GONE);
//                selectAdapter.updateListView(selectedList);
//                    updateSelectedFriendsView(selectedList);
                }
            }
        }

        @Override
        public Object[] getSections() {
            return new Object[0];
        }

        /**
         * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
         */
        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = adapterList.get(i).getLetters();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == sectionIndex) {
                    return i;
                }
            }

            return -1;
        }

        /**
         * 根据ListView的当前位置获取分类的首字母的Char ascii值
         */
        @Override
        public int getSectionForPosition(int position) {
            return adapterList.get(position).getLetters().charAt(0);
        }


        final class ViewHolder {
            /**
             * 首字母
             */
            TextView tvLetter;
            /**
             * 昵称
             */
            TextView tvTitle;
            /**
             * 头像
             */
            SelectableRoundedImageView mImageView;
            /**
             * userid
             */
//            TextView tvUserId;
            /**
             * 是否被选中的checkbox
             */
            CheckBox isSelect;
        }

    }


    @Override
    public Object doInBackground(int requestCode, String id) throws HttpException {
        switch (requestCode) {
            case ADD_GROUP_MEMBER:
                return action.addGroupMember(groupId, startDisList);
            case DELETE_GROUP_MEMBER:
                return action.deleGroupMember(delete_group_id, startDisList);
        }
        return super.doInBackground(requestCode, id);
    }

    @Override
    public void onSuccess(int requestCode, Object result) {
        if (result != null) {
            switch (requestCode) {
                case ADD_GROUP_MEMBER:
                    AddGroupMemberResponse res = (AddGroupMemberResponse) result;
                    if (res.getCode() == 200) {
                        Intent data = new Intent();
                        data.putExtra("newAddMember", (Serializable) createGroupList);
                        setResult(101, data);
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, getString(R.string.add_successful));
                        finish();
                    }
                    break;
                case DELETE_GROUP_MEMBER:
                    DeleteGroupMemberResponse response = (DeleteGroupMemberResponse) result;
                    if (response.getCode() == 200) {
                        Intent intent = new Intent();
                        intent.putExtra("deleteMember", (Serializable) createGroupList);
                        setResult(102, intent);
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, getString(R.string.remove_successful));
                        finish();
                    } else if (response.getCode() == 400) {
                        LoadDialog.dismiss(mContext);
                        NToast.shortToast(mContext, "创建者不能将自己移除");
                    }
                    break;
            }
        }
    }

    @Override
    public void onFailure(int requestCode, int state, Object result) {
        switch (requestCode) {
            case ADD_GROUP_MEMBER:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, "添加群组成员请求失败");
                break;
            case DELETE_GROUP_MEMBER:
                LoadDialog.dismiss(mContext);
                NToast.shortToast(mContext, "移除群组成员请求失败");
                break;
        }
    }

    private List<String> startDisList;
    private List<FriendInfo> createGroupList;


    /**
     * 为ListView填充数据
     */
    private List<FriendInfo> filledData(List<FriendInfo> lsit) {
        List<FriendInfo> mFriendInfoList = new ArrayList<>();

        for (int i = 0; i < lsit.size(); i++) {
            FriendInfo friendInfoModel = new FriendInfo();
            friendInfoModel.setName(lsit.get(i).getName());
            //汉字转换成拼音
            String pinyin = characterParser.getSpelling(lsit.get(i).getName());
            String sortString = pinyin.substring(0, 1).toUpperCase();

            // 正则表达式，判断首字母是否是英文字母
            if (sortString.matches("[A-Z]")) {
                friendInfoModel.setLetters(sortString.toUpperCase());
            } else {
                friendInfoModel.setLetters("#");
            }

            mFriendInfoList.add(friendInfoModel);
        }
        return mFriendInfoList;

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListView = null;
        adapter = null;
    }

    private cn.rongcloud.im.db.Friend getUserInfoById(String userId) {
        if (!TextUtils.isEmpty(userId)) {
            return DBManager.getInstance(mContext).getDaoSession().getFriendDao().queryBuilder().where(FriendDao.Properties.UserId.eq(userId)).unique();
        }
        return null;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_ok:
                mConfirmTextView.setClickable(false);
                if (mCBFlag != null && sourceDataList != null && sourceDataList.size() > 0) {
                    startDisList = new ArrayList<>();
                    List<String> disNameList = new ArrayList<>();
                    createGroupList = new ArrayList<>();
                    for (int i = 0; i < sourceDataList.size(); i++) {
                        if (mCBFlag.get(i)) {
                            startDisList.add(sourceDataList.get(i).getUserId());
                            disNameList.add(sourceDataList.get(i).getName());
                            createGroupList.add(sourceDataList.get(i));
                        }
                    }

                    if (isConversationActivityStartDiscussion) {
                        if (RongIM.getInstance() != null) {
                            RongIM.getInstance().addMemberToDiscussion(conversationStartId, startDisList, new RongIMClient.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    NToast.shortToast(SelectFriendsActivity.this, getString(R.string.add_successful));
                                    BroadcastManager.getInstance(mContext).sendBroadcast(DISCUSSION_UPDATE);
                                    finish();
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {

                                }
                            });
                        }
                    } else if (isConversationActivityStartPrivate) {
                        if (RongIM.getInstance() != null) { // 没有被调用 二人讨论组时候
                            RongIM.getInstance().addMemberToDiscussion(conversationStartId, startDisList, new RongIMClient.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    NToast.shortToast(SelectFriendsActivity.this, getString(R.string.add_successful));
                                    finish();
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {

                                }
                            });
                        }
                    } else if (deleteGroupMemberList != null && startDisList != null && sourceDataList.size() > 0) {
                        DialogWithYesOrNoUtils.getInstance().showDialog(mContext, getString(R.string.remove_group_members), new DialogWithYesOrNoUtils.DialogCallBack() {

                            @Override
                            public void executeEvent() {
                                LoadDialog.show(mContext);
                                request(DELETE_GROUP_MEMBER);
                            }

                            @Override
                            public void executeEditEvent(String editText) {

                            }

                            @Override
                            public void updatePassword(String oldPassword, String newPassword) {

                            }
                        });
                    } else if (deleDisList != null && startDisList != null && startDisList.size() > 0) {
                        Intent intent = new Intent();
                        intent.putExtra("deleteDiscuMember", (Serializable) startDisList);
                        setResult(RESULT_OK, intent);
                        finish();

                    } else if (addGroupMemberList != null && startDisList != null && startDisList.size() > 0) {
                        //TODO 选中添加成员的数据添加到服务端数据库  返回本地也需要更改
                        LoadDialog.show(mContext);
                        request(ADD_GROUP_MEMBER);

                    } else if (addDisList != null && startDisList != null && startDisList.size() > 0) {
                        Intent intent = new Intent();
                        intent.putExtra("addDiscuMember", (Serializable) startDisList);
                        setResult(RESULT_OK, intent);
                        finish();
                    } else if (isCrateGroup) {
                        if (createGroupList.size() > 0) {
                            Intent intent = new Intent(SelectFriendsActivity.this, CreateGroupActivity.class);
                            intent.putExtra("GroupMember", (Serializable) createGroupList);
                            startActivity(intent);
                            finish();
                        } else {
                            NToast.shortToast(mContext, "请至少邀请一位好友创建群组");
                        }
                    } else {

                        if (startDisList != null && startDisList.size() == 1) {
                            RongIM.getInstance().startPrivateChat(mContext, startDisList.get(0), getUserInfoById(startDisList.get(0)).getName());
                        } else if (startDisList.size() > 1) {

                            String disName;
                            if (disNameList.size() < 2) {
                                disName = disNameList.get(0) + "和我的讨论组";
                            } else {
                                StringBuilder sb = new StringBuilder();
                                for (String s : disNameList) {
                                    sb.append(s);
                                    sb.append(",");
                                }
                                String str = sb.toString();
                                disName = str.substring(0, str.length() - 1);
                                disName = disName + "和我的讨论组";
                            }
                            RongIM.getInstance().createDiscussion(disName, startDisList, new RongIMClient.CreateDiscussionCallback() {
                                @Override
                                public void onSuccess(String s) {
                                    NLog.e("disc", "onSuccess" + s);
                                    RongIM.getInstance().startDiscussionChat(SelectFriendsActivity.this, s, "");
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    NLog.e("disc", errorCode.getValue());
                                }
                            });
                        } else {
                            NToast.shortToast(mContext, getString(R.string.least_one_friend));
                        }
                    }
                } else {
                    Toast.makeText(SelectFriendsActivity.this, "无数据", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    public void selectFinish(View view) {
        finish();
    }

}
