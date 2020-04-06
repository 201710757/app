package com.jihoon.callStateProject;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
/*
*
*   광고없는 유료버전*****************************
* */

public class MainActivity extends AppCompatActivity {
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;
    DBHelper dbHelper;
    SQLiteDatabase db = null;
    Cursor cursor;
    EditText nameTextView, emailTextView, phoneNumberTextView, memoTextView;
    String[] permission_list = {
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.FOREGROUND_SERVICE
    };

    private boolean processCommand(Intent intent){
        if(intent != null) return intent.getIntExtra("booting", -1) == 1 ? true:false;
        else return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);



        try {
            Intent passedIntent = getIntent();
            if(processCommand(passedIntent)) {
                if(!isServiceRunningCheck()) {
                    Intent my_intent = new Intent(MainActivity.this, TestService.class);
                    startService(my_intent);
                }
                finish();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, LoadingActivity.class);
        startActivity(intent);

        dbHelper = new DBHelper(this, 1);
        db = dbHelper.getWritableDatabase();

        nameTextView = (EditText)findViewById(R.id.editText1);
        phoneNumberTextView = (EditText)findViewById(R.id.editText2);
        emailTextView = (EditText)findViewById(R.id.editText3);
        memoTextView = (EditText)findViewById(R.id.editText4);

        final Button load_call = (Button)findViewById(R.id.call);
        load_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckTypesTask task = new CheckTypesTask();
                task.execute();
                if(!isServiceRunningCheck()) {
                    Intent my_intent = new Intent(MainActivity.this, TestService.class);
                    startService(my_intent);
                }
                load_call.setText("동기화 완료");

            }
        });

        Button bt_start = (Button) findViewById(R.id.bt_insert);
        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insert();
            }
        });

        Button bt_stop = (Button) findViewById(R.id.bt_delete);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("정보가 삭제됩니다");
                //타이틀설정
                String tv_text = "DB에서만 삭제됩니다.(연락처는 정보유지)";
                builder.setMessage(tv_text);
                //내용설정
                builder.setPositiveButton("삭제",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                delete();
                            }
                        });
                builder.setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, "취소되었습니다.", Toast.LENGTH_LONG).show();
                            }
                        });
                builder.show();
            }
        });

        try {
            String[] checkboxArr = listUpdateCheckBox();
            CheckBox[] checkBoxes = {(CheckBox)findViewById(R.id.checkBoxname), (CheckBox)findViewById(R.id.checkBoxphonenumber),
                    (CheckBox)findViewById(R.id.checkBoxemail), (CheckBox)findViewById(R.id.checkBoxmemo)};
            for(int i=0;i<4;i++) if(checkboxArr[i].equals("1")) checkBoxes[i].setChecked(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Button bt_checkBox = (Button)findViewById(R.id.buttonForCheckBox);
        bt_checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String[] checkS = new String[4];
                    boolean [] check = new boolean[4];
                    final CheckBox checkBoxName = (CheckBox)findViewById(R.id.checkBoxname);
                    final CheckBox checkBoxNumber = (CheckBox)findViewById(R.id.checkBoxphonenumber);
                    final CheckBox checkBoxEmail = (CheckBox)findViewById(R.id.checkBoxemail);
                    final CheckBox checkBoxMemo = (CheckBox)findViewById(R.id.checkBoxmemo);

                    if(checkBoxName.isChecked()) check[0] = true;
                    if(checkBoxNumber.isChecked()) check[1] = true;
                    if(checkBoxEmail.isChecked()) check[2] = true;
                    if(checkBoxMemo.isChecked()) check[3] = true;
                    for(int i=0;i<4;i++)
                    {
                        if(check[i]) checkS[i] = "1";
                        else checkS[i] = "0";
                    }
                    try {
                        db.execSQL("DELETE FROM checkBox");
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    db.execSQL("INSERT INTO checkBox VALUES ('" + checkS[0] + "', '" + checkS[1] + "', '" + checkS[2] + "', '" + checkS[3] + "');");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                Toast.makeText(MainActivity.this, "체크 항목이 설정되었습니다.", Toast.LENGTH_LONG).show();
            }
        });

        if(!isServiceRunningCheck())
        {
            Intent my_intent = new Intent(this, TestService.class);
            startService(my_intent);
        }
    }
    private class CheckTypesTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog asyncDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("불러오는 중입니다 종료하지 말아주세요!");

            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                contacts();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }

    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.jihoon.callStateProject.TestService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    public void checkPermission2(){
        //현재 안드로이드 버전이 6.0미만이면 메서드를 종료한다.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        for(String permission : permission_list){
            //권한 허용 여부를 확인한다.
            int chk = checkCallingOrSelfPermission(permission);

            if(chk == PackageManager.PERMISSION_DENIED){
                //권한 허용을여부를 확인하는 창을 띄운다
                requestPermissions(permission_list,0);
            }
        }
    }
    public void checkPermission() {
        checkPermission2();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 체크
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // TODO 동의를 얻지 못했을 경우의 처리
                checkPermission();
            }
        }
    }
    public String[] listUpdateCheckBox()
    {
        String[] str = new String[4];
        cursor = db.rawQuery("SELECT * FROM checkBox;", null);
        startManagingCursor(cursor);
        while (cursor.moveToNext())
        {
            for(int i=0;i<4;i++) str[i] = cursor.getString(i);
        }
        return str;
    }

    public String replaceFunc(String str)
    {
        str = str.replace("-","");
        str = str.replace("(","");
        str = str.replace(")","");
        str = str.replace(" ","");

        return str;
    }

    String subName;
    String subPhoneNum;
    String subEmail;
    String subMemo;

    public void insert()
    {
        nameTextView = (EditText)findViewById(R.id.editText1);
        phoneNumberTextView = (EditText)findViewById(R.id.editText2);
        emailTextView = (EditText)findViewById(R.id.editText3);
        memoTextView = (EditText)findViewById(R.id.editText4);

        String name = nameTextView.getText().toString();
        String phoneNumber = phoneNumberTextView.getText().toString();
        String email = emailTextView.getText().toString();
        String memo = memoTextView.getText().toString();

        if(name.equals("name")) name = "";
        if(phoneNumber.equals("phone number")) phoneNumber = "";
        if(email.equals("email")) email = "";
        if(memo.equals("memo")) memo = "";

        phoneNumber = replaceFunc(phoneNumber);
        //name + "', '" + phoneNum + "', '" + email + "', '" + memo +
        String phoneNum = phoneNumber;


        subName = name;
        subPhoneNum = phoneNum;
        subEmail = email;
        subMemo = memo;

        //----------------------
        if(isExists(name, phoneNum))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("정보가 중복됩니다");
            //타이틀설정
            String tv_text = "이미 같은 이름/전화번호 정보가 있습니다.\n등록하시면 현재 정보가 출력됩니다."
                    + "\n이름 : " + name + " / 전화번호 : " + phoneNum + "\nEmail : " + email + "\n메모 : " + memo
                    + "\n\n(기존데이터는 유지됨)";
            builder.setMessage(tv_text);
            //내용설정
            builder.setNegativeButton("등록",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            returnFlag = true;

                            new Thread() {
                                @Override
                                public void run() {
                                    Log.d("jihoonDebug", "Thread In");

                                    ArrayList<ContentProviderOperation> list = new ArrayList<>();
                                    try {
                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, subName)   //이름

                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, subPhoneNum)           //전화번호
                                                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)   //번호타입(Type_Mobile : 모바일)

                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, subEmail)  //이메일
                                                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)     //이메일타입(Type_Work : 직장)

                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, subMemo)   //노트

                                                        .build()
                                        );
                                        Log.d("jihoonDebug", "List add Finished");

                                        getApplicationContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, list);  //주소록추가

                                        Log.d("jihoonDebug", "add success");

                                        list.clear();   //리스트 초기화
                                    } catch (Exception e) {
                                        Log.d("jihoonDebug", e.getMessage());
                                    }
                                }
                            }.start();

                            nameTextView = (EditText)findViewById(R.id.editText1);
                            phoneNumberTextView = (EditText)findViewById(R.id.editText2);
                            emailTextView = (EditText)findViewById(R.id.editText3);
                            memoTextView = (EditText)findViewById(R.id.editText4);
                            nameTextView.setText("");
                            phoneNumberTextView.setText("");
                            emailTextView.setText("");
                            memoTextView.setText("");
                            returnFlag = true;

                            db.execSQL("INSERT INTO callState VALUES ('" + subName + "', '" + subPhoneNum + "', '" + subEmail + "', '" + subMemo + "');");
                            Toast.makeText(getApplicationContext(),"등록완료",Toast.LENGTH_LONG).show();
                        }
                    });
            builder.setPositiveButton("취소",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            returnFlag = false;
                            Toast.makeText(getApplicationContext(),"취소완료",Toast.LENGTH_LONG).show();
                        }
                    });
            builder.show();
        }
        else
        {
            new Thread() {
                @Override
                public void run() {
                    Log.d("jihoonDebug", "Thread In");

                    ArrayList<ContentProviderOperation> list = new ArrayList<>();
                    try {
                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                        .build()
                        );

                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, subName)   //이름

                                        .build()
                        );

                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, subPhoneNum)           //전화번호
                                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)   //번호타입(Type_Mobile : 모바일)

                                        .build()
                        );
                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, subEmail)  //이메일
                                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)     //이메일타입(Type_Work : 직장)

                                        .build()
                        );
                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, subMemo)   //노트

                                        .build()
                        );
                        Log.d("jihoonDebug", "List add Finished");

                        getApplicationContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, list);  //주소록추가

                        Log.d("jihoonDebug", "add success");

                        list.clear();   //리스트 초기화
                        db.execSQL("INSERT INTO callState VALUES ('" + subName + "', '" + subPhoneNum + "', '" + subEmail + "', '" + subMemo + "');");
                    } catch (Exception e) {
                        Log.d("jihoonDebug", e.getMessage());
                    }
                }
            }.start();

            nameTextView = (EditText)findViewById(R.id.editText1);
            phoneNumberTextView = (EditText)findViewById(R.id.editText2);
            emailTextView = (EditText)findViewById(R.id.editText3);
            memoTextView = (EditText)findViewById(R.id.editText4);
            nameTextView.setText("");
            phoneNumberTextView.setText("");
            emailTextView.setText("");
            memoTextView.setText("");
            returnFlag = true;
        }
//000000000000000000000


    }
    public void insert(String n, String num, String em, String me)
    {
        String name = n;
        String phoneNumber = num;
        String email = em;
        String memo = me;

        phoneNumber = replaceFunc(phoneNumber);

        db.execSQL("INSERT INTO callState VALUES ('" + name + "', '" + phoneNumber + "', '" + email + "', '" + memo + "');");
    }

    public void delete()
    {
        String phoneNumber = phoneNumberTextView.getText().toString();
        db.execSQL("DELETE FROM callState WHERE phonenum = '" + phoneNumber + "';");
        Toast.makeText(getApplicationContext(), "Delete Success!", Toast.LENGTH_LONG).show();
    }

    public boolean isExists(String n, String phoneNum)
    {
        String phone_num = new String();
        String name = new String();

        String [] arrProjection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };

        String [] arrPhoneProjection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        Cursor clsCursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, arrProjection,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1",
                null, null
        );
        while (clsCursor.moveToNext()) {

            String strContactId = clsCursor.getString(0);
            name = clsCursor.getString(1);

            Cursor clsPhoneCursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrPhoneProjection,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId,
                    null, null
            );
            while (clsPhoneCursor.moveToNext()) {
                phone_num = clsPhoneCursor.getString(0);        //전화번호 저장.
                phone_num = replaceFunc(phone_num);
            }
            clsPhoneCursor.close();
        }

        phone_num = replaceFunc(phone_num);
        phoneNum = replaceFunc(phoneNum);
        if(n.equals(name) && phoneNum.equals(phone_num)) return true;
        else return false;
    }

    public void contacts()
    {
        String [] arrProjection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };

        String [] arrPhoneProjection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        String [] arrEmailProjection = {
                ContactsContract.CommonDataKinds.Email.DATA
        };


        Cursor clsCursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, arrProjection,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1",
                null, null
        );
        while (clsCursor.moveToNext())
        {
            String memo = new String();
            String phone_num = new String();
            String name = new String();
            String email = new String();
            String strContactId = clsCursor.getString(0);
            name = clsCursor.getString(1);

            Cursor clsPhoneCursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrPhoneProjection,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId,
                    null, null
            );
            while (clsPhoneCursor.moveToNext())
            {
                phone_num = clsPhoneCursor.getString(0);        //전화번호 저장.
                phone_num = replaceFunc(phone_num);
            }
            clsPhoneCursor.close();


            Cursor clsEmailCursor = getContentResolver().query (
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrEmailProjection,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId,
                    null, null);
            while( clsEmailCursor.moveToNext() )
            {
                email = clsEmailCursor.getString(0);
                //Log.i("Phone", "이메일 : " + email);
            }
            clsEmailCursor.close();


            String noteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] noteWhereParams = new String[]{
                    strContactId,
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
            };
            Cursor clsMemoCursor = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    noteWhere,
                    noteWhereParams, null
            );
            while (clsMemoCursor.moveToNext()) {
                memo = clsMemoCursor.getString(clsMemoCursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));       //노트내용
            }
            clsMemoCursor.close();
            insert(name, phone_num, email, memo);
            System.out.println("name : " + name + " phone num : " + phone_num + " email : " + email + " memo : " + memo);

        }
        clsCursor.close();
    }


    public boolean returnFlag = false;
    //연락처 추가
    public boolean ContactAdd(final String name, final String phoneNum, final String email, final String memo) {
        if(isExists(name, phoneNum))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("정보가 중복됩니다");
            //타이틀설정
            String tv_text = "이미 같은 이름/전화번호 정보가 있습니다.\n등록하시면 현재 정보가 출력됩니다."
                    + "\n이름 : " + name + " / 전화번호 : " + phoneNum + "\nEmail : " + email + "\n메모 : " + memo
                    + "\n\n(기존데이터는 유지됨)";
            builder.setMessage(tv_text);
            //내용설정
            builder.setNegativeButton("등록",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            returnFlag = true;

                            new Thread() {
                                @Override
                                public void run() {
                                    Log.d("jihoonDebug", "Thread In");

                                    ArrayList<ContentProviderOperation> list = new ArrayList<>();
                                    try {
                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)   //이름

                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNum)           //전화번호
                                                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)   //번호타입(Type_Mobile : 모바일)

                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)  //이메일
                                                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)     //이메일타입(Type_Work : 직장)

                                                        .build()
                                        );

                                        list.add(
                                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, memo)   //노트

                                                        .build()
                                        );
                                        Log.d("jihoonDebug", "List add Finished");

                                        getApplicationContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, list);  //주소록추가

                                        Log.d("jihoonDebug", "add success");

                                        list.clear();   //리스트 초기화
                                    } catch (Exception e) {
                                        Log.d("jihoonDebug", e.getMessage());
                                    }
                                }
                            }.start();

                            nameTextView = (EditText)findViewById(R.id.editText1);
                            phoneNumberTextView = (EditText)findViewById(R.id.editText2);
                            emailTextView = (EditText)findViewById(R.id.editText3);
                            memoTextView = (EditText)findViewById(R.id.editText4);
                            nameTextView.setText("");
                            phoneNumberTextView.setText("");
                            emailTextView.setText("");
                            memoTextView.setText("");
                            returnFlag = true;

                            db.execSQL("INSERT INTO callState VALUES ('" + name + "', '" + phoneNum + "', '" + email + "', '" + memo + "');");
                            Toast.makeText(getApplicationContext(),"등록완료",Toast.LENGTH_LONG).show();
                        }
                    });
            builder.setPositiveButton("취소",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            returnFlag = false;
                            Toast.makeText(getApplicationContext(),"취소완료",Toast.LENGTH_LONG).show();
                        }
                    });
            builder.show();
        }
        else
        {
            new Thread() {
                @Override
                public void run() {
                    Log.d("jihoonDebug", "Thread In");

                    ArrayList<ContentProviderOperation> list = new ArrayList<>();
                    try {
                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                        .build()
                        );

                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)   //이름

                                        .build()
                        );

                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNum)           //전화번호
                                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)   //번호타입(Type_Mobile : 모바일)

                                        .build()
                        );
                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)  //이메일
                                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)     //이메일타입(Type_Work : 직장)

                                        .build()
                        );
                        list.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, memo)   //노트

                                        .build()
                        );
                        Log.d("jihoonDebug", "List add Finished");

                        getApplicationContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, list);  //주소록추가

                        Log.d("jihoonDebug", "add success");

                        list.clear();   //리스트 초기화
                    } catch (Exception e) {
                        Log.d("jihoonDebug", e.getMessage());
                    }
                }
            }.start();

            nameTextView = (EditText)findViewById(R.id.editText1);
            phoneNumberTextView = (EditText)findViewById(R.id.editText2);
            emailTextView = (EditText)findViewById(R.id.editText3);
            memoTextView = (EditText)findViewById(R.id.editText4);
            nameTextView.setText("");
            phoneNumberTextView.setText("");
            emailTextView.setText("");
            memoTextView.setText("");
            returnFlag = true;
        }
        return returnFlag;
    }
}


