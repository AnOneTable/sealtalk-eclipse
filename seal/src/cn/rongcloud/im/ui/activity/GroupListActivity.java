package cn.rongcloud.im.ui.activity;

import android.content.Context;
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
import cn.rongcloud.im.db.Groups;
import cn.rongcloud.im.server.SealAction;
import cn.rongcloud.im.server.network.async.AsyncTaskManager;
import cn.rongcloud.im.server.network.async.OnDataListener;
import cn.rongcloud.im.server.network.http.HttpException;
import cn.rongcloud.im.server.response.GetGroupResponse;
import cn.rongcloud.im.server.utils.NToast;
import cn.rongcloud.im.server.utils.RongGenerate;
import cn.rongcloud.im.server.widget.SelectableRoundedImageView;
import io.rong.imkit.RongIM;

/**
 * Created by AMing on 16/3/8.
 * Company RongCloud
 */
public class GroupListActivity extends BaseActivity {

    private static final int REFRESH_GROUP_UI = 22;
    private ListView mGroupListView;
    private GroupAdapter adapter;
    private TextView mNoGroups;
    private EditText mSearch;
    private List<Groups> mList;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fr_group_list);
        setTitle(R.string.my_groups);
        mGroupListView = (ListView) findViewById(R.id.group_listview);
        mNoGroups = (TextView) findViewById(R.id.show_no_group);
        mSearch = (EditText) findViewById(R.id.group_search);
        LayoutInflater mLayoutInflater = LayoutInflater.from(mContext);
        View mFootView = mLayoutInflater.inflate(R.layout.item_group_list_foot,
                         null);
        mTextView = (TextView) mFootView.findViewById(R.id.foot_group_size);
        mGroupListView.addFooterView(mFootView);
        initData();
        initNetUpdateUI();
    }


    private void initNetUpdateUI() {
        AsyncTaskManager.getInstance(mContext).request(REFRESH_GROUP_UI, new OnDataListener() {
            @Override
            public Object doInBackground(int requestCode, String id) throws HttpException {
                return new SealAction(mContext).getGroups();
            }


            @Override
            public void onSuccess(int requestCode, Object result) {
                if (result != null) {
                    GetGroupResponse response = (GetGroupResponse) result;
                    if (response.getCode() == 200) {
                        if (response.getResult().size() != DBManager.getInstance(mContext).getDaoSession().getGroupsDao().loadAll().size()) {
                            DBManager.getInstance(mContext).getDaoSession().getGroupsDao().deleteAll();
                            List<GetGroupResponse.ResultEntity> list = response.getResult();
                            if (list.size() > 0) { //服务端上也没有群组数据
                                for (GetGroupResponse.ResultEntity g : list) {
                                    DBManager.getInstance(mContext).getDaoSession().getGroupsDao().insertOrReplace(
                                        new Groups(g.getGroup().getId(), g.getGroup().getName(), g.getGroup().getPortraitUri(), String.valueOf(g.getRole()))
                                    );
                                }
                            }
                            mTextView.setVisibility(View.VISIBLE);
                            mTextView.setText(list.size() + " 个群组");
                            new android.os.Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    List<Groups> list = DBManager.getInstance(mContext).getDaoSession().getGroupsDao().loadAll();
                                    if (adapter != null) {
                                        adapter.updateListView(list);
                                    } else {
                                        GroupAdapter gAdapter = new GroupAdapter(mContext, list);
                                        mGroupListView.setAdapter(gAdapter);
                                    }
                                }
                            }, 500);
                        }
                    }
                }
            }

            @Override
            public void onFailure(int requestCode, int state, Object result) {
                NToast.shortToast(mContext, "刷新群组数据请求失败");
            }
        });

    }


    private void initData() {
        mList = DBManager.getInstance(mContext).getDaoSession().getGroupsDao().loadAll();
        if (mList != null && mList.size() > 0) {
            adapter = new GroupAdapter(mContext, mList);
            mGroupListView.setAdapter(adapter);
            mNoGroups.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
            mTextView.setText(mList.size() + " 个群组");
            mGroupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    Intent intent = new Intent(mContext, NewGroupDetailActivity.class);
//                    intent.putExtra("QunBean", (Serializable) adapter.getItem(position));
//                    startActivityForResult(intent, 99);
                    Groups bean = (Groups) adapter.getItem(position);
                    RongIM.getInstance().startGroupChat(GroupListActivity.this, bean.getQunId(), bean.getName());
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
        } else {
            mNoGroups.setVisibility(View.VISIBLE);
        }

    }

    private void filterData(String s) {
        List<Groups> filterDataList = new ArrayList<>();
        if (TextUtils.isEmpty(s)) {
            filterDataList = mList;
        } else {
            for (Groups groups : mList) {
                if (groups.getName().contains(s)) {
                    filterDataList.add(groups);
                }
            }
        }
        adapter.updateListView(filterDataList);
    }


    class GroupAdapter extends BaseAdapter {

        private Context context;

        private List<Groups> list;

        public GroupAdapter(Context context, List<Groups> list) {
            this.context = context;
            this.list = list;
        }

        /**
         * 传入新的数据 刷新UI的方法
         */
        public void updateListView(List<Groups> list) {
            this.list = list;
            notifyDataSetChanged();
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
            ViewHolder viewHolder;
            final Groups mContent = list.get(position);
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.group_item_new, null);
                viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.groupname);
                viewHolder.mImageView = (SelectableRoundedImageView) convertView.findViewById(R.id.groupuri);
                viewHolder.groupId = (TextView) convertView.findViewById(R.id.group_id);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvTitle.setText(mContent.getName());
            if (TextUtils.isEmpty(mContent.getPortraitUri())) {
                ImageLoader.getInstance().displayImage(RongGenerate.generateDefaultAvatar(mContent.getName(), mContent.getQunId()), viewHolder.mImageView, App.getOptions());
            } else {
                ImageLoader.getInstance().displayImage(mContent.getPortraitUri(), viewHolder.mImageView, App.getOptions());
            }
            if (context.getSharedPreferences("config", MODE_PRIVATE).getBoolean("isDebug", false)) {
                viewHolder.groupId.setVisibility(View.VISIBLE);
                viewHolder.groupId.setText(mContent.getQunId());
            }
            return convertView;
        }


        class ViewHolder {
            /**
             * 昵称
             */
            TextView tvTitle;
            /**
             * 头像
             */
            SelectableRoundedImageView mImageView;
            /**
             * userId
             */
            TextView groupId;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
