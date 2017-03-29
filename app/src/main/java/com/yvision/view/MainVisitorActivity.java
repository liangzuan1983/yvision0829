package com.yvision.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yvision.R;
import com.yvision.adapter.MainSpinnerAdapter;
import com.yvision.adapter.VisitorListAdapter;
import com.yvision.common.MyApplication;
import com.yvision.common.MyException;
import com.yvision.dialog.Loading;
import com.yvision.helper.UserHelper;
import com.yvision.inject.ViewInject;
import com.yvision.model.VisitorBModel;
import com.yvision.widget.NiceSpinner;
import com.yvision.widget.RefreshAndLoadListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.yvision.dialog.Loading.noDialogRun;
import static com.yvision.dialog.Loading.run;

/**
 * 访客管理系统详细界面
 */
public class MainVisitorActivity extends BaseActivity implements RefreshAndLoadListView.ILoadMoreListener, RefreshAndLoadListView.IReflashListener {
    //back
    @ViewInject(id = R.id.layout_back, click = "forBack")
    RelativeLayout layout_back;

    // spinner日期
    @ViewInject(id = R.id.tv_title)
    NiceSpinner spinner;

    // 编辑
    @ViewInject(id = R.id.tv_right, click = "edit")
    TextView tv_edit;

    // 添加访客
    @ViewInject(id = R.id.btn_addCustomerFace, click = "addCustomerFace")
    Button btn_addface;

    // xml:添加访客
    @ViewInject(id = R.id.layout_addCustomerFace)
    LinearLayout layout_addCustomerFace;

    // xml:全选 删除
    @ViewInject(id = R.id.layout_edit)
    LinearLayout layout_edit;

    //
    //    //搜索
    //    @ViewInject(id = R.id.forSearch)
    //    SearchView tv_forSearch;

    //checkbox_all 全选
    @ViewInject(id = R.id.checkBox_all)
    CheckBox checkBox_all;

    //删除按钮
    @ViewInject(id = R.id.btn_delete, click = "delete")
    Button btn_delete;

    //listView
    @ViewInject(id = R.id.visitorList, click = "delete")
    RefreshAndLoadListView listView;

    // 常量
    private static final int GET_NEWDATA_SUCCESS = -40;// 刷新新数据 标志
    private static final int GET_DATA_SUCCESS = -39;// 获取所有数据列表 标志
    private static final int LOAD_MORE_SUCCESS = -38;
    private static final int SEARCH_NAME_SUCCESS = -37;//搜索
    private static final int DELETE_SUCCESS = -36;//删除成功
    private static final int GET_NONE_NEWDATA = -35;//没有新数据

    // 变量
    private Context context;
    private ArrayAdapter<String> mSpinnerAdapter;
    private String[] mSpinnerArray;
    private boolean editFlag = false;// 编辑按钮标记
    private String IminTime = "";
    private String ImaxTime = "";

    private VisitorListAdapter vAdapter;//记录适配
    private boolean ifLoading = false;//标记
    private int pageSize = 20;
    private ArrayList list = null;
    private List<String> spinnerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.act_visitormain);

        initMainView();// spinner监听/search监听
        vAdapter = new VisitorListAdapter(this);// 加载全部数据
        listView.setAdapter(vAdapter);
        initListener();

        //加载全部数据
        getData();

    }

    // 加载spinner,加载SearchView格式
    public void initMainView() {
        MyApplication.getInstance().setCheckBoxStatus(false);//进入界面 设置checkBox不可见
        context = this;

        //        // 搜索
        //        tv_forSearch.setIconifiedByDefault(true);
        //        tv_forSearch.setSubmitButtonEnabled(true);
        //        tv_forSearch.setIconifiedByDefault(true);
        //        tv_forSearch.setOnQueryTextListener(searchListener);// searchView监听

        // spinner
        mSpinnerArray = getResources().getStringArray(R.array.user_date);
        // 使用自定义的ArrayAdapter
        mSpinnerAdapter = new MainSpinnerAdapter(this, mSpinnerArray);
        // 设置下拉列表风格()
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);

        listView.setIRefreshListener(this);
        listView.setILoadMoreListener(this);
        //全选
        checkBox_all.setOnCheckedChangeListener(selectAll);
        //spinner绑定数据
        spinnerData = new LinkedList<>(Arrays.asList("全部访客", "今日访客"));
        spinner.attachDataSource(spinnerData);//绑定数据

    }


    private void initListener() {
        //spinner监听，筛选数据
        spinner.addOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getSelectTimeData(spinnerData.get(position).trim());//参数2必填GET_NEW_DATA

            }
        });


        //		 点击一条记录后，跳转到登记时详细的信息
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainVisitorActivity.this, "position=" + position, Toast.LENGTH_SHORT).show();
                //方法1：获取一条详细记录
                final String storeID = UserHelper.getCurrentUser().getStoreID();//参数storeId

                int headerViewsCount = listView.getHeaderViewsCount();//得到header的总数量
                int newPosition = position - headerViewsCount;//得到新的修正后的position
                final String recordID = ((VisitorBModel) vAdapter.getItem(newPosition)).getRecordID();//recordID

                run(MainVisitorActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            VisitorBModel model = UserHelper.getOneVisitorRecordByID(MainVisitorActivity.this, recordID, storeID);
                            Intent intent = new Intent(MainVisitorActivity.this, VisitorInfoActivity.class);
                            intent.putExtra("VisitorBModel", model);
                            startActivityForResult(intent, VisitorInfoActivity.REQUEST_CODE_FOR_EDIT_USER);
                        } catch (MyException e) {
                            sendToastMessage(e.getMessage());
                        }
                    }
                });

            }
        });
    }

    private void getSelectTimeData(String date) {
        switch (date) {
            case "全部访客":
                vAdapter = new VisitorListAdapter(MainVisitorActivity.this);// 加载全部数据
                listView.setAdapter(vAdapter);
                MyApplication.getInstance().setTimespan(0);
                getData();
                break;
            case "今日访客":
                vAdapter = new VisitorListAdapter(MainVisitorActivity.this);// 加载今天数据
                listView.setAdapter(vAdapter);
                MyApplication.getInstance().setTimespan(1);
                getData();
                break;
        }
    }

    // 获取全部记录
    private void getData() {
        if (ifLoading) {
            return;
        }

        run(this, new Runnable() {
            @Override
            public void run() {
                ifLoading = true;
                String storeID = UserHelper.getCurrentUser().getStoreID();
                try {
                    vAdapter.IsEnd = false;

                    ArrayList<VisitorBModel> visitorModelList = UserHelper.getVisitorRecordsByPageA(
                            MainVisitorActivity.this,
                            "",//iMaxTime
                            "",//iMinTime
                            pageSize,
                            MyApplication.getInstance().timespan,
                            storeID);

                    //                  Log.d("SJY", "MainVisitorActivity--getData=" + visitorModelList.toString());//
                    if (visitorModelList == null) {
                        vAdapter.IsEnd = true;
                    } else if (visitorModelList.size() < pageSize) {
                        vAdapter.IsEnd = true;
                    }

                    if (visitorModelList != null) {
                        sendMessage(GET_DATA_SUCCESS, visitorModelList);
                    } else {
                        sendMessage(GET_NONE_NEWDATA, null);
                    }

                } catch (MyException e) {
                    sendToastMessage(e.getMessage());
                    ifLoading = false;
                }
            }
        });
    }

    @Override
    public void onLoadMore() {

        if (ifLoading) {
            return;
        }

        noDialogRun(MainVisitorActivity.this, new Runnable() {

            @Override
            public void run() {
                ifLoading = true;//
                String storeID = UserHelper.getCurrentUser().getStoreID();
                try {
                    List<VisitorBModel> visitorModelList = UserHelper.getVisitorRecordsByPageA(
                            MainVisitorActivity.this,
                            "",//iMaxTime
                            IminTime,//iMinTime /获取前20条数据的最后后一条的iLastUpdateTime参数
                            pageSize,
                            MyApplication.getInstance().timespan,
                            storeID);

                    if (visitorModelList == null || visitorModelList.size() < pageSize) {
                        vAdapter.IsEnd = true;
                    }

                    sendMessage(LOAD_MORE_SUCCESS, visitorModelList);

                } catch (MyException e) {
                    Log.d("SJY", "异常=" + e.getMessage());
                    sendMessage(GET_NONE_NEWDATA, e.getMessage());
                }
            }
        });
    }

    @Override
    public void onRefresh() {
        if (ifLoading) {
            return;
        }
        Loading.noDialogRun(this, new Runnable() {
            @Override
            public void run() {
                ifLoading = true;
                String storeID = UserHelper.getCurrentUser().getStoreID();
                try {
                    vAdapter.IsEnd = false;

                    ArrayList<VisitorBModel> visitorModelList = UserHelper.getVisitorRecordsByPageA(
                            MainVisitorActivity.this,
                            ImaxTime,//iMaxTime
                            "",//iMinTime
                            pageSize,
                            MyApplication.getInstance().timespan,
                            storeID);

                    if (visitorModelList == null || visitorModelList.size() < pageSize) {
                        vAdapter.IsEnd = true;
                    }

                    sendMessage(GET_NEWDATA_SUCCESS, visitorModelList);
                } catch (MyException e) {
                    sendMessage(GET_NONE_NEWDATA, e.getMessage());
                    return;
                }
            }
        });
    }

    @Override
    protected void handleMessage(Message msg) {
        switch (msg.what) {
            case GET_NEWDATA_SUCCESS://刷新数据并拼接
                list = (ArrayList) msg.obj;
                vAdapter.insertEntityList(list);
                //数据处理/只存最大值,做刷新新数据使用
                setMaxTime(list);
                listView.loadAndFreshComplete();
                ifLoading = false;
                break;
            case GET_DATA_SUCCESS://进入页面加载最新
                // 数据显示
                list = (ArrayList) msg.obj;
                vAdapter.setEntityList(list);
                //数据处理，获取iLastUpdateTime参数方便后续上拉/下拉使用
                setMinTime(list);
                setMaxTime(list);
                ifLoading = false;
                listView.loadAndFreshComplete();
                break;
            case LOAD_MORE_SUCCESS://加载全部/今天
                list = (ArrayList) msg.obj;
                vAdapter.addEntityList(list);
                //数据处理，只存最小值
                setMinTime(list);
                ifLoading = false;
                listView.loadAndFreshComplete();
                break;
            case SEARCH_NAME_SUCCESS://搜索
                vAdapter.setEntityList((ArrayList) msg.obj);
                ifLoading = false;
                break;
            case DELETE_SUCCESS://删除
                sendToastMessage((String) msg.obj);
                checkBox_all.setChecked(false);
                vAdapter.notifyDataSetChanged();
                ifLoading = false;
                break;
            case GET_NONE_NEWDATA://没有获取新数据
                sendToastMessage((String) msg.obj);
                ifLoading = false;
                listView.loadAndFreshComplete();
                break;

            default:
                break;
        }
        super.handleMessage(msg);
    }

    /**
     * 获取minTime，上拉加载应用
     */
    private void setMinTime(ArrayList list) {
        if (list.size() > 0) {
            VisitorBModel model = (VisitorBModel) list.get(list.size() - 1);//获取最后一条记录
            IminTime = model.getiLastUpdateTime();
        }
    }

    /**
     * 获取maxTime,下拉刷新使用
     */
    private void setMaxTime(ArrayList list) {
        if (list.size() > 0) {
            VisitorBModel model = (VisitorBModel) list.get(0);//获取第一条记录
            ImaxTime = model.getiLastUpdateTime();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vAdapter != null) {
            vAdapter.destroy();
        }
    }

    // 添加访客
    public void addCustomerFace(View view) {
        startActivity(AddVisitorFaceActivity.class);
    }


    //    // 搜索
    //    public OnQueryTextListener searchListener = new OnQueryTextListener() {
    //        @Override
    //        public boolean onQueryTextChange(String newText) {// 在输入时触发的方法
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean onQueryTextSubmit(final String query) {//输入完成后，提交时触发的方法
    //            final String storeID = UserHelper.getCurrentUser().getStoreID();
    //            if (tv_forSearch != null) {
    //                // 得到输入管理对象
    //                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    //                if (inputMethodManager != null) {
    //                    // 隐藏键盘
    //                    inputMethodManager.hideSoftInputFromWindow(tv_forSearch.getWindowToken(), 0); // 输入法如果是显示状态，那么就隐藏输入法
    //                }
    //                tv_forSearch.clearFocus(); // 不获取焦点
    //            }
    //
    //            Loading.run(MainVisitorActivity.this, new Runnable() {
    //                @Override
    //                public void run() {
    //                    try {
    //                        vAdapter.IsEnd = false;
    //                        List<VisitorBModel> visitorModelList = UserHelper.getVisitorWithSameName(MainVisitorActivity.this, storeID, query);
    //
    //                        if (visitorModelList.size() < pageSize) {// 20
    //                            vAdapter.IsEnd = true;// 标识列表获取结束了，没有翻页
    //                        }
    //                        sendMessage(SEARCH_NAME_SUCCESS, visitorModelList);
    //                    } catch (MyException e) {
    //                        e.printStackTrace();
    //                        sendToastMessage(e.getMessage());
    //                    }
    //                }
    //            });
    //            return true;
    //        }
    //
    //    };

    /**
     * 编辑 完成
     */
    public void edit(View view) {
        if (!editFlag) {
            forEdit();// 编辑
            editFlag = true;
        } else {
            forComplite();// 完成
            editFlag = false;
        }
    }

    //编辑
    private void forEdit() {
        tv_edit.setText(R.string.main_finish);
        // 更换底部布局 删除
        layout_addCustomerFace.setVisibility(View.GONE);
        layout_edit.setVisibility(View.VISIBLE);
        //listview布局添加checkBox按钮
        MyApplication.getInstance().setCheckBoxStatus(true);//checkBox可见设置
        listView.setAdapter(vAdapter);
    }

    //完成
    private void forComplite() {
        tv_edit.setText(R.string.main_edit);
        // 更换底部布局：添加人脸
        layout_addCustomerFace.setVisibility(View.VISIBLE);
        layout_edit.setVisibility(View.GONE);
        MyApplication.getInstance().setCheckBoxStatus(false);//checkBox不可见设置
        listView.setAdapter(vAdapter);
        vAdapter.setisCheckedList(null);//标记checkBox的list清零
    }

    /**
     * 全选
     */
    CompoundButton.OnCheckedChangeListener selectAll = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                for (int i = 0; i < vAdapter.entityList.size(); i++) {
                    vAdapter.isCheckedList.set(i, true);
                    vAdapter.setisCheckedList(vAdapter.isCheckedList);
                    //刷新
                    vAdapter.notifyDataSetChanged();

                }
                Log.d("List", "全选true=" + vAdapter.isCheckedList.toString());
            } else {
                for (int i = 0; i < vAdapter.entityList.size(); i++) {
                    vAdapter.isCheckedList.set(i, false);
                    //刷新
                    vAdapter.notifyDataSetChanged();
                }
                Log.d("List", "全选false=" + vAdapter.isCheckedList.toString());
            }
        }
    };

    /**
     * 删除按钮
     */
    public void delete(View view) {
        String recordID = "";
        //获取recordID，删除数据（倒遍历避免动态长度影响）
        for (int i = vAdapter.isCheckedList.size() - 1; i >= 0; i--) {//isCheckedList/entityList
            Log.d("List", "MainVisitorActivity--for遍历--删除按钮--checkBox=" + i + "=" + vAdapter.isCheckedList.get(i));
            if (vAdapter.isCheckedList.get(i) == true) {
                recordID += ((VisitorBModel) vAdapter.getItem(i)).getRecordID() + ",";
                Log.d("List", "true--list--选中的recordId=" + ((VisitorBModel) vAdapter.getItem(i)).getRecordID() + ",选中的i=" + i);
                //删除
                vAdapter.entityList.remove(i);
                vAdapter.isCheckedList.remove(i);
                vAdapter.setisCheckedList(vAdapter.isCheckedList);
            }
        }
        //checkBox不显示选中

        if (!recordID.equals("")) {
            final String recordIDList = recordID.substring(0, recordID.length() - 1);//删除最后一个逗号处理
            final String storeID = UserHelper.getCurrentUser().getStoreID();//获取storeID

            run(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        String msg = UserHelper.getDeleteVisitorRecordsByIDList(MainVisitorActivity.this, storeID, recordIDList);
                        sendMessage(DELETE_SUCCESS, msg);
                    } catch (MyException e) {
                        sendToastMessage(e.getMessage());
                        return;
                    }
                }
            });
        } else {
            sendToastMessage("请选择要删除的记录！");
        }
    }

    // 后退
    public void forBack(View view) {
        this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VisitorInfoActivity.REQUEST_CODE_FOR_EDIT_USER) {
            // 修改 信息界面

            // if(resultCode ==
            // EditUserActivity.RESULT_CODE_FOR_EDIT_USER_SUCCESS){
            // getData();
            // }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}