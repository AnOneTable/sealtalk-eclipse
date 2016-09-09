package cn.rongcloud.im.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import cn.rongcloud.im.App;
import cn.rongcloud.im.R;
import cn.rongcloud.im.db.DBManager;
import cn.rongcloud.im.db.Friend;
import cn.rongcloud.im.db.FriendDao;
import cn.rongcloud.im.server.response.GetGroupMemberResponse;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by AMing on 16/7/1.
 * Company RongCloud
 */
public class TotalGroupMemberActivity extends BaseActivity {

    private List<GetGroupMemberResponse.ResultEntity> mGroupMember;

    private ListView mTotalListView;
    private TotalGroupMember adapter;
    private EditText mSearch;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toatl_member);
        setTitle(R.string.total_member);
        initViews();
        mGroupMember = (List<GetGroupMemberResponse.ResultEntity>) getIntent().getSerializableExtra("TotalMember");
        if (mGroupMember != null && mGroupMember.size() > 0) {
            setTitle(getString(R.string.total_member) + "(" + mGroupMember.size() + ")");
            adapter = new TotalGroupMember(mGroupMember, mContext);
            mTotalListView.setAdapter(adapter);
            mTotalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    GetGroupMemberResponse.ResultEntity bean = (GetGroupMemberResponse.ResultEntity) adapter.getItem(position);
                    UserInfo userInfo = new UserInfo(bean.getUser().getId(), bean.getUser().getNickname(), Uri.parse(TextUtils.isEmpty(bean.getUser().getPortraitUri()) ? RongGenerate.generateDefaultAvatar(bean.getUser().getNickname(), bean.getUser().getId()) : bean.getUser().getPortraitUri()));
                    Intent intent = new Intent(mContext, PersonalProfileActivity.class);
                    intent.putExtra("userinfo", userInfo);
                    intent.putExtra("conversationType", Conversation.ConversationType.GROUP.getValue());
//                    intent.putExtra("groupinfo", mGroup);
                    startActivity(intent);
                }
            });
            mSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterData(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }

    }

    private void filterData(String s) {
        List<GetGroupMemberResponse.ResultEntity> filterDateList = new ArrayList<>();
        if (TextUtils.isEmpty(s)) {
            filterDateList = mGroupMember;
        } else {
            for (GetGroupMemberResponse.ResultEntity resultEntity : mGroupMember) {
                if (resultEntity.getDisplayName().contains(s) || resultEntity.getUser().getNickname().contains(s)) {
                    filterDateList.add(resultEntity);
                }
            }
        }
        adapter.updateListView(filterDateList);
    }

    private void initViews() {
        mTotalListView = (ListView) findViewById(R.id.total_listview);
        mSearch = (EditText) findViewById(R.id.group_member_search);
    }


    class TotalGroupMember extends BaseAdapter {

        private List<GetGroupMemberResponse.ResultEntity> list;

        private Context context;

        private ViewHolder holder;


        public TotalGroupMember(List<GetGroupMemberResponse.ResultEntity> list, Context mContext) {
            this.list = list;
            this.context = mContext;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.share_item, null);
                holder.mImageView = (SelectableRoundedImageView) convertView.findViewById(R.id.share_icon);
                holder.title = (TextView) convertView.findViewById(R.id.share_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            GetGroupMemberResponse.ResultEntity bean = list.get(position);
            Friend friend = getUserInfoById(bean.getUser().getId());
            if (friend != null && !TextUtils.isEmpty(friend.getDisplayName())) {
                holder.title.setText(friend.getDisplayName());
            } else {
                holder.title.setText(bean.getUser().getNickname());
            }
            ImageLoader.getInstance().displayImage(TextUtils.isEmpty(bean.getUser().getPortraitUri()) ? RongGenerate.generateDefaultAvatar(bean.getUser().getNickname(), bean.getUser().getId()) : bean.getUser().getPortraitUri(), holder.mImageView, App.getOptions());
            return convertView;
        }


        public void updateListView(List<GetGroupMemberResponse.ResultEntity> list) {
            this.list = list;
            notifyDataSetChanged();
        }
    }


    final static class ViewHolder {
        /**
         * 头像
         */
        SelectableRoundedImageView mImageView;

        TextView title;
    }


    private Friend getUserInfoById(String userId) {
        if (!TextUtils.isEmpty(userId)) {
            return DBManager.getInstance(mContext).getDaoSession().getFriendDao().queryBuilder().where(FriendDao.Properties.UserId.eq(userId)).unique();
        }
        return null;
    }
}
